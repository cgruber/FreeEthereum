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

import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.vm.DataWord;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.apache.commons.lang3.ArrayUtils.*;
import static org.ethereum.util.ByteUtil.toHexString;

public class InternalTransaction extends Transaction {

    private byte[] parentHash;
    private int deep;
    private int index;
    private boolean rejected = false;
    private String note;

    public InternalTransaction(final byte[] rawData) {
        super(rawData);
    }

    public InternalTransaction(final byte[] parentHash, final int deep, final int index, final byte[] nonce, final DataWord gasPrice, final DataWord gasLimit,
                               final byte[] sendAddress, final byte[] receiveAddress, final byte[] value, final byte[] data, final String note) {

        super(nonce, getData(gasPrice), getData(gasLimit), receiveAddress, nullToEmpty(value), nullToEmpty(data));

        this.parentHash = parentHash;
        this.deep = deep;
        this.index = index;
        this.sendAddress = nullToEmpty(sendAddress);
        this.note = note;
        this.parsed = true;
    }

    private static byte[] getData(final DataWord gasPrice) {
        return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
    }

    private static byte[] intToBytes(final int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    private static int bytesToInt(final byte[] bytes) {
        return isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static byte[] encodeInt(final int value) {
        return RLP.encodeElement(intToBytes(value));
    }

    private static int decodeInt(final byte[] encoded) {
        return bytesToInt(encoded);
    }

    public void reject() {
        this.rejected = true;
    }

    public int getDeep() {
        rlpParse();
        return deep;
    }

    public int getIndex() {
        rlpParse();
        return index;
    }

    private boolean isRejected() {
        rlpParse();
        return rejected;
    }

    public String getNote() {
        rlpParse();
        return note;
    }

    @Override
    public byte[] getSender() {
        rlpParse();
        return sendAddress;
    }

    public byte[] getParentHash() {
        rlpParse();
        return parentHash;
    }

    @Override
    public byte[] getEncoded() {
        if (rlpEncoded == null) {

            final byte[] nonce = getNonce();
            final boolean isEmptyNonce = isEmpty(nonce) || (getLength(nonce) == 1 && nonce[0] == 0);

            this.rlpEncoded = RLP.encodeList(
                    RLP.encodeElement(isEmptyNonce ? null : nonce),
                    RLP.encodeElement(this.parentHash),
                    RLP.encodeElement(getSender()),
                    RLP.encodeElement(getReceiveAddress()),
                    RLP.encodeElement(getValue()),
                    RLP.encodeElement(getGasPrice()),
                    RLP.encodeElement(getGasLimit()),
                    RLP.encodeElement(getData()),
                    RLP.encodeString(this.note),
                    encodeInt(this.deep),
                    encodeInt(this.index),
                    encodeInt(this.rejected ? 1 : 0)
            );
        }

        return rlpEncoded;
    }

    @Override
    public byte[] getEncodedRaw() {
        return getEncoded();
    }

    @Override
    public synchronized void rlpParse() {
        if (parsed) return;
        final RLPList decodedTxList = RLP.decode2(rlpEncoded);
        final RLPList transaction = (RLPList) decodedTxList.get(0);

        setNonce(transaction.get(0).getRLPData());
        this.parentHash = transaction.get(1).getRLPData();
        this.sendAddress = transaction.get(2).getRLPData();
        setReceiveAddress(transaction.get(3).getRLPData());
        setValue(transaction.get(4).getRLPData());
        setGasPrice(transaction.get(5).getRLPData());
        setGasLimit(transaction.get(6).getRLPData());
        setData(transaction.get(7).getRLPData());
        this.note = new String(transaction.get(8).getRLPData());
        this.deep = decodeInt(transaction.get(9).getRLPData());
        this.index = decodeInt(transaction.get(10).getRLPData());
        this.rejected = decodeInt(transaction.get(11).getRLPData()) == 1;

        this.parsed = true;
    }

    @Override
    public ECKey getKey() {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    @Override
    public void sign(final byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        throw new UnsupportedOperationException("Cannot sign internal transaction.");
    }

    @Override
    public String toString() {
        return "TransactionData [" +
                "  parentHash=" + toHexString(getParentHash()) +
                ", hash=" + toHexString(getHash()) +
                ", nonce=" + toHexString(getNonce()) +
                ", gasPrice=" + toHexString(getGasPrice()) +
                ", gas=" + toHexString(getGasLimit()) +
                ", sendAddress=" + toHexString(getSender()) +
                ", receiveAddress=" + toHexString(getReceiveAddress()) +
                ", value=" + toHexString(getValue()) +
                ", data=" + toHexString(getData()) +
                ", note=" + getNote() +
                ", deep=" + getDeep() +
                ", index=" + getIndex() +
                ", rejected=" + isRejected() +
                "]";
    }
}
