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

package org.ethereum.net.rlpx.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.ethereum.config.SystemProperties;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.WireTrafficStats;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class UDPListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");
    private final int port;
    private final String address;
    @Autowired
    private
    SystemProperties config = SystemProperties.getDefault();
    @Autowired
    private
    WireTrafficStats stats;
    private String[] bootPeers;
    @Autowired
    private NodeManager nodeManager;
    private Channel channel;
    private volatile boolean shutdown = false;
    private DiscoveryExecutor discoveryExecutor;

    @Autowired
    public UDPListener(final SystemProperties config, final NodeManager nodeManager) {
        this.config = config;
        this.nodeManager = nodeManager;

        this.address = config.bindIp();
        port = config.listenPort();
        if (config.peerDiscovery()) {
            bootPeers = config.peerDiscoveryIPList().toArray(new String[0]);
        }
        if (config.peerDiscovery()) {
            if (port == 0) {
                logger.error("Discovery can't be started while listen port == 0");
            } else {
                new Thread("UDPListener") {
                    @Override
                    public void run() {
                        try {
                            UDPListener.this.start(bootPeers);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }.start();
            }
        }
    }

    private UDPListener(final String address, final int port) {
        this.address = address;
        this.port = port;
    }

    public static Node parseNode(final String s) {
        final int idx1 = s.indexOf('@');
        final int idx2 = s.indexOf(':');
        final String id = s.substring(0, idx1);
        final String host = s.substring(idx1 + 1, idx2);
        final int port = Integer.parseInt(s.substring(idx2 + 1));
        return new Node(Hex.decode(id), host, port);
    }

    public static void main(final String[] args) throws Exception {
        String address = "0.0.0.0";
        int port = 30303;
        if (args.length >= 2) {
            address = args[0];
            port = Integer.parseInt(args[1]);
        }
        new UDPListener(address, port).start(Arrays.copyOfRange(args, 2, args.length));
    }

    private void start(final String[] args) throws Exception {

        logger.info("Discovery UDPListener started");
        final NioEventLoopGroup group = new NioEventLoopGroup(1);

        final List<Node> bootNodes = new ArrayList<>();

        for (final String boot : args) {
            // since discover IP list has no NodeIds we will generate random but persistent
            bootNodes.add(Node.instanceOf(boot));
        }

        nodeManager.setBootNodes(bootNodes);


        try {
            discoveryExecutor = new DiscoveryExecutor(nodeManager);
            discoveryExecutor.start();

            while (!shutdown) {
                final Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            public void initChannel(final NioDatagramChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(stats.udp);
                                ch.pipeline().addLast(new PacketDecoder());
                                final MessageHandler messageHandler = new MessageHandler(ch, nodeManager);
                                nodeManager.setMessageSender(messageHandler);
                                ch.pipeline().addLast(messageHandler);
                            }
                        });

                channel = b.bind(address, port).sync().channel();

                channel.closeFuture().sync();
                if (shutdown) {
                    logger.info("Shutdown discovery UDPListener");
                    break;
                }
                logger.warn("UDP channel closed. Recreating after 5 sec pause...");
                Thread.sleep(5000);
            }
        } catch (final Exception e) {
            if (e instanceof BindException && e.getMessage().contains("Address already in use")) {
                logger.error("Port " + port + " is busy. Check if another instance is running with the same port.");
            } else {
                logger.error("Can't start discover: ", e);
            }
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void close() {
        logger.info("Closing UDPListener...");
        shutdown = true;
        if (channel != null) {
            try {
                channel.close().await(10, TimeUnit.SECONDS);
            } catch (final Exception e) {
                logger.warn("Problems closing UDPListener", e);
            }
        }

        if (discoveryExecutor != null) {
            try {
                discoveryExecutor.close();
            } catch (final Exception e) {
                logger.warn("Problems closing DiscoveryExecutor", e);
            }
        }
    }
}
