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

package org.ethereum.samples;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;
import org.ethereum.util.ByteUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 *  The base sample class which creates EthereumJ instance, tracks and report all the stages
 *  of starting up like discovering nodes, connecting, syncing
 *
 *  The class can be started as a standalone sample it should just run until full blockchain
 *  sync and then just hanging, listening for new blocks and importing them into a DB
 *
 *  This class is a Spring Component which makes it convenient to easily get access (autowire) to
 *  all components created within EthereumJ. However almost all this could be done without dealing
 *  with the Spring machinery from within a simple main method
 *
 *  Created by Anton Nashatyrev on 05.02.2016.
 */
public class BasicSample implements Runnable {

    public static final Logger sLogger = LoggerFactory.getLogger("sample");
    private static CustomFilter CUSTOM_FILTER;

    private final String loggerName;
    private final List<Node> nodesDiscovered = new Vector<>();
    private final Map<Node, StatusMessage> ethNodes = new Hashtable<>();
    private final List<Node> syncPeers = new Vector<>();
    Logger logger;
    @Autowired
    Ethereum ethereum;
    @Autowired
    SystemProperties config;
    private volatile long txCount;
    private volatile long gasSpent;
    private Block bestBlock = null;
    private boolean synced = false;
    private boolean syncComplete = false;
    /**
     * The main EthereumJ callback.
     */
    private final EthereumListener listener = new EthereumListenerAdapter() {
        @Override
        public void onSyncDone(final SyncState state) {
            synced = true;
        }

        @Override
        public void onNodeDiscovered(final Node node) {
            if (nodesDiscovered.size() < 1000) {
                nodesDiscovered.add(node);
            }
        }

        @Override
        public void onEthStatusUpdated(final Channel channel, final StatusMessage statusMessage) {
            ethNodes.put(channel.getNode(), statusMessage);
        }

        @Override
        public void onPeerAddedToSyncPool(final Channel peer) {
            syncPeers.add(peer.getNode());
        }

        @Override
        public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
            bestBlock = block;
            txCount += receipts.size();
            for (final TransactionReceipt receipt : receipts) {
                gasSpent += ByteUtil.byteArrayToLong(receipt.getGasUsed());
            }
            if (syncComplete) {
                logger.info("New block: " + block.getShortDescr());
            }
        }

        @Override
        public void onRecvMessage(final Channel channel, final Message message) {
        }

        @Override
        public void onSendMessage(final Channel channel, final Message message) {
        }

        @Override
        public void onPeerDisconnect(final String host, final long port) {
        }

        @Override
        public void onPendingTransactionsReceived(@NotNull final List<? extends Transaction> transactions) {
        }

        @Override
        public void onPendingStateChanged(final PendingState pendingState) {
        }

        @Override
        public void onHandShakePeer(final Channel channel, final HelloMessage helloMessage) {
        }

        @Override
        public void onNoConnections() {
        }

        @Override
        public void onVMTraceCreated(final String transactionHash, final String trace) {
        }

        @Override
        public void onTransactionExecuted(final TransactionExecutionSummary summary) {
        }
    };

    public BasicSample() {
        this("sample");
    }

    /**
     * logger name can be passed if more than one EthereumJ instance is created
     * in a single JVM to distinguish logging output from different instances
     */
    public BasicSample(final String loggerName) {
        this.loggerName = loggerName;
    }

    public static void main(final String[] args) throws Exception {
        sLogger.info("Starting EthereumJ!");

        // Based on Config class the BasicSample would be created by Spring
        // and its springInit() method would be called as an entry point
        EthereumFactory.createEthereum(Config.class);
    }

    /**
     * Allow only selected logger to print DEBUG events to STDOUT and FILE.
     * Other loggers are allowed to print ERRORS only.
     */
    private static void addSampleLogger(final String loggerName) {
        if (CUSTOM_FILTER == null) {
            CUSTOM_FILTER = new CustomFilter();
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            final Appender ca = loggerContext.getLogger("ROOT").getAppender("STDOUT");
            ca.clearAllFilters();
            ca.addFilter(CUSTOM_FILTER);
        }
        CUSTOM_FILTER.addVisibleLogger(loggerName);
    }

    private void setupLogging() {
        addSampleLogger(loggerName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * The method is called after all EthereumJ instances are created
     */
    @PostConstruct
    private void springInit() {
        setupLogging();

        // adding the main EthereumJ callback to be notified on different kind of events
        ethereum.addListener(listener);

        logger.info("Sample component created. Listening for ethereum events...");

        // starting lifecycle tracking method run()
        new Thread(this, "SampleWorkThread").start();
    }

    /**
     * The method tracks step-by-step the instance lifecycle from node discovery till sync completion.
     * At the end the method onSyncDone() is called which might be overridden by a sample subclass
     * to start making other things with the Ethereum network
     */
    public void run() {
        try {
            logger.info("Sample worker thread started.");

            if (config.peerDiscovery()) {
                waitForDiscovery();
            } else {
                logger.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections");
            }

            waitForAvailablePeers();

            waitForSyncPeers();

            waitForFirstBlock();

            waitForSync();

            onSyncDone();

        } catch (final Exception e) {
            logger.error("Error occurred in Sample: ", e);
        }
    }

    /**
     * Is called when the whole blockchain sync is complete
     */
    public void onSyncDone() throws Exception {
        logger.info("Monitoring new blocks in real-time...");
    }

    /**
     * Waits until any new nodes are discovered by the UDP discovery protocol
     */
    void waitForDiscovery() throws Exception {
        logger.info("Waiting for nodes discovery...");

        final int bootNodes = config.peerDiscoveryIPList().size();
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 300 : 5000);

            if (nodesDiscovered.size() > bootNodes) {
                logger.info("[v] Discovery works, new nodes started being discovered.");
                return;
            }

            if (cnt >= 30) logger.warn("Discovery keeps silence. Waiting more...");
            if (cnt > 50) {
                logger.error("Looks like discovery failed, no nodes were found.\n" +
                        "Please check your Firewall/NAT UDP protocol settings.\n" +
                        "Your IP interface was detected as " + config.bindIp() + ", please check " +
                        "if this interface is correct, otherwise set it manually via 'peer.discovery.bind.ip' option.");
                throw new RuntimeException("Discovery failed.");
            }
            cnt++;
        }
    }

    /**
     * Discovering nodes is only the first step. No we need to find among discovered nodes
     * those ones which are live, accepting inbound connections, and has compatible subprotocol versions
     */
    private void waitForAvailablePeers() throws Exception {
        logger.info("Waiting for available Eth capable nodes...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (ethNodes.size() > 0) {
                logger.info("[v] Available Eth nodes found.");
                return;
            }

            if (cnt >= 30) logger.info("No Eth nodes found so far. Keep searching...");
            if (cnt > 60) {
                logger.error("No eth capable nodes found. Logs need to be investigated.");
//                throw new RuntimeException("Eth nodes failed.");
            }
            cnt++;
        }
    }

    /**
     * When live nodes found SyncManager should select from them the most
     * suitable and add them as peers for syncing the blocks
     */
    private void waitForSyncPeers() throws Exception {
        logger.info("Searching for peers to sync with...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 30 ? 1000 : 5000);

            if (syncPeers.size() > 0) {
                logger.info("[v] At least one sync peer found.");
                return;
            }

            if (cnt >= 30) logger.info("No sync peers found so far. Keep searching...");
            if (cnt > 60) {
                logger.error("No sync peers found. Logs need to be investigated.");
//                throw new RuntimeException("Sync peers failed.");
            }
            cnt++;
        }
    }

    /**
     * Waits until blocks import started
     */
    private void waitForFirstBlock() throws Exception {
        final Block currentBest = ethereum.getBlockchain().getBestBlock();
        logger.info("Current BEST block: " + currentBest.getShortDescr());
        logger.info("Waiting for blocks start importing (may take a while)...");
        int cnt = 0;
        while(true) {
            Thread.sleep(cnt < 300 ? 1000 : 60000);

            if (bestBlock != null && bestBlock.getNumber() > currentBest.getNumber()) {
                logger.info("[v] Blocks import started.");
                return;
            }

            if (cnt >= 300) logger.info("Still no blocks. Be patient...");
            if (cnt > 330) {
                logger.error("No blocks imported during a long period. Must be a problem, logs need to be investigated.");
//                throw new RuntimeException("Block import failed.");
            }
            cnt++;
        }
    }

    /**
     * Waits until the whole blockchain sync is complete
     */
    private void waitForSync() throws Exception {
        logger.info("Waiting for the whole blockchain sync (will take up to several hours for the whole chain)...");
        while(true) {
            Thread.sleep(10000);

            if (synced) {
                logger.info("[v] Sync complete! The best block: " + bestBlock.getShortDescr());
                syncComplete = true;
                return;
            }

            logger.info("Blockchain sync in progress. Last imported block: " + bestBlock.getShortDescr() +
                    " (Total: txs: " + txCount + ", gas: " + (gasSpent / 1000) + "k)");
            txCount = 0;
            gasSpent = 0;
        }
    }

    // Spring config class which add this sample class as a bean to the components collections
    // and make it possible for autowiring other components
    private static class Config {
        @Bean
        public BasicSample basicSample() {
            return new BasicSample();
        }
    }

    private static class CustomFilter extends Filter<ILoggingEvent> {
        private final Set<String> visibleLoggers = new HashSet<>();
        @Override
        public synchronized FilterReply decide(final ILoggingEvent event) {
            return visibleLoggers.contains(event.getLoggerName()) && event.getLevel().isGreaterOrEqual(Level.INFO) ||
                    event.getLevel().isGreaterOrEqual(Level.ERROR) ? FilterReply.NEUTRAL : FilterReply.DENY;
        }

        public synchronized void addVisibleLogger(final String name) {
            visibleLoggers.add(name);
        }
    }
}
