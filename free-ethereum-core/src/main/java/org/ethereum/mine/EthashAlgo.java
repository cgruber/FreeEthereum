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

package org.ethereum.mine;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.crypto.HashUtil;
import org.spongycastle.util.Arrays;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import static java.lang.System.arraycopy;
import static java.math.BigInteger.valueOf;
import static org.ethereum.util.ByteUtil.*;
import static org.spongycastle.util.Arrays.reverse;

/**
 * The Ethash algorithm described in https://github.com/ethereum/wiki/wiki/Ethash
 *
 * Created by Anton Nashatyrev on 27.11.2015.
 */
class EthashAlgo {
    private static final int FNV_PRIME = 0x01000193;
    private final EthashParams params;

    public EthashAlgo() {
        this(new EthashParams());
    }

    public EthashAlgo(final EthashParams params) {
        this.params = params;
    }

    // Little-Endian !
    private static int getWord(final byte[] arr, final int wordOff) {
        return ByteBuffer.wrap(arr, wordOff * 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    static void setWord(final byte[] arr, final int wordOff, final long val) {
        final ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) val);
        bb.rewind();
        bb.get(arr, wordOff * 4, 4);
    }

    private static int remainderUnsigned(int dividend, final int divisor) {
        if (divisor >= 0) {
            if (dividend >= 0) {
                return dividend % divisor;
            }
            // The implementation is a Java port of algorithm described in the book
            // "Hacker's Delight" (section "Unsigned short division from signed division").
            final int q = ((dividend >>> 1) / divisor) << 1;
            dividend -= q * divisor;
            if (dividend < 0 || dividend >= divisor) {
                dividend -= divisor;
            }
            return dividend;
        }
        return dividend >= 0 || dividend < divisor ? dividend : dividend - divisor;
    }

    private static int fnv(final int v1, final int v2) {
        return (v1 * FNV_PRIME) ^ v2;
    }

    public EthashParams getParams() {
        return params;
    }

    private byte[][] makeCacheBytes(final long cacheSize, final byte[] seed) {
        final int n = (int) (cacheSize / params.getHASH_BYTES());
        final byte[][] o = new byte[n][];
        o[0] = HashUtil.INSTANCE.sha512(seed);
        for (int i = 1; i < n; i++) {
            o[i] = HashUtil.INSTANCE.sha512(o[i - 1]);
        }

        for (int cacheRound = 0; cacheRound < params.getCACHE_ROUNDS(); cacheRound++) {
            for (int i = 0; i < n; i++) {
                final int v = remainderUnsigned(getWord(o[i], 0), n);
                o[i] = HashUtil.INSTANCE.sha512(xor(o[(i - 1 + n) % n], o[v]));
            }
        }
        return o;
    }

    public int[] makeCache(final long cacheSize, final byte[] seed) {
        final byte[][] bytes = makeCacheBytes(cacheSize, seed);
        final int[] ret = new int[bytes.length * bytes[0].length / 4];
        final int[] ints = new int[bytes[0].length / 4];
        for (int i = 0; i < bytes.length; i++) {
            bytesToInts(bytes[i], ints, false);
            arraycopy(ints, 0, ret, i * ints.length, ints.length);
        }
        return ret;
    }

    private int[] sha512(final int[] arr, final boolean bigEndian) {
        byte[] bytesTmp = new byte[arr.length << 2];
        intsToBytes(arr, bytesTmp, bigEndian);
        bytesTmp = HashUtil.INSTANCE.sha512(bytesTmp);
        bytesToInts(bytesTmp, arr, bigEndian);
        return arr;
    }

    public final int[] calcDatasetItem(final int[] cache, final int i) {
        final int r = params.getHASH_BYTES() / params.getWORD_BYTES();
        final int n = cache.length / r;
        int[] mix = Arrays.copyOfRange(cache, i % n * r, (i % n + 1) * r);

        mix[0] = i ^ mix[0];
        mix = sha512(mix, false);
        final int dsParents = (int) params.getDATASET_PARENTS();
        final int mixLen = mix.length;
        for (int j = 0; j < dsParents; j++) {
            int cacheIdx = fnv(i ^ j, mix[j % r]);
            cacheIdx = remainderUnsigned(cacheIdx, n);
            final int off = cacheIdx * r;
            for (int k = 0; k < mixLen; k++) {
                mix[k] = fnv(mix[k], cache[off + k]);
            }
        }
        return sha512(mix, false);
    }

    public int[] calcDataset(final long fullSize, final int[] cache) {
        final int hashesCount = (int) (fullSize / params.getHASH_BYTES());
        final int[] ret = new int[hashesCount * (params.getHASH_BYTES() / 4)];
        for (int i = 0; i < hashesCount; i++) {
            final int[] item = calcDatasetItem(cache, i);
            arraycopy(item, 0, ret, i * (params.getHASH_BYTES() / 4), item.length);
        }
        return ret;
    }

    private Pair<byte[], byte[]> hashimoto(final byte[] blockHeaderTruncHash, final byte[] nonce, final long fullSize,
                                           final int[] cacheOrDataset, final boolean full) {
        if (nonce.length != 8) throw new RuntimeException("nonce.length != 8");

        final int hashWords = params.getHASH_BYTES() / 4;
        final int w = params.getMIX_BYTES() / params.getWORD_BYTES();
        final int mixhashes = params.getMIX_BYTES() / params.getHASH_BYTES();
        final int[] s = bytesToInts(HashUtil.INSTANCE.sha512(merge(blockHeaderTruncHash, reverse(nonce))), false);
        final int[] mix = new int[params.getMIX_BYTES() / 4];
        for (int i = 0; i < mixhashes; i++) {
            arraycopy(s, 0, mix, i * s.length, s.length);
        }

        final int numFullPages = (int) (fullSize / params.getMIX_BYTES());
        for (int i = 0; i < params.getACCESSES(); i++) {
            final int p = remainderUnsigned(fnv(i ^ s[0], mix[i % w]), numFullPages);
            final int[] newData = new int[mix.length];
            final int off = p * mixhashes;
            for (int j = 0; j < mixhashes; j++) {
                final int itemIdx = off + j;
                if (!full) {
                    final int[] lookup1 = calcDatasetItem(cacheOrDataset, itemIdx);
                    arraycopy(lookup1, 0, newData, j * lookup1.length, lookup1.length);
                } else {
                    arraycopy(cacheOrDataset, itemIdx * hashWords, newData, j * hashWords, hashWords);
                }
            }
            for (int i1 = 0; i1 < mix.length; i1++) {
                mix[i1] = fnv(mix[i1], newData[i1]);
            }
        }

        final int[] cmix = new int[mix.length / 4];
        for (int i = 0; i < mix.length; i += 4 /* ? */) {
            final int fnv1 = fnv(mix[i], mix[i + 1]);
            final int fnv2 = fnv(fnv1, mix[i + 2]);
            final int fnv3 = fnv(fnv2, mix[i + 3]);
            cmix[i >> 2] = fnv3;
        }

        return Pair.of(intsToBytes(cmix, false), HashUtil.INSTANCE.sha3(merge(intsToBytes(s, false), intsToBytes(cmix, false))));
    }

    public Pair<byte[], byte[]> hashimotoLight(final long fullSize, final int[] cache, final byte[] blockHeaderTruncHash,
                                               final byte[] nonce) {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, cache, false);
    }

    public Pair<byte[], byte[]> hashimotoFull(final long fullSize, final int[] dataset, final byte[] blockHeaderTruncHash,
                                              final byte[] nonce) {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, dataset, true);
    }

    public long mine(final long fullSize, final int[] dataset, final byte[] blockHeaderTruncHash, final long difficulty) {
        return mine(fullSize, dataset, blockHeaderTruncHash, difficulty, new Random().nextLong());
    }

    public long mine(final long fullSize, final int[] dataset, final byte[] blockHeaderTruncHash, final long difficulty, final long startNonce) {
        long nonce = startNonce;
        final BigInteger target = valueOf(2).pow(256).divide(valueOf(difficulty));
        while (!Thread.currentThread().isInterrupted()) {
            nonce++;
            final Pair<byte[], byte[]> pair = hashimotoFull(fullSize, dataset, blockHeaderTruncHash, longToBytes(nonce));
            final BigInteger h = new BigInteger(1, pair.getRight() /* ?? */);
            if (h.compareTo(target) < 0) break;
        }
        return nonce;
    }

    /**
     * This the slower miner version which uses only cache thus taking much less memory than
     * regular {@link #mine} method
     */
    public long mineLight(final long fullSize, final int[] cache, final byte[] blockHeaderTruncHash, final long difficulty) {
        return mineLight(fullSize, cache, blockHeaderTruncHash, difficulty, new Random().nextLong());
    }

    public long mineLight(final long fullSize, final int[] cache, final byte[] blockHeaderTruncHash, final long difficulty, final long startNonce) {
        long nonce = startNonce;
        final BigInteger target = valueOf(2).pow(256).divide(valueOf(difficulty));
        while(!Thread.currentThread().isInterrupted()) {
            nonce++;
            final Pair<byte[], byte[]> pair = hashimotoLight(fullSize, cache, blockHeaderTruncHash, longToBytes(nonce));
            final BigInteger h = new BigInteger(1, pair.getRight() /* ?? */);
            if (h.compareTo(target) < 0) break;
        }
        return nonce;
    }

    public byte[] getSeedHash(final long blockNumber) {
        byte[] ret = new byte[32];
        for (int i = 0; i < blockNumber / params.getEPOCH_LENGTH(); i++) {
            ret = HashUtil.INSTANCE.sha3(ret);
        }
        return ret;
    }
}
