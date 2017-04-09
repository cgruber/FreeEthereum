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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.p2p.DisconnectMessage;
import org.ethereum.net.p2p.PingMessage;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ethereum.net.message.StaticMessages.DISCONNECT_MESSAGE;

/**
 * This class contains the logic for sending messages in a queue
 *
 * Messages open by send and answered by receive of appropriate message
 *      PING by PONG
 *      GET_PEERS by PEERS
 *      GET_TRANSACTIONS by TRANSACTIONS
 *      GET_BLOCK_HASHES by BLOCK_HASHES
 *      GET_BLOCKS by BLOCKS
 *
 * The following messages will not be answered:
 *      PONG, PEERS, HELLO, STATUS, TRANSACTIONS, BLOCKS
 *
 * @author Roman Mandeleil
 */
@Component
@Scope("prototype")
public class MessageQueue {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private final AtomicInteger cnt = new AtomicInteger(0);

        public Thread newThread(final Runnable r) {
            return new Thread(r, "MessageQueueTimer-" + cnt.getAndIncrement());
        }
    });
    private final Queue<MessageRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();
    private final Queue<MessageRoundtrip> respondQueue = new ConcurrentLinkedQueue<>();
    @Autowired
    private
    EthereumListener ethereumListener;
    private boolean hasPing = false;
    private ChannelHandlerContext ctx = null;
    private ScheduledFuture<?> timerTask;
    private Channel channel;

    public MessageQueue() {
    }

    public void activate(final ChannelHandlerContext ctx) {
        this.ctx = ctx;
        timerTask = timer.scheduleAtFixedRate(() -> {
            try {
                nudgeQueue();
            } catch (final Throwable t) {
                logger.error("Unhandled exception", t);
            }
        }, 10, 10, TimeUnit.MILLISECONDS);
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public void sendMessage(final Message msg) {
        if (msg instanceof PingMessage) {
            if (hasPing) return;
            hasPing = true;
        }

        if (msg.getAnswerMessage() != null)
            requestQueue.add(new MessageRoundtrip(msg));
        else
            respondQueue.add(new MessageRoundtrip(msg));
    }

    public void disconnect() {
        disconnect(DISCONNECT_MESSAGE);
    }

    public void disconnect(final ReasonCode reason) {
        disconnect(new DisconnectMessage(reason));
    }

    private void disconnect(final DisconnectMessage msg) {
        ctx.writeAndFlush(msg);
        ctx.close();
    }

    public void receivedMessage(final Message msg) throws InterruptedException {

        ethereumListener.trace("[Recv: " + msg + "]");

        if (requestQueue.peek() != null) {
            final MessageRoundtrip messageRoundtrip = requestQueue.peek();
            final Message waitingMessage = messageRoundtrip.getMsg();

            if (waitingMessage instanceof PingMessage) hasPing = false;

            if (waitingMessage.getAnswerMessage() != null
                    && msg.getClass() == waitingMessage.getAnswerMessage()) {
                messageRoundtrip.answer();
                if (waitingMessage instanceof EthMessage)
                    channel.getPeerStats().pong(messageRoundtrip.lastTimestamp);
                logger.trace("Message round trip covered: [{}] ",
                        messageRoundtrip.getMsg().getClass());
            }
        }
    }

    private void removeAnsweredMessage(final MessageRoundtrip messageRoundtrip) {
        if (messageRoundtrip != null && messageRoundtrip.isAnswered())
            requestQueue.remove();
    }

    private void nudgeQueue() {
        // remove last answered message on the queue
        removeAnsweredMessage(requestQueue.peek());
        // Now send the next message
        sendToWire(respondQueue.poll());
        sendToWire(requestQueue.peek());
    }

    private void sendToWire(final MessageRoundtrip messageRoundtrip) {

        if (messageRoundtrip != null && messageRoundtrip.getRetryTimes() == 0) {
            // TODO: retry logic || messageRoundtrip.hasToRetry()){

            final Message msg = messageRoundtrip.getMsg();

            ethereumListener.onSendMessage(channel, msg);

            ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (msg.getAnswerMessage() != null) {
                messageRoundtrip.incRetryTimes();
                messageRoundtrip.saveTime();
            }
        }
    }

    public void close() {
        if (timerTask != null) {
            timerTask.cancel(false);
        }
    }
}
