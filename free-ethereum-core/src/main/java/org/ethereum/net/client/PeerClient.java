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

package org.ethereum.net.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.ethereum.config.SystemProperties;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.server.EthereumChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class creates the connection to an remote address using the Netty framework
 *
 * @see <a href="http://netty.io">http://netty.io</a>
 */
@Component
public class PeerClient {

    private static final Logger logger = LoggerFactory.getLogger("net");
    private final EventLoopGroup workerGroup;
    @Autowired
    private
    SystemProperties config;
    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private
    EthereumListener ethereumListener;

    public PeerClient() {
        workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
            final AtomicInteger cnt = new AtomicInteger(0);
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "EthJClientWorker-" + cnt.getAndIncrement());
            }
        });
    }

    public void connect(final String host, final int port, final String remoteId) {
        connect(host, port, remoteId, false);
    }

    /**
     *  Connects to the node and returns only upon connection close
     */
    public void connect(final String host, final int port, final String remoteId, final boolean discoveryMode) {
        try {
            final ChannelFuture f = connectAsync(host, port, remoteId, discoveryMode);

            f.sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

            logger.debug("Connection is closed");

        } catch (final Exception e) {
            if (discoveryMode) {
                logger.trace("Exception:", e);
            } else {
                if (e instanceof IOException) {
                    logger.info("PeerClient: Can't connect to " + host + ":" + port + " (" + e.getMessage() + ")");
                    logger.debug("PeerClient.connect(" + host + ":" + port + ") exception:", e);
                } else {
                    logger.error("Exception:", e);
                }
            }
        }
    }

    public ChannelFuture connectAsync(final String host, final int port, final String remoteId, final boolean discoveryMode) {
        ethereumListener.trace("Connecting to: " + host + ":" + port);

        final EthereumChannelInitializer ethereumChannelInitializer = ctx.getBean(EthereumChannelInitializer.class, remoteId);
        ethereumChannelInitializer.setPeerDiscoveryMode(discoveryMode);

        final Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.peerConnectionTimeout());
        b.remoteAddress(host, port);

        b.handler(ethereumChannelInitializer);

        // Start the client.
        return b.connect();
    }

    public void close() {
        logger.info("Shutdown peerClient");
        workerGroup.shutdownGracefully();
        workerGroup.terminationFuture().syncUninterruptibly();
    }
}
