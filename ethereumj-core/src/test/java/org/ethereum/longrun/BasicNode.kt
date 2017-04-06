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

import org.ethereum.config.CommonConfig
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.TransactionReceipt
import org.ethereum.db.DbFlushManager
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.ethereum.sync.SyncPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import java.lang.Thread.sleep
import java.util.*
import javax.annotation.PostConstruct

/**
 * BasicNode of ethereum instance
 */
open class BasicNode
/**
 * logger name can be passed if more than one EthereumJ instance is created
 * in a single JVM to distinguish logging output from different instances
 */
@JvmOverloads constructor(private val loggerName: String = "sample") : Runnable {
    private val ethNodes = Hashtable<Node, StatusMessage>()
    private val syncPeers = Vector<Node>()
    @Autowired
    protected var dbFlushManager: DbFlushManager? = null
    @Autowired
    var ethereum: Ethereum? = null
    @Autowired
    var config: SystemProperties? = null
    @Autowired
    var syncPool: SyncPool? = null
    @Autowired
    var commonConfig: CommonConfig? = null
    var bestBlock: Block? = null
    var syncState: EthereumListener.SyncState? = null
    var syncComplete = false
    private var logger: Logger? = null
    /**
     * The main EthereumJ callback.
     */
    private val listener = object : EthereumListenerAdapter() {
        override fun onSyncDone(state: EthereumListener.SyncState) {
            syncState = state
            if (state == EthereumListener.SyncState.COMPLETE) syncComplete = true
            onSyncDoneImpl(state)
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
                logger!!.info("New block: " + block.shortDescr)
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

        logger!!.info("Sample component created. Listening for ethereum events...")

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
            logger!!.info("Sample worker thread started.")

            if (!config!!.peerDiscovery()) {
                logger!!.info("Peer discovery disabled. We should actively connect to another peers or wait for incoming connections")
            }

            waitForSync()

            onSyncDone()

        } catch (e: Exception) {
            logger!!.error("Error occurred in Sample: ", e)
        }

    }

    /**
     * Waits until the whole blockchain sync is complete
     */
    @Throws(Exception::class)
    open fun waitForSync() {
        logger!!.info("Waiting for the whole blockchain sync (will take up to an hour on fast sync for the whole chain)...")
        while (true) {
            sleep(10000)
            if (syncComplete) {
                logger!!.info("[v] Sync complete! The best block: " + bestBlock!!.shortDescr)
                return
            }
        }
    }

    /**
     * Is called when the whole blockchain sync is complete
     */
    @Throws(Exception::class)
    open fun onSyncDone() {
        logger!!.info("Monitoring new blocks in real-time...")
    }

    open fun onSyncDoneImpl(state: EthereumListener.SyncState) {
        logger!!.info("onSyncDone: " + state)
    }

    // Spring config class which add this sample class as a bean to the components collections
    // and make it possible for autowiring other components
    private class Config {
        @Bean
        fun basicSample(): BasicNode {
            return BasicNode()
        }
    }

    companion object {
        private val sLogger = LoggerFactory.getLogger("sample")

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            sLogger.info("Starting EthereumJ!")

            // Based on Config class the BasicNode would be created by Spring
            // and its springInit() method would be called as an entry point
            EthereumFactory.createEthereum(Config::class.java)
        }
    }
}
