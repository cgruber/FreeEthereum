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

package org.ethereum;

import org.ethereum.core.Block;
import org.ethereum.crypto.HashUtil;
import org.ethereum.vm.DataWord;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TestUtils {

    private TestUtils() {
    }

    public static byte[] randomBytes(final int length) {
        final byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public static DataWord randomDataWord() {
        return new DataWord(randomBytes(32));
    }

    public static byte[] randomAddress() {
        return randomBytes(20);
    }

    public static List<Block> getRandomChain(final byte[] startParentHash, final long startNumber, final long length) {

        final List<Block> result = new ArrayList<>();

        byte[] lastHash = startParentHash;
        long lastIndex = startNumber;


        for (int i = 0; i < length; ++i){

            final byte[] difficulty = BigIntegers.asUnsignedByteArray(new BigInteger(8, new Random()));
            final byte[] newHash = HashUtil.INSTANCE.randomHash();

            final Block block = new Block(lastHash, newHash, null, null, difficulty, lastIndex, new byte[]{0}, 0, 0, null, null,
                    null, null, HashUtil.INSTANCE.getEMPTY_TRIE_HASH(), HashUtil.INSTANCE.randomHash(), null, null);

            ++lastIndex;
            lastHash = block.getHash();
            result.add(block);
        }

        return result;
    }

    // Generates chain with alternative sub-chains, maxHeight blocks on each level
    public static List<Block> getRandomAltChain(final byte[] startParentHash, final long startNumber, final long length, final int maxHeight) {

        final List<Block> result = new ArrayList<>();

        final List<byte[]> lastHashes = new ArrayList<>();
        lastHashes.add(startParentHash);
        long lastIndex = startNumber;
        final Random rnd = new Random();

        for (int i = 0; i < length; ++i){
            final List<byte[]> currentHashes = new ArrayList<>();
            int curMaxHeight = maxHeight;
            if (i == 0) curMaxHeight = 1;

            for (int j = 0; j < curMaxHeight; ++j){
                final byte[] parentHash = lastHashes.get(rnd.nextInt(lastHashes.size()));
                final byte[] difficulty = BigIntegers.asUnsignedByteArray(new BigInteger(8, new Random()));
                final byte[] newHash = HashUtil.INSTANCE.randomHash();

                final Block block = new Block(parentHash, newHash, null, null, difficulty, lastIndex, new byte[]{0}, 0, 0, null, null,
                        null, null, HashUtil.INSTANCE.getEMPTY_TRIE_HASH(), HashUtil.INSTANCE.randomHash(), null, null);
                currentHashes.add(block.getHash());
                result.add(block);
            }

            ++lastIndex;
            lastHashes.clear();
            lastHashes.addAll(currentHashes);
        }

        return result;
    }
}
