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
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Since decoder field is not modifiable in the ByteToMessageCodec this class
 * overrides it to set the COMPOSITE_CUMULATOR for ByteToMessageDecoder as it
 * is more effective than the default one.
 */
abstract class NettyByteToMessageCodec<I> extends ByteToMessageCodec<I> {

    private final ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
        {
            setCumulator(COMPOSITE_CUMULATOR);
        }

        @Override
        public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
            NettyByteToMessageCodec.this.decode(ctx, in, out);
        }

        @Override
        protected void decodeLast(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
            NettyByteToMessageCodec.this.decodeLast(ctx, in, out);
        }
    };

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        decoder.channelReadComplete(ctx);
        super.channelReadComplete(ctx);
    }
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        decoder.channelInactive(ctx);
        super.channelInactive(ctx);
    }
    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        decoder.handlerAdded(ctx);
        super.handlerAdded(ctx);
    }
    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        decoder.handlerRemoved(ctx);
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        decoder.channelRead(ctx, msg);
    }
}
