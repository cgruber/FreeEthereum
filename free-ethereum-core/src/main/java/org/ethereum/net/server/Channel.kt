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

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.timeout.ReadTimeoutHandler
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.BlockHeaderWrapper
import org.ethereum.core.Transaction
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.net.MessageQueue
import org.ethereum.net.client.Capability
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.eth.handler.Eth
import org.ethereum.net.eth.handler.EthAdapter
import org.ethereum.net.eth.handler.EthHandlerFactory
import org.ethereum.net.eth.message.Eth62MessageFactory
import org.ethereum.net.eth.message.Eth63MessageFactory
import org.ethereum.net.message.MessageFactory
import org.ethereum.net.message.ReasonCode
import org.ethereum.net.message.StaticMessages
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.p2p.P2pHandler
import org.ethereum.net.p2p.P2pMessageFactory
import org.ethereum.net.rlpx.*
import org.ethereum.net.rlpx.discover.NodeManager
import org.ethereum.net.rlpx.discover.NodeStatistics
import org.ethereum.net.shh.ShhHandler
import org.ethereum.net.shh.ShhMessageFactory
import org.ethereum.net.swarm.bzz.BzzHandler
import org.ethereum.net.swarm.bzz.BzzMessageFactory
import org.ethereum.sync.SyncStatistics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.IOException
import java.math.BigInteger
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * @author Roman Mandeleil
 * *
 * @since 01.11.2014
 */
@Component
@Scope("prototype")
open class Channel {
    val peerStats = PeerStatistics()
    @Autowired
    private var config: SystemProperties? = null
    @Autowired
    private val msgQueue: MessageQueue? = null
    @Autowired
    private val p2pHandler: P2pHandler? = null
    @Autowired
    private val shhHandler: ShhHandler? = null
    @Autowired
    private val bzzHandler: BzzHandler? = null
    @Autowired
    private var messageCodec: MessageCodec? = null
    @Autowired
    private val handshakeHandler: HandshakeHandler? = null
    @Autowired
    private val nodeManager: NodeManager? = null
    @Autowired
    private val ethHandlerFactory: EthHandlerFactory? = null
    @Autowired
    private val staticMessages: StaticMessages? = null
    @Autowired
    private val stats: WireTrafficStats? = null
    var channelManager: ChannelManager? = null
        private set
    var ethHandler: Eth = EthAdapter()
        private set
    var inetSocketAddress: InetSocketAddress? = null
    var node: Node? = null
        private set
    var nodeStatistics: NodeStatistics? = null
        private set
    var isDiscoveryMode: Boolean = false
        private set
    /**
     * Indicates whether this connection was initiated by our peer
     */
    var isActive: Boolean = false
        private set
    var isDisconnected: Boolean = false
        private set
    private var remoteId: String? = null

    fun init(pipeline: ChannelPipeline, remoteId: String?, discoveryMode: Boolean, channelManager: ChannelManager) {
        this.channelManager = channelManager
        this.remoteId = remoteId

        isActive = remoteId != null && !remoteId.isEmpty()

        pipeline.addLast("readTimeoutHandler",
                ReadTimeoutHandler(config!!.peerChannelReadTimeout()!!.toLong(), TimeUnit.SECONDS))
        pipeline.addLast(stats!!.tcp)
        pipeline.addLast("handshakeHandler", handshakeHandler)

        this.isDiscoveryMode = discoveryMode

        if (discoveryMode) {
            // temporary key/nodeId to not accidentally smear our reputation with
            // unexpected disconnect
            //            handshakeHandler.generateTempKey();
        }

        handshakeHandler!!.setRemoteId(remoteId, this)

        messageCodec!!.setChannel(this)

        msgQueue!!.setChannel(this)

        p2pHandler!!.setMsgQueue(msgQueue)
        messageCodec!!.setP2pMessageFactory(P2pMessageFactory())

        shhHandler!!.setMsgQueue(msgQueue)
        messageCodec!!.setShhMessageFactory(ShhMessageFactory())

        bzzHandler!!.setMsgQueue(msgQueue)
        messageCodec!!.setBzzMessageFactory(BzzMessageFactory())
    }

    @Throws(IOException::class, InterruptedException::class)
    fun publicRLPxHandshakeFinished(ctx: ChannelHandlerContext, frameCodec: FrameCodec,
                                    helloRemote: HelloMessage) {

        logger.debug("publicRLPxHandshakeFinished with " + ctx.channel().remoteAddress())
        if (P2pHandler.isProtocolVersionSupported(helloRemote.p2PVersion)) {

            if (helloRemote.p2PVersion < 5) {
                messageCodec!!.setSupportChunkedFrames(false)
            }

            val frameCodecHandler = FrameCodecHandler(frameCodec, this)
            ctx.pipeline().addLast("medianFrameCodec", frameCodecHandler)
            ctx.pipeline().addLast("messageCodec", messageCodec)
            ctx.pipeline().addLast(Capability.P2P, p2pHandler)

            p2pHandler!!.setChannel(this)
            p2pHandler.setHandshake(helloRemote, ctx)

            nodeStatistics!!.rlpxHandshake.add()
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: String,
                         inboundHelloMessage: HelloMessage?) {

        val helloMessage = staticMessages!!.createHelloMessage(nodeId)

        if (inboundHelloMessage != null && P2pHandler.isProtocolVersionSupported(inboundHelloMessage.p2PVersion)) {
            // the p2p version can be downgraded if requested by peer and supported by us
            helloMessage.setP2pVersion(inboundHelloMessage.p2PVersion)
        }

        val byteBufMsg = ctx.alloc().buffer()
        frameCodec.writeFrame(FrameCodec.Frame(helloMessage.code.toInt(), helloMessage.encoded), byteBufMsg)
        ctx.writeAndFlush(byteBufMsg).sync()

        if (logger.isDebugEnabled)
            logger.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), helloMessage)
        nodeStatistics!!.rlpxOutHello.add()
    }

    fun activateEth(ctx: ChannelHandlerContext, version: EthVersion) {
        val handler = ethHandlerFactory!!.create(version)
        val messageFactory = createEthMessageFactory(version)
        messageCodec!!.setEthVersion(version)
        messageCodec!!.setEthMessageFactory(messageFactory)

        logger.debug("Eth{} [ address = {} | id = {} ]", handler.version, inetSocketAddress, peerIdShort)

        ctx.pipeline().addLast(Capability.ETH, handler)

        handler.setMsgQueue(msgQueue)
        handler.setChannel(this)
        handler.setPeerDiscoveryMode(isDiscoveryMode)

        handler.activate()

        ethHandler = handler
    }

    private fun createEthMessageFactory(version: EthVersion): MessageFactory {
        when (version) {
            EthVersion.V62 -> return Eth62MessageFactory()
            EthVersion.V63 -> return Eth63MessageFactory()
            else -> throw IllegalArgumentException("Eth $version is not supported")
        }
    }

    fun activateShh(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(Capability.SHH, shhHandler)
        shhHandler!!.activate()
    }

    fun activateBzz(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(Capability.BZZ, bzzHandler)
        bzzHandler!!.activate()
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    @JvmOverloads fun initWithNode(nodeId: ByteArray, remotePort: Int = inetSocketAddress!!.port) {
        node = Node(nodeId, inetSocketAddress!!.hostString, remotePort)
        nodeStatistics = nodeManager!!.getNodeStatistics(node)
    }

    fun initMessageCodes(caps: List<Capability>) {
        messageCodec!!.initMessageCodes(caps)
    }

    val isProtocolsInitialized: Boolean
        get() = ethHandler.hasStatusPassed()

    fun onDisconnect() {
        isDisconnected = true
    }

    fun onSyncDone(done: Boolean) {

        if (done) {
            ethHandler.enableTransactions()
        } else {
            ethHandler.disableTransactions()
        }

        ethHandler.onSyncDone(done)
    }

    val peerId: String
        get() = if (node == null) "<null>" else node!!.hexId

    val peerIdShort: String
        get() = if (node == null)
            if (remoteId != null && remoteId!!.length >= 8) remoteId!!.substring(0, 8) else remoteId!!
        else
            node!!.hexIdShort

    val nodeId: ByteArray?
        get() = if (node == null) null else node!!.id

    val nodeIdWrapper: ByteArrayWrapper?
        get() = if (node == null) null else ByteArrayWrapper(node!!.id)

    fun disconnect(reason: ReasonCode) {
        msgQueue!!.disconnect(reason)
    }

    // ETH sub protocol

    fun fetchBlockBodies(headers: List<BlockHeaderWrapper>) {
        ethHandler.fetchBodies(headers)
    }

    fun isEthCompatible(peer: Channel?): Boolean {
        return peer != null && peer.ethVersion.isCompatible(ethVersion)
    }

    fun hasEthStatusSucceeded(): Boolean {
        return ethHandler.hasStatusSucceeded()
    }

    fun logSyncStats(): String {
        return ethHandler.syncStats
    }

    val totalDifficulty: BigInteger
        get() = ethHandler.totalDifficulty

    val syncStats: SyncStatistics
        get() = ethHandler.stats

    val isHashRetrievingDone: Boolean
        get() = ethHandler.isHashRetrievingDone

    val isHashRetrieving: Boolean
        get() = ethHandler.isHashRetrieving

    val isMaster: Boolean
        get() = ethHandler.isHashRetrieving || ethHandler.isHashRetrievingDone

    val isIdle: Boolean
        get() = ethHandler.isIdle

    fun prohibitTransactionProcessing() {
        ethHandler.disableTransactions()
    }

    fun sendTransaction(tx: List<Transaction>) {
        ethHandler.sendTransaction(tx)
    }

    fun sendNewBlock(block: Block) {
        ethHandler.sendNewBlock(block)
    }

    fun sendNewBlockHashes(block: Block) {
        ethHandler.sendNewBlockHashes(block)
    }

    private val ethVersion: EthVersion
        get() = ethHandler.version

    fun dropConnection() {
        ethHandler.dropConnection()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val channel = o as Channel?


        if (if (inetSocketAddress != null) inetSocketAddress != channel!!.inetSocketAddress else channel!!.inetSocketAddress != null) return false
        if (if (node != null) node != channel.node else channel.node != null) return false
        return this === channel
    }

    override fun hashCode(): Int {
        var result = if (inetSocketAddress != null) inetSocketAddress!!.hashCode() else 0
        result = 31 * result + if (node != null) node!!.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return String.format("%s | %s", peerIdShort, inetSocketAddress)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("net")
    }
}
