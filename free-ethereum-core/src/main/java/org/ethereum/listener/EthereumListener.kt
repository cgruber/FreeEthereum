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

package org.ethereum.listener

import org.ethereum.core.*
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel

/**
 * @author Roman Mandeleil
 * *
 * @since 27.07.2014
 */
interface EthereumListener {

    enum class PendingTransactionState {
        /**
         * Transaction may be dropped due to:
         * - Invalid transaction (invalid nonce, low gas price, insufficient account funds,
         * invalid signature)
         * - Timeout (when pending transaction is not included to any block for
         * last [transaction.outdated.threshold] blocks
         * This is the final state
         */
        DROPPED,

        /**
         * The same as PENDING when transaction is just arrived
         * Next state can be either PENDING or INCLUDED
         */
        NEW_PENDING,

        /**
         * State when transaction is not included to any blocks (on the main chain), and
         * was executed on the last best block. The repository state is reflected in the PendingState
         * Next state can be either INCLUDED, DROPPED (due to timeout)
         * or again PENDING when a new block (without this transaction) arrives
         */
        PENDING,

        /**
         * State when the transaction is included to a block.
         * This could be the final state, however next state could also be
         * PENDING: when a fork became the main chain but doesn't include this tx
         * INCLUDED: when a fork became the main chain and tx is included into another
         * block from the new main chain
         * DROPPED: If switched to a new (long enough) main chain without this Tx
         */
        INCLUDED;

        val isPending: Boolean
            get() = this == NEW_PENDING || this == PENDING
    }

    enum class SyncState {
        /**
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         */
        UNSECURE,
        /**
         * When doing fast sync SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         */
        SECURE,
        /**
         * Sync is fully complete. All blocks and receipts are downloaded.
         */
        COMPLETE
    }

    fun trace(output: String)

    fun onNodeDiscovered(node: Node)

    fun onHandShakePeer(channel: Channel, helloMessage: HelloMessage)

    fun onEthStatusUpdated(channel: Channel, status: StatusMessage)

    fun onRecvMessage(channel: Channel, message: Message)

    fun onSendMessage(channel: Channel, message: Message)

    fun onBlock(blockSummary: BlockSummary)

    fun onPeerDisconnect(host: String, port: Long)


    @Deprecated("use onPendingTransactionUpdate filtering state NEW_PENDING\n      Will be removed in the next release")
    fun onPendingTransactionsReceived(transactions: List<Transaction>)

    /**
     * PendingState changes on either new pending transaction or new best block receive
     * When a new transaction arrives it is executed on top of the current pending state
     * When a new best block arrives the PendingState is adjusted to the new Repository state
     * and all transactions which remain pending are executed on top of the new PendingState
     */
    fun onPendingStateChanged(pendingState: PendingState)

    /**
     * Is called when PendingTransaction arrives, executed or dropped and included to a block

     * @param txReceipt Receipt of the tx execution on the current PendingState
     * *
     * @param state Current state of pending tx
     * *
     * @param block The block which the current pending state is based on (for PENDING tx state)
     * *              or the block which tx was included to (for INCLUDED state)
     */
    fun onPendingTransactionUpdate(txReceipt: TransactionReceipt, state: PendingTransactionState, block: Block)

    fun onSyncDone(state: SyncState)

    fun onNoConnections()

    fun onVMTraceCreated(transactionHash: String, trace: String)

    fun onTransactionExecuted(summary: TransactionExecutionSummary)

    fun onPeerAddedToSyncPool(peer: Channel)
}
