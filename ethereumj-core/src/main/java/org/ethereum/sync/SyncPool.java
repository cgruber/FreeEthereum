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

package org.ethereum.sync;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.Blockchain;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.rlpx.discover.NodeHandler;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.server.Channel;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.util.Functional;
import org.ethereum.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;
import static org.ethereum.util.BIUtil.isIn20PercentRange;

/**
 * <p>Encapsulates logic which manages peers involved in blockchain sync</p>
 *
 * Holds connections, bans, disconnects and other peers logic<br>
 * The pool is completely threadsafe<br>
 * Implements {@link Iterable} and can be used in "foreach" loop<br>
 * Used by {@link SyncManager}
 *
 * @author Mikhail Kalinin
 * @since 10.08.2015
 */
@Component
public class SyncPool {

    private static final Logger logger = LoggerFactory.getLogger("sync");

    private static final long WORKER_TIMEOUT = 3; // 3 seconds

    private final List<Channel> activePeers = Collections.synchronizedList(new ArrayList<Channel>());
    private final Blockchain blockchain;
    private final SystemProperties config;
    private final ScheduledExecutorService poolLoopExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();
    private BigInteger lowerUsefulDifficulty = BigInteger.ZERO;
    @Autowired
    private EthereumListener ethereumListener;
    @Autowired
    private NodeManager nodeManager;
    private ChannelManager channelManager;
    private Functional.Predicate<NodeHandler> nodesSelector;

    @Autowired
    public SyncPool(final SystemProperties config, final Blockchain blockchain) {
        this.config = config;
        this.blockchain = blockchain;
    }

    public void init(final ChannelManager channelManager) {
        if (this.channelManager != null) return; // inited already
        this.channelManager = channelManager;
        updateLowerUsefulDifficulty();

        poolLoopExecutor.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            heartBeat();
                            updateLowerUsefulDifficulty();
                            fillUp();
                            prepareActive();
                            cleanupActive();
                        } catch (final Throwable t) {
                            logger.error("Unhandled exception", t);
                        }
                    }
                }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.SECONDS
        );
        logExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    logActivePeers();
                    logger.info("\n");
                } catch (final Throwable t) {
                    t.printStackTrace();
                    logger.error("Exception in log worker", t);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public void setNodesSelector(final Functional.Predicate<NodeHandler> nodesSelector) {
        this.nodesSelector = nodesSelector;
    }

    public void close() {
        try {
            poolLoopExecutor.shutdownNow();
            logExecutor.shutdownNow();
        } catch (final Exception e) {
            logger.warn("Problems shutting down executor", e);
        }
    }

    @Nullable
    public synchronized Channel getAnyIdle() {
        final ArrayList<Channel> channels = new ArrayList<>(activePeers);
        Collections.shuffle(channels);
        for (final Channel peer : channels) {
            if (peer.isIdle())
                return peer;
        }

        return null;
    }

    @Nullable
    public synchronized Channel getBestIdle() {
        for (final Channel peer : activePeers) {
            if (peer.isIdle())
                return peer;
        }
        return null;
    }

    @Nullable
    public synchronized Channel getNotLastIdle() {
        final ArrayList<Channel> channels = new ArrayList<>(activePeers);
        Collections.shuffle(channels);
        Channel candidate = null;
        for (final Channel peer : channels) {
            if (peer.isIdle()) {
                if (candidate == null) {
                    candidate = peer;
                } else {
                    return candidate;
                }
            }
        }

        return null;
    }

    public synchronized List<Channel> getAllIdle() {
        final List<Channel> ret = new ArrayList<>();
        for (final Channel peer : activePeers) {
            if (peer.isIdle())
                ret.add(peer);
        }
        return ret;
    }

    public synchronized List<Channel> getActivePeers() {
        return new ArrayList<>(activePeers);
    }

    public synchronized int getActivePeersCount() {
        return activePeers.size();
    }

    @Nullable
    public synchronized Channel getByNodeId(final byte[] nodeId) {
        return channelManager.getActivePeer(nodeId);
    }

    public synchronized void onDisconnect(final Channel peer) {
        if (activePeers.remove(peer)) {
            logger.info("Peer {}: disconnected", peer.getPeerIdShort());
        }
    }

    private synchronized Set<String> nodesInUse() {
        final Set<String> ids = new HashSet<>();
        for (final Channel peer : channelManager.getActivePeers()) {
            ids.add(peer.getPeerId());
        }
        return ids;
    }

    private synchronized void logActivePeers() {
        if (logger.isInfoEnabled()) {
            final StringBuilder sb = new StringBuilder("Peer stats:\n");
            sb.append("Active peers\n");
            sb.append("============\n");
            final Set<Node> activeSet = new HashSet<>();
            for (final Channel peer : new ArrayList<>(activePeers)) {
                sb.append(peer.logSyncStats()).append('\n');
                activeSet.add(peer.getNode());
            }
            sb.append("Other connected peers\n");
            sb.append("============\n");
            for (final Channel peer : new ArrayList<>(channelManager.getActivePeers())) {
                if (!activeSet.contains(peer.getNode())) {
                    sb.append(peer.logSyncStats()).append('\n');
                }
            }
            logger.info(sb.toString());
        }
    }

    private void fillUp() {
        final int lackSize = config.maxActivePeers() - channelManager.getActivePeers().size();
        if(lackSize <= 0) return;

        final Set<String> nodesInUse = nodesInUse();
        nodesInUse.add(Hex.toHexString(config.nodeId()));   // exclude home node

        List<NodeHandler> newNodes;
        newNodes = nodeManager.getNodes(new NodeSelector(lowerUsefulDifficulty, nodesInUse), lackSize);
        if (lackSize > 0 && newNodes.isEmpty()) {
            newNodes = nodeManager.getNodes(new NodeSelector(BigInteger.ZERO, nodesInUse), lackSize);
        }

        if (logger.isTraceEnabled()) {
            logDiscoveredNodes(newNodes);
        }

        for (final NodeHandler n : newNodes) {
            channelManager.connect(n.getNode());
        }
    }

    private synchronized void prepareActive() {
        final List<Channel> managerActive = new ArrayList<>(channelManager.getActivePeers());

        // Filtering out with nodeSelector because server-connected nodes were not tested
        final NodeSelector nodeSelector = new NodeSelector(BigInteger.ZERO);
        final List<Channel> active = new ArrayList<>();
        for (final Channel channel : managerActive) {
            if (nodeSelector.test(nodeManager.getNodeHandler(channel.getNode()))) {
                active.add(channel);
            }
        }

        if (active.isEmpty()) return;

        // filtering by 20% from top difficulty
        active.sort(new Comparator<Channel>() {
            @Override
            public int compare(final Channel c1, final Channel c2) {
                return c2.getTotalDifficulty().compareTo(c1.getTotalDifficulty());
            }
        });

        final BigInteger highestDifficulty = active.get(0).getTotalDifficulty();
        int thresholdIdx = min(config.syncPeerCount(), active.size()) - 1;

        for (int i = thresholdIdx; i >= 0; i--) {
            if (isIn20PercentRange(active.get(i).getTotalDifficulty(), highestDifficulty)) {
                thresholdIdx = i;
                break;
            }
        }

        final List<Channel> filtered = active.subList(0, thresholdIdx + 1);

        // sorting by latency in asc order
        filtered.sort(new Comparator<Channel>() {
            @Override
            public int compare(final Channel c1, final Channel c2) {
                return Double.valueOf(c1.getPeerStats().getAvgLatency()).compareTo(c2.getPeerStats().getAvgLatency());
            }
        });

        for (final Channel channel : filtered) {
            if (!activePeers.contains(channel)) {
                ethereumListener.onPeerAddedToSyncPool(channel);
            }
        }

        activePeers.clear();
        activePeers.addAll(filtered);
    }

    private synchronized void cleanupActive() {
        final Iterator<Channel> iterator = activePeers.iterator();
        while (iterator.hasNext()) {
            final Channel next = iterator.next();
            if (next.isDisconnected()) {
                logger.info("Removing peer " + next + " from active due to disconnect.");
                iterator.remove();
            }
        }
    }

    private void logDiscoveredNodes(final List<NodeHandler> nodes) {
        final StringBuilder sb = new StringBuilder();
        for (final NodeHandler n : nodes) {
            sb.append(Utils.getNodeIdShort(Hex.toHexString(n.getNode().getId())));
            sb.append(", ");
        }
        if(sb.length() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        logger.trace(
                "Node list obtained from discovery: {}",
                nodes.size() > 0 ? sb.toString() : "empty"
        );
    }

    private void updateLowerUsefulDifficulty() {
        final BigInteger td = blockchain.getTotalDifficulty();
        if (td.compareTo(lowerUsefulDifficulty) > 0) {
            lowerUsefulDifficulty = td;
        }
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    private void heartBeat() {
//        for (Channel peer : channelManager.getActivePeers()) {
//            if (!peer.isIdle() && peer.getSyncStats().secondsSinceLastUpdate() > config.peerChannelReadTimeout()) {
//                logger.info("Peer {}: no response after {} seconds", peer.getPeerIdShort(), config.peerChannelReadTimeout());
//                peer.dropConnection();
//            }
//        }
    }

    class NodeSelector implements Functional.Predicate<NodeHandler> {
        final BigInteger lowerDifficulty;
        Set<String> nodesInUse;

        public NodeSelector(final BigInteger lowerDifficulty) {
            this.lowerDifficulty = lowerDifficulty;
        }

        public NodeSelector(final BigInteger lowerDifficulty, final Set<String> nodesInUse) {
            this.lowerDifficulty = lowerDifficulty;
            this.nodesInUse = nodesInUse;
        }

        @Override
        public boolean test(final NodeHandler handler) {
            if (nodesInUse != null && nodesInUse.contains(handler.getNode().getHexId())) {
                return false;
            }

            if (handler.getNodeStatistics().isPredefined()) return true;

            if (nodesSelector != null && !nodesSelector.test(handler)) return false;

            if (lowerDifficulty.compareTo(BigInteger.ZERO) > 0 &&
                    handler.getNodeStatistics().getEthTotalDifficulty() == null) {
                return false;
            }

            if (handler.getNodeStatistics().getReputation() < 100) return false;

            return handler.getNodeStatistics().getEthTotalDifficulty().compareTo(lowerDifficulty) >= 0;
        }
    }
}
