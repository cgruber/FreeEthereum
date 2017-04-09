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

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.PeerSource;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.rlpx.*;
import org.ethereum.net.rlpx.discover.table.NodeTable;
import org.ethereum.util.CollectionUtils;
import org.ethereum.util.Functional;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The central class for Peer Discovery machinery.
 *
 * The NodeManager manages info on all the Nodes discovered by the peer discovery
 * protocol, routes protocol messages to the corresponding NodeHandlers and
 * supplies the info about discovered Nodes and their usage statistics
 *
 * Created by Anton Nashatyrev on 16.07.2015.
 */
@Component
public class NodeManager implements Functional.Consumer<DiscoveryEvent>{
    static final int MAX_NODES = 2000;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");
    private static final int NODES_TRIM_THRESHOLD = 3000;
    private static final long LISTENER_REFRESH_RATE = 1000;
    private static final long DB_COMMIT_RATE = 1 * 60 * 1000;
    final ECKey key;
    final Node homeNode;
    final NodeTable table;
    private final boolean PERSIST;
    private final PeerConnectionTester peerConnectionManager;
    private final EthereumListener ethereumListener;
    // option to handle inbounds only from known peers (i.e. which were discovered by ourselves)
    private final boolean inboundOnlyFromKnownNodes = false;
    private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    private final boolean discoveryEnabled;
    private final Map<DiscoverListener, ListenerHandler> listeners = new IdentityHashMap<>();
    private final Timer logStatsTimer = new Timer();
    private final Timer nodeManagerTasksTimer = new Timer("NodeManagerTasks");
    private final ScheduledExecutorService pongTimer;
    private PeerSource peerSource;
    private SystemProperties config = SystemProperties.getDefault();
    private Functional.Consumer<DiscoveryEvent> messageSender;
    private List<Node> bootNodes;
    private boolean inited = false;

    @Autowired
    public NodeManager(final SystemProperties config, final EthereumListener ethereumListener,
                       final ApplicationContext ctx, final PeerConnectionTester peerConnectionManager) {
        this.config = config;
        this.ethereumListener = ethereumListener;
        this.peerConnectionManager = peerConnectionManager;

        PERSIST = config.peerDiscoveryPersist();
        if (PERSIST) peerSource = ctx.getBean(PeerSource.class);
        discoveryEnabled = config.peerDiscovery();

        key = config.getMyKey();
        homeNode = new Node(config.nodeId(), config.externalIp(), config.listenPort());
        table = new NodeTable(homeNode, config.isPublicHomeNode());

        logStatsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                logger.trace("Statistics:\n {}", dumpAllStatistics());
            }
        }, 1 * 1000, 60 * 1000);

        this.pongTimer = Executors.newSingleThreadScheduledExecutor();
        for (final Node node : config.peerActive()) {
            getNodeHandler(node).getNodeStatistics().setPredefined(true);
        }
    }

    public ScheduledExecutorService getPongTimer() {
        return pongTimer;
    }

    void setBootNodes(final List<Node> bootNodes) {
        this.bootNodes = bootNodes;
    }

    void channelActivated() {
        // channel activated now can send messages
        if (!inited) {
            // no another init on a new channel activation
            inited = true;

            // this task is done asynchronously with some fixed rate
            // to avoid any overhead in the NodeStatistics classes keeping them lightweight
            // (which might be critical since they might be invoked from time critical sections)
            nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    processListeners();
                }
            }, LISTENER_REFRESH_RATE, LISTENER_REFRESH_RATE);

            if (PERSIST) {
                dbRead();
                nodeManagerTasksTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        dbWrite();
                    }
                }, DB_COMMIT_RATE, DB_COMMIT_RATE);
            }

            for (final Node node : bootNodes) {
                getNodeHandler(node);
            }
        }
    }

    private void dbRead() {
        logger.info("Reading Node statistics from DB: " + peerSource.getNodes().size() + " nodes.");
        for (final Pair<Node, Integer> nodeElement : peerSource.getNodes()) {
            getNodeHandler(nodeElement.getLeft()).getNodeStatistics().setPersistedReputation(nodeElement.getRight());
        }
    }

    private void dbWrite() {
        final List<Pair<Node, Integer>> batch = new ArrayList<>();
        synchronized (this) {
            for (final NodeHandler handler : nodeHandlerMap.values()) {
                batch.add(Pair.of(handler.getNode(), handler.getNodeStatistics().getPersistedReputation()));
            }
        }
        peerSource.clear();
        for (final Pair<Node, Integer> nodeElement : batch) {
            peerSource.getNodes().add(nodeElement);
        }
        peerSource.getNodes().flush();
        logger.info("Write Node statistics to DB: " + peerSource.getNodes().size() + " nodes.");
    }

    public void setMessageSender(final Functional.Consumer<DiscoveryEvent> messageSender) {
        this.messageSender = messageSender;
    }

    private String getKey(final Node n) {
        return getKey(new InetSocketAddress(n.getHost(), n.getPort()));
    }

    private String getKey(final InetSocketAddress address) {
        final InetAddress addr = address.getAddress();
        // addr == null if the hostname can't be resolved
        return (addr == null ? address.getHostString() : addr.getHostAddress()) + ":" + address.getPort();
    }

    public synchronized NodeHandler getNodeHandler(final Node n) {
        final String key = getKey(n);
        NodeHandler ret = nodeHandlerMap.get(key);
        if (ret == null) {
            trimTable();
            ret = new NodeHandler(n ,this);
            nodeHandlerMap.put(key, ret);
            logger.debug(" +++ New node: " + ret + " " + n);
            if (!n.isDiscoveryNode() && !n.getHexId().equals(homeNode.getHexId())) {
                ethereumListener.onNodeDiscovered(ret.getNode());
            }
        } else if (ret.getNode().isDiscoveryNode() && !n.isDiscoveryNode()) {
            // we found discovery node with same host:port,
            // replace node with correct nodeId
            ret.node = n;
            if (!n.getHexId().equals(homeNode.getHexId())) {
                ethereumListener.onNodeDiscovered(ret.getNode());
            }
            logger.debug(" +++ Found real nodeId for discovery endpoint {}", n);
        }

        return ret;
    }

    private void trimTable() {
        if (nodeHandlerMap.size() > NODES_TRIM_THRESHOLD) {

            final List<NodeHandler> sorted = new ArrayList<>(nodeHandlerMap.values());
            // reverse sort by reputation
            sorted.sort((o1, o2) -> o1.getNodeStatistics().getReputation() - o2.getNodeStatistics().getReputation());

            for (final NodeHandler handler : sorted) {
                nodeHandlerMap.remove(getKey(handler.getNode()));
                if (nodeHandlerMap.size() <= MAX_NODES) break;
            }
        }
    }


    private boolean hasNodeHandler(final Node n) {
        return nodeHandlerMap.containsKey(getKey(n));
    }

    public NodeTable getTable() {
        return table;
    }

    public NodeStatistics getNodeStatistics(final Node n) {
        return getNodeHandler(n).getNodeStatistics();
    }

    @Override
    public void accept(final DiscoveryEvent discoveryEvent) {
        handleInbound(discoveryEvent);
    }

    public void handleInbound(final DiscoveryEvent discoveryEvent) {
        final Message m = discoveryEvent.getMessage();
        final InetSocketAddress sender = discoveryEvent.getAddress();

        final Node n = new Node(m.getNodeId(), sender.getHostString(), sender.getPort());

        if (inboundOnlyFromKnownNodes && !hasNodeHandler(n)) {
            logger.debug("=/=> (" + sender + "): inbound packet from unknown peer rejected due to config option.");
            return;
        }
        final NodeHandler nodeHandler = getNodeHandler(n);

        logger.trace("===> ({}) {} [{}] {}", sender, m.getClass().getSimpleName(), nodeHandler, m);

        final byte type = m.getType()[0];
        switch (type) {
            case 1:
                nodeHandler.handlePing((PingMessage) m);
                break;
            case 2:
                nodeHandler.handlePong((PongMessage) m);
                break;
            case 3:
                nodeHandler.handleFindNode((FindNodeMessage) m);
                break;
            case 4:
                nodeHandler.handleNeighbours((NeighborsMessage) m);
                break;
        }
    }

    public void sendOutbound(final DiscoveryEvent discoveryEvent) {
        if (discoveryEnabled && messageSender != null) {
            logger.trace(" <===({}) {} [{}] {}", discoveryEvent.getAddress(),
                    discoveryEvent.getMessage().getClass().getSimpleName(), this, discoveryEvent.getMessage());
            messageSender.accept(discoveryEvent);
        }
    }

    public void stateChanged(final NodeHandler nodeHandler, final NodeHandler.State oldState, final NodeHandler.State newState) {
        if (discoveryEnabled && peerConnectionManager != null) {  // peerConnectionManager can be null if component not inited yet
            peerConnectionManager.nodeStatusChanged(nodeHandler);
        }
    }

    public synchronized List<NodeHandler> getNodes(final int minReputation) {
        final List<NodeHandler> ret = new ArrayList<>();
        for (final NodeHandler nodeHandler : nodeHandlerMap.values()) {
            if (nodeHandler.getNodeStatistics().getReputation() >= minReputation) {
                ret.add(nodeHandler);
            }
        }
        return ret;
    }

    /**
     * Returns limited list of nodes matching {@code predicate} criteria<br>
     * The nodes are sorted then by their totalDifficulties
     *
     * @param predicate only those nodes which are satisfied to its condition are included in results
     * @param limit max size of returning list
     *
     * @return list of nodes matching criteria
     */
    public List<NodeHandler> getNodes(
            final Functional.Predicate<NodeHandler> predicate,
            final int limit) {
        final ArrayList<NodeHandler> filtered = new ArrayList<>();
        synchronized (this) {
            for (final NodeHandler handler : nodeHandlerMap.values()) {
                if (predicate.test(handler)) {
                    filtered.add(handler);
                }
            }
        }
        filtered.sort((o1, o2) -> o2.getNodeStatistics().getEthTotalDifficulty().compareTo(
                o1.getNodeStatistics().getEthTotalDifficulty()));
        return CollectionUtils.truncate(filtered, limit);
    }

    private synchronized void processListeners() {
        for (final ListenerHandler handler : listeners.values()) {
            try {
                handler.checkAll();
            } catch (final Exception e) {
                logger.error("Exception processing listener: " + handler, e);
            }
        }
    }

    /**
     * Add a listener which is notified when the node statistics starts or stops meeting
     * the criteria specified by [filter] param.
     */
    public synchronized void addDiscoverListener(final DiscoverListener listener, final Functional.Predicate<NodeStatistics> filter) {
        listeners.put(listener, new ListenerHandler(listener, filter));
    }

    public synchronized void removeDiscoverListener(final DiscoverListener listener) {
        listeners.remove(listener);
    }

    private synchronized String dumpAllStatistics() {
        final List<NodeHandler> l = new ArrayList<>(nodeHandlerMap.values());
        l.sort((o1, o2) -> -(o1.getNodeStatistics().getReputation() - o2.getNodeStatistics().getReputation()));

        final StringBuilder sb = new StringBuilder();
        int zeroReputCount = 0;
        for (final NodeHandler nodeHandler : l) {
            if (nodeHandler.getNodeStatistics().getReputation() > 0) {
                sb.append(nodeHandler).append("\t").append(nodeHandler.getNodeStatistics()).append("\n");
            } else {
                zeroReputCount++;
        }
        }
        sb.append("0 reputation: ").append(zeroReputCount).append(" nodes.\n");
        return sb.toString();
    }

    /**
     * @return home node if config defines it as public, otherwise null
     */
    Node getPublicHomeNode() {
        if (config.isPublicHomeNode()) {
            return homeNode;
        }
        return null;
    }

    public void close() {
        peerConnectionManager.close();
        try {
            nodeManagerTasksTimer.cancel();
            if (PERSIST) {
                try {
                    dbWrite();
                } catch (final Throwable e) {     // IllegalAccessError is expected
                    // NOTE: logback stops context right after shutdown initiated. It is problematic to see log output
                    // System out could help
                    logger.warn("Problem during NodeManager persist in close: " + e.getMessage());
                }
            }
        } catch (final Exception e) {
            logger.warn("Problems canceling nodeManagerTasksTimer", e);
        }
        try {
            logger.info("Cancelling pongTimer");
            pongTimer.shutdownNow();
        } catch (final Exception e) {
            logger.warn("Problems cancelling pongTimer", e);
        }
        try {
            logStatsTimer.cancel();
        } catch (final Exception e) {
            logger.warn("Problems canceling logStatsTimer", e);
        }
    }

    private class ListenerHandler {
        final Map<NodeHandler, Object> discoveredNodes = new IdentityHashMap<>();
        final DiscoverListener listener;
        final Functional.Predicate<NodeStatistics> filter;

        ListenerHandler(final DiscoverListener listener, final Functional.Predicate<NodeStatistics> filter) {
            this.listener = listener;
            this.filter = filter;
        }

        void checkAll() {
            for (final NodeHandler handler : nodeHandlerMap.values()) {
                final boolean has = discoveredNodes.containsKey(handler);
                final boolean test = filter.test(handler.getNodeStatistics());
                if (!has && test) {
                    listener.nodeAppeared(handler);
                    discoveredNodes.put(handler, null);
                } else if (has && !test) {
                    listener.nodeDisappeared(handler);
                    discoveredNodes.remove(handler);
                }
            }
        }
    }
}
