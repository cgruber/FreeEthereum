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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

/**
 * @author Roman Mandeleil
 * *
 * @since 05.12.2014
 */
class TransactionReceiptTest {


    @Test // rlp decode
    fun test_1() {

        val rlp = Hex.decode("f88aa0966265cc49fa1f10f0445f035258d116563931022a3570a640af5d73a214a8da822b6fb84000000010000000010000000000008000000000000000000000000000000000000000000000000000000000020000000000000014000000000400000000000440d8d7948513d39a34a1a8570c9c9f0af2cba79ac34e0ac8c0808301e24086873423437898")

        val txReceipt = TransactionReceipt(rlp)

        assertEquals(1, txReceipt.logInfoList.size.toLong())

        assertEquals("966265cc49fa1f10f0445f035258d116563931022a3570a640af5d73a214a8da",
                Hex.toHexString(txReceipt.postTxState))

        assertEquals("2b6f",
                Hex.toHexString(txReceipt.cumulativeGas))

        assertEquals("01e240",
                Hex.toHexString(txReceipt.gasUsed))

        assertEquals("00000010000000010000000000008000000000000000000000000000000000000000000000000000000000020000000000000014000000000400000000000440",
                Hex.toHexString(txReceipt.bloomFilter.getData()))

        assertEquals("873423437898",
                Hex.toHexString(txReceipt.executionResult))
        logger.info("{}", txReceipt)
    }

    @Test
    fun test_2() {
        val rlp = Hex.decode("f9012ea02d0cd041158c807326dae7cf5f044f3b9d4bd91a378cc55781b75455206e0c368339dc68b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c08252088080")

        val txReceipt = TransactionReceipt(rlp)
        txReceipt.executionResult = ByteArray(0)
        val encoded = txReceipt.encoded
        val txReceipt1 = TransactionReceipt(encoded)
        println(txReceipt1)

    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
    }
}
