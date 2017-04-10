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

package org.ethereum.sync

import ch.qos.logback.classic.Level
import com.typesafe.config.ConfigFactory
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.BlockSummary
import org.ethereum.core.Transaction
import org.ethereum.core.TransactionReceipt
import org.ethereum.crypto.ECKey
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.mine.Ethash
import org.ethereum.mine.MinerListener
import org.ethereum.net.eth.message.*
import org.ethereum.net.message.Message
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.ethereum.util.ByteUtil
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

/**
 * Long running test

 * 3 peers: A <-> B <-> C where A is miner, C is issuing txs, and B should forward Txs/Blocks
 */
@Ignore
class BlockTxForwardTest {
    init {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.level = Level.INFO
    }

    private fun logStats(): Boolean {
        testLogger.info("---------====---------")
        val arrivedBlocks = blocks.values.count { it!! }
        testLogger.info("Arrived blocks / Total: {}/{}", arrivedBlocks, blocks.size)
        val arrivedTxs = txs.values.count { it!! }
        testLogger.info("Arrived txs / Total: {}/{}", arrivedTxs, txs.size)
        testLogger.info("fatalErrors: {}", fatalErrors)
        testLogger.info("---------====---------")

        return fatalErrors.get() == 0 && blocks.size == arrivedBlocks && txs.size == arrivedTxs
    }

    /**
     * Creating 3 EthereumJ instances with different config classes
     * 1st - Miner node, no sync
     * 2nd - Regular node, synced with both Miner and Generator
     * 3rd - Generator node, sync is on, but can see only 2nd node
     * We want to check that blocks mined on Miner will reach Generator and
     * txs from Generator will reach Miner node
     */
    @Test
    @Throws(Exception::class)
    fun testTest() {

        statTimer.scheduleAtFixedRate({
            try {
                logStats()
                if (fatalErrors.get() > 0 || blocks.size >= STOP_ON_BLOCK) {
                    statTimer.shutdownNow()
                }
            } catch (t: Throwable) {
                testLogger.error("Unhandled exception", t)
            }
        }, 0, 15, TimeUnit.SECONDS)

        testLogger.info("Starting EthereumJ miner instance!")
        val miner = EthereumFactory.createEthereum(MinerConfig::class.java)

        miner.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(blockSummary: BlockSummary) {
                if (blockSummary.block.number != 0L) {
                    blocks.put(Hex.toHexString(blockSummary.block.hash), java.lang.Boolean.FALSE)
                }
            }

            override fun onRecvMessage(channel: Channel, message: Message) {
                super.onRecvMessage(channel, message)
                if (message !is EthMessage) return
                when (message.command) {
                    EthMessageCodes.NEW_BLOCK_HASHES -> {
                        testLogger.error("Received new block hash message at miner: {}", message.toString())
                        fatalErrors.incrementAndGet()
                    }
                    EthMessageCodes.NEW_BLOCK -> {
                        testLogger.error("Received new block message at miner: {}", message.toString())
                        fatalErrors.incrementAndGet()
                    }
                    EthMessageCodes.TRANSACTIONS -> {
                        val msgCopy = TransactionsMessage(message.getEncoded())
                        for (transaction in msgCopy.transactions!!) {
                            if (txs.put(Hex.toHexString(transaction.hash), java.lang.Boolean.TRUE) == null) {
                                testLogger.error("Received strange transaction at miner: {}", transaction)
                                fatalErrors.incrementAndGet()
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        })

        testLogger.info("Starting EthereumJ regular instance!")
        EthereumFactory.createEthereum(RegularConfig::class.java)

        testLogger.info("Starting EthereumJ txSender instance!")
        val txGenerator = EthereumFactory.createEthereum(GeneratorConfig::class.java)
        txGenerator.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                super.onRecvMessage(channel, message)
                if (message !is EthMessage) return
                when (message.command) {
                    EthMessageCodes.NEW_BLOCK_HASHES -> {
                        testLogger.info("Received new block hash message at generator: {}", message.toString())
                        val msgCopy = NewBlockHashesMessage(message.getEncoded())
                        for (identifier in msgCopy.blockIdentifiers) {
                            if (blocks.put(Hex.toHexString(identifier.hash), java.lang.Boolean.TRUE) == null) {
                                testLogger.error("Received strange block: {}", identifier)
                                fatalErrors.incrementAndGet()
                            }
                        }
                    }
                    EthMessageCodes.NEW_BLOCK -> {
                        testLogger.info("Received new block message at generator: {}", message.toString())
                        val msgCopy2 = NewBlockMessage(message.getEncoded())
                        val block = msgCopy2.block
                        if (blocks.put(Hex.toHexString(block.hash), java.lang.Boolean.TRUE) == null) {
                            testLogger.error("Received strange block: {}", block)
                            fatalErrors.incrementAndGet()
                        }
                    }
                    EthMessageCodes.BLOCK_BODIES -> testLogger.info("Received block bodies message at generator: {}", message.toString())
                    EthMessageCodes.TRANSACTIONS -> testLogger.warn("Received new transaction message at generator: {}, " + "allowed only after disconnect.", message.toString())
                    else -> {
                    }
                }
            }

            override fun onSendMessage(channel: Channel, message: Message) {
                super.onSendMessage(channel, message)
                if (message !is EthMessage) return
                if (message.command == EthMessageCodes.TRANSACTIONS) {
                    val msgCopy = TransactionsMessage(message.getEncoded())
                    msgCopy.transactions
                            ?.map { Transaction(it.encoded) }
                            ?.forEach { txs.put(Hex.toHexString(it.hash), java.lang.Boolean.FALSE) }
                }
            }
        })

        if (statTimer.awaitTermination(MAX_RUN_MINUTES, TimeUnit.MINUTES)) {
            logStats()
            // Stop generating new txs
            stopTxGeneration.set(true)
            Thread.sleep(60000)
            // Stop miner
            miner.blockMiner.stopMining()
            // Wait to be sure that last mined blocks will reach Generator
            Thread.sleep(60000)
            // Checking stats
            if (!logStats()) assert(false)
        }
    }

    open class BasicSample
    /**
     * logger name can be passed if more than one EthereumJ instance is created
     * in a single JVM to distinguish logging output from different instances
     */
    @JvmOverloads constructor(private val loggerName: String = "sample") : Runnable {
        internal val ethNodes: MutableMap<Node, StatusMessage> = Hashtable()
        internal val syncPeers: MutableList<Node> = Vector()
        var logger: Logger = LoggerFactory.getLogger(BlockTxForwardTest::class.java)
        @Autowired
        internal var ethereum: Ethereum? = null
        @Autowired
        internal var config: SystemProperties? = null
        internal var bestBlock: Block? = null
        internal var synced = false
        internal var syncComplete = false
        /**
         * The main EthereumJ callback.
         */
        internal val listener: EthereumListener = object : EthereumListenerAdapter() {
            override fun onSyncDone(state: EthereumListener.SyncState) {
                synced = true
            }

            override fun onEthStatusUpdated(channel: Channel, statusMessage: StatusMessage) {
                ethNodes.put(channel.node, statusMessage)
            }

            override fun onPeerAddedToSyncPool(peer: Channel) {
                syncPeers.add(peer.node)
            }

            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                bestBlock = block

                if (syncComplete) {
                    logger.info("New block: " + block.shortDescr)
                }
            }
        }

        /**
         * The method is called after all EthereumJ instances are created
         */
        @PostConstruct
        private fun springInit() {
            logger = LoggerFactory.getLogger(loggerName)
            // adding the main EthereumJ callback to be notified on different kind of events
            ethereum!!.addListener(listener)

            logger.info("Sample component created. Listening for ethereum events...")

            // starting lifecycle tracking method run()
            Thread(this, "SampleWorkThread").start()
        }

        /**
         * The method tracks step-by-step the instance lifecycle from node discovery till sync completion.
         * At the end the method onSyncDone() is called which might be overridden by a sample subclass
         * to start making other things with the Ethereum network
         */
        override fun run() {
            try {
                logger.info("Sample worker thread started.")

                if (!config!!.peerDiscovery()) {
                    logger.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections")
                }

                waitForSync()

                onSyncDone()

            } catch (e: Exception) {
                logger.error("Error occurred in Sample: ", e)
            }

        }

        /**
         * Waits until the whole blockchain sync is complete
         */
        @Throws(Exception::class)
        private fun waitForSync() {
            logger.info("Waiting for the whole blockchain sync (will take up to several hours for the whole chain)...")
            while (true) {
                Thread.sleep(10000)

                if (synced) {
                    logger.info("[v] Sync complete! The best block: " + bestBlock!!.shortDescr)
                    syncComplete = true
                    return
                }
            }
        }

        /**
         * Is called when the whole blockchain sync is complete
         */
        @Throws(Exception::class)
        open fun onSyncDone() {
            logger.info("Monitoring new blocks in real-time...")
        }

        // Spring config class which add this sample class as a bean to the components collections
        // and make it possible for autowiring other components
        private open class Config {
            @Bean
            open fun basicSample(): BasicSample {
                return BasicSample()
            }
        }

        companion object {
            internal val sLogger = LoggerFactory.getLogger("sample")

            @Throws(Exception::class)
            @JvmStatic fun main(args: Array<String>) {
                sLogger.info("Starting EthereumJ!")

                // Based on Config class the BasicSample would be created by Spring
                // and its springInit() method would be called as an entry point
                EthereumFactory.createEthereum(Config::class.java)
            }
        }
    }

    /**
     * Spring configuration class for the Miner peer (A)
     */
    private open class MinerConfig {

        private val config =
                // no need for discovery in that small network
                "peer.discovery.enabled = false \n" +
                        "peer.listen.port = 30335 \n" +
                        // need to have different nodeId's for the peers
                        "peer.privateKey = 6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec \n" +
                        // our private net ID
                        "peer.networkId = 555 \n" +
                        // we have no peers to sync with
                        "sync.enabled = false \n" +
                        // genesis with a lower initial difficulty and some predefined known funded accounts
                        "genesis = sample-genesis.json \n" +
                        // two peers need to have separate database dirs
                        "database.dir = sampleDB-1 \n" +
                        "keyvalue.datasource = leveldb \n" +
                        // when more than 1 miner exist on the network extraData helps to identify the block creator
                        "mine.extraDataHex = cccccccccccccccccccc \n" +
                        "mine.cpuMineThreads = 2 \n" +
                        "cache.flush.blocks = 1"

        @Bean
        open fun node(): MinerNode {
            return MinerNode()
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        open fun systemProperties(): SystemProperties {
            val props = SystemProperties()
            props.overrideParams(ConfigFactory.parseString(config.replace("'".toRegex(), "\"")))
            return props
        }
    }

    /**
     * Miner bean, which just start a miner upon creation and prints miner events
     */
    internal class MinerNode : BasicSample("sampleMiner"), MinerListener {

        // overriding run() method since we don't need to wait for any discovery,
        // networking or sync events
        override fun run() {
            if (config!!.isMineFullDataset) {
                logger.info("Generating Full Dataset (may take up to 10 min if not cached)...")
                // calling this just for indication of the dataset generation
                // basically this is not required
                val ethash = Ethash.getForBlock(config, ethereum!!.blockchain.bestBlock.number)
                ethash.fullDataset
                logger.info("Full dataset generated (loaded).")
            }
            ethereum!!.blockMiner.addListener(this)
            ethereum!!.blockMiner.startMining()
        }

        override fun miningStarted() {
            logger.info("Miner started")
        }

        override fun miningStopped() {
            logger.info("Miner stopped")
        }

        override fun blockMiningStarted(block: Block) {
            logger.info("Start mining block: " + block.shortDescr)
        }

        override fun blockMined(block: Block) {
            logger.info("Block mined! : \n" + block)
        }

        override fun blockMiningCanceled(block: Block) {
            logger.info("Cancel mining block: " + block.shortDescr)
        }
    }// peers need different loggers

    /**
     * Spring configuration class for the Regular peer (B)
     * It will see nodes A and C, which is not connected directly and proves that tx's from (C) reaches miner (A)
     * and new blocks both A and C
     */
    private open class RegularConfig {
        private val config =
                // no discovery: we are connecting directly to the generator and miner peers
                "peer.discovery.enabled = false \n" +
                        "peer.listen.port = 30339 \n" +
                        "peer.privateKey = 1f0bbd4ffd61128a7d150c07d3f5b7dcd078359cd708ada8b60e4b9ffd90b3f5 \n" +
                        "peer.networkId = 555 \n" +
                        // actively connecting to the miner and tx generator
                        "peer.active = [" +
                        // miner
                        "    { url = 'enode://26ba1aadaf59d7607ad7f437146927d79e80312f026cfa635c6b2ccf2c5d3521f5812ca2beb3b295b14f97110e6448c1c7ff68f14c5328d43a3c62b44143e9b1@localhost:30335' }, \n" +
                        // tx generator
                        "    { url = 'enode://3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6@localhost:30336' } \n" +
                        "] \n" +
                        "sync.enabled = true \n" +
                        // all peers in the same network need to use the same genesis block
                        "genesis = sample-genesis.json \n" +
                        // two peers need to have separate database dirs
                        "database.dir = sampleDB-2 \n" +
                        "keyvalue.datasource = leveldb \n"

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
            props.overrideParams(ConfigFactory.parseString(config.replace("'".toRegex(), "\"")))
            return props
        }
    }

    /**
     * This node doing nothing special, but by default as any other node will resend txs and new blocks
     */
    internal class RegularNode : BasicSample("sampleNode")// peers need different loggers

    /**
     * Spring configuration class for the TX-sender peer (C)
     */
    private open class GeneratorConfig {
        private val config =
                // no discovery: forwarder will connect to us
                "peer.discovery.enabled = false \n" +
                        "peer.listen.port = 30336 \n" +
                        "peer.privateKey = 3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c \n" +
                        "peer.networkId = 555 \n" +
                        "sync.enabled = true \n" +
                        // all peers in the same network need to use the same genesis block
                        "genesis = sample-genesis.json \n" +
                        // two peers need to have separate database dirs
                        "database.dir = sampleDB-3 \n" +
                        "keyvalue.datasource = leveldb \n"

        @Bean
        open fun node(): GeneratorNode {
            return GeneratorNode()
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        open fun systemProperties(): SystemProperties {
            val props = SystemProperties()
            props.overrideParams(ConfigFactory.parseString(config.replace("'".toRegex(), "\"")))
            return props
        }
    }

    /**
     * The tx generator node in the network which connects to the regular
     * waits for the sync and starts submitting transactions.
     * Those transactions should be included into mined blocks and the peer
     * should receive those blocks back
     */
    internal class GeneratorNode : BasicSample("txSenderNode") {

        override fun onSyncDone() {
            Thread(Runnable {
                try {
                    generateTransactions()
                } catch (e: Exception) {
                    logger.error("Error generating tx: ", e)
                }
            }).start()
        }


        /**
         * Generate one simple value transfer transaction each 7 seconds.
         * Thus blocks will include one, several and none transactions
         */
        @Throws(Exception::class)
        private fun generateTransactions() {
            logger.info("Start generating transactions...")

            // the sender which some coins from the genesis
            val senderKey = ECKey.fromPrivate(Hex.decode("6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec"))
            val receiverAddr = Hex.decode("5db10750e8caff27f906b41c71b3471057dd2004")

            var i = ethereum!!.repository.getNonce(senderKey.address).toInt()
            var j = 0
            while (j < 20000) {
                run {
                    if (stopTxGeneration.get()) return
                    val tx = Transaction(ByteUtil.intToBytesNoLeadZeroes(i),
                            ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L), ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                            receiverAddr, byteArrayOf(77), ByteArray(0))
                    tx.sign(senderKey)
                    logger.info("<== Submitting tx: " + tx)
                    ethereum!!.submitTransaction(tx)
                }
                Thread.sleep(7000)
                i++
                j++
            }
        }
    }// peers need different loggers

    companion object {

        private val testLogger = LoggerFactory.getLogger("TestLogger")
        private val blocks = Collections.synchronizedMap(HashMap<String, Boolean>())
        private val txs = Collections.synchronizedMap(HashMap<String, Boolean>())
        private val fatalErrors = AtomicInteger(0)
        private val stopTxGeneration = AtomicBoolean(false)
        private val MAX_RUN_MINUTES = 360L
        // Actually there will be several blocks mined after, it's a very soft shutdown
        private val STOP_ON_BLOCK = 100
        private val statTimer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "StatTimer") }
    }
}
