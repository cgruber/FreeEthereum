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

package org.ethereum.core

/**
 * @author Mikhail Kalinin
 * *
 * @since 28.09.2015
 */
interface PendingState : org.ethereum.facade.PendingState {

    /**
     * Adds transactions received from the net to the list of wire transactions <br></br>
     * Triggers an update of pending state

     * @param transactions txs received from the net
     * *
     * @return sublist of transactions with NEW_PENDING status
     */
    fun addPendingTransactions(transactions: List<Transaction>): List<Transaction>

    /**
     * Adds transaction to the list of pending state txs  <br></br>
     * For the moment this list is populated with txs sent by our peer only <br></br>
     * Triggers an update of pending state

     * @param tx transaction
     */
    fun addPendingTransaction(tx: Transaction)

    /**
     * It should be called on each block imported as **BEST** <br></br>
     * Does several things:
     *
     *  * removes block's txs from pending state and wire lists
     *  * removes outdated wire txs
     *  * updates pending state
     *

     * @param block block imported into blockchain as a **BEST** one
     */
    fun processBest(block: Block, receipts: List<TransactionReceipt>)
}
