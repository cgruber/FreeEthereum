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

package org.ethereum.samples;

import com.google.common.base.Joiner;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.rlpx.discover.table.NodeEntry;
import org.ethereum.net.server.Channel;
import org.ethereum.net.server.ChannelManager;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Testing peers discovery.
 *
 * The sample creates a small private net with three peers:
 *  - first is point for discovery;
 *  - two other ones will connect to first.
 *
 * Peers run on same IP 127.0.0.1 and are different by ports.
 *
 * After some time all peers should find each other. We use `peer.discovery.ip.list` config
 * option to point to discovery peer.
 *
 * Created by Stan Reshetnyk on 06.10.2016.
 */
public class PrivateNetworkDiscoverySample {

    /**
     *  Creating 3 instances with different config classes
     */
    public static void main(final String[] args) throws Exception {
        BasicSample.sLogger.info("Starting main node to which others will connect to");
        EthereumFactory.INSTANCE.createEthereum(Node0Config.class);

        BasicSample.sLogger.info("Starting regular instance 1!");
        EthereumFactory.INSTANCE.createEthereum(Node1Config.class);

        BasicSample.sLogger.info("Starting regular instance 2!");
        EthereumFactory.INSTANCE.createEthereum(Node2Config.class);
    }

    private static Config getConfig(final int index, final String discoveryNode) {
        return ConfigFactory.empty()
                .withValue("peer.discovery.enabled", value(true))
                .withValue("peer.discovery.external.ip", value("127.0.0.1"))
                .withValue("peer.discovery.bind.ip", value("127.0.0.1"))
                .withValue("peer.discovery.persist", value("false"))

                .withValue("peer.listen.port", value(20000 + index))
                .withValue("peer.privateKey", value(Hex.toHexString(ECKey.fromPrivate(("" + index).getBytes()).getPrivKeyBytes())))
                .withValue("peer.networkId", value(555))
                .withValue("sync.enabled", value(true))
                .withValue("database.incompatibleDatabaseBehavior", value("RESET"))
                .withValue("genesis", value("sample-genesis.json"))
                .withValue("database.dir", value("sampleDB-" + index))
                .withValue("peer.discovery.ip.list", value(discoveryNode != null ? Collections.singletonList(discoveryNode) : Collections.emptyList()));
    }

    private static ConfigValue value(final Object value) {
        return ConfigValueFactory.fromAnyRef(value);
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private static class RegularConfig {

        private final String discoveryNode;

        private final int nodeIndex;

        public RegularConfig(final int nodeIndex, final String discoveryNode) {
            this.nodeIndex = nodeIndex;
            this.discoveryNode = discoveryNode;
        }

        @Bean
        public BasicSample node() {
            return new BasicSample("sampleNode-" + nodeIndex) {

                ChannelManager channelManager;

                NodeManager nodeManager;

                {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5000);
                                while (true) {
                                    if (logger != null) {
                                        Thread.sleep(15000);
                                        if (channelManager != null) {
                                            final Collection<Channel> activePeers = channelManager.getActivePeers();
                                            final ArrayList<String> ports = new ArrayList<>();
                                            for (final Channel channel : activePeers) {
                                                ports.add(channel.getInetSocketAddress().getHostName() + ":" + channel.getInetSocketAddress().getPort());
                                            }

                                            final Collection<NodeEntry> nodes = nodeManager.getTable().getAllNodes();
                                            final ArrayList<String> nodesString = new ArrayList<>();
                                            for (final NodeEntry node : nodes) {
                                                nodesString.add(node.getNode().getHost() + ":" + node.getNode().getPort() + "@" + node.getNode().getHexId().substring(0, 6) );
                                            }

                                            logger.info("channelManager.getActivePeers() " + activePeers.size() + " " + Joiner.on(", ").join(ports));
                                            logger.info("nodeManager.getTable().getAllNodes() " + nodesString.size() + " " + Joiner.on(", ").join(nodesString));
                                        } else {
                                            logger.info("Channel manager is null");
                                        }
                                    } else {
                                        System.err.println("Logger is null for " + nodeIndex);
                                    }
                                }
                            } catch (final Exception e) {
                                logger.error("Error checking peers count: ", e);
                            }
                        }
                    }).start();
                }


                @Override
                public void onSyncDone() {
                    logger.info("onSyncDone");


                }
            };
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        public SystemProperties systemProperties() {
            return new SystemProperties(getConfig(nodeIndex, discoveryNode));
        }
    }

    public static class Node0Config extends RegularConfig {

        public Node0Config() {
            super(0, null);
        }

        @Bean
        public SystemProperties systemProperties() {
            return super.systemProperties();
        }

        @Bean
        public BasicSample node() {
            return super.node();
        }
    }

    private static class Node1Config extends RegularConfig {

        public Node1Config() {
            super(1, "127.0.0.1:20000");
        }

        @Bean
        public SystemProperties systemProperties() {
            return super.systemProperties();
        }

        @Bean
        public BasicSample node() {
            return super.node();
        }
    }

    private static class Node2Config extends RegularConfig{

        public Node2Config() {
            super(2, "127.0.0.1:20000");
        }

        @Bean
        public SystemProperties systemProperties() {
            return super.systemProperties();
        }

        @Bean
        public BasicSample node() {
            return super.node();
        }
    }
}
