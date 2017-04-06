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

package org.ethereum.net.swarm;

import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.min;

/**
 * Created by Admin on 17.06.2015.
 */
public class Util {

    // for testing purposes when the timer might be changed
    // to manage current time according to test scenarios
    private static final Timer TIMER = new Timer();

    public static String getCommonPrefix(final String s1, final String s2) {
        int pos = 0;
        while(pos < s1.length() && pos < s2.length() && s1.charAt(pos) == s2.charAt(pos)) pos++;
        return s1.substring(0, pos);
    }

    public static String ipBytesToString(final byte[] ipAddr) {
        final StringBuilder sip = new StringBuilder();
        for (int i = 0; i < ipAddr.length; i++) {
            sip.append(i == 0 ? "" : ".").append(0xFF & ipAddr[i]);
        }
        return sip.toString();
    }

    public static <P extends StringTrie.TrieNode<P>> String dumpTree(final P n) {
        return dumpTree(n, 0);
    }

    private static <P extends StringTrie.TrieNode<P>> String dumpTree(final P n, final int indent) {
        final StringBuilder ret = new StringBuilder(Utils.repeat("  ", indent) + "[" + n.path + "] " + n + "\n");
        for (final P c : n.getChildren()) {
            ret.append(dumpTree(c, indent + 1));
        }
        return ret.toString();
    }

    public static byte[] uInt16ToBytes(final int uInt16) {
        return new byte[] {(byte) ((uInt16 >> 8) & 0xFF), (byte) (uInt16 & 0xFF)};
    }

    public static long curTime() { return TIMER.curTime();}

    private static byte[] rlpEncodeLong(final long n) {
        // TODO for now leaving int cast
        return RLP.encodeInt((int) n);
    }

    public static byte rlpDecodeByte(final RLPElement elem) {
        return (byte) rlpDecodeInt(elem);
    }

    public static long rlpDecodeLong(final RLPElement elem) {
        return rlpDecodeInt(elem);
    }

    public static int rlpDecodeInt(final RLPElement elem) {
        final byte[] b = elem.getRLPData();
        if (b == null) return 0;
        return ByteUtil.byteArrayToInt(b);
    }

    public static String rlpDecodeString(final RLPElement elem) {
        final byte[] b = elem.getRLPData();
        if (b == null) return null;
        return new String(b);
    }

    public static byte[] rlpEncodeList(final Object... elems) {
        final byte[][] encodedElems = new byte[elems.length][];
        for (int i =0; i < elems.length; i++) {
            if (elems[i] instanceof Byte) {
                encodedElems[i] = RLP.encodeByte((Byte) elems[i]);
            } else if (elems[i] instanceof Integer) {
                encodedElems[i] = RLP.encodeInt((Integer) elems[i]);
            } else if (elems[i] instanceof Long) {
                encodedElems[i] = rlpEncodeLong((Long) elems[i]);
            } else if (elems[i] instanceof String) {
                encodedElems[i] = RLP.encodeString((String) elems[i]);
            } else if (elems[i] instanceof byte[]) {
                encodedElems[i] = ((byte[]) elems[i]);
            } else {
                throw new RuntimeException("Unsupported object: " + elems[i]);
            }
        }
        return RLP.encodeList(encodedElems);
    }

    public static SectionReader stringToReader(final String s) {
        return new ArrayReader(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String readerToString(final SectionReader sr) {
        final byte[] bb = new byte[(int) sr.getSize()];
        sr.read(bb, 0);
        final String s = new String(bb, StandardCharsets.UTF_8);
        return s;
    }

    public static class ChunkConsumer extends LinkedBlockingQueue<Chunk> {
        final ChunkStore destination;
        final boolean synchronous = true;

        public ChunkConsumer(final ChunkStore destination) {
            this.destination = destination;
        }

        @Override
        public boolean add(final Chunk chunk) {
            if (synchronous) {
                destination.put(chunk);
                return true;
            } else {
                return super.add(chunk);
            }
        }
    }

    public static class ArrayReader implements SectionReader {
        final byte[] arr;

        public ArrayReader(final byte[] arr) {
            this.arr = arr;
        }

        @Override
        public long seek(final long offset, final int whence) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public int read(final byte[] dest, final int destOff) {
            return readAt(dest, destOff, 0);
        }

        @Override
        public int readAt(final byte[] dest, final int destOff, final long readerOffset) {
            final int len = min(dest.length - destOff, arr.length - (int) readerOffset);
            System.arraycopy(arr, (int) readerOffset, dest, destOff, len);
            return len;
        }

        @Override
        public long getSize() {
            return arr.length;
        }
    }

    public static class Timer {
        public long curTime() {
            return System.currentTimeMillis();
        }
    }
}
