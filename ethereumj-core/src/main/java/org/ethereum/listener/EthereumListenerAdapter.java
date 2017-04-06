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

package org.ethereum.listener;

import org.ethereum.core.*;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;

import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 08.08.2014
 */
public class EthereumListenerAdapter implements EthereumListener {

    @Override
    public void trace(final String output) {
    }

    public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
    }

    @Override
    public void onBlock(final BlockSummary blockSummary) {
        onBlock(blockSummary.getBlock(), blockSummary.getReceipts());
    }

    @Override
    public void onRecvMessage(final Channel channel, final Message message) {
    }

    @Override
    public void onSendMessage(final Channel channel, final Message message) {
    }

    @Override
    public void onPeerDisconnect(final String host, final long port) {
    }

    @Override
    public void onPendingTransactionsReceived(final List<Transaction> transactions) {
    }

    @Override
    public void onPendingStateChanged(final PendingState pendingState) {
    }

    @Override
    public void onSyncDone(final SyncState state) {

    }

    @Override
    public void onHandShakePeer(final Channel channel, final HelloMessage helloMessage) {

    }

    @Override
    public void onNoConnections() {

    }


    @Override
    public void onVMTraceCreated(final String transactionHash, final String trace) {

    }

    @Override
    public void onNodeDiscovered(final Node node) {

    }

    @Override
    public void onEthStatusUpdated(final Channel channel, final StatusMessage statusMessage) {

    }

    @Override
    public void onTransactionExecuted(final TransactionExecutionSummary summary) {

    }

    @Override
    public void onPeerAddedToSyncPool(final Channel peer) {

    }

    @Override
    public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {

    }
}
