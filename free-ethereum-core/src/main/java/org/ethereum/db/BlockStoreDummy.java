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

package org.ethereum.db;

import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.crypto.HashUtil;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 10.02.2015
 */
public class BlockStoreDummy implements BlockStore {

    @Override
    public byte[] getBlockHashByNumber(final long blockNumber) {

        final byte[] data = String.valueOf(blockNumber).getBytes();
        return HashUtil.INSTANCE.sha3(data);
    }

    @Override
    public byte[] getBlockHashByNumber(final long blockNumber, final byte[] branchBlockHash) {
        return getBlockHashByNumber(blockNumber);
    }

    @Override
    public Block getChainBlockByNumber(final long blockNumber) {
        return null;
    }

    @Override
    public Block getBlockByHash(final byte[] hash) {
        return null;
    }

    @Override
    public boolean isBlockExist(final byte[] hash) {
        return false;
    }

    @Override
    public List<byte[]> getListHashesEndWith(final byte[] hash, final long qty) {
        return null;
    }

    @Override
    public List<BlockHeader> getListHeadersEndWith(final byte[] hash, final long qty) {
        return null;
    }

    @Override
    public List<Block> getListBlocksEndWith(final byte[] hash, final long qty) {
        return null;
    }

    @Override
    public void saveBlock(final Block block, final BigInteger cummDifficulty, final boolean mainChain) {

    }


    @Override
    public BigInteger getTotalDifficulty() {
        return null;
    }

    @Override
    public Block getBestBlock() {
        return null;
    }


    @Override
    public void flush() {
    }

    @Override
    public void load() {
    }

    @Override
    public long getMaxNumber() {
        return 0;
    }


    @Override
    public void reBranch(final Block forkBlock) {

    }

    @Override
    public BigInteger getTotalDifficultyForHash(final byte[] hash) {
        return null;
    }

    @Override
    public void close() {}
}
