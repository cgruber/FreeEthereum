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
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/**
 * Sync with sanity check
 *
 * Runs sync with defined config
 * - checks State Trie is not broken
 * - checks whether all blocks are in blockstore, validates parent connection and bodies
 * - checks and validate transaction receipts
 * Stopped, than restarts in 1 minute, syncs and pass all checks again
 *
 * Run with '-Dlogback.configurationFile=longrun/logback.xml' for proper logging
 * Also following flags are available:
 *     -Dreset.db.onFirstRun=true
 *     -Doverride.config.res=longrun/conf/live.conf
 */
@Ignore
public class SyncSanityTest {

    private static final AtomicBoolean firstRun = new AtomicBoolean(true);
    private static final Logger testLogger = LoggerFactory.getLogger("TestLogger");
    private static final MutableObject<String> configPath = new MutableObject<>("longrun/conf/ropsten.conf");
    private static final MutableObject<Boolean> resetDBOnFirstRun = new MutableObject<>(null);
    private static final AtomicBoolean allChecksAreOver =  new AtomicBoolean(false);
    private final static AtomicInteger fatalErrors = new AtomicInteger(0);
    private final static long MAX_RUN_MINUTES = 180L;
    private static final ScheduledExecutorService statTimer =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(final Runnable r) {
                    return new Thread(r, "StatTimer");
                }
            });
    private Ethereum regularNode;

    public SyncSanityTest() throws Exception {

        final String resetDb = System.getProperty("reset.db.onFirstRun");
        final String overrideConfigPath = System.getProperty("override.config.res");
        if (Boolean.parseBoolean(resetDb)) {
            resetDBOnFirstRun.setValue(true);
        } else if (resetDb != null && resetDb.equalsIgnoreCase("false")) {
            resetDBOnFirstRun.setValue(false);
        }
        if (overrideConfigPath != null) configPath.setValue(overrideConfigPath);

        statTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (fatalErrors.get() > 0) {
                        statTimer.shutdownNow();
                    }
                } catch (final Throwable t) {
                    SyncSanityTest.testLogger.error("Unhandled exception", t);
                }
            }
        }, 0, 15, TimeUnit.SECONDS);
    }

    private static boolean logStats() {
        testLogger.info("---------====---------");
        testLogger.info("fatalErrors: {}", fatalErrors);
        testLogger.info("---------====---------");

        return fatalErrors.get() == 0;
    }

    private static void fullSanityCheck(final Ethereum ethereum, final CommonConfig commonConfig) {

        BlockchainValidation.fullCheck(ethereum, commonConfig, fatalErrors);
        logStats();

        if (!firstRun.get()) {
            allChecksAreOver.set(true);
            statTimer.shutdownNow();
        }

        firstRun.set(false);
    }

    @Test
    public void testDoubleCheck() throws Exception {

        runEthereum();

        new Thread(new Runnable() {
            @Override
            public void run() {
            try {
                while(firstRun.get()) {
                    sleep(1000);
                }
                testLogger.info("Stopping first run");
                regularNode.close();
                testLogger.info("First run stopped");
                sleep(60_000);
                testLogger.info("Starting second run");
                runEthereum();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            }
        }).start();

        if(statTimer.awaitTermination(MAX_RUN_MINUTES, TimeUnit.MINUTES)) {
            logStats();
            // Checking for errors
            assert allChecksAreOver.get();
            if (!logStats()) assert false;
        }
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
        public RegularNode() {
            super("sampleNode");
        }

        @Override
        public void waitForSync() throws Exception {
            testLogger.info("Waiting for the whole blockchain sync (will take up to an hour on fast sync for the whole chain)...");
            while (true) {
                sleep(10000);

                if (syncComplete) {
                    testLogger.info("[v] Sync complete! The best block: " + bestBlock.getShortDescr());

                    // Stop syncing
                    config.setSyncEnabled(false);
                    config.setDiscoveryEnabled(false);
                    ethereum.getChannelManager().close();
                    syncPool.close();

                    return;
                }
            }
        }

        @Override
        public void onSyncDone() throws Exception {
            // Full sanity check
            fullSanityCheck(ethereum, commonConfig);
        }
    }
}
