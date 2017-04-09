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

package org.ethereum.net.swarm.bzz;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.MessageQueue;
import org.ethereum.net.swarm.NetStore;
import org.ethereum.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process the messages between peers with 'bzz' capability on the network.
 */
@Component
@Scope("prototype")
public class BzzHandler extends SimpleChannelInboundHandler<BzzMessage>
        implements Functional.Consumer<BzzMessage> {

    public final static byte VERSION = 0;
    private final static Logger logger = LoggerFactory.getLogger("net");
    private MessageQueue msgQueue = null;
    private boolean active = false;
    private BzzProtocol bzzProtocol;

    @Autowired
    private
    EthereumListener ethereumListener;

    @Autowired
    private
    NetStore netStore;

    public BzzHandler() {
    }

    public BzzHandler(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final BzzMessage msg) throws InterruptedException {

        if (!isActive()) return;

        if (BzzMessageCodes.inRange(msg.getCommand().asByte()))
            logger.debug("BzzHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("BzzHandler invoke: [%s]", msg.getCommand()));

        if (bzzProtocol != null) {
            bzzProtocol.accept(msg);
        }
    }

    @Override
    public void accept(final BzzMessage bzzMessage) {
        msgQueue.sendMessage(bzzMessage);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        logger.error("Bzz handling failed", cause);
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        active = false;
        logger.debug("handlerRemoved: ... ");
    }

    public void activate() {
        logger.info("BZZ protocol activated");
        ethereumListener.trace("BZZ protocol activated");
        createBzzProtocol();
        this.active = true;
    }

    private void createBzzProtocol() {
        bzzProtocol = new BzzProtocol(netStore /*NetStore.getInstance()*/);
        bzzProtocol.setMessageSender(this);
        bzzProtocol.start();
    }

    private boolean isActive() {
        return active;
    }

    public void setMsgQueue(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }
}