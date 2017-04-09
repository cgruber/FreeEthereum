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

package org.ethereum.vm.program

import org.ethereum.vm.DataWord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class InternalTransactionTest {

    private fun randomBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        Random().nextBytes(bytes)
        return bytes
    }

    @Test
    fun testRlpEncoding() {
        val parentHash = randomBytes(32)
        val deep = Integer.MAX_VALUE
        val index = Integer.MAX_VALUE
        val nonce = randomBytes(2)
        val gasPrice = DataWord.ZERO
        val gasLimit = DataWord.ZERO
        val sendAddress = randomBytes(20)
        val receiveAddress = randomBytes(20)
        val value = randomBytes(2)
        val data = randomBytes(128)
        val note = "transaction note"

        val encoded = InternalTransaction(parentHash, deep, index, nonce, gasPrice, gasLimit, sendAddress, receiveAddress, value, data, note).encoded

        val tx = InternalTransaction(encoded)

        assertEquals(deep.toLong(), tx.deep.toLong())
        assertEquals(index.toLong(), tx.index.toLong())
        assertArrayEquals(parentHash, tx.parentHash)
        assertArrayEquals(nonce, tx.nonce)
        assertArrayEquals(gasPrice.data, tx.gasPrice)
        assertArrayEquals(gasLimit.data, tx.gasLimit)
        assertArrayEquals(sendAddress, tx.sender)
        assertArrayEquals(receiveAddress, tx.receiveAddress)
        assertArrayEquals(value, tx.value)
        assertArrayEquals(data, tx.data)
        assertEquals(note, tx.note)
    }

}