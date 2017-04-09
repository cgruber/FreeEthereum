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
import java.util.List;

/**
 * Used for several purposes
 * - the main is to ask for a {@link org.ethereum.net.swarm.Chunk} with the specified hash
 * - ask to send back {#PEERS} message with the known nodes nearest to the specified hash
 * - initial request after handshake with zero hash. On this request the nearest known
 *   neighbours are sent back with the {#PEERS} message.
 */
public class BzzRetrieveReqMessage extends BzzMessage {

    private Key key;

    // optional
    private long maxSize = -1;
    private long maxPeers = -1;
    private long timeout = -1;

    public BzzRetrieveReqMessage(final byte[] encoded) {
        super(encoded);
    }

    public BzzRetrieveReqMessage(final Key key) {
        this.key = key;
    }

    @Override
    protected void decode() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        key = new Key(paramsList.get(0).getRLPData());

        if (paramsList.size() > 1) {
            id = ByteUtil.byteArrayToLong(paramsList.get(1).getRLPData());
        }
        if (paramsList.size() > 2) {
            maxSize = ByteUtil.byteArrayToLong(paramsList.get(2).getRLPData());
        }
        if (paramsList.size() > 3) {
            maxPeers = ByteUtil.byteArrayToLong(paramsList.get(3).getRLPData());
        }
        if (paramsList.size() > 4) {
            timeout = ByteUtil.byteArrayToLong(paramsList.get(3).getRLPData());
        }

        parsed = true;
    }

    private void encode() {
        final List<byte[]> elems = new ArrayList<>();
        elems.add(RLP.encodeElement(key.getBytes()));
        elems.add(RLP.encodeInt((int) id));
        elems.add(RLP.encodeInt((int) maxSize));
        elems.add(RLP.encodeInt((int) maxPeers));
        elems.add(RLP.encodeInt((int) timeout));
        this.encoded = RLP.encodeList(elems.toArray(new byte[0][]));

    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public Key getKey() {
        return key;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getMaxPeers() {
        return maxPeers;
    }

    public long getTimeout() {
        return timeout;
    }


    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.RETRIEVE_REQUEST;
    }

    @Override
    public String toString() {
        return "BzzRetrieveReqMessage{" +
                "key=" + key +
                ", id=" + id +
                ", maxSize=" + maxSize +
                ", maxPeers=" + maxPeers +
                '}';
    }
}
