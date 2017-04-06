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

package org.ethereum.longrun

import com.typesafe.config.ConfigFactory
import org.apache.commons.lang3.mutable.MutableObject
import org.ethereum.config.CommonConfig
import org.ethereum.config.SystemProperties
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListener
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fast sync with sanity check

 * Runs sync with defined config. Stops when all nodes are downloaded in 1 minute.
 * - checks State Trie is not broken
 * Restarts, waits until SECURE sync state (all headers are downloaded) in 1 minute.
 * - checks block headers
 * Restarts, waits until full sync is over
 * - checks nodes/headers/blocks/tx receipts

 * Run with '-Dlogback.configurationFile=longrun/logback.xml' for proper logging
 * Also following flags are available:
 * -Dreset.db.onFirstRun=true
 * -Doverride.config.res=longrun/conf/live.conf
 */
@Ignore
class FastSyncSanityTest @Throws(Exception::class)
constructor() {
    private var regularNode: Ethereum? = null

    init {

        val resetDb = System.getProperty("reset.db.onFirstRun")
        val overrideConfigPath = System.getProperty("override.config.res")
        if (java.lang.Boolean.parseBoolean(resetDb)) {
            resetDBOnFirstRun.setValue(true)
        } else if (resetDb != null && resetDb.equals("false", ignoreCase = true)) {
            resetDBOnFirstRun.value = false
        }
        if (overrideConfigPath != null) configPath.value = overrideConfigPath

        statTimer.scheduleAtFixedRate({
            try {
                if (fatalErrors.get() > 0) {
                    statTimer.shutdownNow()
                }
            } catch (t: Throwable) {
                FastSyncSanityTest.testLogger.error("Unhandled exception", t)
            }
        }, 0, 15, TimeUnit.SECONDS)
    }

    @Test
    @Throws(Exception::class)
    fun testTripleCheck() {

        runEthereum()

        Thread(Runnable {
            try {
                while (firstRun.get()) {
                    sleep(1000)
                }
                testLogger.info("Stopping first run")
                regularNode!!.close()
                testLogger.info("First run stopped")
                sleep(10000)
                testLogger.info("Starting second run")
                runEthereum()
                while (secondRun.get()) {
                    sleep(1000)
                }
                testLogger.info("Stopping second run")
                regularNode!!.close()
                testLogger.info("Second run stopped")
                sleep(10000)
                testLogger.info("Starting third run")
                runEthereum()
                while (!allChecksAreOver.get()) {
                    sleep(1000)
                }
                testLogger.info("Stopping third run")
                regularNode!!.close()
                testLogger.info("All checks are finished")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }).start()

        if (statTimer.awaitTermination(MAX_RUN_MINUTES, TimeUnit.MINUTES)) {
            logStats()
            // Checking for errors
            assert(allChecksAreOver.get())
            if (!logStats()) assert(false)
        }
    }

    @Throws(Exception::class)
    private fun runEthereum() {
        testLogger.info("Starting EthereumJ regular instance!")
        this.regularNode = EthereumFactory.createEthereum(RegularConfig::class.java)
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private class RegularConfig {

        @Bean
        fun node(): RegularNode {
            return RegularNode()
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        fun systemProperties(): SystemProperties {
            val props = SystemProperties()
            props.overrideParams(ConfigFactory.parseResources(configPath.value))
            if (firstRun.get() && resetDBOnFirstRun.value != null) {
                props.setDatabaseReset(resetDBOnFirstRun.value)
            }
            return props
        }
    }

    /**
     * Just regular EthereumJ node
     */
    internal class RegularNode : BasicNode("sampleNode") {

        private fun stopSync() {
            config!!.isSyncEnabled = false
            config!!.setDiscoveryEnabled(false)
            ethereum!!.channelManager.close()
            syncPool!!.close()
        }

        @Throws(Exception::class)
        override fun waitForSync() {
            testLogger.info("Waiting for the complete blockchain sync (will take up to an hour on fast sync for the whole chain)...")
            while (true) {
                sleep(10000)
                if (syncState == null) continue

                when (syncState) {
                    EthereumListener.SyncState.UNSECURE -> {
                        if (!firstRun.get()) return
                        testLogger.info("[v] Unsecure sync completed")
                        sleep(60000)
                        stopSync()
                        BlockchainValidation.checkNodes(ethereum!!, commonConfig!!, fatalErrors)
                        firstRun.set(false)
                    }
                    EthereumListener.SyncState.SECURE -> {
                        if (!secondRun.get()) return
                        testLogger.info("[v] Secure sync completed")
                        sleep(60000)
                        stopSync()
                        BlockchainValidation.checkFastHeaders(ethereum!!, commonConfig!!, fatalErrors)
                        secondRun.set(false)
                    }
                    EthereumListener.SyncState.COMPLETE -> {
                        testLogger.info("[v] Sync complete! The best block: " + bestBlock!!.shortDescr)
                        stopSync()
                        return
                    }
                }
            }
        }

        @Throws(Exception::class)
        override fun onSyncDone() {
            // Full sanity check
            fullSanityCheck(ethereum!!, commonConfig!!)
        }
    }

    companion object {

        private val firstRun = AtomicBoolean(true)
        private val secondRun = AtomicBoolean(true)
        private val testLogger = LoggerFactory.getLogger("TestLogger")
        private val configPath = MutableObject("longrun/conf/ropsten-fast.conf")
        private val resetDBOnFirstRun = MutableObject<Boolean>(null)
        private val allChecksAreOver = AtomicBoolean(false)
        private val fatalErrors = AtomicInteger(0)
        private val MAX_RUN_MINUTES = 180L
        private val statTimer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "StatTimer") }

        private fun logStats(): Boolean {
            testLogger.info("---------====---------")
            testLogger.info("fatalErrors: {}", fatalErrors)
            testLogger.info("---------====---------")

            return fatalErrors.get() == 0
        }

        private fun fullSanityCheck(ethereum: Ethereum, commonConfig: CommonConfig) {
            BlockchainValidation.fullCheck(ethereum, commonConfig, fatalErrors)
            logStats()
            allChecksAreOver.set(true)
            statTimer.shutdownNow()
        }
    }
}
