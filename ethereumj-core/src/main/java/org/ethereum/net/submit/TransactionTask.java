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

package org.ethereum.net.submit;

import org.ethereum.core.Transaction;
import org.ethereum.net.server.Channel;
import org.ethereum.net.server.ChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Roman Mandeleil
 * @since 23.05.2014
 */
public class TransactionTask implements Callable<List<Transaction>> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final List<Transaction> tx;
    private final ChannelManager channelManager;
    private final Channel receivedFrom;

    public TransactionTask(final Transaction tx, final ChannelManager channelManager) {
        this(Collections.singletonList(tx), channelManager);
    }

    private TransactionTask(final List<Transaction> tx, final ChannelManager channelManager) {
        this(tx, channelManager, null);
    }

    public TransactionTask(final List<Transaction> tx, final ChannelManager channelManager, final Channel receivedFrom) {
        this.tx = tx;
        this.channelManager = channelManager;
        this.receivedFrom = receivedFrom;
    }

    @Override
    public List<Transaction> call() throws Exception {

        try {
            logger.info("submit tx: {}", tx.toString());
            channelManager.sendTransaction(tx, receivedFrom);
            return tx;

        } catch (final Throwable th) {
            logger.warn("Exception caught: {}", th);
        }
        return null;
    }
}
