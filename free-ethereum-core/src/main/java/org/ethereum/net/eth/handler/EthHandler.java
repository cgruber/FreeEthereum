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

package org.ethereum.net.eth.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.db.BlockStore;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.net.MessageQueue;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.net.eth.message.EthMessageCodes;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Process the messages between peers with 'eth' capability on the network<br>
 * Contains common logic to all supported versions
 * delegating version specific stuff to its descendants
 *
 */
public abstract class EthHandler extends SimpleChannelInboundHandler<EthMessage> implements Eth {

    private final static Logger logger = LoggerFactory.getLogger("net");
    private final EthVersion version;
    protected Blockchain blockchain;
    SystemProperties config;
    CompositeEthereumListener ethereumListener;
    Channel channel;
    boolean peerDiscoveryMode = false;
    boolean processTransactions = false;
    private MessageQueue msgQueue = null;
    private Block bestBlock;
    private final EthereumListener listener = new EthereumListenerAdapter() {
        @Override
        public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
            bestBlock = block;
        }
    };

    EthHandler(final EthVersion version) {
        this.version = version;
    }

    EthHandler(final EthVersion version, final SystemProperties config,
               final Blockchain blockchain, final BlockStore blockStore,
               final CompositeEthereumListener ethereumListener) {
        this.version = version;
        this.config = config;
        this.ethereumListener = ethereumListener;
        this.blockchain = blockchain;
        bestBlock = blockStore.getBestBlock();
        this.ethereumListener.addListener(listener);
        // when sync enabled we delay transactions processing until sync is complete
        processTransactions = !config.isSyncEnabled();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final EthMessage msg) throws InterruptedException {

        if (EthMessageCodes.Companion.inRange(msg.getCommand().asByte(), version))
            logger.trace("EthHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("EthHandler invoke: [%s]", msg.getCommand()));

        channel.getNodeStatistics().ethInbound.add();

        msgQueue.receivedMessage(msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        logger.warn("Eth handling failed", cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        logger.debug("handlerRemoved: kill timers in EthHandler");
        ethereumListener.removeListener(listener);
        onShutdown();
    }

    public void activate() {
        logger.debug("ETH protocol activated");
        ethereumListener.trace("ETH protocol activated");
        sendStatus();
    }

    void disconnect(final ReasonCode reason) {
        msgQueue.disconnect(reason);
        channel.getNodeStatistics().nodeDisconnectedLocal(reason);
    }

    protected void sendMessage(final EthMessage message) {
        msgQueue.sendMessage(message);
        channel.getNodeStatistics().ethOutbound.add();
    }

    public StatusMessage getHandshakeStatusMessage() {
        return channel.getNodeStatistics().getEthLastInboundStatusMsg();
    }

    public void setMsgQueue(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void setPeerDiscoveryMode(final boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public EthVersion getVersion() {
        return version;
    }

}