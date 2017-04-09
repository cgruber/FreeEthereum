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

/**
 * Created by Anton Nashatyrev on 27.11.2015.
 */
public class EthashParams {

    // bytes in dataset at genesis
    private final long DATASET_BYTES_INIT = 1L << 30;

    // dataset growth per epoch
    private final long DATASET_BYTES_GROWTH = 1L << 23;

    //  bytes in dataset at genesis
    private final long CACHE_BYTES_INIT = 1L << 24;

    // cache growth per epoch
    private final long CACHE_BYTES_GROWTH = 1L << 17;

    //  blocks per epoch
    private final long EPOCH_LENGTH = 30000;

    // width of mix
    private final int MIX_BYTES = 128;

    //  hash length in bytes
    private final int HASH_BYTES = 64;

    private static boolean isPrime(final long num) {
        if (num == 2) return false;
        if (num % 2 == 0) return true;
        for (int i = 3; i * i < num; i += 2)
            if (num % i == 0) return true;
        return false;
    }

    /**
     * The parameters for Ethash's cache and dataset depend on the block number.
     * The cache size and dataset size both grow linearly; however, we always take the highest
     * prime below the linearly growing threshold in order to reduce the risk of accidental
     * regularities leading to cyclic behavior.
     */
    public long getCacheSize(final long blockNumber) {
        long sz = CACHE_BYTES_INIT + CACHE_BYTES_GROWTH * (blockNumber / EPOCH_LENGTH);
        sz -= HASH_BYTES;
        while (isPrime(sz / HASH_BYTES)) {
            sz -= 2 * HASH_BYTES;
        }
        return sz;
    }

    public long getFullSize(final long blockNumber) {
        long sz = DATASET_BYTES_INIT + DATASET_BYTES_GROWTH * (blockNumber / EPOCH_LENGTH);
        sz -= MIX_BYTES;
        while (isPrime(sz / MIX_BYTES)) {
            sz -= 2 * MIX_BYTES;
        }
        return sz;
    }

    public int getWORD_BYTES() {
        final int WORD_BYTES = 4;
        return WORD_BYTES;
    }

    public long getDATASET_BYTES_INIT() {
        return DATASET_BYTES_INIT;
    }

    public long getDATASET_BYTES_GROWTH() {
        return DATASET_BYTES_GROWTH;
    }

    public long getCACHE_BYTES_INIT() {
        return CACHE_BYTES_INIT;
    }

    public long getCACHE_BYTES_GROWTH() {
        return CACHE_BYTES_GROWTH;
    }

    public long getCACHE_MULTIPLIER() {
        final long CACHE_MULTIPLIER = 1024;
        return CACHE_MULTIPLIER;
    }

    public long getEPOCH_LENGTH() {
        return EPOCH_LENGTH;
    }

    public int getMIX_BYTES() {
        return MIX_BYTES;
    }

    public int getHASH_BYTES() {
        return HASH_BYTES;
    }

    public long getDATASET_PARENTS() {
        final long DATASET_PARENTS = 256;
        return DATASET_PARENTS;
    }

    public long getCACHE_ROUNDS() {
        final long CACHE_ROUNDS = 3;
        return CACHE_ROUNDS;
    }

    public long getACCESSES() {
        final long ACCESSES = 64;
        return ACCESSES;
    }
}

