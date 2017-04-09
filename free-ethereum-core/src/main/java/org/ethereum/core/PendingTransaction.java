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

package org.ethereum.core;

import org.ethereum.util.ByteUtil;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Decorates {@link Transaction} class with additional attributes
 * related to Pending Transaction logic
 *
 * @author Mikhail Kalinin
 * @since 11.08.2015
 */
class PendingTransaction {

    /**
     * transaction
     */
    private Transaction transaction;

    /**
     * number of block that was best at the moment when transaction's been added
     */
    private long blockNumber;

    public PendingTransaction(final byte[] bytes) {
        parse(bytes);
    }

    public PendingTransaction(final Transaction transaction) {
        this(transaction, 0);
    }

    public PendingTransaction(final Transaction transaction, final long blockNumber) {
        this.transaction = transaction;
        this.blockNumber = blockNumber;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    private byte[] getSender() {
        return transaction.getSender();
    }

    public byte[] getHash() {
        return transaction.getHash();
    }

    public byte[] getBytes() {
        final byte[] numberBytes = BigInteger.valueOf(blockNumber).toByteArray();
        final byte[] txBytes = transaction.getEncoded();
        final byte[] bytes = new byte[1 + numberBytes.length + txBytes.length];

        bytes[0] = (byte) numberBytes.length;
        System.arraycopy(numberBytes, 0, bytes, 1, numberBytes.length);

        System.arraycopy(txBytes, 0, bytes, 1 + numberBytes.length, txBytes.length);

        return bytes;
    }

    private void parse(final byte[] bytes) {
        final byte[] numberBytes = new byte[bytes[0]];
        final byte[] txBytes = new byte[bytes.length - 1 - numberBytes.length];

        System.arraycopy(bytes, 1, numberBytes, 0, numberBytes.length);

        System.arraycopy(bytes, 1 + numberBytes.length, txBytes, 0, txBytes.length);

        this.blockNumber = new BigInteger(numberBytes).longValue();
        this.transaction = new Transaction(txBytes);
    }

    @Override
    public String toString() {
        return "PendingTransaction [" +
                "  transaction=" + transaction +
                ", blockNumber=" + blockNumber +
                ']';
    }

    /**
     *  Two pending transaction are equal if equal their sender + nonce
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingTransaction)) return false;

        final PendingTransaction that = (PendingTransaction) o;

        return Arrays.equals(getSender(), that.getSender()) &&
                Arrays.equals(transaction.getNonce(), that.getTransaction().getNonce());
    }

    @Override
    public int hashCode() {
        return ByteUtil.byteArrayToInt(getSender()) + ByteUtil.byteArrayToInt(transaction.getNonce());
    }
}
