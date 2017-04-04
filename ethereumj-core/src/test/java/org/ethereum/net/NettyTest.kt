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

package org.ethereum.net

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToMessageCodec
import org.junit.Assert
import org.junit.Test

/**
 * Created by Anton Nashatyrev on 16.10.2015.
 */
class NettyTest {
    @Test
    fun pipelineTest() {

        val int2 = IntArray(1)
        val exception = BooleanArray(1)

        val decoder2 = object : ByteToMessageDecoder() {
            @Throws(Exception::class)
            override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
                val i = `in`.readInt()
                println("decoder2 read int (4 bytes): " + Integer.toHexString(i))
                int2[0] = i
                if (i == 0) out.add("aaa")
            }

            @Throws(Exception::class)
            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                println("Decoder2 exception: " + cause)
            }
        }

        val decoder3 = object : MessageToMessageCodec<Any, Any>() {
            @Throws(Exception::class)
            override fun decode(ctx: ChannelHandlerContext, msg: Any, out: List<Any>) {
                println("NettyTest.decode: msg = [$msg]")
                if (msg === "aaa") {
                    throw RuntimeException("Test exception 3")
                }
            }

            @Throws(Exception::class)
            override fun encode(ctx: ChannelHandlerContext, msg: Any, out: List<Any>) {
                throw RuntimeException("Test exception 4")
            }

            @Throws(Exception::class)
            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                println("Decoder3 exception: " + cause)
                exception[0] = true
            }
        }

        val decoder1 = object : ByteToMessageDecoder() {
            @Throws(Exception::class)
            override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: List<Any>) {
                val i = `in`.readInt()
                println("decoder1 read int (4 bytes). Needs no more: " + Integer.toHexString(i))
                ctx.pipeline().addAfter("decoder1", "decoder2", decoder2)
                ctx.pipeline().addAfter("decoder2", "decoder3", decoder3)
                ctx.pipeline().remove(this)
            }

            @Throws(Exception::class)
            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                println("Decoder1 exception: " + cause)
            }
        }

        val initiator = object : ChannelInboundHandlerAdapter() {
            @Throws(Exception::class)
            override fun channelActive(ctx: ChannelHandlerContext) {
                ctx.pipeline().addFirst("decoder1", decoder1)
                println("NettyTest.channelActive")
            }
        }

        val channel0 = EmbeddedChannel(object : ChannelOutboundHandlerAdapter() {
            @Throws(Exception::class)
            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                throw RuntimeException("Test")
            }

            @Throws(Exception::class)
            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                println("Exception caught: " + cause)
            }

        })
        val channel = EmbeddedChannel(initiator)
        val buffer = Unpooled.buffer()
        buffer.writeInt(0x12345678)
        buffer.writeInt(0xabcdefff.toInt())
        channel.writeInbound(buffer)
        Assert.assertEquals(0xabcdefff.toInt(), int2[0].toLong())

        channel.writeInbound(Unpooled.buffer().writeInt(0))
        Assert.assertTrue(exception[0])

        // Need the following for the exception in outbound handler to be fired
        // ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

        //        exception[0] = false;
        //        channel.writeOutbound("outMsg");
        //        Assert.assertTrue(exception[0]);
    }

}
