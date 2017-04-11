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

package org.ethereum.net.eth.message

import org.ethereum.core.TransactionReceipt
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import java.util.*

/**
 * Wrapper around an Ethereum Receipts message on the network
 * Tx Receipts grouped by blocks

 * @see EthMessageCodes.RECEIPTS
 */
class ReceiptsMessage : EthMessage {

    private var receipts: MutableList<List<TransactionReceipt>>? = null

    constructor(encoded: ByteArray) : super(encoded)

    constructor(receiptList: MutableList<List<TransactionReceipt>>) {
        this.receipts = receiptList
        parsed = true
    }

    @Synchronized private fun parse() {
        if (parsed) return
        val paramsList = RLP.decode2(encoded)[0] as RLPList

        this.receipts = ArrayList<List<TransactionReceipt>>()
        for (aParamsList in paramsList) {
            val blockRLP = aParamsList as RLPList

            val blockReceipts = blockRLP
                    .asSequence()
                    .map { it as RLPList }
                    .filter { it.size == 4 }
                    .map { TransactionReceipt(it) }
                    .toList()
            this.receipts!!.add(blockReceipts)
        }
        this.parsed = true
    }

    private fun encode() {
        val blocks = ArrayList<ByteArray>()

        for (blockReceipts in receipts!!) {

            val encodedBlockReceipts = blockReceipts.map { it.getEncoded(true) }
            val encodedElementArray = encodedBlockReceipts.toTypedArray()
            val blockReceiptsEncoded = RLP.encodeList(*encodedElementArray)

            blocks.add(blockReceiptsEncoded)
        }

        val encodedElementArray = blocks.toTypedArray()
        this.encoded = RLP.encodeList(*encodedElementArray)
    }

    override fun getEncoded(): ByteArray {
        if (encoded == null) encode()
        return encoded
    }


    fun getReceipts(): List<List<TransactionReceipt>> {
        parse()
        return receipts!!
    }

    override fun getCommand(): EthMessageCodes {
        return EthMessageCodes.RECEIPTS
    }

    override fun getAnswerMessage(): Class<*>? {
        return null
    }

    override fun toString(): String {
        parse()
        val sb = StringBuilder()
        if (receipts!!.size < 4) {
            for (blockReceipts in receipts!!)
                sb.append("\n   ").append(blockReceipts.size).append(" receipts in block")
        } else {
            for (i in 0..2) {
                sb.append("\n   ").append(receipts!![i].size).append(" receipts in block")
            }
            sb.append("\n   ").append("[Skipped ").append(receipts!!.size - 3).append(" blocks]")
        }
        return "[" + command.name + " num:"
        (+receipts!!.size).toString() + " " + sb.toString() + "]"
    }
}