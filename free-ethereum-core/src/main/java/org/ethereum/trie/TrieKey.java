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

package org.ethereum.trie;

import org.spongycastle.util.encoders.Hex;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Created by Anton Nashatyrev on 13.02.2017.
 */
public final class TrieKey {
    private static final int ODD_OFFSET_FLAG = 0x1;
    private static final int TERMINATOR_FLAG = 0x2;
    private final byte[] key;
    private final int off;
    private final boolean terminal;

    private TrieKey(final byte[] key, final int off, final boolean terminal) {
        this.terminal = terminal;
        this.off = off;
        this.key = key;
    }

    private TrieKey(final byte[] key) {
        this(key, 0, true);
    }

    public static TrieKey fromNormal(final byte[] key) {
        return new TrieKey(key);
    }

    public static TrieKey fromPacked(final byte[] key) {
        return new TrieKey(key, ((key[0] >> 4) & ODD_OFFSET_FLAG) != 0 ? 1 : 2, ((key[0] >> 4) & TERMINATOR_FLAG) != 0);
    }

    public static TrieKey empty(final boolean terminal) {
        return new TrieKey(EMPTY_BYTE_ARRAY, 0, terminal);
    }

    public static TrieKey singleHex(final int hex) {
        final TrieKey ret = new TrieKey(new byte[1], 1, false);
        ret.setHex(0, hex);
        return ret;
    }

    public byte[] toPacked() {
        final int flags = ((off & 1) != 0 ? ODD_OFFSET_FLAG : 0) | (terminal ? TERMINATOR_FLAG : 0);
        final byte[] ret = new byte[getLength() / 2 + 1];
        final int toCopy = (flags & ODD_OFFSET_FLAG) != 0 ? ret.length : ret.length - 1;
        System.arraycopy(key, key.length - toCopy, ret, ret.length - toCopy, toCopy);
        ret[0] &= 0x0F;
        ret[0] |= flags << 4;
        return ret;
    }

    public byte[] toNormal() {
        if ((off & 1) != 0) throw new RuntimeException("Can't convert a key with odd number of hexes to normal: " + this);
        final int arrLen = key.length - off / 2;
        final byte[] ret = new byte[arrLen];
        System.arraycopy(key, key.length - arrLen, ret, 0, arrLen);
        return ret;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isEmpty() {
        return getLength() == 0;
    }

    public TrieKey shift(final int hexCnt) {
        return new TrieKey(this.key, off + hexCnt, terminal);
    }

    public TrieKey getCommonPrefix(final TrieKey k) {
        // TODO can be optimized
        int prefixLen = 0;
        final int thisLenght = getLength();
        final int kLength = k.getLength();
        while (prefixLen < thisLenght && prefixLen < kLength && getHex(prefixLen) == k.getHex(prefixLen))
            prefixLen++;
        final byte[] prefixKey = new byte[(prefixLen + 1) >> 1];
        final TrieKey ret = new TrieKey(prefixKey, (prefixLen & 1) == 0 ? 0 : 1,
                prefixLen == getLength() && prefixLen == k.getLength() && terminal && k.isTerminal());
        for (int i = 0; i < prefixLen; i++) {
            ret.setHex(i, k.getHex(i));
        }
        return ret;
    }

    public TrieKey matchAndShift(final TrieKey k) {
        final int len = getLength();
        final int kLen = k.getLength();
        if (len < kLen) return null;

        if ((off & 1) == (k.off & 1)) {
            // optimization to compare whole keys bytes
            if ((off & 1) == 1) {
                if (getHex(0) != k.getHex(0)) return null;
            }
            int idx1 = (off + 1) >> 1;
            int idx2 = (k.off + 1) >> 1;
            final int l = kLen >> 1;
            for (int i = 0; i < l; i++, idx1++, idx2++) {
                if (key[idx1] != k.key[idx2]) return null;
            }
        } else {
            for (int i = 0; i < kLen; i++) {
                if (getHex(i) != k.getHex(i)) return null;
            }
        }
        return shift(kLen);
    }

    public int getLength() {
        return (key.length << 1) - off;
    }

    private void setHex(final int idx, final int hex) {
        final int byteIdx = (off + idx) >> 1;
        if (((off + idx) & 1) == 0) {
            key[byteIdx] &= 0x0F;
            key[byteIdx] |= hex << 4;
        } else {
            key[byteIdx] &= 0xF0;
            key[byteIdx] |= hex;
        }
    }

    public int getHex(final int idx) {
        final byte b = key[(off + idx) >> 1];
        return (((off + idx) & 1) == 0 ? (b >> 4) : b) & 0xF;
    }

    public TrieKey concat(final TrieKey k) {
        if (isTerminal()) throw new RuntimeException("Can' append to terminal key: " + this + " + " + k);
        final int len = getLength();
        final int kLen = k.getLength();
        final int newLen = len + kLen;
        final byte[] newKeyBytes = new byte[(newLen + 1) >> 1];
        final TrieKey ret = new TrieKey(newKeyBytes, newLen & 1, k.isTerminal());
        for (int i = 0; i < len; i++) {
            ret.setHex(i, getHex(i));
        }
        for (int i = 0; i < kLen; i++) {
            ret.setHex(len + i, k.getHex(i));
        }
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        final TrieKey k = (TrieKey) obj;
        final int len = getLength();

        if (len != k.getLength()) return false;
        // TODO can be optimized
        for (int i = 0; i < len; i++) {
            if (getHex(i) != k.getHex(i)) return false;
        }
        return isTerminal() == k.isTerminal();
    }

    @Override
    public String toString() {
        return Hex.toHexString(key).substring(off) + (isTerminal() ? "T" : "");
    }
}
