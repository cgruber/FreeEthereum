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

package org.ethereum.net.rlpx;

import com.google.common.io.ByteStreams;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutException;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECIESCoder;
import org.ethereum.crypto.ECKey;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.DisconnectMessage;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.p2p.P2pMessageCodes;
import org.ethereum.net.p2p.P2pMessageFactory;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.ethereum.net.rlpx.FrameCodec.Frame;
import static org.ethereum.util.ByteUtil.bigEndianToShort;

/**
 * The Netty handler which manages initial negotiation with peer
 * (when either we initiating connection or remote peer initiates)
 *
 * The initial handshake includes:
 * - first AuthInitiate -> AuthResponse messages when peers exchange with secrets
 * - second P2P Hello messages when P2P protocol and subprotocol capabilities are negotiated
 *
 * After the handshake is done this handler reports secrets and other data to the Channel
 * which installs further handlers depending on the protocol parameters.
 * This handler is finally removed from the pipeline.
 */
@Component
@Scope("prototype")
public class HandshakeHandler extends ByteToMessageDecoder {

    private static final Logger loggerWire = LoggerFactory.getLogger("wire");
    private static final Logger loggerNet = LoggerFactory.getLogger("net");
    private final ECKey myKey;
    private final SystemProperties config;
    private FrameCodec frameCodec;
    private byte[] nodeId;
    private byte[] remoteId;
    private EncryptionHandshake handshake;
    private byte[] initiatePacket;
    private Channel channel;
    private boolean isHandshakeDone;

    @Autowired
    public HandshakeHandler(final SystemProperties config, final NodeManager nodeManager) {
        this.config = config;
        final NodeManager nodeManager1 = nodeManager;

        myKey = config.getMyKey();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        channel.setInetSocketAddress((InetSocketAddress) ctx.channel().remoteAddress());
        if (remoteId.length == 64) {
            channel.initWithNode(remoteId);
            initiate(ctx);
        } else {
            handshake = new EncryptionHandshake();
            nodeId = myKey.getNodeId();
        }
    }

    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        loggerWire.debug("Decoding handshake... (" + in.readableBytes() + " bytes available)");
        decodeHandshake(ctx, in);
        if (isHandshakeDone) {
            loggerWire.debug("Handshake done, removing HandshakeHandler from pipeline.");
            ctx.pipeline().remove(this);
        }
    }

    private void initiate(final ChannelHandlerContext ctx) throws Exception {

        loggerNet.debug("RLPX protocol activated");

        nodeId = myKey.getNodeId();

        handshake = new EncryptionHandshake(ECKey.fromNodeId(this.remoteId).getPubKeyPoint());

        final Object msg;
        if (config.eip8()) {
            final AuthInitiateMessageV4 initiateMessage = handshake.createAuthInitiateV4(myKey);
            initiatePacket = handshake.encryptAuthInitiateV4(initiateMessage);
            msg = initiateMessage;
        } else {
            final AuthInitiateMessage initiateMessage = handshake.createAuthInitiate(null, myKey);
            initiatePacket = handshake.encryptAuthMessage(initiateMessage);
            msg = initiateMessage;
        }

        final ByteBuf byteBufMsg = ctx.alloc().buffer(initiatePacket.length);
        byteBufMsg.writeBytes(initiatePacket);
        ctx.writeAndFlush(byteBufMsg).sync();

        channel.getNodeStatistics().rlpxAuthMessagesSent.add();

        if (loggerNet.isDebugEnabled())
            loggerNet.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), msg);
    }

    // consume handshake, producing no resulting message to upper layers
    private void decodeHandshake(final ChannelHandlerContext ctx, final ByteBuf buffer) throws Exception {

        if (handshake.isInitiator()) {
            if (frameCodec == null) {

                byte[] responsePacket = new byte[AuthResponseMessage.getLength() + ECIESCoder.getOverhead()];
                if (!buffer.isReadable(responsePacket.length))
                    return;
                buffer.readBytes(responsePacket);

                try {

                    // trying to decode as pre-EIP-8

                    final AuthResponseMessage response = handshake.handleAuthResponse(myKey, initiatePacket, responsePacket);
                    loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response);

                } catch (final Throwable t) {

                    // it must be format defined by EIP-8 then

                    responsePacket = readEIP8Packet(buffer, responsePacket);

                    if (responsePacket == null) return;

                    final AuthResponseMessageV4 response = handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket);
                    loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), response);
                }

                final EncryptionHandshake.Secrets secrets = this.handshake.getSecrets();
                this.frameCodec = new FrameCodec(secrets);

                loggerNet.debug("auth exchange done");
                channel.sendHelloMessage(ctx, frameCodec, Hex.toHexString(nodeId), null);
            } else {
                loggerWire.info("MessageCodec: Buffer bytes: " + buffer.readableBytes());
                final List<Frame> frames = frameCodec.readFrames(buffer);
                if (frames == null || frames.isEmpty())
                    return;
                final Frame frame = frames.get(0);
                final byte[] payload = ByteStreams.toByteArray(frame.getStream());
                if (frame.getType() == P2pMessageCodes.HELLO.asByte()) {
                    final HelloMessage helloMessage = new HelloMessage(payload);
                    if (loggerNet.isDebugEnabled())
                        loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), helloMessage);
                    isHandshakeDone = true;
                    this.channel.publicRLPxHandshakeFinished(ctx, frameCodec, helloMessage);
                } else {
                    final DisconnectMessage message = new DisconnectMessage(payload);
                    if (loggerNet.isDebugEnabled())
                        loggerNet.debug("From: {}    Recv:  {}", channel, message);
                    channel.getNodeStatistics().nodeDisconnectedRemote(message.getReason());
                }
            }
        } else {
            loggerWire.debug("Not initiator.");
            if (frameCodec == null) {
                loggerWire.debug("FrameCodec == null");
                byte[] authInitPacket = new byte[AuthInitiateMessage.getLength() + ECIESCoder.getOverhead()];
                if (!buffer.isReadable(authInitPacket.length))
                    return;
                buffer.readBytes(authInitPacket);

                this.handshake = new EncryptionHandshake();

                byte[] responsePacket;

                try {

                    // trying to decode as pre-EIP-8
                    final AuthInitiateMessage initiateMessage = handshake.decryptAuthInitiate(authInitPacket, myKey);
                    loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), initiateMessage);

                    final AuthResponseMessage response = handshake.makeAuthInitiate(initiateMessage, myKey);
                    loggerNet.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response);
                    responsePacket = handshake.encryptAuthResponse(response);

                } catch (final Throwable t) {

                    // it must be format defined by EIP-8 then
                    try {

                        authInitPacket = readEIP8Packet(buffer, authInitPacket);

                        if (authInitPacket == null) return;

                        final AuthInitiateMessageV4 initiateMessage = handshake.decryptAuthInitiateV4(authInitPacket, myKey);
                        loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), initiateMessage);

                        final AuthResponseMessageV4 response = handshake.makeAuthInitiateV4(initiateMessage, myKey);
                        loggerNet.debug("To:   {}    Send:  {}", ctx.channel().remoteAddress(), response);
                        responsePacket = handshake.encryptAuthResponseV4(response);

                    } catch (final InvalidCipherTextException ce) {
                        loggerNet.warn("Can't decrypt AuthInitiateMessage from " + ctx.channel().remoteAddress() +
                                ". Most likely the remote peer used wrong public key (NodeID) to encrypt message.");
                        return;
                    }
                }

                handshake.agreeSecret(authInitPacket, responsePacket);

                final EncryptionHandshake.Secrets secrets = this.handshake.getSecrets();
                this.frameCodec = new FrameCodec(secrets);

                final ECPoint remotePubKey = this.handshake.getRemotePublicKey();

                final byte[] compressed = remotePubKey.getEncoded();

                this.remoteId = new byte[compressed.length - 1];
                System.arraycopy(compressed, 1, this.remoteId, 0, this.remoteId.length);

                final ByteBuf byteBufMsg = ctx.alloc().buffer(responsePacket.length);
                byteBufMsg.writeBytes(responsePacket);
                ctx.writeAndFlush(byteBufMsg).sync();
            } else {
                final List<Frame> frames = frameCodec.readFrames(buffer);
                if (frames == null || frames.isEmpty())
                    return;
                final Frame frame = frames.get(0);

                final Message message = new P2pMessageFactory().create((byte) frame.getType(),
                        ByteStreams.toByteArray(frame.getStream()));
                loggerNet.debug("From: {}    Recv:  {}", ctx.channel().remoteAddress(), message);

                if (frame.getType() == P2pMessageCodes.DISCONNECT.asByte()) {
                    loggerNet.debug("Active remote peer disconnected right after handshake.");
                    return;
                }

                if (frame.getType() != P2pMessageCodes.HELLO.asByte()) {
                    throw new RuntimeException("The message type is not HELLO or DISCONNECT: " + message);
                }

                final HelloMessage inboundHelloMessage = (HelloMessage) message;

                // now we know both remote nodeId and port
                // let's set node, that will cause registering node in NodeManager
                channel.initWithNode(remoteId, inboundHelloMessage.getListenPort());

                // Secret authentication finish here
                channel.sendHelloMessage(ctx, frameCodec, Hex.toHexString(nodeId), inboundHelloMessage);
                isHandshakeDone = true;
                this.channel.publicRLPxHandshakeFinished(ctx, frameCodec, inboundHelloMessage);
                channel.getNodeStatistics().rlpxInHello.add();
            }
        }
    }

    private byte[] readEIP8Packet(final ByteBuf buffer, final byte[] plainPacket) {

        final int size = bigEndianToShort(plainPacket);
        if (size < plainPacket.length)
            throw new IllegalArgumentException("AuthResponse packet size is too low");

        final int bytesLeft = size - plainPacket.length + 2;
        final byte[] restBytes = new byte[bytesLeft];

        if (!buffer.isReadable(restBytes.length))
            return null;

        buffer.readBytes(restBytes);

        final byte[] fullResponse = new byte[size + 2];
        System.arraycopy(plainPacket, 0, fullResponse, 0, plainPacket.length);
        System.arraycopy(restBytes, 0, fullResponse, plainPacket.length, restBytes.length);

        return fullResponse;
    }

    public void setRemoteId(final String remoteId, final Channel channel) {
        this.remoteId = Hex.decode(remoteId);
        this.channel = channel;
    }

    public byte[] getRemoteId() {
        return remoteId;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (channel.isDiscoveryMode()) {
            loggerNet.trace("Handshake failed: " + cause);
        } else {
            if (cause instanceof IOException || cause instanceof ReadTimeoutException) {
                loggerNet.debug("Handshake failed: " + ctx.channel().remoteAddress() + ": " + cause);
            } else {
                loggerNet.warn("Handshake failed: ", cause);
            }
        }
        ctx.close();
    }
}
