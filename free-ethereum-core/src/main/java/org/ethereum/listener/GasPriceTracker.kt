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

import org.ethereum.core.BlockSummary
import org.ethereum.core.Transaction
import org.ethereum.util.ByteUtil

import java.util.Arrays

/**
 * Calculates a 'reasonable' Gas price based on statistics of the latest transaction's Gas prices

 * Normally the price returned should be sufficient to execute a transaction since ~25% of the latest
 * transactions were executed at this or lower price.

 * Created by Anton Nashatyrev on 22.09.2015.
 */
class GasPriceTracker : EthereumListenerAdapter() {

    private val window = LongArray(512)
    private var idx = window.size - 1
    private var filled = false

    private var lastVal: Long = 0

    override fun onBlock(blockSummary: BlockSummary) {
        for (tx in blockSummary.block.transactionsList) {
            onTransaction(tx)
        }
    }

    private fun onTransaction(tx: Transaction) {
        if (idx == -1) {
            idx = window.size - 1
            filled = true
            lastVal = 0  // recalculate only 'sometimes'
        }
        window[idx--] = ByteUtil.byteArrayToLong(tx.gasPrice)
    }

    // 25% percentile
    val gasPrice: Long
        get() {
            if (!filled) {
                return defaultPrice
            } else {
                if (lastVal.equals(0)) {
                    val longs = Arrays.copyOf(window, window.size)
                    Arrays.sort(longs)
                    lastVal = longs[longs.size / 4]
                }
                return lastVal
            }
        }

    companion object {

        private val defaultPrice = 70_000_000_000L
    }
}
