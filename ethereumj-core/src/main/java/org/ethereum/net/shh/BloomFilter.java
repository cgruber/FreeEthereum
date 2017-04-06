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

import java.util.BitSet;

/**
 * Created by Anton Nashatyrev on 24.09.2015.
 */
public class BloomFilter implements Cloneable {
    private static final int BITS_PER_BLOOM = 3;
    private static final int BLOOM_BYTES = 64;
    private final int[] counters = new int[BLOOM_BYTES * 8];
    private BitSet mask = new BitSet(BLOOM_BYTES * 8);

    private BloomFilter() {
    }

    private BloomFilter(final Topic topic) {
        addTopic(topic);
    }

    public BloomFilter(final byte[] bloomMask) {
        if (bloomMask.length != BLOOM_BYTES)
            throw new RuntimeException("Invalid bloom filter array length: " + bloomMask.length);
        mask = BitSet.valueOf(bloomMask);
    }

    public static BloomFilter createNone() {
        return new BloomFilter();
    }

    public static BloomFilter createAll() {
        final BloomFilter bloomFilter = new BloomFilter();
        bloomFilter.mask.set(0, bloomFilter.mask.length());
        return bloomFilter;
    }

    private void incCounters(final BitSet bs) {
        int idx = -1;
        while(true) {
            idx = bs.nextSetBit(idx + 1);
            if (idx < 0) break;
            counters[idx]++;
        }
    }

    private void decCounters(final BitSet bs) {
        int idx = -1;
        while(true) {
            idx = bs.nextSetBit(idx + 1);
            if (idx < 0) break;
            if (counters[idx] > 0) counters[idx]--;
        }
    }

    private BitSet getTopicMask(final Topic topic) {
        final BitSet topicMask = new BitSet(BLOOM_BYTES * 8);
        for (int i = 0; i < BITS_PER_BLOOM; i++) {
            int x = topic.getBytes()[i] & 0xFF;
            if ((topic.getBytes()[BITS_PER_BLOOM] & (1 << i)) != 0) {
                x += 256;
            }
            topicMask.set(x);
        }
        return topicMask;
    }

    public void addTopic(final Topic topic) {
        final BitSet topicMask = getTopicMask(topic);
        incCounters(topicMask);
        mask.or(topicMask);
    }

    public void removeTopic(final Topic topic) {
        final BitSet topicMask = getTopicMask(topic);
        decCounters(topicMask);
        int idx = -1;
        while(true) {
            idx = topicMask.nextSetBit(idx + 1);
            if (idx < 0) break;
            if (counters[idx] == 0) mask.clear(idx);
        }
    }


    public boolean hasTopic(final Topic topic) {
        final BitSet m = new BloomFilter(topic).mask;
        final BitSet m1 = (BitSet) m.clone();
        m1.and(mask);
        return m1.equals(m);
    }

    public byte[] toBytes() {
        final byte[] ret = new byte[BLOOM_BYTES];
        final byte[] bytes = mask.toByteArray();
        System.arraycopy(bytes, 0, ret, 0, bytes.length);
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof BloomFilter && mask.equals(((BloomFilter) obj).mask);

    }

    @Override
    protected BloomFilter clone() throws CloneNotSupportedException {
        try {
            return (BloomFilter) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
