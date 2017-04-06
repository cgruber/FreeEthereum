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

package org.ethereum.net.swarm.bzz;

import org.ethereum.net.swarm.Key;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BzzStoreReqMessage extends BzzMessage {

    // optional
    private byte[] metadata = new byte[0];
    private Key key;
    private byte[] data;

    public BzzStoreReqMessage(final byte[] encoded) {
        super(encoded);
    }

    public BzzStoreReqMessage(final long id, final Key key, final byte[] data) {
        this.id = id;
        this.key = key;
        this.data = data;
    }

    public BzzStoreReqMessage(final Key key, final byte[] data) {
        this.key = key;
        this.data = data;
    }

    @Override
    protected void decode() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        key = new Key(paramsList.get(0).getRLPData());
        data = paramsList.get(1).getRLPData();

        if (paramsList.size() > 2) {
            id = ByteUtil.byteArrayToLong(paramsList.get(2).getRLPData());
        }
        if (paramsList.size() > 3) {
            metadata = paramsList.get(2).getRLPData();
        }

        parsed = true;
    }

    private void encode() {
        final List<byte[]> elems = new ArrayList<>();
        elems.add(RLP.encodeElement(key.getBytes()));
        elems.add(RLP.encodeElement(data));
//        if (id >= 0 || metadata != null) {
            elems.add(RLP.encodeInt((int) id));
//        }
//        if (metadata != null) {
            elems.add(RLP.encodeList(metadata));
//        }
        this.encoded = RLP.encodeList(elems.toArray(new byte[0][]));
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.STORE_REQUEST;
    }

    public Key getKey() {
        return key;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "BzzStoreReqMessage{" +
                "key=" + key +
                ", data=" + Arrays.toString(data) +
                ", id=" + id +
                ", metadata=" + Arrays.toString(metadata) +
                '}';
    }
}
