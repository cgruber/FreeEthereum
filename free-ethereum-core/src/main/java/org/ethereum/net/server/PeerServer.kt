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

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultMessageSizeEstimator
import io.netty.channel.MessageSizeEstimator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import org.ethereum.config.SystemProperties
import org.ethereum.listener.EthereumListener
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * This class establishes a listener for incoming connections.
 * See [http://netty.io](http://netty.io).

 * @author Roman Mandeleil
 * *
 * @since 01.11.2014
 */
@Component
class PeerServer @Autowired
constructor(private val config: SystemProperties, private val ctx: ApplicationContext,
            private val ethereumListener: EthereumListener) {

    var isListening: Boolean = false
        private set

    private var channelFuture: ChannelFuture? = null

    fun start(port: Int) {

        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()

        val ethereumChannelInitializer = ctx.getBean(EthereumChannelInitializer::class.java, "")

        ethereumListener.trace("Listening on port " + port)


        try {
            val b = ServerBootstrap()

            b.group(bossGroup, workerGroup)
            b.channel(NioServerSocketChannel::class.java)

            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.option<MessageSizeEstimator>(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.peerConnectionTimeout())

            b.handler(LoggingHandler())
            b.childHandler(ethereumChannelInitializer)

            // Start the client.
            logger.info("Listening for incoming connections, port: [{}] ", port)
            logger.info("NodeId: [{}] ", Hex.toHexString(config.nodeId()))

            channelFuture = b.bind(port).sync()

            isListening = true
            // Wait until the connection is closed.
            channelFuture!!.channel().closeFuture().sync()
            logger.debug("Connection is closed")

        } catch (e: Exception) {
            logger.debug("Exception: {} ({})", e.message, e.javaClass.name)
            throw Error("Server Disconnected")
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
            isListening = false
        }
    }

    fun close() {
        if (isListening && channelFuture != null && channelFuture!!.channel().isOpen) {
            try {
                logger.info("Closing PeerServer...")
                channelFuture!!.channel().close().sync()
                logger.info("PeerServer closed.")
            } catch (e: Exception) {
                logger.warn("Problems closing server channel", e)
            }

        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger("net")
    }
}
