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

package org.ethereum.net.shh;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process the messages between peers with 'shh' capability on the network.
 *
 * Peers with 'shh' capability can send/receive:
 */
@Component
@Scope("prototype")
public class ShhHandler extends SimpleChannelInboundHandler<ShhMessage> {
    public final static byte VERSION = 3;
    private final static Logger logger = LoggerFactory.getLogger("net.shh");
    private MessageQueue msgQueue = null;
    private boolean active = false;

    @Autowired
    private EthereumListener ethereumListener;

    @Autowired
    private WhisperImpl whisper;

    public ShhHandler() {
    }

    public ShhHandler(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final ShhMessage msg) throws InterruptedException {

        if (!isActive()) return;

        if (ShhMessageCodes.inRange(msg.getCommand().asByte()))
            logger.info("ShhHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("ShhHandler invoke: [%s]", msg.getCommand()));

        switch (msg.getCommand()) {
            case STATUS:
                ethereumListener.trace("[Recv: " + msg + "]");
                break;
            case MESSAGE:
                whisper.processEnvelope((ShhEnvelopeMessage) msg, this);
                break;
            case FILTER:
                setBloomFilter((ShhFilterMessage) msg);
                break;
            default:
                logger.error("Unknown SHH message type: " + msg.getCommand());
                break;
        }
    }

    private void setBloomFilter(final ShhFilterMessage msg) {
        final BloomFilter peerBloomFilter = new BloomFilter(msg.getBloomFilter());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        logger.error("Shh handling failed", cause);
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        active = false;
        whisper.removePeer(this);
        logger.debug("handlerRemoved: ... ");
    }

    public void activate() {
        logger.info("SHH protocol activated");
        ethereumListener.trace("SHH protocol activated");
        whisper.addPeer(this);
        sendStatus();
        sendHostBloom();
        this.active = true;
    }

    private void sendStatus() {
        final byte protocolVersion = ShhHandler.VERSION;
        final ShhStatusMessage msg = new ShhStatusMessage(protocolVersion);
        sendMessage(msg);
    }

    void sendHostBloom() {
        final ShhFilterMessage msg = ShhFilterMessage.createFromFilter(whisper.hostBloomFilter.toBytes());
        sendMessage(msg);
    }

    void sendEnvelope(final ShhEnvelopeMessage env) {
        sendMessage(env);
//        Topic[] topics = env.getTopics();
//        for (Topic topic : topics) {
//            if (peerBloomFilter.hasTopic(topic)) {
//                sendMessage(env);
//                break;
//            }
//        }
    }

    private void sendMessage(final ShhMessage msg) {
        msgQueue.sendMessage(msg);
    }

    private boolean isActive() {
        return active;
    }

    public void setMsgQueue(final MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }
}