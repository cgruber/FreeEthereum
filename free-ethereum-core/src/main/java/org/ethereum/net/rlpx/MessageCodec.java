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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.SystemProperties;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.client.Capability;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.EthMessageCodes;
import org.ethereum.net.message.Message;
import org.ethereum.net.message.MessageFactory;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.p2p.P2pMessageCodes;
import org.ethereum.net.server.Channel;
import org.ethereum.net.shh.ShhMessageCodes;
import org.ethereum.net.swarm.bzz.BzzMessageCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static org.ethereum.net.rlpx.FrameCodec.Frame;

/**
 * The Netty codec which encodes/decodes RPLx frames to subprotocol Messages
 */
@Component
@Scope("prototype")
public class MessageCodec extends MessageToMessageCodec<Frame, Message> {

    public static final int NO_FRAMING = Integer.MAX_VALUE >> 1;
    private static final Logger loggerWire = LoggerFactory.getLogger("wire");
    private static final Logger loggerNet = LoggerFactory.getLogger("net");
    private final Map<Integer, Pair<? extends List<Frame>, AtomicInteger>> incompleteFrames = new LRUMap<>(16);
    // LRU avoids OOM on invalid peers
    private final AtomicInteger contextIdCounter = new AtomicInteger(1);
    private int maxFramePayloadSize = NO_FRAMING;
    private Channel channel;
    private MessageCodesResolver messageCodesResolver;
    private MessageFactory p2pMessageFactory;
    private MessageFactory ethMessageFactory;
    private MessageFactory shhMessageFactory;
    private MessageFactory bzzMessageFactory;
    private EthVersion ethVersion;
    @Autowired
    private
    EthereumListener ethereumListener;
    private boolean supportChunkedFrames = true;

    public MessageCodec() {
    }

    @Autowired
    private MessageCodec(final SystemProperties config) {
        final SystemProperties config1 = config;
        setMaxFramePayloadSize(config.rlpxMaxFrameSize());
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final Frame frame, final List<Object> out) throws Exception {
        final Frame completeFrame = null;
        if (frame.isChunked()) {
            if (!supportChunkedFrames && frame.totalFrameSize > 0) {
                throw new RuntimeException("Faming is not supported in this configuration.");
            }

            Pair<? extends List<Frame>, AtomicInteger> frameParts = incompleteFrames.get(frame.contextId);
            if (frameParts == null) {
                if (frame.totalFrameSize < 0) {
//                    loggerNet.warn("No initial frame received for context-id: " + frame.contextId + ". Discarding this frame as invalid.");
                    // TODO: refactor this logic (Cpp sends non-chunked frames with context-id)
                    final Message message = decodeMessage(ctx, Collections.singletonList(frame));
                    if (message == null) return;
                    out.add(message);
                    return;
                } else {
                    frameParts = Pair.of(new ArrayList<Frame>(), new AtomicInteger(0));
                    incompleteFrames.put(frame.contextId, frameParts);
                }
            } else {
                if (frame.totalFrameSize >= 0) {
                    loggerNet.warn("Non-initial chunked frame shouldn't contain totalFrameSize field (context-id: " + frame.contextId + ", totalFrameSize: " + frame.totalFrameSize + "). Discarding this frame and all previous.");
                    incompleteFrames.remove(frame.contextId);
                    return;
                }
            }

            frameParts.getLeft().add(frame);
            final int curSize = frameParts.getRight().addAndGet(frame.size);

            if (loggerWire.isDebugEnabled())
                loggerWire.debug("Recv: Chunked (" + curSize + " of " + frameParts.getLeft().get(0).totalFrameSize + ") [size: " + frame.getSize() + "]");

            if (curSize > frameParts.getLeft().get(0).totalFrameSize) {
                loggerNet.warn("The total frame chunks size (" + curSize + ") is greater than expected (" + frameParts.getLeft().get(0).totalFrameSize + "). Discarding the frame.");
                incompleteFrames.remove(frame.contextId);
                return;
            }
            if (curSize == frameParts.getLeft().get(0).totalFrameSize) {
                final Message message = decodeMessage(ctx, frameParts.getLeft());
                incompleteFrames.remove(frame.contextId);
                out.add(message);
            }
        } else {
            final Message message = decodeMessage(ctx, Collections.singletonList(frame));
            out.add(message);
        }
    }

    private Message decodeMessage(final ChannelHandlerContext ctx, final List<Frame> frames) throws IOException {
        final long frameType = frames.get(0).getType();

        final byte[] payload = new byte[frames.size() == 1 ? frames.get(0).getSize() : frames.get(0).totalFrameSize];
        int pos = 0;
        for (final Frame frame : frames) {
            pos += ByteStreams.read(frame.getStream(), payload, pos, frame.getSize());
        }

        if (loggerWire.isDebugEnabled())
            loggerWire.debug("Recv: Encoded: {} [{}]", frameType, Hex.toHexString(payload));

        final Message msg;
        try {
            msg = createMessage((byte) frameType, payload);
        } catch (final Exception ex) {
            loggerNet.debug("Incorrectly encoded message from: \t{}, dropping peer", channel);
            channel.disconnect(ReasonCode.BAD_PROTOCOL);
            return null;
        }

        if (loggerNet.isDebugEnabled())
            loggerNet.debug("From: {}    Recv:  {}", channel, msg.toString());

        ethereumListener.onRecvMessage(channel, msg);

        channel.getNodeStatistics().rlpxInMessages.add();
        return msg;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Message msg, final List<Object> out) throws Exception {
        final String output = String.format("To: \t%s \tSend: \t%s", ctx.channel().remoteAddress(), msg);
        ethereumListener.trace(output);

        if (loggerNet.isDebugEnabled())
            loggerNet.debug("To:   {}    Send:  {}", channel, msg);

        final byte[] encoded = msg.getEncoded();

        if (loggerWire.isDebugEnabled())
            loggerWire.debug("Send: Encoded: {} [{}]", getCode(msg.getCommand()), Hex.toHexString(encoded));

        final List<Frame> frames = splitMessageToFrames(msg);

        out.addAll(frames);

        channel.getNodeStatistics().rlpxOutMessages.add();
    }

    private List<Frame> splitMessageToFrames(final Message msg) {
        final byte code = getCode(msg.getCommand());
        final List<Frame> ret = new ArrayList<>();
        final byte[] bytes = msg.getEncoded();
        int curPos = 0;
        while(curPos < bytes.length) {
            final int newPos = min(curPos + maxFramePayloadSize, bytes.length);
            final byte[] frameBytes = curPos == 0 && newPos == bytes.length ? bytes :
                    Arrays.copyOfRange(bytes, curPos, newPos);
            ret.add(new Frame(code, frameBytes));
            curPos = newPos;
        }

        if (ret.size() > 1) {
            // frame has been split
            final int contextId = contextIdCounter.getAndIncrement();
            ret.get(0).totalFrameSize = bytes.length;
            loggerWire.debug("Message (size " + bytes.length + ") split to " + ret.size() + " frames. Context-id: " + contextId);
            for (final Frame frame : ret) {
                frame.contextId = contextId;
            }
        }
        return ret;
    }

    public void setSupportChunkedFrames(final boolean supportChunkedFrames) {
        this.supportChunkedFrames = supportChunkedFrames;
        if (!supportChunkedFrames) {
            setMaxFramePayloadSize(NO_FRAMING);
        }
    }

    /* TODO: this dirty hack is here cause we need to use message
           TODO: adaptive id on high message abstraction level,
           TODO: need a solution here*/
    private byte getCode(final Enum msgCommand) {
        byte code = 0;

        if (msgCommand instanceof P2pMessageCodes){
            code = messageCodesResolver.withP2pOffset(((P2pMessageCodes) msgCommand).asByte());
        }

        if (msgCommand instanceof EthMessageCodes){
            code = messageCodesResolver.withEthOffset(((EthMessageCodes) msgCommand).asByte());
        }

        if (msgCommand instanceof ShhMessageCodes){
            code = messageCodesResolver.withShhOffset(((ShhMessageCodes)msgCommand).asByte());
        }

        if (msgCommand instanceof BzzMessageCodes){
            code = messageCodesResolver.withBzzOffset(((BzzMessageCodes) msgCommand).asByte());
        }

        return code;
    }

    private Message createMessage(final byte code, final byte[] payload) {

        byte resolved = messageCodesResolver.resolveP2p(code);
        if (p2pMessageFactory != null && P2pMessageCodes.inRange(resolved)) {
            return p2pMessageFactory.create(resolved, payload);
        }

        resolved = messageCodesResolver.resolveEth(code);
        if (ethMessageFactory != null && EthMessageCodes.inRange(resolved, ethVersion)) {
            return ethMessageFactory.create(resolved, payload);
        }

        resolved = messageCodesResolver.resolveShh(code);
        if (shhMessageFactory != null && ShhMessageCodes.inRange(resolved)) {
            return shhMessageFactory.create(resolved, payload);
        }

        resolved = messageCodesResolver.resolveBzz(code);
        if (bzzMessageFactory != null && BzzMessageCodes.inRange(resolved)) {
            return bzzMessageFactory.create(resolved, payload);
        }

        throw new IllegalArgumentException("No such message: " + code + " [" + Hex.toHexString(payload) + "]");
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public void setEthVersion(final EthVersion ethVersion) {
        this.ethVersion = ethVersion;
    }

    public void setMaxFramePayloadSize(final int maxFramePayloadSize) {
        this.maxFramePayloadSize = maxFramePayloadSize;
    }

    public void initMessageCodes(final List<Capability> caps) {
        this.messageCodesResolver = new MessageCodesResolver(caps);
    }

    public void setP2pMessageFactory(final MessageFactory p2pMessageFactory) {
        this.p2pMessageFactory = p2pMessageFactory;
    }

    public void setEthMessageFactory(final MessageFactory ethMessageFactory) {
        this.ethMessageFactory = ethMessageFactory;
    }

    public void setShhMessageFactory(final MessageFactory shhMessageFactory) {
        this.shhMessageFactory = shhMessageFactory;
    }

    public void setBzzMessageFactory(final MessageFactory bzzMessageFactory) {
        this.bzzMessageFactory = bzzMessageFactory;
    }
}