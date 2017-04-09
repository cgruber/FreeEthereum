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

package org.ethereum.longrun;

import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.db.ContractDetails;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.sync.SyncManager;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.sleep;

/**
 * Regular sync with load
 * Loads ethereumJ during sync with various onBlock/repo track/callback usages
 *
 * Runs sync with defined config for 1-30 minutes
 * - checks State Trie is not broken
 * - checks whether all blocks are in blockstore, validates parent connection and bodies
 * - checks and validate transaction receipts
 * Stopped, than restarts in 1 minute, syncs and pass all checks again.
 * Repeats forever or until first error occurs
 *
 * Run with '-Dlogback.configurationFile=longrun/logback.xml' for proper logging
 * Also following flags are available:
 *     -Dreset.db.onFirstRun=true
 *     -Doverride.config.res=longrun/conf/live.conf
 */
@Ignore
public class SyncWithLoadTest {

    private final static CountDownLatch errorLatch = new CountDownLatch(1);
    private static final Logger testLogger = LoggerFactory.getLogger("TestLogger");
    private static final MutableObject<String> configPath = new MutableObject<>("longrun/conf/ropsten-noprune.conf");
    private static final MutableObject<Boolean> resetDBOnFirstRun = new MutableObject<>(null);
    // Timer stops while not syncing
    private static final AtomicLong lastImport =  new AtomicLong();
    private static final int LAST_IMPORT_TIMEOUT = 10 * 60 * 1000;
    private final static AtomicInteger fatalErrors = new AtomicInteger(0);
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final AtomicBoolean firstRun = new AtomicBoolean(true);
    private static final ScheduledExecutorService statTimer =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "StatTimer"));
    private Ethereum regularNode;

    public SyncWithLoadTest() throws Exception {

        final String resetDb = System.getProperty("reset.db.onFirstRun");
        final String overrideConfigPath = System.getProperty("override.config.res");
        if (Boolean.parseBoolean(resetDb)) {
            resetDBOnFirstRun.setValue(true);
        } else if (resetDb != null && resetDb.equalsIgnoreCase("false")) {
            resetDBOnFirstRun.setValue(false);
        }
        if (overrideConfigPath != null) configPath.setValue(overrideConfigPath);

        statTimer.scheduleAtFixedRate(() -> {
            // Adds error if no successfully imported blocks for LAST_IMPORT_TIMEOUT
            final long currentMillis = System.currentTimeMillis();
            if (lastImport.get() != 0 && currentMillis - lastImport.get() > LAST_IMPORT_TIMEOUT) {
                testLogger.error("No imported block for {} seconds", LAST_IMPORT_TIMEOUT / 1000);
                fatalErrors.incrementAndGet();
            }

            try {
                if (fatalErrors.get() > 0) {
                    statTimer.shutdownNow();
                    errorLatch.countDown();
                }
            } catch (final Throwable t) {
                SyncWithLoadTest.testLogger.error("Unhandled exception", t);
            }

            if (lastImport.get() == 0 && isRunning.get()) lastImport.set(currentMillis);
            if (lastImport.get() != 0 && !isRunning.get()) lastImport.set(0);
        }, 0, 15, TimeUnit.SECONDS);
    }

    private static boolean logStats() {
        testLogger.info("---------====---------");
        testLogger.info("fatalErrors: {}", fatalErrors);
        testLogger.info("---------====---------");

        return fatalErrors.get() == 0;
    }

    private static void fullSanityCheck(final Ethereum ethereum, final CommonConfig commonConfig) {

        BlockchainValidation.INSTANCE.fullCheck(ethereum, commonConfig, fatalErrors);
        logStats();

        firstRun.set(false);
    }

    @Test
    public void testDelayedCheck() throws Exception {

        runEthereum();

        new Thread(() -> {
            try {
                while (firstRun.get()) {
                    sleep(1000);
                }
                testLogger.info("Stopping first run");

                while (true) {
                    while (isRunning.get()) {
                        sleep(1000);
                    }
                    regularNode.close();
                    testLogger.info("Run stopped");
                    sleep(10_000);
                    testLogger.info("Starting next run");
                    runEthereum();
                    isRunning.set(true);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }).start();

        errorLatch.await();
        if (!logStats()) assert false;
    }

    private void runEthereum() throws Exception {
        testLogger.info("Starting EthereumJ regular instance!");
        this.regularNode = EthereumFactory.createEthereum(RegularConfig.class);
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private static class RegularConfig {

        @Bean
        public RegularNode node() {
            return new RegularNode();
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        public SystemProperties systemProperties() {
            final SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseResources(configPath.getValue()));
            if (firstRun.get() && resetDBOnFirstRun.getValue() != null) {
                props.setDatabaseReset(resetDBOnFirstRun.getValue());
            }
            return props;
        }
    }

    /**
     * Just regular EthereumJ node
     */
    static class RegularNode extends BasicNode {

        @Autowired
        ProgramInvokeFactory programInvokeFactory;
        /**
         * The main EthereumJ callback.
         */
        final EthereumListener blockListener = new EthereumListenerAdapter() {
            @Override
            public void onBlock(final BlockSummary blockSummary) {
                lastImport.set(System.currentTimeMillis());
            }

            @Override
            public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
                for (final TransactionReceipt receipt : receipts) {
                    // Getting contract details
                    final byte[] contractAddress = receipt.getTransaction().getContractAddress();
                    if (contractAddress != null) {
                        final ContractDetails details = ((Repository) getEthereum().getRepository()).getContractDetails(contractAddress);
                        assert FastByteComparisons.equal(details.getAddress(), contractAddress);
                    }

                    // Getting AccountState for sender in the past
                    final Random rnd = new Random();
                    final Block bestBlock = getEthereum().getBlockchain().getBestBlock();
                    final Block randomBlock = getEthereum().getBlockchain().getBlockByNumber(rnd.nextInt((int) bestBlock.getNumber()));
                    final byte[] sender = receipt.getTransaction().getSender();
                    final AccountState senderState = ((Repository) getEthereum().getRepository()).getSnapshotTo(randomBlock.getStateRoot()).getAccountState(sender);
                    if (senderState != null) senderState.getBalance();

                    // Getting receiver's nonce somewhere in the past
                    final Block anotherRandomBlock = getEthereum().getBlockchain().getBlockByNumber(rnd.nextInt((int) bestBlock.getNumber()));
                    final byte[] receiver = receipt.getTransaction().getReceiveAddress();
                    if (receiver != null) {
                        ((Repository) getEthereum().getRepository()).getSnapshotTo(anotherRandomBlock.getStateRoot()).getNonce(receiver);
                    }
                }
            }

            @Override
            public void onPendingTransactionsReceived(final List<Transaction> transactions) {
                final Random rnd = new Random();
                final Block bestBlock = getEthereum().getBlockchain().getBestBlock();
                for (final Transaction tx : transactions) {
                    final Block block = getEthereum().getBlockchain().getBlockByNumber(rnd.nextInt((int) bestBlock.getNumber()));
                    final Repository repository = ((Repository) getEthereum().getRepository())
                            .getSnapshotTo(block.getStateRoot())
                            .startTracking();
                    try {
                        final TransactionExecutor executor = new TransactionExecutor
                                (tx, block.getCoinbase(), repository, getEthereum().getBlockchain().getBlockStore(),
                                        programInvokeFactory, block, new EthereumListenerAdapter(), 0)
                                .withCommonConfig(getCommonConfig())
                                .setLocalCall(true);

                        executor.init();
                        executor.execute();
                        executor.go();
                        executor.finalization();

                        executor.getReceipt();
                    } finally {
                        repository.rollback();
                    }
                }
            }
        };
        @Autowired
        SyncManager syncManager;

        public RegularNode() {
            super("sampleNode");
        }

        @Override
        public void run() {
            try {
                getEthereum().addListener(blockListener);

                // Run 1-30 minutes
                final Random generator = new Random();
                final int i = generator.nextInt(30) + 1;
                testLogger.info("Running for {} minutes until stop and check", i);
                sleep(i * 60_000);

                // Stop syncing
                getSyncPool().close();
                syncManager.close();
            } catch (final Exception ex) {
                testLogger.error("Error occurred during run: ", ex);
            }

            if (getSyncComplete()) {
                testLogger.info("[v] Sync complete! The best block: " + getBestBlock().getShortDescr());
            }

            fullSanityCheck(getEthereum(), getCommonConfig());
            isRunning.set(false);
        }
    }
}
