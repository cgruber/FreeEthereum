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

package org.ethereum.vm.program;

import org.ethereum.vm.DataWord;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class InternalTransactionTest {

    private static byte[] randomBytes(final int len) {
        final byte[] bytes = new byte[len];
        new Random().nextBytes(bytes);
        return bytes;
    }

    @Test
    public void testRlpEncoding() {
        final byte[] parentHash = randomBytes(32);
        final int deep = Integer.MAX_VALUE;
        final int index = Integer.MAX_VALUE;
        final byte[] nonce = randomBytes(2);
        final DataWord gasPrice = DataWord.ZERO;
        final DataWord gasLimit = DataWord.ZERO;
        final byte[] sendAddress = randomBytes(20);
        final byte[] receiveAddress = randomBytes(20);
        final byte[] value = randomBytes(2);
        final byte[] data = randomBytes(128);
        final String note = "transaction note";

        final byte[] encoded = new InternalTransaction(parentHash, deep, index, nonce, gasPrice, gasLimit, sendAddress, receiveAddress, value, data, note).getEncoded();

        final InternalTransaction tx = new InternalTransaction(encoded);

        assertEquals(deep, tx.getDeep());
        assertEquals(index, tx.getIndex());
        assertArrayEquals(parentHash, tx.getParentHash());
        assertArrayEquals(nonce, tx.getNonce());
        assertArrayEquals(gasPrice.getData(), tx.getGasPrice());
        assertArrayEquals(gasLimit.getData(), tx.getGasLimit());
        assertArrayEquals(sendAddress, tx.getSender());
        assertArrayEquals(receiveAddress, tx.getReceiveAddress());
        assertArrayEquals(value, tx.getValue());
        assertArrayEquals(data, tx.getData());
        assertEquals(note, tx.getNote());
    }

}