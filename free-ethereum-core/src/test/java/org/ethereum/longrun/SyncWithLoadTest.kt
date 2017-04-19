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
import org.ethereum.core.*
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.sync.SyncManager
import org.ethereum.util.FastByteComparisons
import org.ethereum.vm.program.invoke.ProgramInvokeFactory
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Regular sync with load
 * Loads ethereumJ during sync with various onBlock/repo track/callback usages

 * Runs sync with defined config for 1-30 minutes
 * - checks State Trie is not broken
 * - checks whether all blocks are in blockstore, validates parent connection and bodies
 * - checks and validate transaction receipts
 * Stopped, than restarts in 1 minute, syncs and pass all checks again.
 * Repeats forever or until first error occurs

 * Run with '-Dlogback.configurationFile=longrun/logback.xml' for proper logging
 * Also following flags are available:
 * -Dreset.db.onFirstRun=true
 * -Doverride.config.res=longrun/conf/live.conf
 */
@Ignore
class SyncWithLoadTest @Throws(Exception::class)
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
            // Adds error if no successfully imported blocks for LAST_IMPORT_TIMEOUT
            val currentMillis = System.currentTimeMillis()
            if (!lastImport.get().equals(0) && currentMillis - lastImport.get() > LAST_IMPORT_TIMEOUT) {
                testLogger.error("No imported block for {} seconds", LAST_IMPORT_TIMEOUT / 1000)
                fatalErrors.incrementAndGet()
            }

            try {
                if (fatalErrors.get() > 0) {
                    statTimer.shutdownNow()
                    errorLatch.countDown()
                }
            } catch (t: Throwable) {
                SyncWithLoadTest.testLogger.error("Unhandled exception", t)
            }

            if (lastImport.get().equals(0) && isRunning.get()) lastImport.set(currentMillis)
            if (!lastImport.get().equals(0) && !isRunning.get()) lastImport.set(0)
        }, 0, 15, TimeUnit.SECONDS)
    }

    @Test
    @Throws(Exception::class)
    fun testDelayedCheck() {

        runEthereum()

        Thread {
            try {
                while (firstRun.get()) {
                    sleep(1000)
                }
                testLogger.info("Stopping first run")

                while (true) {
                    while (isRunning.get()) {
                        sleep(1000)
                    }
                    regularNode!!.close()
                    testLogger.info("Run stopped")
                    sleep(10000)
                    testLogger.info("Starting next run")
                    runEthereum()
                    isRunning.set(true)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }.start()

        errorLatch.await()
        if (!logStats()) assert(false)
    }

    @Throws(Exception::class)
    private fun runEthereum() {
        testLogger.info("Starting EthereumJ regular instance!")
        this.regularNode = EthereumFactory.createEthereum(RegularConfig::class.java)
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private open class RegularConfig {

        @Bean
        open fun node(): RegularNode {
            return RegularNode()
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        open fun systemProperties(): SystemProperties {
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

        @Autowired
        var programInvokeFactory: ProgramInvokeFactory? = null
        /**
         * The main EthereumJ callback.
         */
        val blockListener: EthereumListener = object : EthereumListenerAdapter() {
            override fun onBlock(blockSummary: BlockSummary) {
                lastImport.set(System.currentTimeMillis())
            }

            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                for (receipt in receipts) {
                    // Getting contract details
                    val contractAddress = receipt.transaction.contractAddress
                    if (contractAddress != null) {
                        val details = (ethereum!!.repository as Repository).getContractDetails(contractAddress)
                        assert(FastByteComparisons.equal(details.address, contractAddress))
                    }

                    // Getting AccountState for sender in the past
                    val rnd = Random()
                    val bestBlock = ethereum!!.blockchain.bestBlock
                    val randomBlock = ethereum!!.blockchain.getBlockByNumber(rnd.nextInt(bestBlock.number.toInt()).toLong())
                    val sender = receipt.transaction.sender
                    val senderState = (ethereum!!.repository as Repository).getSnapshotTo(randomBlock.stateRoot).getAccountState(sender)
                    senderState.balance

                    // Getting receiver's nonce somewhere in the past
                    val anotherRandomBlock = ethereum!!.blockchain.getBlockByNumber(rnd.nextInt(bestBlock.number.toInt()).toLong())
                    val receiver = receipt.transaction.receiveAddress
                    if (receiver != null) {
                        (ethereum!!.repository as Repository).getSnapshotTo(anotherRandomBlock.stateRoot).getNonce(receiver)
                    }
                }
            }

            override fun onPendingTransactionsReceived(transactions: List<Transaction>) {
                val rnd = Random()
                val bestBlock = ethereum!!.blockchain.bestBlock
                for (tx in transactions) {
                    val block = ethereum!!.blockchain.getBlockByNumber(rnd.nextInt(bestBlock.number.toInt()).toLong())
                    val repository = (ethereum!!.repository as Repository)
                            .getSnapshotTo(block.stateRoot)
                            .startTracking()
                    try {
                        val executor = TransactionExecutor(tx, block.coinbase, repository, ethereum!!.blockchain.blockStore,
                                programInvokeFactory, block, EthereumListenerAdapter(), 0)
                                .withCommonConfig(commonConfig)
                                .setLocalCall(true)

                        executor.init()
                        executor.execute()
                        executor.go()
                        executor.finalization()

                        executor.receipt
                    } finally {
                        repository.rollback()
                    }
                }
            }
        }
        @Autowired
        var syncManager: SyncManager? = null

        override fun run() {
            try {
                ethereum!!.addListener(blockListener)

                // Run 1-30 minutes
                val generator = Random()
                val i = generator.nextInt(30) + 1
                testLogger.info("Running for {} minutes until stop and check", i)
                sleep((i * 60000).toLong())

                // Stop syncing
                syncPool!!.close()
                syncManager!!.close()
            } catch (ex: Exception) {
                testLogger.error("Error occurred during run: ", ex)
            }

            if (syncComplete) {
                testLogger.info("[v] Sync complete! The best block: " + bestBlock!!.shortDescr)
            }

            fullSanityCheck(ethereum!!, commonConfig!!)
            isRunning.set(false)
        }
    }

    companion object {

        private val errorLatch = CountDownLatch(1)
        private val testLogger = LoggerFactory.getLogger("TestLogger")
        private val configPath = MutableObject("longrun/conf/ropsten-noprune.conf")
        private val resetDBOnFirstRun = MutableObject<Boolean>(null)
        // Timer stops while not syncing
        private val lastImport = AtomicLong()
        private val LAST_IMPORT_TIMEOUT = 10 * 60 * 1000
        private val fatalErrors = AtomicInteger(0)
        private val isRunning = AtomicBoolean(true)
        private val firstRun = AtomicBoolean(true)
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

            firstRun.set(false)
        }
    }
}
