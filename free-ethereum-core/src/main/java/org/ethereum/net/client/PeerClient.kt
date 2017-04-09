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

package org.ethereum.net.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.ethereum.config.SystemProperties
import org.ethereum.listener.EthereumListener
import org.ethereum.net.server.EthereumChannelInitializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger


/**
 * This class creates the connection to an remote address using the Netty framework

 * @see [http://netty.io](http://netty.io)
 */
@Component
class PeerClient {
    private val workerGroup: EventLoopGroup
    @Autowired
    private val config: SystemProperties? = null
    @Autowired
    private val ctx: ApplicationContext? = null
    @Autowired
    private val ethereumListener: EthereumListener? = null

    init {
        workerGroup = NioEventLoopGroup(0, object : ThreadFactory {
            internal val cnt = AtomicInteger(0)
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "EthJClientWorker-" + cnt.getAndIncrement())
            }
        })
    }

    /**
     * Connects to the node and returns only upon connection close
     */
    @JvmOverloads fun connect(host: String, port: Int, remoteId: String, discoveryMode: Boolean = false) {
        try {
            val f = connectAsync(host, port, remoteId, discoveryMode)

            f.sync()

            // Wait until the connection is closed.
            f.channel().closeFuture().sync()

            logger.debug("Connection is closed")

        } catch (e: Exception) {
            if (discoveryMode) {
                logger.trace("Exception:", e)
            } else {
                if (e is IOException) {
                    logger.info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.message + ")")
                    logger.debug("PeerClient.connect($host:$port) exception:", e)
                } else {
                    logger.error("Exception:", e)
                }
            }
        }

    }

    fun connectAsync(host: String, port: Int, remoteId: String, discoveryMode: Boolean): ChannelFuture {
        ethereumListener!!.trace("Connecting to: $host:$port")

        val ethereumChannelInitializer = ctx!!.getBean(EthereumChannelInitializer::class.java, remoteId)
        ethereumChannelInitializer.setPeerDiscoveryMode(discoveryMode)

        val b = Bootstrap()
        b.group(workerGroup)
        b.channel(NioSocketChannel::class.java)

        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.option<MessageSizeEstimator>(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config!!.peerConnectionTimeout())
        b.remoteAddress(host, port)

        b.handler(ethereumChannelInitializer)

        // Start the client.
        return b.connect()
    }

    fun close() {
        logger.info("Shutdown peerClient")
        workerGroup.shutdownGracefully()
        workerGroup.terminationFuture().syncUninterruptibly()
    }

    companion object {

        private val logger = LoggerFactory.getLogger("net")
    }
}
