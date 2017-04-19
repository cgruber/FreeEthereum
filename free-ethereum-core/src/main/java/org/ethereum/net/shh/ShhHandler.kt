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

package org.ethereum.net.shh

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.ethereum.listener.EthereumListener
import org.ethereum.net.MessageQueue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Process the messages between peers with 'shh' capability on the network.

 * Peers with 'shh' capability can send/receive:
 */
@Component
@Scope("prototype")
class ShhHandler : SimpleChannelInboundHandler<ShhMessage> {
    private var msgQueue: MessageQueue? = null
    private var isActive = false

    @Autowired
    private val ethereumListener: EthereumListener? = null

    @Autowired
    private val whisper: WhisperImpl? = null

    constructor()

    constructor(msgQueue: MessageQueue) {
        this.msgQueue = msgQueue
    }

    @Throws(InterruptedException::class)
    public override fun channelRead0(ctx: ChannelHandlerContext, msg: ShhMessage) {

        if (!isActive) return

        if (ShhMessageCodes.inRange(msg.command.asByte()))
            logger.info("ShhHandler invoke: [{}]", msg.command)

        ethereumListener!!.trace(String.format("ShhHandler invoke: [%s]", msg.command))

        when (msg.command) {
            ShhMessageCodes.STATUS -> ethereumListener.trace("[Recv: $msg]")
            ShhMessageCodes.MESSAGE -> whisper!!.processEnvelope(msg as ShhEnvelopeMessage, this)
            ShhMessageCodes.FILTER -> setBloomFilter(msg as ShhFilterMessage)
            else -> logger.error("Unknown SHH message type: " + msg.command)
        }
    }

    private fun setBloomFilter(msg: ShhFilterMessage) {
        val peerBloomFilter = BloomFilter(msg.bloomFilter)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error("Shh handling failed", cause)
        super.exceptionCaught(ctx, cause)
        ctx.close()
    }

    @Throws(Exception::class)
    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
        isActive = false
        whisper!!.removePeer(this)
        logger.debug("handlerRemoved: ... ")
    }

    fun activate() {
        logger.info("SHH protocol activated")
        ethereumListener!!.trace("SHH protocol activated")
        whisper!!.addPeer(this)
        sendStatus()
        sendHostBloom()
        this.isActive = true
    }

    private fun sendStatus() {
        val protocolVersion = ShhHandler.VERSION
        val msg = ShhStatusMessage(protocolVersion)
        sendMessage(msg)
    }

    fun sendHostBloom() {
        val msg = ShhFilterMessage.createFromFilter(whisper!!.hostBloomFilter.toBytes())
        sendMessage(msg)
    }

    fun sendEnvelope(env: ShhEnvelopeMessage) {
        sendMessage(env)
        //        Topic[] topics = env.getTopics();
        //        for (Topic topic : topics) {
        //            if (peerBloomFilter.hasTopic(topic)) {
        //                sendMessage(env);
        //                break;
        //            }
        //        }
    }

    private fun sendMessage(msg: ShhMessage) {
        msgQueue!!.sendMessage(msg)
    }

    fun setMsgQueue(msgQueue: MessageQueue) {
        this.msgQueue = msgQueue
    }

    companion object {
        val VERSION: Byte = 3
        private val logger = LoggerFactory.getLogger("net.shh")
    }
}