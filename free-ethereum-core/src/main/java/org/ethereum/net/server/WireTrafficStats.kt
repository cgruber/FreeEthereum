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

package org.ethereum.net.server

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.socket.DatagramPacket
import org.ethereum.util.Utils.sizeToStr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PreDestroy

@Component
class WireTrafficStats : Runnable {
    internal var tcp = TrafficStatHandler()
    var udp = TrafficStatHandler()
    private val executor: ScheduledExecutorService

    init {
        executor = Executors.newSingleThreadScheduledExecutor(ThreadFactoryBuilder().setNameFormat("WireTrafficStats-%d").build())
        executor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS)
    }

    override fun run() {
        logger.info("TCP: " + tcp.stats())
        logger.info("UDP: " + udp.stats())
    }

    @PreDestroy
    fun close() {
        executor.shutdownNow()
    }

    @ChannelHandler.Sharable class TrafficStatHandler : ChannelDuplexHandler() {
        val outSize = AtomicLong()
        val inSize = AtomicLong()
        val outPackets = AtomicLong()
        val inPackets = AtomicLong()
        var outSizeTot: Long = 0
        var inSizeTot: Long = 0
        var lastTime = System.currentTimeMillis()

        fun stats(): String {
            val out = outSize.getAndSet(0)
            val outPac = outPackets.getAndSet(0)
            val `in` = inSize.getAndSet(0)
            val inPac = inPackets.getAndSet(0)
            outSizeTot += out
            inSizeTot += `in`
            val curTime = System.currentTimeMillis()
            val d = curTime - lastTime
            val outSpeed = out * 1000 / d
            val inSpeed = `in` * 1000 / d
            lastTime = curTime
            return "Speed in/out " + sizeToStr(inSpeed) + " / " + sizeToStr(outSpeed) +
                    "(sec), packets in/out " + inPac + "/" + outPac +
                    ", total in/out: " + sizeToStr(inSizeTot) + " / " + sizeToStr(outSizeTot)
        }


        @Throws(Exception::class)
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            inPackets.incrementAndGet()
            if (msg is ByteBuf) {
                inSize.addAndGet(msg.readableBytes().toLong())
            } else if (msg is DatagramPacket) {
                inSize.addAndGet(msg.content().readableBytes().toLong())
            }
            super.channelRead(ctx, msg)
        }

        @Throws(Exception::class)
        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            outPackets.incrementAndGet()
            if (msg is ByteBuf) {
                outSize.addAndGet(msg.readableBytes().toLong())
            } else if (msg is DatagramPacket) {
                outSize.addAndGet(msg.content().readableBytes().toLong())
            }
            super.write(ctx, msg, promise)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("net")
    }
}
