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

package org.ethereum.jsonrpc

import org.ethereum.core.Block
import org.ethereum.core.TransactionInfo
import org.ethereum.jsonrpc.TypeConverter.toJsonHex
import org.ethereum.util.ByteUtil

open class TransactionReceiptDTO(block: Block?, txInfo: TransactionInfo) {

    val gasUsed: Long             //The amount of gas used by this specific transaction alone.
    var blockNumber: Long = 0         // block number where this transaction was in.
    var contractAddress: String = "" // The contract address created, if the transaction was a contract creation, otherwise  null .

    init {
        val receipt = txInfo.receipt

        val transactionHash = toJsonHex(receipt.transaction.hash)
        val transactionIndex = txInfo.index
        val cumulativeGasUsed = ByteUtil.byteArrayToLong(receipt.cumulativeGas)
        gasUsed = ByteUtil.byteArrayToLong(receipt.gasUsed)
        if (receipt.transaction.contractAddress != null)
            contractAddress = toJsonHex(receipt.transaction.contractAddress)
        val logs = arrayOfNulls<JsonRpc.LogFilterElement>(receipt.logInfoList.size)
        if (block != null) {
            blockNumber = block.number
            val blockHash = toJsonHex(txInfo.blockHash)
            for (i in logs.indices) {
                val logInfo = receipt.logInfoList[i]
                logs[i] = JsonRpc.LogFilterElement(logInfo, block, txInfo.index,
                        txInfo.receipt.transaction, i)
            }
        }
    }
}
