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

import org.ethereum.util.RLP
import org.ethereum.util.RLPItem
import org.ethereum.util.RLPList

import java.math.BigInteger

/**
 * Contains Transaction execution info:
 * its receipt and execution context
 * If the transaction is still in pending state the context is the
 * hash of the parent block on top of which the transaction was executed
 * If the transaction is already mined into a block the context
 * is the containing block and the index of the transaction in that block

 * Created by Ruben on 8/1/2016.
 */
class TransactionInfo {

    val receipt: TransactionReceipt
    var blockHash: ByteArray? = null
        internal set
    // user for pending transaction
    var parentBlockHash: ByteArray? = null
    var index: Int = 0
        private set

    constructor(receipt: TransactionReceipt, blockHash: ByteArray, index: Int) {
        this.receipt = receipt
        this.blockHash = blockHash
        this.index = index
    }

    /**
     * Creates a pending tx info
     */
    constructor(receipt: TransactionReceipt) {
        this.receipt = receipt
    }

    constructor(rlp: ByteArray) {
        val params = RLP.decode2(rlp)
        val txInfo = params[0] as RLPList
        val receiptRLP = txInfo[0] as RLPList
        val blockHashRLP = txInfo[1] as RLPItem
        val indexRLP = txInfo[2] as RLPItem

        receipt = TransactionReceipt(receiptRLP.rlpData)
        blockHash = blockHashRLP.rlpData
        if (indexRLP.rlpData == null)
            index = 0
        else
            index = BigInteger(1, indexRLP.rlpData).toInt()
    }

    fun setTransaction(tx: Transaction) {
        receipt.transaction = tx
    }

    /* [receipt, blockHash, index] */
    val encoded: ByteArray
        get() {

            val receiptRLP = this.receipt.encoded
            val blockHashRLP = RLP.encodeElement(blockHash)
            val indexRLP = RLP.encodeInt(index)

            val rlpEncoded = RLP.encodeList(receiptRLP, blockHashRLP, indexRLP)

            return rlpEncoded
        }

    val isPending: Boolean
        get() = blockHash == null
}
