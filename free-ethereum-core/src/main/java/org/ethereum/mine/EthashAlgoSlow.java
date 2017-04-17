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
 * This is the non-optimized Ethash implementation. It is left here for reference only
 * since the non-optimized version is slightly better for understanding the Ethash algorithm
 *
 * Created by Anton Nashatyrev on 27.11.2015.
 * @deprecated Use a faster version {@link EthashAlgo}, this class is for reference only
 */
public class EthashAlgoSlow {
    private static final long FNV_PRIME = 0x01000193;
    private final EthashParams params;

    public EthashAlgoSlow() {
        this(new EthashParams());
    }

    public EthashAlgoSlow(final EthashParams params) {
        this.params = params;
    }

    // Little-Endian !
    private static long getWord(final byte[] arr, final int wordOff) {
        return ByteBuffer.wrap(arr, wordOff * 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
    }

    private static void setWord(final byte[] arr, final int wordOff, final long val) {
        final ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) val);
        bb.rewind();
        bb.get(arr, wordOff * 4, 4);
    }

    public EthashParams getParams() {
        return params;
    }

    public byte[][] makeCache(final long cacheSize, final byte[] seed) {
        final int n = (int) (cacheSize / params.getHASH_BYTES());
        final byte[][] o = new byte[n][];
        o[0] = HashUtil.INSTANCE.sha512(seed);
        for (int i = 1; i < n; i++) {
            o[i] = HashUtil.INSTANCE.sha512(o[i - 1]);
        }

        for (int cacheRound = 0; cacheRound < params.getCACHE_ROUNDS(); cacheRound++) {
            for (int i = 0; i < n; i++) {
                final int v = (int) (getWord(o[i], 0) % n);
                o[i] = HashUtil.INSTANCE.sha512(xor(o[(i - 1 + n) % n], o[v]));
            }
        }
        return o;
    }

    private long fnv(final long v1, final long v2) {
        return ((v1 * FNV_PRIME) ^ v2) % (1L << 32);
    }

    private byte[] fnv(final byte[] b1, final byte[] b2) {
        if (b1.length != b2.length || b1.length % 4 != 0) throw new RuntimeException();

        final byte[] ret = new byte[b1.length];
        for (int i = 0; i < b1.length / 4; i++) {
            final long i1 = getWord(b1, i);
            final long i2 = getWord(b2, i);
            setWord(ret, i, fnv(i1, i2));
        }
        return ret;
    }

    private byte[] calcDatasetItem(final byte[][] cache, final int i) {
        final int n = cache.length;
        final int r = params.getHASH_BYTES() / params.getWORD_BYTES();
        byte[] mix = cache[i % n].clone();

        setWord(mix, 0, i ^ getWord(mix, 0));
        mix = HashUtil.INSTANCE.sha512(mix);
        for (int j = 0; j < params.getDATASET_PARENTS(); j++) {
            final long cacheIdx = fnv(i ^ j, getWord(mix, j % r));
            mix = fnv(mix, cache[(int) (cacheIdx % n)]);
        }
        return HashUtil.INSTANCE.sha512(mix);
    }

    public byte[][] calcDataset(final long fullSize, final byte[][] cache) {
        final byte[][] ret = new byte[(int) (fullSize / params.getHASH_BYTES())][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = calcDatasetItem(cache, i);
        }
        return ret;
    }

    private Pair<byte[], byte[]> hashimoto(final byte[] blockHeaderTruncHash, final byte[] nonce, final long fullSize, final DatasetLookup lookup) {
//        if (nonce.length != 4) throw new RuntimeException("nonce.length != 4");

        final int w = params.getMIX_BYTES() / params.getWORD_BYTES();
        final int mixhashes = params.getMIX_BYTES() / params.getHASH_BYTES();
        final byte[] s = HashUtil.INSTANCE.sha512(merge(blockHeaderTruncHash, reverse(nonce)));
        byte[] mix = new byte[params.getMIX_BYTES()];
        for (int i = 0; i < mixhashes; i++) {
            arraycopy(s, 0, mix, i * s.length, s.length);
        }

        final int numFullPages = (int) (fullSize / params.getMIX_BYTES());
        for (int i = 0; i < params.getACCESSES(); i++) {
            final long p = fnv(i ^ getWord(s, 0), getWord(mix, i % w)) % numFullPages;
            final byte[] newData = new byte[params.getMIX_BYTES()];
            for (int j = 0; j < mixhashes; j++) {
                final byte[] lookup1 = lookup.lookup((int) (p * mixhashes + j));
                arraycopy(lookup1, 0, newData, j * lookup1.length, lookup1.length);
            }
            mix = fnv(mix, newData);
        }

        final byte[] cmix = new byte[mix.length / 4];
        for (int i = 0; i < mix.length / 4; i += 4 /* ? */) {
            final long fnv1 = fnv(getWord(mix, i), getWord(mix, i + 1));
            final long fnv2 = fnv(fnv1, getWord(mix, i + 2));
            final long fnv3 = fnv(fnv2, getWord(mix, i + 3));
            setWord(cmix, i / 4, fnv3);
        }

        return Pair.of(cmix, HashUtil.INSTANCE.sha3(merge(s, cmix)));
    }

    private Pair<byte[], byte[]> hashimotoLight(final long fullSize, final byte[][] cache, final byte[] blockHeaderTruncHash,
                                                final byte[] nonce) {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, idx -> calcDatasetItem(cache, idx));
    }

    private Pair<byte[], byte[]> hashimotoFull(final long fullSize, final byte[][] dataset, final byte[] blockHeaderTruncHash,
                                               final byte[] nonce) {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, idx -> dataset[idx]);
    }

    public long mine(final long fullSize, final byte[][] dataset, final byte[] blockHeaderTruncHash, final long difficulty) {
        final BigInteger target = valueOf(2).pow(256).divide(valueOf(difficulty));
        long nonce = new Random().nextLong();
        while(!Thread.currentThread().isInterrupted()) {
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
    public long mineLight(final long fullSize, final byte[][] cache, final byte[] blockHeaderTruncHash, final long difficulty) {
        final BigInteger target = valueOf(2).pow(256).divide(valueOf(difficulty));
        long nonce = new Random().nextLong();
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

    private interface DatasetLookup {
        byte[] lookup(int idx);
    }
}
