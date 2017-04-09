/*
 * The MIT License (MIT)
 *
 * Copyright 2017 Alexander Orlov <alexander.orlov@loxal.net>. All rights reserved.
 * Copyright (c) [2016] [ <ether.camp> ]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package org.ethereum.net.rlpx.discover;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.config.SystemProperties;
import org.ethereum.net.client.PeerClient;
import org.ethereum.net.rlpx.Node;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Makes test RLPx connection to the peers to acquire statistics
 *
 * Created by Anton Nashatyrev on 17.07.2015.
 */
@Component
class PeerConnectionTester {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    private final long ReconnectPeriod;
    private final long ReconnectMaxPeers;
    // NodeHandler instance should be unique per Node instance
    private final Map<NodeHandler, ?> connectedCandidates = Collections.synchronizedMap(new IdentityHashMap());
    // executor with Queue which picks up the Node with the best reputation
    private final ExecutorService peerConnectionPool;
    private final Timer reconnectTimer = new Timer("DiscoveryReconnectTimer");
    @Autowired
    private PeerClient peerClient;
    private int reconnectPeersCount = 0;

    @Autowired
    public PeerConnectionTester(final SystemProperties config) {
        final SystemProperties config1 = config;
        final int connectThreads = config.peerDiscoveryWorkers();
        ReconnectPeriod = config.peerDiscoveryTouchPeriod() * 1000;
        ReconnectMaxPeers = config.peerDiscoveryTouchMaxNodes();
        peerConnectionPool = new ThreadPoolExecutor(connectThreads,
                connectThreads, 0L, TimeUnit.SECONDS,
                new MutablePriorityQueue<>((Comparator<ConnectTask>) (h1, h2) -> h2.nodeHandler.getNodeStatistics().getReputation() -
                        h1.nodeHandler.getNodeStatistics().getReputation()), new ThreadFactoryBuilder().setDaemon(true).setNameFormat("discovery-tester-%d").build());
    }

    public void close() {
        logger.info("Closing PeerConnectionTester...");
        try {
            peerConnectionPool.shutdownNow();
        } catch (final Exception e) {
            logger.warn("Problems closing PeerConnectionTester", e);
        }
        try {
            reconnectTimer.cancel();
        } catch (final Exception e) {
            logger.warn("Problems cancelling reconnectTimer", e);
        }
    }

    public void nodeStatusChanged(final NodeHandler nodeHandler) {
        if (peerConnectionPool.isShutdown()) return;
        if (connectedCandidates.size() < NodeManager.MAX_NODES
                && !connectedCandidates.containsKey(nodeHandler)
                && !nodeHandler.getNode().isDiscoveryNode()) {
            logger.debug("Submitting node for RLPx connection : " + nodeHandler);
            connectedCandidates.put(nodeHandler, null);
            peerConnectionPool.execute(new ConnectTask(nodeHandler));
        }
    }

    /**
     * The same as PriorityBlockQueue but with assumption that elements are mutable
     * and priority changes after enqueueing, thus the list is sorted by priority
     * each time the head queue element is requested.
     * The class has poor synchronization since the prioritization might be approximate
     * though the implementation should be inheritedly thread-safe
     */
    public static class MutablePriorityQueue<T, C extends T> extends LinkedBlockingQueue<T> {
        final Comparator<C> comparator;

        public MutablePriorityQueue(final Comparator<C> comparator) {
            this.comparator = comparator;
        }

        @Override
        public synchronized T take() throws InterruptedException {
            if (isEmpty()) {
                return super.take();
            } else {
                final T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T poll(final long timeout, final TimeUnit unit) throws InterruptedException {
            if (isEmpty()) {
                return super.poll(timeout, unit);
            } else {
                final T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T poll() {
            if (isEmpty()) {
                return super.poll();
            } else {
                final T ret = Collections.min(this, (Comparator<? super T>) comparator);
                remove(ret);
                return ret;
            }
        }

        @Override
        public synchronized T peek() {
            if (isEmpty()) {
                return super.peek();
            } else {
                final T ret = Collections.min(this, (Comparator<? super T>) comparator);
                return ret;
            }
        }
    }

    private class ConnectTask implements Runnable {
        final NodeHandler nodeHandler;

        public ConnectTask(final NodeHandler nodeHandler) {
            this.nodeHandler = nodeHandler;
        }

        @Override
        public void run() {
            try {
                if (nodeHandler != null) {
                    nodeHandler.getNodeStatistics().rlpxConnectionAttempts.add();
                    logger.debug("Trying node connection: " + nodeHandler);
                    final Node node = nodeHandler.getNode();
                    peerClient.connect(node.getHost(), node.getPort(),
                            Hex.encodeHexString(node.getId()), true);
                    logger.debug("Terminated node connection: " + nodeHandler);
                    nodeHandler.getNodeStatistics().disconnected();
                    if (!nodeHandler.getNodeStatistics().getEthTotalDifficulty().equals(BigInteger.ZERO) &&
                            ReconnectPeriod > 0 && (reconnectPeersCount < ReconnectMaxPeers || ReconnectMaxPeers == -1)) {
                        // trying to keep good peers information up-to-date
                        reconnectPeersCount++;
                        reconnectTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                logger.debug("Trying the node again: " + nodeHandler);
                                peerConnectionPool.execute(new ConnectTask(nodeHandler));
                                reconnectPeersCount--;
                            }
                        }, ReconnectPeriod);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                connectedCandidates.remove(nodeHandler);
            }
        }
    }

}
