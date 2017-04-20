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

package org.ethereum.net.p2p;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.MessageQueue;
import org.ethereum.net.client.Capability;
import org.ethereum.net.client.ConfigCapabilities;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.NewBlockMessage;
import org.ethereum.net.eth.message.TransactionsMessage;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.message.StaticMessages;
import org.ethereum.net.server.Channel;
import org.ethereum.net.shh.ShhHandler;
import org.ethereum.net.swarm.Util;
import org.ethereum.net.swarm.bzz.BzzHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Process the basic protocol messages between every peer on the network.
 *
 * Peers can send/receive
 * <ul>
 *  <li>HELLO       :   Announce themselves to the network</li>
 *  <li>DISCONNECT  :   Disconnect themselves from the network</li>
 *  <li>GET_PEERS   :   Request a list of other knows peers</li>
 *  <li>PEERS       :   Send a list of known peers</li>
 *  <li>PING        :   Check if another peer is still alive</li>
 *  <li>PONG        :   Confirm that they themselves are still alive</li>
 * </ul>
 */
@Component
@Scope("prototype")
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

    public final static byte VERSION = 4;

    private final static byte[] SUPPORTED_VERSIONS = {4, 5};

    private final static Logger logger = LoggerFactory.getLogger("net");

    private static final ScheduledExecutorService pingTimer =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "P2pPingTimer"));
    private
    EthereumListener ethereumListener;
    private
    ConfigCapabilities configCapabilities;
    private 
    SystemProperties config;
    private MessageQueue msgQueue;
    private boolean peerDiscoveryMode = false;
    private HelloMessage handshakeHelloMessage = null;
    private int ethInbound;
    private int ethOutbound;
    private Channel channel;
    private ScheduledFuture<?> pingTask;


    public P2pHandler() {

        this.peerDiscoveryMode = false;
    }

    public P2pHandler(final MessageQueue msgQueue, final boolean peerDiscoveryMode) {
        this.msgQueue = msgQueue;
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    @Autowired
    public P2pHandler(EthereumListener ethereumListener, ConfigCapabilities configCapabilities, SystemProperties config) {
        this.ethereumListener = ethereumListener;
        this.configCapabilities = configCapabilities;
        this.config = config;
    }

    public static boolean isProtocolVersionSupported(final byte ver) {
        for (final byte v : SUPPORTED_VERSIONS) {
            if (v == ver) return true;
        }
        return false;
    }

    public void setPeerDiscoveryMode(final boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        logger.debug("P2P protocol activated");
        msgQueue.activate(ctx);
        ethereumListener.trace("P2P protocol activated");
        startTimers();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final P2pMessage msg) throws InterruptedException {

        if (P2pMessageCodes.inRange(msg.getCommand().asByte()))
            logger.trace("P2PHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("P2PHandler invoke: [%s]", msg.getCommand()));

        switch (msg.getCommand()) {
            case HELLO:
                msgQueue.receivedMessage(msg);
                setHandshake((HelloMessage) msg, ctx);
//                sendGetPeers();
                break;
            case DISCONNECT:
                msgQueue.receivedMessage(msg);
                channel.getNodeStatistics().nodeDisconnectedRemote(((DisconnectMessage) msg).getReason());
                processDisconnect(ctx, (DisconnectMessage) msg);
                break;
            case PING:
                msgQueue.receivedMessage(msg);
                ctx.writeAndFlush(StaticMessages.Companion.getPONG_MESSAGE());
                break;
            case PONG:
                msgQueue.receivedMessage(msg);
                channel.getNodeStatistics().lastPongReplyTime.set(Util.curTime());
                break;
            case PEERS:
                msgQueue.receivedMessage(msg);

                if (peerDiscoveryMode ||
                        !handshakeHelloMessage.getCapabilities().contains(Capability.Companion.getETH())) {
                    disconnect(ReasonCode.REQUESTED);
                    killTimers();
                    ctx.close().sync();
                    ctx.disconnect().sync();
                }
                break;
            default:
                ctx.fireChannelRead(msg);
                break;
        }
    }

    private void disconnect(final ReasonCode reasonCode) {
        msgQueue.sendMessage(new DisconnectMessage(reasonCode));
        channel.getNodeStatistics().nodeDisconnectedLocal(reasonCode);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        logger.debug("channel inactive: ", ctx.toString());
        this.killTimers();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        logger.warn("P2p handling failed", cause);
        ctx.close();
        killTimers();
    }

    private void processDisconnect(final ChannelHandlerContext ctx, final DisconnectMessage msg) {

        if (logger.isInfoEnabled() && msg.getReason() == ReasonCode.USELESS_PEER) {

            if (channel.getNodeStatistics().ethInbound.get() - ethInbound > 1 ||
                    channel.getNodeStatistics().ethOutbound.get() - ethOutbound > 1) {

                // it means that we've been disconnected
                // after some incorrect action from our peer
                // need to log this moment
                logger.debug("From: \t{}\t [DISCONNECT reason=BAD_PEER_ACTION]", channel);
            }
        }
        ctx.close();
        killTimers();
    }

    private void sendGetPeers() {
        msgQueue.sendMessage(StaticMessages.Companion.getGET_PEERS_MESSAGE());
    }

    public void setHandshake(final HelloMessage msg, final ChannelHandlerContext ctx) {

        channel.getNodeStatistics().setClientId(msg.getClientId());
        channel.getNodeStatistics().capabilities.clear();
        channel.getNodeStatistics().capabilities.addAll(msg.getCapabilities());

        this.ethInbound = (int) channel.getNodeStatistics().ethInbound.get();
        this.ethOutbound = (int) channel.getNodeStatistics().ethOutbound.get();

        this.handshakeHelloMessage = msg;
        if (!isProtocolVersionSupported(msg.getP2PVersion())) {
            disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
        }
        else {
            final List<Capability> capInCommon = getSupportedCapabilities(msg);
            channel.initMessageCodes(capInCommon);
            for (final Capability capability : capInCommon) {
                if (capability.getName().equals(Capability.Companion.getETH())) {

                    // Activate EthHandler for this peer
                    channel.activateEth(ctx, EthVersion.Companion.fromCode(capability.getVersion()));
                } else if
                        (capability.getName().equals(Capability.Companion.getSHH()) &&
                    capability.getVersion() == ShhHandler.Companion.getVERSION()) {

                    // Activate ShhHandler for this peer
                    channel.activateShh(ctx);
                } else if
                        (capability.getName().equals(Capability.Companion.getBZZ()) &&
                    capability.getVersion() == BzzHandler.VERSION) {

                    // Activate ShhHandler for this peer
                    channel.activateBzz(ctx);
                }
            }

            //todo calculate the Offsets
            ethereumListener.onHandShakePeer(channel, msg);

        }
    }

    /**
     * submit transaction to the network
     *
     * @param tx - fresh transaction object
     */
    public void sendTransaction(final Transaction tx) {

        final TransactionsMessage msg = new TransactionsMessage(tx);
        msgQueue.sendMessage(msg);
    }

    public void sendNewBlock(final Block block) {

        final NewBlockMessage msg = new NewBlockMessage(block, block.getDifficulty());
        msgQueue.sendMessage(msg);
    }

    public void sendDisconnect() {
        msgQueue.disconnect();
    }

    public HelloMessage getHandshakeHelloMessage() {
        return handshakeHelloMessage;
    }

    private void startTimers() {
        // sample for pinging in background
        pingTask = pingTimer.scheduleAtFixedRate(() -> {
            try {
                msgQueue.sendMessage(StaticMessages.Companion.getPING_MESSAGE());
            } catch (final Throwable t) {
                logger.error("Unhandled exception", t);
            }
        }, 2, config.getProperty("peer.p2p.pingInterval", 5L), TimeUnit.SECONDS);
    }

    private void killTimers() {
        pingTask.cancel(false);
        msgQueue.close();
    }

    public void setMsgQueue(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    private List<Capability> getSupportedCapabilities(final HelloMessage hello) {
        final List<Capability> configCaps = configCapabilities.getConfigCapabilities();
        final List<Capability> supported = new ArrayList<>();

        final List<Capability> eths = new ArrayList<>();

        for (final Capability cap : hello.getCapabilities()) {
            if (configCaps.contains(cap)) {
                if (cap.isEth()) {
                    eths.add(cap);
                } else {
                    supported.add(cap);
                }
            }
        }

        if (eths.isEmpty()) {
            return supported;
        }

        // we need to pick up
        // the most recent Eth version
        Capability highest = null;
        for (final Capability eth : eths) {
            if (highest == null || highest.getVersion() < eth.getVersion()) {
                highest = eth;
            }
        }

        supported.add(highest);
        return supported;
    }

}