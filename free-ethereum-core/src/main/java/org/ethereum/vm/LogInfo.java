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

package org.ethereum.vm;

import org.ethereum.core.Bloom;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPItem;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 19.11.2014
 */
public class LogInfo {

    private byte[] address = new byte[]{};
    private List<DataWord> topics = new ArrayList<>();
    private byte[] data = new byte[]{};

    public LogInfo(final byte[] rlp) {

        final RLPList params = RLP.decode2(rlp);
        final RLPList logInfo = (RLPList) params.get(0);

        final RLPItem address = (RLPItem) logInfo.get(0);
        final RLPList topics = (RLPList) logInfo.get(1);
        final RLPItem data = (RLPItem) logInfo.get(2);

        this.address = address.getRLPData() != null ? address.getRLPData() : new byte[]{};
        this.data = data.getRLPData() != null ? data.getRLPData() : new byte[]{};

        for (final RLPElement topic1 : topics) {
            final byte[] topic = topic1.getRLPData();
            this.topics.add(new DataWord(topic));
        }

        final byte[] rlpEncoded = rlp;
    }

    public LogInfo(final byte[] address, final List<DataWord> topics, final byte[] data) {
        this.address = (address != null) ? address : new byte[]{};
        this.topics = (topics != null) ? topics : new ArrayList<>();
        this.data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return address;
    }

    public List<DataWord> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    /*  [address, [topic, topic ...] data] */
    public byte[] getEncoded() {

        final byte[] addressEncoded = RLP.encodeElement(this.address);

        byte[][] topicsEncoded = null;
        if (topics != null) {
            topicsEncoded = new byte[topics.size()][];
            int i = 0;
            for (final DataWord topic : topics) {
                final byte[] topicData = topic.getData();
                topicsEncoded[i] = RLP.encodeElement(topicData);
                ++i;
            }
        }

        final byte[] dataEncoded = RLP.encodeElement(data);
        return RLP.encodeList(addressEncoded, RLP.encodeList(topicsEncoded), dataEncoded);
    }

    public Bloom getBloom() {
        final Bloom ret = Bloom.create(HashUtil.INSTANCE.sha3(address));
        for (final DataWord topic : topics) {
            final byte[] topicData = topic.getData();
            ret.or(Bloom.create(HashUtil.INSTANCE.sha3(topicData)));
        }
        return ret;
    }

    @Override
    public String toString() {

        final StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (final DataWord topic : topics) {
            final String topicStr = Hex.toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");


        return "LogInfo{" +
                "address=" + Hex.toHexString(address) +
                ", topics=" + topicsStr +
                ", data=" + Hex.toHexString(data) +
                '}';
    }


}
