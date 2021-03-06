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

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.FixedRecvByteBufAllocator
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * @author Roman Mandeleil
 * *
 * @since 01.11.2014
 */
@Component
@Scope("prototype")
class EthereumChannelInitializer @Autowired
constructor(private val remoteId: String?, private val ctx: ApplicationContext, private val channelManager: ChannelManager) : ChannelInitializer<NioSocketChannel>() {
    private var peerDiscoveryMode = false

    @Throws(Exception::class)
    public override fun initChannel(ch: NioSocketChannel) {
        try {
            if (!peerDiscoveryMode) {
                logger.debug("Open {} connection, channel: {}", if (isInbound) "inbound" else "outbound", ch.toString())
            }

            if (isInbound && channelManager.isRecentlyDisconnected(ch.remoteAddress().address)) {
                // avoid too frequent connection attempts
                logger.debug("Drop connection - the same IP was disconnected recently, channel: {}", ch.toString())
                ch.disconnect()
                return
            }

            val channel = ctx.getBean(Channel::class.java)
            channel.init(ch.pipeline(), remoteId, peerDiscoveryMode, channelManager)

            if (!peerDiscoveryMode) {
                channelManager.add(channel)
            }

            // limit the size of receiving buffer to 1024
            ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(256 * 1024)
            ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024)
            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024)

            // be aware of channel closing
            ch.closeFuture().addListener({ ->
                if (!peerDiscoveryMode) {
                    channelManager.notifyDisconnect(channel)
                }
            } as ChannelFutureListener)

        } catch (e: Exception) {
            logger.error("Unexpected error: ", e)
        }

    }

    private val isInbound: Boolean
        get() = remoteId == null || remoteId.isEmpty()

    fun setPeerDiscoveryMode(peerDiscoveryMode: Boolean) {
        this.peerDiscoveryMode = peerDiscoveryMode
    }

    companion object {

        private val logger = LoggerFactory.getLogger("net")
    }
}
