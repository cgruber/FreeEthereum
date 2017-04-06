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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.ethereum.util.Utils.sizeToStr;

/**
 * Created by Anton Nashatyrev on 27.02.2017.
 */
@Component
public class WireTrafficStats  implements Runnable  {
    private final static Logger logger = LoggerFactory.getLogger("net");
    public final TrafficStatHandler tcp = new TrafficStatHandler();
    public final TrafficStatHandler udp = new TrafficStatHandler();
    private final ScheduledExecutorService executor;

    public WireTrafficStats() {
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("WireTrafficStats-%d").build());
        executor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        logger.info("TCP: " + tcp.stats());
        logger.info("UDP: " + udp.stats());
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    @ChannelHandler.Sharable
    static class TrafficStatHandler extends ChannelDuplexHandler {
        final AtomicLong outSize = new AtomicLong();
        final AtomicLong inSize = new AtomicLong();
        final AtomicLong outPackets = new AtomicLong();
        final AtomicLong inPackets = new AtomicLong();
        long outSizeTot;
        long inSizeTot;
        long lastTime = System.currentTimeMillis();

        public String stats() {
            final long out = outSize.getAndSet(0);
            final long outPac = outPackets.getAndSet(0);
            final long in = inSize.getAndSet(0);
            final long inPac = inPackets.getAndSet(0);
            outSizeTot += out;
            inSizeTot += in;
            final long curTime = System.currentTimeMillis();
            final long d = (curTime - lastTime);
            final long outSpeed = out * 1000 / d;
            final long inSpeed = in * 1000 / d;
            lastTime = curTime;
            return "Speed in/out " + sizeToStr(inSpeed) + " / " + sizeToStr(outSpeed) +
                    "(sec), packets in/out " + inPac + "/" + outPac +
                    ", total in/out: " + sizeToStr(inSizeTot) + " / " + sizeToStr(outSizeTot);
        }


        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            inPackets.incrementAndGet();
            if (msg instanceof ByteBuf) {
                inSize.addAndGet(((ByteBuf) msg).readableBytes());
            } else if (msg instanceof DatagramPacket) {
                inSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
            outPackets.incrementAndGet();
            if (msg instanceof ByteBuf) {
                outSize.addAndGet(((ByteBuf) msg).readableBytes());
            } else if (msg instanceof DatagramPacket) {
                outSize.addAndGet(((DatagramPacket) msg).content().readableBytes());
            }
            super.write(ctx, msg, promise);
        }
    }
}
