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

package org.ethereum.net.shh;

import org.ethereum.util.RLP;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

import static org.ethereum.crypto.HashUtil.sha3;

/**
 * @author by Konstantin Shabalin
 */
public class Topic {
    private String originalTopic;
    private byte[] fullTopic;
    private byte[] abrigedTopic = new byte[4];

    public Topic(final byte[] data) {
        this.abrigedTopic = data;
    }

    public Topic(final String data) {
        originalTopic = data;
        fullTopic = sha3(RLP.encode(originalTopic));
        this.abrigedTopic = buildAbrigedTopic(fullTopic);
    }

    public static Topic[] createTopics(final String... topicsString) {
        if (topicsString == null) return new Topic[0];
        final Topic[] topics = new Topic[topicsString.length];
        for (int i = 0; i < topicsString.length; i++) {
            topics[i] = new Topic(topicsString[i]);
        }
        return topics;
    }

    public byte[] getBytes() {
        return abrigedTopic;
    }

    private byte[] buildAbrigedTopic(final byte[] data) {
        final byte[] hash = sha3(data);
        final byte[] topic = new byte[4];
        System.arraycopy(hash, 0, topic, 0, 4);
        return topic;
    }

    public byte[] getAbrigedTopic() {
        return abrigedTopic;
    }

    public byte[] getFullTopic() {
        return fullTopic;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Topic))return false;
        return Arrays.equals(this.abrigedTopic, ((Topic) obj).getBytes());
    }

    @Override
    public String toString() {
        return "#" + Hex.toHexString(abrigedTopic) + (originalTopic == null ? "" : "(" + originalTopic + ")");
    }
}
