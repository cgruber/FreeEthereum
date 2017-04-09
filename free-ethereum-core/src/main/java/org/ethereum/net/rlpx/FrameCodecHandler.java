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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * The Netty handler responsible for decrypting/encrypting RLPx frames
 * with the FrameCodec crated during HandshakeHandler initial work
 *
 * Created by Anton Nashatyrev on 15.10.2015.
 */
public class FrameCodecHandler extends NettyByteToMessageCodec<FrameCodec.Frame> {
    private static final Logger loggerWire = LoggerFactory.getLogger("wire");
    private static final Logger loggerNet = LoggerFactory.getLogger("net");

    private final FrameCodec frameCodec;
    private final Channel channel;

    public FrameCodecHandler(final FrameCodec frameCodec, final Channel channel) {
        this.frameCodec = frameCodec;
        this.channel = channel;
    }

    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws IOException {
        if (in.readableBytes() == 0) {
            loggerWire.trace("in.readableBytes() == 0");
            return;
        }

        loggerWire.trace("Decoding frame (" + in.readableBytes() + " bytes)");
        final List<FrameCodec.Frame> frames = frameCodec.readFrames(in);


        // Check if a full frame was available.  If not, we'll try later when more bytes come in.
        if (frames == null || frames.isEmpty()) return;

        for (final FrameCodec.Frame frame : frames) {
            channel.getNodeStatistics().rlpxInMessages.add();
        }

        out.addAll(frames);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final FrameCodec.Frame frame, final ByteBuf out) throws Exception {

        frameCodec.writeFrame(frame, out);

        channel.getNodeStatistics().rlpxOutMessages.add();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (channel.isDiscoveryMode()) {
            loggerNet.trace("FrameCodec failed: " + cause);
        } else {
            if (cause instanceof IOException) {
                loggerNet.debug("FrameCodec failed: " + ctx.channel().remoteAddress() + ": " + cause);
            } else {
                loggerNet.warn("FrameCodec failed: ", cause);
            }
        }
        ctx.close();
    }
}
