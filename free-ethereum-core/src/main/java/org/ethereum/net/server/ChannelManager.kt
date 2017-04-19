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

package org.ethereum.net.server

import org.apache.commons.collections4.map.LRUMap
import org.ethereum.config.NodeFilter
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.BlockWrapper
import org.ethereum.core.PendingState
import org.ethereum.core.Transaction
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.facade.Ethereum
import org.ethereum.net.message.ReasonCode
import org.ethereum.net.message.ReasonCode.DUPLICATE_PEER
import org.ethereum.net.message.ReasonCode.TOO_MANY_PEERS
import org.ethereum.net.rlpx.Node
import org.ethereum.sync.SyncManager
import org.ethereum.sync.SyncPool
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.util.*
import java.util.concurrent.*

/**
 * @author Roman Mandeleil
 * *
 * @since 11.11.2014
 */
@Component
open class ChannelManager @Autowired
private constructor(config: SystemProperties, private val syncManager: SyncManager,
                    private val peerServer: PeerServer) {
    private val activePeers = ConcurrentHashMap<ByteArrayWrapper, Channel>()
    private val rnd = Random()  // Used for distributing new blocks / hashes logic
    private val newPeers = CopyOnWriteArrayList<Channel>()
    private val mainWorker = Executors.newSingleThreadScheduledExecutor()
    private val maxActivePeers: Int
    private val recentlyDisconnected = Collections.synchronizedMap(LRUMap<InetAddress, Date>(500))
    private val trustedPeers: NodeFilter
    /**
     * Queue with new blocks from other peers
     */
    private val newForeignBlocks = LinkedBlockingQueue<BlockWrapper>()
    /**
     * Queue with new peers used for after channel init tasks
     */
    private val newActivePeers = LinkedBlockingQueue<Channel>()
    private val blockDistributeThread: Thread?
    private val txDistributeThread: Thread?
    @Autowired
    private val syncPool: SyncPool? = null
    @Autowired
    private val ethereum: Ethereum? = null
    @Autowired
    private val pendingState: PendingState? = null

    init {
        val config1 = config
        maxActivePeers = config.maxActivePeers()
        trustedPeers = config.peerTrusted()
        mainWorker.scheduleWithFixedDelay({
            try {
                processNewPeers()
            } catch (t: Throwable) {
                logger.error("Error", t)
            }
        }, 0, 1, TimeUnit.SECONDS)

        if (config.listenPort() > 0) {
            Thread({ peerServer.start(config.listenPort()) },
                    "PeerServerThread").start()
        }

        // Resending new blocks to network in loop
        this.blockDistributeThread = Thread(Runnable { this.newBlocksDistributeLoop() }, "NewSyncThreadBlocks")
        this.blockDistributeThread.start()

        // Resending pending txs to newly connected peers
        this.txDistributeThread = Thread(Runnable { this.newTxDistributeLoop() }, "NewPeersThread")
        this.txDistributeThread.start()
    }

    fun connect(node: Node) {
        if (logger.isTraceEnabled)
            logger.trace(
                    "Peer {}: initiate connection",
                    node.hexIdShort
            )
        if (nodesInUse().contains(node.hexId)) {
            if (logger.isTraceEnabled)
                logger.trace(
                        "Peer {}: connection already initiated",
                        node.hexIdShort
                )
            return
        }

        ethereum!!.connect(node)
    }

    private fun nodesInUse(): Set<String> {
        val ids = getActivePeers()
                .asSequence()
                .map { it.peerId }
                .toMutableSet()
        newPeers.mapTo(ids) { it.peerId }
        return ids
    }

    private fun processNewPeers() {
        if (newPeers.isEmpty()) return

        val processed = ArrayList<Channel>()

        var addCnt = 0
        for (peer in newPeers) {

            logger.debug("Processing new peer: " + peer)

            if (peer.isProtocolsInitialized) {

                logger.debug("Protocols initialized")

                if (!activePeers.containsKey(peer.nodeIdWrapper)) {
                    if (!peer.isActive &&
                            activePeers.size >= maxActivePeers &&
                            !trustedPeers.accept(peer.node!!)) {

                        // restricting inbound connections unless this is a trusted peer

                        disconnect(peer, TOO_MANY_PEERS)
                    } else {
                        process(peer)
                        addCnt++
                    }
                } else {
                    disconnect(peer, DUPLICATE_PEER)
                }

                processed.add(peer)
            }
        }

        if (addCnt > 0) {
            logger.info("New peers processed: " + processed + ", active peers added: " + addCnt + ", total active peers: " + activePeers.size)
        }

        newPeers.removeAll(processed)
    }

    private fun disconnect(peer: Channel, reason: ReasonCode) {
        logger.debug("Disconnecting peer with reason $reason: $peer")
        peer.disconnect(reason)
        recentlyDisconnected.put(peer.inetSocketAddress!!.address, Date())
    }

    fun isRecentlyDisconnected(peerAddr: InetAddress): Boolean {
        val disconnectTime = recentlyDisconnected[peerAddr]
        if (disconnectTime != null && System.currentTimeMillis() - disconnectTime.time < inboundConnectionBanTimeout) {
            return true
        } else {
            recentlyDisconnected.remove(peerAddr)
            return false
        }
    }

    private fun process(peer: Channel) {
        if (peer.hasEthStatusSucceeded()) {
            // prohibit transactions processing until main sync is done
            if (syncManager.isSyncDone) {
                peer.onSyncDone(true)
                // So we could perform some tasks on recently connected peer
                newActivePeers.add(peer)
            }
            activePeers.put(peer.nodeIdWrapper!!, peer)
        }
    }

    /**
     * Propagates the transactions message across active peers with exclusion of
     * 'receivedFrom' peer.
     * @param tx  transactions to be sent
     * *
     * @param receivedFrom the peer which sent original message or null if
     * *                     the transactions were originated by this peer
     */
    fun sendTransaction(tx: List<Transaction>, receivedFrom: Channel) {
        activePeers.values
                .asSequence()
                .filter { it !== receivedFrom }
                .forEach { it.sendTransaction(tx) }
    }

    /**
     * Propagates the new block message across active peers
     * Suitable only for self-mined blocks
     * Use [.sendNewBlock] for sending blocks received from net
     * @param block  new Block to be sent
     */
    fun sendNewBlock(block: Block) {
        for (channel in activePeers.values) {
            channel.sendNewBlock(block)
        }
    }

    /**
     * Called on new blocks received from other peers
     * @param blockWrapper  Block with additional info
     */
    fun onNewForeignBlock(blockWrapper: BlockWrapper) {
        newForeignBlocks.add(blockWrapper)
    }

    /**
     * Processing new blocks received from other peers from queue
     */
    private fun newBlocksDistributeLoop() {
        while (!Thread.currentThread().isInterrupted) {
            var wrapper: BlockWrapper? = null
            try {
                wrapper = newForeignBlocks.take()
                val receivedFrom = getActivePeer(wrapper!!.nodeId)
                sendNewBlock(wrapper.block, receivedFrom)
            } catch (e: InterruptedException) {
                break
            } catch (e: Throwable) {
                if (wrapper != null) {
                    logger.error("Error broadcasting new block {}: ", wrapper.block.shortDescr, e)
                    logger.error("Block dump: {}", wrapper.block)
                } else {
                    logger.error("Error broadcasting unknown block", e)
                }
            }

        }
    }

    /**
     * Sends all pending txs to new active peers
     */
    private fun newTxDistributeLoop() {
        while (!Thread.currentThread().isInterrupted) {
            var channel: Channel? = null
            try {
                channel = newActivePeers.take()
                val pendingTransactions = pendingState!!.pendingTransactions
                if (!pendingTransactions.isEmpty()) {
                    channel!!.sendTransaction(pendingTransactions)
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Throwable) {
                if (channel != null) {
                    logger.error("Error sending transactions to peer {}: ", channel.node!!.hexIdShort, e)
                } else {
                    logger.error("Unknown error when sending transactions to new peer", e)
                }
            }

        }
    }

    /**
     * Propagates the new block message across active peers with exclusion of
     * 'receivedFrom' peer.
     * Distributes full block to 30% of peers and only its hash to remains
     * @param block  new Block to be sent
     * *
     * @param receivedFrom the peer which sent original message
     */
    private fun sendNewBlock(block: Block, receivedFrom: Channel) {
        activePeers.values
                .asSequence()
                .filter { it !== receivedFrom }
                .forEach {
                    if (rnd.nextInt(10) < 3) {  // 30%
                        it.sendNewBlock(block)
                    } else {                    // 70%
                        it.sendNewBlockHashes(block)
                    }
                }
    }

    fun add(peer: Channel) {
        logger.debug("New peer in ChannelManager {}", peer)
        newPeers.add(peer)
    }

    fun notifyDisconnect(channel: Channel) {
        logger.debug("Peer {}: notifies about disconnect", channel)
        channel.onDisconnect()
        syncPool!!.onDisconnect(channel)
        activePeers.values.remove(channel)
        newPeers.remove(channel)
    }

    fun onSyncDone(done: Boolean) {
        for (channel in activePeers.values)
            channel.onSyncDone(done)
    }

    fun getActivePeers(): Collection<Channel> {
        return ArrayList(activePeers.values)
    }

    fun getActivePeer(nodeId: ByteArray): Channel {
        return activePeers[ByteArrayWrapper(nodeId)]!!
    }

    fun close() {
        try {
            logger.info("Shutting down block and tx distribute threads...")
            blockDistributeThread?.interrupt()
            txDistributeThread?.interrupt()

            logger.info("Shutting down ChannelManager worker thread...")
            mainWorker.shutdownNow()
            mainWorker.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.warn("Problems shutting down", e)
        }

        peerServer.close()

        val allPeers = ArrayList(activePeers.values)
        allPeers.addAll(newPeers)

        for (channel in allPeers) {
            try {
                channel.dropConnection()
            } catch (e: Exception) {
                logger.warn("Problems disconnecting channel " + channel, e)
            }

        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger("net")

        // If the inbound peer connection was dropped by us with a reason message
        // then we ban that peer IP on any connections for some time to protect from
        // too active peers
        private val inboundConnectionBanTimeout = 10 * 1000
    }
}
