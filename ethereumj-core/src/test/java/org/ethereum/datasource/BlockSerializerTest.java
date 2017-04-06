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

package org.ethereum.datasource;

import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.IndexedBlockStore.BlockInfo;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.ethereum.crypto.HashUtil.sha3;

/**
 * Test for {@link IndexedBlockStore.BLOCK_INFO_SERIALIZER}
 */
public class BlockSerializerTest {

    private static final Random rnd = new Random();

    private List<BlockInfo> generateBlockInfos(final int count) {
        final List<BlockInfo> blockInfos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final BlockInfo blockInfo = new BlockInfo();
            blockInfo.setHash(sha3(ByteUtil.intToBytes(i)));
            blockInfo.setCummDifficulty(BigInteger.probablePrime(512, rnd));
            blockInfo.setMainChain(rnd.nextBoolean());
            blockInfos.add(blockInfo);
        }

        return blockInfos;
    }

    @Test
    public void testTest() {
        final List<BlockInfo> blockInfoList = generateBlockInfos(100);
        final byte[] data = IndexedBlockStore.BLOCK_INFO_SERIALIZER.serialize(blockInfoList);
        System.out.printf("Blocks total byte size: %s%n", data.length);
        final List<BlockInfo> blockInfoList2 = IndexedBlockStore.BLOCK_INFO_SERIALIZER.deserialize(data);

        assert blockInfoList.size() == blockInfoList2.size();
        for (int i = 0; i < blockInfoList2.size(); i++) {
            assert FastByteComparisons.equal(blockInfoList2.get(i).getHash(), blockInfoList.get(i).getHash());
            assert blockInfoList2.get(i).getCummDifficulty().compareTo(blockInfoList.get(i).getCummDifficulty()) == 0;
            assert blockInfoList2.get(i).isMainChain() == blockInfoList.get(i).isMainChain();
        }
    }

    @Test
    @Ignore
    public void testTime() {
        final int BLOCKS = 100;
        final int PASSES = 10_000;
        final List<BlockInfo> blockInfoList = generateBlockInfos(BLOCKS);

        final long s = System.currentTimeMillis();
        for (int i = 0; i < PASSES; i++) {
            final byte[] data = IndexedBlockStore.BLOCK_INFO_SERIALIZER.serialize(blockInfoList);
            final List<BlockInfo> blockInfoList2 = IndexedBlockStore.BLOCK_INFO_SERIALIZER.deserialize(data);
        }
        final long e = System.currentTimeMillis();

        System.out.printf("Serialize/deserialize blocks per 1 ms: %s%n", PASSES * BLOCKS / (e - s));
    }

    @Test(expected = RuntimeException.class)
    public void testNullCummDifficulty() {
        final BlockInfo blockInfo = new BlockInfo();
        blockInfo.setMainChain(true);
        blockInfo.setCummDifficulty(null);
        blockInfo.setHash(new byte[0]);
        final byte[] data = IndexedBlockStore.BLOCK_INFO_SERIALIZER.serialize(Collections.singletonList(blockInfo));
        final List<BlockInfo> blockInfos = IndexedBlockStore.BLOCK_INFO_SERIALIZER.deserialize(data);
    }

    @Test(expected = RuntimeException.class)
    public void testNegativeCummDifficulty() {
        final BlockInfo blockInfo = new BlockInfo();
        blockInfo.setMainChain(true);
        blockInfo.setCummDifficulty(BigInteger.valueOf(-1));
        blockInfo.setHash(new byte[0]);
        final byte[] data = IndexedBlockStore.BLOCK_INFO_SERIALIZER.serialize(Collections.singletonList(blockInfo));
        final List<BlockInfo> blockInfos = IndexedBlockStore.BLOCK_INFO_SERIALIZER.deserialize(data);
    }

    @Test
    public void testZeroCummDifficultyEmptyHash() {
        final BlockInfo blockInfo = new BlockInfo();
        blockInfo.setMainChain(true);
        blockInfo.setCummDifficulty(BigInteger.ZERO);
        blockInfo.setHash(new byte[0]);
        final byte[] data = IndexedBlockStore.BLOCK_INFO_SERIALIZER.serialize(Collections.singletonList(blockInfo));
        final List<BlockInfo> blockInfos = IndexedBlockStore.BLOCK_INFO_SERIALIZER.deserialize(data);
        assert blockInfos.size() == 1;
        final BlockInfo actualBlockInfo = blockInfos.get(0);
        assert actualBlockInfo.isMainChain();
        assert actualBlockInfo.getCummDifficulty().compareTo(BigInteger.ZERO) == 0;
        assert actualBlockInfo.getHash().length == 0;
    }
}
