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

package org.ethereum.net.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.core.Transaction;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.net.MessageQueue;
import org.ethereum.net.client.Capability;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.handler.Eth;
import org.ethereum.net.eth.handler.EthAdapter;
import org.ethereum.net.eth.handler.EthHandler;
import org.ethereum.net.eth.handler.EthHandlerFactory;
import org.ethereum.net.eth.message.Eth62MessageFactory;
import org.ethereum.net.eth.message.Eth63MessageFactory;
import org.ethereum.net.message.MessageFactory;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.message.StaticMessages;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.p2p.P2pHandler;
import org.ethereum.net.p2p.P2pMessageFactory;
import org.ethereum.net.rlpx.*;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.rlpx.discover.NodeStatistics;
import org.ethereum.net.shh.ShhHandler;
import org.ethereum.net.shh.ShhMessageFactory;
import org.ethereum.net.swarm.bzz.BzzHandler;
import org.ethereum.net.swarm.bzz.BzzMessageFactory;
import org.ethereum.sync.SyncStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
@Scope("prototype")
public class Channel {

    private final static Logger logger = LoggerFactory.getLogger("net");
    private final PeerStatistics peerStats = new PeerStatistics();
    @Autowired
    private
    SystemProperties config;
    @Autowired
    private MessageQueue msgQueue;
    @Autowired
    private P2pHandler p2pHandler;
    @Autowired
    private ShhHandler shhHandler;
    @Autowired
    private BzzHandler bzzHandler;
    @Autowired
    private MessageCodec messageCodec;
    @Autowired
    private HandshakeHandler handshakeHandler;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private EthHandlerFactory ethHandlerFactory;
    @Autowired
    private StaticMessages staticMessages;
    @Autowired
    private WireTrafficStats stats;
    private ChannelManager channelManager;
    private Eth eth = new EthAdapter();
    private InetSocketAddress inetSocketAddress;
    private Node node;
    private NodeStatistics nodeStatistics;
    private boolean discoveryMode;
    private boolean isActive;
    private boolean isDisconnected;
    private String remoteId;

    public void init(final ChannelPipeline pipeline, final String remoteId, final boolean discoveryMode, final ChannelManager channelManager) {
        this.channelManager = channelManager;
        this.remoteId = remoteId;

        isActive = remoteId != null && !remoteId.isEmpty();

        pipeline.addLast("readTimeoutHandler",
                new ReadTimeoutHandler(config.peerChannelReadTimeout(), TimeUnit.SECONDS));
        pipeline.addLast(stats.tcp);
        pipeline.addLast("handshakeHandler", handshakeHandler);

        this.discoveryMode = discoveryMode;

        if (discoveryMode) {
            // temporary key/nodeId to not accidentally smear our reputation with
            // unexpected disconnect
//            handshakeHandler.generateTempKey();
        }

        handshakeHandler.setRemoteId(remoteId, this);

        messageCodec.setChannel(this);

        msgQueue.setChannel(this);

        p2pHandler.setMsgQueue(msgQueue);
        messageCodec.setP2pMessageFactory(new P2pMessageFactory());

        shhHandler.setMsgQueue(msgQueue);
        messageCodec.setShhMessageFactory(new ShhMessageFactory());

        bzzHandler.setMsgQueue(msgQueue);
        messageCodec.setBzzMessageFactory(new BzzMessageFactory());
    }

    public void publicRLPxHandshakeFinished(final ChannelHandlerContext ctx, final FrameCodec frameCodec,
                                            final HelloMessage helloRemote) throws IOException, InterruptedException {

        logger.debug("publicRLPxHandshakeFinished with " + ctx.channel().remoteAddress());
        if (P2pHandler.isProtocolVersionSupported(helloRemote.getP2PVersion())) {

            if (helloRemote.getP2PVersion() < 5) {
                messageCodec.setSupportChunkedFrames(false);
            }

            final FrameCodecHandler frameCodecHandler = new FrameCodecHandler(frameCodec, this);
            ctx.pipeline().addLast("medianFrameCodec", frameCodecHandler);
            ctx.pipeline().addLast("messageCodec", messageCodec);
            ctx.pipeline().addLast(Capability.Companion.getP2P(), p2pHandler);

            p2pHandler.setChannel(this);
            p2pHandler.setHandshake(helloRemote, ctx);

            getNodeStatistics().rlpxHandshake.add();
        }
    }

    public void sendHelloMessage(final ChannelHandlerContext ctx, final FrameCodec frameCodec, final String nodeId,
                                 final HelloMessage inboundHelloMessage) throws IOException, InterruptedException {

        final HelloMessage helloMessage = staticMessages.createHelloMessage(nodeId);

        if (inboundHelloMessage != null && P2pHandler.isProtocolVersionSupported(inboundHelloMessage.getP2PVersion())) {
            // the p2p version can be downgraded if requested by peer and supported by us
            helloMessage.setP2pVersion(inboundHelloMessage.getP2PVersion());
        }

        final ByteBuf byteBufMsg = ctx.alloc().buffer();
        frameCodec.writeFrame(new FrameCodec.Frame(helloMessage.getCode(), helloMessage.getEncoded()), byteBufMsg);
        ctx.writeAndFlush(byteBufMsg).sync();

        if (logger.isDebugEnabled())
            logger.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), helloMessage);
        getNodeStatistics().rlpxOutHello.add();
    }

    public void activateEth(final ChannelHandlerContext ctx, final EthVersion version) {
        final EthHandler handler = ethHandlerFactory.create(version);
        final MessageFactory messageFactory = createEthMessageFactory(version);
        messageCodec.setEthVersion(version);
        messageCodec.setEthMessageFactory(messageFactory);

        logger.debug("Eth{} [ address = {} | id = {} ]", handler.getVersion(), inetSocketAddress, getPeerIdShort());

        ctx.pipeline().addLast(Capability.Companion.getETH(), handler);

        handler.setMsgQueue(msgQueue);
        handler.setChannel(this);
        handler.setPeerDiscoveryMode(discoveryMode);

        handler.activate();

        eth = handler;
    }

    private MessageFactory createEthMessageFactory(final EthVersion version) {
        switch (version) {
            case V62:   return new Eth62MessageFactory();
            case V63:   return new Eth63MessageFactory();
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }

    public void activateShh(final ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(Capability.Companion.getSHH(), shhHandler);
        shhHandler.activate();
    }

    public void activateBzz(final ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(Capability.Companion.getBZZ(), bzzHandler);
        bzzHandler.activate();
    }

    public NodeStatistics getNodeStatistics() {
        return nodeStatistics;
    }

    /**
     * Set node and register it in NodeManager if it is not registered yet.
     */
    public void initWithNode(final byte[] nodeId, final int remotePort) {
        node = new Node(nodeId, inetSocketAddress.getHostString(), remotePort);
        nodeStatistics = nodeManager.getNodeStatistics(node);
    }

    public void initWithNode(final byte[] nodeId) {
        initWithNode(nodeId, inetSocketAddress.getPort());
    }

    public Node getNode() {
        return node;
    }

    public void initMessageCodes(final List<Capability> caps) {
        messageCodec.initMessageCodes(caps);
    }

    public boolean isProtocolsInitialized() {
        return eth.hasStatusPassed();
    }

    public void onDisconnect() {
        isDisconnected = true;
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    public void onSyncDone(final boolean done) {

        if (done) {
            eth.enableTransactions();
        } else {
            eth.disableTransactions();
        }

        eth.onSyncDone(done);
    }

    public boolean isDiscoveryMode() {
        return discoveryMode;
    }

    public String getPeerId() {
        return node == null ? "<null>" : node.getHexId();
    }

    public String getPeerIdShort() {
        return node == null ? (remoteId != null && remoteId.length() >= 8 ? remoteId.substring(0,8) :remoteId)
                : node.getHexIdShort();
    }

    public byte[] getNodeId() {
        return node == null ? null : node.getId();
    }

    /**
     * Indicates whether this connection was initiated by our peer
     */
    public boolean isActive() {
        return isActive;
    }

    public ByteArrayWrapper getNodeIdWrapper() {
        return node == null ? null : new ByteArrayWrapper(node.getId());
    }

    public void disconnect(final ReasonCode reason) {
        msgQueue.disconnect(reason);
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public void setInetSocketAddress(final InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    public PeerStatistics getPeerStats() {
        return peerStats;
    }

    // ETH sub protocol

    public void fetchBlockBodies(final List<BlockHeaderWrapper> headers) {
        eth.fetchBodies(headers);
    }

    public boolean isEthCompatible(final Channel peer) {
        return peer != null && peer.getEthVersion().isCompatible(getEthVersion());
    }

    public Eth getEthHandler() {
        return eth;
    }

    public boolean hasEthStatusSucceeded() {
        return eth.hasStatusSucceeded();
    }

    public String logSyncStats() {
        return eth.getSyncStats();
    }

    public BigInteger getTotalDifficulty() {
        return getEthHandler().getTotalDifficulty();
    }

    public SyncStatistics getSyncStats() {
        return eth.getStats();
    }

    public boolean isHashRetrievingDone() {
        return eth.isHashRetrievingDone();
    }

    public boolean isHashRetrieving() {
        return eth.isHashRetrieving();
    }

    public boolean isMaster() {
        return eth.isHashRetrieving() || eth.isHashRetrievingDone();
    }

    public boolean isIdle() {
        return eth.isIdle();
    }

    public void prohibitTransactionProcessing() {
        eth.disableTransactions();
    }

    public void sendTransaction(final List<Transaction> tx) {
        eth.sendTransaction(tx);
    }

    public void sendNewBlock(final Block block) {
        eth.sendNewBlock(block);
    }

    public void sendNewBlockHashes(final Block block) {
        eth.sendNewBlockHashes(block);
    }

    private EthVersion getEthVersion() {
        return eth.getVersion();
    }

    public void dropConnection() {
        eth.dropConnection();
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Channel channel = (Channel) o;


        if (inetSocketAddress != null ? !inetSocketAddress.equals(channel.inetSocketAddress) : channel.inetSocketAddress != null) return false;
        if (node != null ? !node.equals(channel.node) : channel.node != null) return false;
        return this == channel;
    }

    @Override
    public int hashCode() {
        int result = inetSocketAddress != null ? inetSocketAddress.hashCode() : 0;
        result = 31 * result + (node != null ? node.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", getPeerIdShort(), inetSocketAddress);
    }
}
