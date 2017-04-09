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

package org.ethereum.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageCodec;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class NettyTest {
    @Test
    public void pipelineTest() {

        final int[] int2 = new int[1];
        final boolean[] exception = new boolean[1];

        final ByteToMessageDecoder decoder2 = new ByteToMessageDecoder() {
            @Override
            protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
                final int i = in.readInt();
                System.out.println("decoder2 read int (4 bytes): " + Integer.toHexString(i));
                int2[0] = i;
                if (i == 0) out.add("aaa");
            }

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                System.out.println("Decoder2 exception: " + cause);
            }
        };

        final MessageToMessageCodec decoder3 = new MessageToMessageCodec<Object, Object>() {
            @Override
            protected void decode(final ChannelHandlerContext ctx, final Object msg, final List<Object> out) throws Exception {
                System.out.println("NettyTest.decode: msg = [" + msg + "]");
                if (msg == "aaa") {
                    throw new RuntimeException("Test exception 3");
                }
            }

            @Override
            protected void encode(final ChannelHandlerContext ctx, final Object msg, final List<Object> out) throws Exception {
                throw new RuntimeException("Test exception 4");
            }

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                System.out.println("Decoder3 exception: " + cause);
                exception[0] = true;
            }
        };

        final ByteToMessageDecoder decoder1 = new ByteToMessageDecoder() {
            @Override
            protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
                final int i = in.readInt();
                System.out.println("decoder1 read int (4 bytes). Needs no more: " + Integer.toHexString(i));
                ctx.pipeline().addAfter("decoder1", "decoder2", decoder2);
                ctx.pipeline().addAfter("decoder2", "decoder3", decoder3);
                ctx.pipeline().remove(this);
            }

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                System.out.println("Decoder1 exception: " + cause);
            }
        };

        final ChannelInboundHandlerAdapter initiator = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                ctx.pipeline().addFirst("decoder1", decoder1);
                System.out.println("NettyTest.channelActive");
            }
        };

        final EmbeddedChannel channel0 = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                throw new RuntimeException("Test");
            }

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                System.out.println("Exception caught: " + cause);
            }

        });
        final EmbeddedChannel channel = new EmbeddedChannel(initiator);
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(0x12345678);
        buffer.writeInt(0xabcdefff);
        channel.writeInbound(buffer);
        Assert.assertEquals(0xabcdefff, int2[0]);

        channel.writeInbound(Unpooled.buffer().writeInt(0));
        Assert.assertTrue(exception[0]);

        // Need the following for the exception in outbound handler to be fired
        // ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

//        exception[0] = false;
//        channel.writeOutbound("outMsg");
//        Assert.assertTrue(exception[0]);
    }

}
