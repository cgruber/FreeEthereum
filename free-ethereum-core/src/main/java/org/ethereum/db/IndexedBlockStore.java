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
import org.ethereum.datasource.DataSourceArray;
import org.ethereum.datasource.ObjectDataSource;
import org.ethereum.datasource.Serializer;
import org.ethereum.datasource.Source;
import org.ethereum.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigInteger.ZERO;
import static org.spongycastle.util.Arrays.areEqual;

public class IndexedBlockStore extends AbstractBlockstore{

    public static final Serializer<List<BlockInfo>, byte[]> BLOCK_INFO_SERIALIZER = new Serializer<List<BlockInfo>, byte[]>() {

        @Override
        public byte[] serialize(final List<BlockInfo> value) {
            final List<byte[]> rlpBlockInfoList = new ArrayList<>();
            for (final BlockInfo blockInfo : value) {
                final byte[] hash = RLP.encodeElement(blockInfo.getHash());
                // Encoding works correctly only with positive BigIntegers
                if (blockInfo.getCummDifficulty() == null || blockInfo.getCummDifficulty().compareTo(BigInteger.ZERO) < 0) {
                    throw new RuntimeException("BlockInfo cummDifficulty should be positive BigInteger");
                }
                final byte[] cummDiff = RLP.encodeBigInteger(blockInfo.getCummDifficulty());
                final byte[] isMainChain = RLP.encodeInt(blockInfo.isMainChain() ? 1 : 0);
                rlpBlockInfoList.add(RLP.encodeList(hash, cummDiff, isMainChain));
            }
            final byte[][] elements = rlpBlockInfoList.toArray(new byte[rlpBlockInfoList.size()][]);

            return RLP.encodeList(elements);
        }

        @Override
        public List<BlockInfo> deserialize(final byte[] bytes) {
            if (bytes == null) return null;

            final List<BlockInfo> blockInfoList = new ArrayList<>();
            final RLPList list = (RLPList) RLP.decode2(bytes).get(0);
            for (final RLPElement element : list) {
                final RLPList rlpBlock = (RLPList) element;
                final BlockInfo blockInfo = new BlockInfo();
                final byte[] rlpHash = rlpBlock.get(0).getRLPData();
                blockInfo.setHash(rlpHash == null ? new byte[0] : rlpHash);
                final byte[] rlpCummDiff = rlpBlock.get(1).getRLPData();
                blockInfo.setCummDifficulty(rlpCummDiff == null ? BigInteger.ZERO : ByteUtil.bytesToBigInteger(rlpCummDiff));
                blockInfo.setMainChain(ByteUtil.byteArrayToInt(rlpBlock.get(2).getRLPData()) == 1);
                blockInfoList.add(blockInfo);
            }

            return blockInfoList;
        }
    };
    private static final Logger logger = LoggerFactory.getLogger("general");
    Source<byte[], byte[]> indexDS;
    Source<byte[], byte[]> blocksDS;
    private DataSourceArray<List<BlockInfo>> index;
    private ObjectDataSource<Block> blocks;

    public IndexedBlockStore(){
    }

    private static BlockInfo getBlockInfoForHash(final List<BlockInfo> blocks, final byte[] hash) {

        for (final BlockInfo blockInfo : blocks)
            if (areEqual(hash, blockInfo.getHash())) return blockInfo;

        return null;
    }

    public void init(final Source<byte[], byte[]> index, final Source<byte[], byte[]> blocks) {
        indexDS = index;
        this.index = new DataSourceArray<>(
                new ObjectDataSource<>(index, BLOCK_INFO_SERIALIZER, 512));
        this.blocksDS = blocks;
        this.blocks = new ObjectDataSource<>(blocks, new Serializer<Block, byte[]>() {
            @Override
            public byte[] serialize(final Block block) {
                return block.getEncoded();
            }

            @Override
            public Block deserialize(final byte[] bytes) {
                return bytes == null ? null : new Block(bytes);
            }
        }, 512);
    }

    public synchronized Block getBestBlock(){

        Long maxLevel = getMaxNumber();
        if (maxLevel < 0) return null;

        Block bestBlock = getChainBlockByNumber(maxLevel);
        if (bestBlock != null) return  bestBlock;

        // That scenario can happen
        // if there is a fork branch that is
        // higher than main branch but has
        // less TD than the main branch TD
        while (bestBlock == null){
            --maxLevel;
            bestBlock = getChainBlockByNumber(maxLevel);
        }

        return bestBlock;
    }

    public synchronized byte[] getBlockHashByNumber(final long blockNumber) {
        final Block chainBlock = getChainBlockByNumber(blockNumber);
        return chainBlock == null ? null : chainBlock.getHash(); // FIXME: can be improved by accessing the hash directly in the index
    }

    @Override
    public synchronized void flush(){
        blocks.flush();
        index.flush();
        blocksDS.flush();
        indexDS.flush();
    }

    @Override
    public synchronized void saveBlock(final Block block, final BigInteger cummDifficulty, final boolean mainChain) {
        addInternalBlock(block, cummDifficulty, mainChain);
    }

    private void addInternalBlock(final Block block, final BigInteger cummDifficulty, final boolean mainChain) {

        List<BlockInfo> blockInfos = block.getNumber() >= index.size() ?  null : index.get((int) block.getNumber());
        blockInfos = blockInfos == null ? new ArrayList<>() : blockInfos;

        final BlockInfo blockInfo = new BlockInfo();
        blockInfo.setCummDifficulty(cummDifficulty);
        blockInfo.setHash(block.getHash());
        blockInfo.setMainChain(mainChain); // FIXME:maybe here I should force reset main chain for all uncles on that level

        putBlockInfo(blockInfos, blockInfo);
        index.set((int) block.getNumber(), blockInfos);

        blocks.put(block.getHash(), block);
    }

    private void putBlockInfo(final List<BlockInfo> blockInfos, final BlockInfo blockInfo) {
        for (int i = 0; i < blockInfos.size(); i++) {
            final BlockInfo curBlockInfo = blockInfos.get(i);
            if (FastByteComparisons.equal(curBlockInfo.getHash(), blockInfo.getHash())) {
                blockInfos.set(i, blockInfo);
                return;
            }
        }
        blockInfos.add(blockInfo);
    }

    public synchronized List<Block> getBlocksByNumber(final long number) {

        final List<Block> result = new ArrayList<>();

        if (number >= index.size()) {
            return result;
        }

        final List<BlockInfo> blockInfos = index.get((int) number);

        if (blockInfos == null) {
            return result;
        }

        for (final BlockInfo blockInfo : blockInfos) {

            final byte[] hash = blockInfo.getHash();
            final Block block = blocks.get(hash);

            result.add(block);
        }
        return result;
    }

    @Override
    public synchronized Block getChainBlockByNumber(final long number) {
        if (number >= index.size()){
            return null;
        }

        final List<BlockInfo> blockInfos = index.get((int) number);

        if (blockInfos == null) {
            return null;
        }

        for (final BlockInfo blockInfo : blockInfos) {

            if (blockInfo.isMainChain()){

                final byte[] hash = blockInfo.getHash();
                return blocks.get(hash);
            }
        }

        return null;
    }

    @Override
    public synchronized Block getBlockByHash(final byte[] hash) {
        return blocks.get(hash);
    }

    @Override
    public synchronized boolean isBlockExist(final byte[] hash) {
        return blocks.get(hash) != null;
    }

    @Override
    public synchronized BigInteger getTotalDifficultyForHash(final byte[] hash) {
        final Block block = this.getBlockByHash(hash);
        if (block == null) return ZERO;

        final Long level = block.getNumber();
        final List<BlockInfo> blockInfos = index.get(level.intValue());
        for (final BlockInfo blockInfo : blockInfos)
                 if (areEqual(blockInfo.getHash(), hash)) {
                     return blockInfo.cummDifficulty;
                 }

        return ZERO;
    }

    @Override
    public synchronized BigInteger getTotalDifficulty(){
        long maxNumber = getMaxNumber();

        final List<BlockInfo> blockInfos = index.get((int) maxNumber);
        for (final BlockInfo blockInfo : blockInfos) {
            if (blockInfo.isMainChain()){
                return blockInfo.getCummDifficulty();
            }
        }

        while (true){
            --maxNumber;
            final List<BlockInfo> infos = getBlockInfoForLevel(maxNumber);

            for (final BlockInfo blockInfo : infos) {
                if (blockInfo.isMainChain()) {
                    return blockInfo.getCummDifficulty();
                }
            }
        }
    }

    public synchronized void updateTotDifficulties(final long index) {
        final List<BlockInfo> level = getBlockInfoForLevel(index);
        for (final BlockInfo blockInfo : level) {
            final Block block = getBlockByHash(blockInfo.getHash());
            final List<BlockInfo> parentInfos = getBlockInfoForLevel(index - 1);
            final BlockInfo parentInfo = getBlockInfoForHash(parentInfos, block.getParentHash());
            blockInfo.setCummDifficulty(parentInfo.getCummDifficulty().add(block.getDifficultyBI()));
        }
        this.index.set((int) index, level);
    }

    @Override
    public synchronized long getMaxNumber(){

        Long bestIndex = 0L;

        if (index.size() > 0){
            bestIndex = (long) index.size();
        }

        return bestIndex - 1L;
    }

    @Override
    public synchronized List<byte[]> getListHashesEndWith(final byte[] hash, final long number) {

        final List<Block> blocks = getListBlocksEndWith(hash, number);
        final List<byte[]> hashes = new ArrayList<>(blocks.size());

        for (final Block b : blocks) {
            hashes.add(b.getHash());
        }

        return hashes;
    }

    @Override
    public synchronized List<BlockHeader> getListHeadersEndWith(final byte[] hash, final long qty) {

        final List<Block> blocks = getListBlocksEndWith(hash, qty);
        final List<BlockHeader> headers = new ArrayList<>(blocks.size());

        for (final Block b : blocks) {
            headers.add(b.getHeader());
        }

        return headers;
    }

    @Override
    public synchronized List<Block> getListBlocksEndWith(final byte[] hash, final long qty) {
        return getListBlocksEndWithInner(hash, qty);
    }

    private List<Block> getListBlocksEndWithInner(final byte[] hash, final long qty) {

        Block block = this.blocks.get(hash);

        if (block == null) return new ArrayList<>();

        final List<Block> blocks = new ArrayList<>((int) qty);

        for (int i = 0; i < qty; ++i) {
            blocks.add(block);
            block = this.blocks.get(block.getParentHash());
            if (block == null) break;
        }

        return blocks;
    }

    @Override
    public synchronized void reBranch(final Block forkBlock) {

        final Block bestBlock = getBestBlock();

        final long maxLevel = Math.max(bestBlock.getNumber(), forkBlock.getNumber());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        Block forkLine = forkBlock;
        if (forkBlock.getNumber() > bestBlock.getNumber()){

            while(currentLevel > bestBlock.getNumber()){
                final List<BlockInfo> blocks = getBlockInfoForLevel(currentLevel);
                final BlockInfo blockInfo = getBlockInfoForHash(blocks, forkLine.getHash());
                if (blockInfo != null)  {
                    blockInfo.setMainChain(true);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                forkLine = getBlockByHash(forkLine.getParentHash());
                --currentLevel;
            }
        }

        Block bestLine = bestBlock;
        if (bestBlock.getNumber() > forkBlock.getNumber()){

            while(currentLevel > forkBlock.getNumber()){

                final List<BlockInfo> blocks = getBlockInfoForLevel(currentLevel);
                final BlockInfo blockInfo = getBlockInfoForHash(blocks, bestLine.getHash());
                if (blockInfo != null)  {
                    blockInfo.setMainChain(false);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                bestLine = getBlockByHash(bestLine.getParentHash());
                --currentLevel;
            }
        }


        // 2. Loop back on each level until common block
        while( !bestLine.isEqual(forkLine) ) {

            final List<BlockInfo> levelBlocks = getBlockInfoForLevel(currentLevel);
            final BlockInfo bestInfo = getBlockInfoForHash(levelBlocks, bestLine.getHash());
            if (bestInfo != null) {
                bestInfo.setMainChain(false);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }

            final BlockInfo forkInfo = getBlockInfoForHash(levelBlocks, forkLine.getHash());
            if (forkInfo != null) {
                forkInfo.setMainChain(true);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }


            bestLine = getBlockByHash(bestLine.getParentHash());
            forkLine = getBlockByHash(forkLine.getParentHash());

            --currentLevel;
        }


    }

    public synchronized List<byte[]> getListHashesStartWith(long number, long maxBlocks){

        final List<byte[]> result = new ArrayList<>();

        int i;
        for ( i = 0; i < maxBlocks; ++i){
            final List<BlockInfo> blockInfos = index.get((int) number);
            if (blockInfos == null) break;

            for (final BlockInfo blockInfo : blockInfos)
               if (blockInfo.isMainChain()){
                   result.add(blockInfo.getHash());
                   break;
               }

            ++number;
        }
        maxBlocks -= i;

        return result;
    }

    public synchronized void printChain(){

        final Long number = getMaxNumber();

        for (int i = 0; i < number; ++i){
            final List<BlockInfo> levelInfos = index.get(i);

            if (levelInfos != null) {
                System.out.print(i);
                for (final BlockInfo blockInfo : levelInfos) {
                    if (blockInfo.isMainChain())
                        System.out.print(" [" + HashUtil.INSTANCE.shortHash(blockInfo.getHash()) + "] ");
                    else
                        System.out.print(" " + HashUtil.INSTANCE.shortHash(blockInfo.getHash()) + " ");
                }
                System.out.println();
            }

        }

    }

    private synchronized List<BlockInfo> getBlockInfoForLevel(final long level) {
        return index.get((int) level);
    }

    private synchronized void setBlockInfoForLevel(final long level, final List<BlockInfo> infos) {
        index.set((int) level, infos);
    }

    @Override
    public synchronized void load() {
    }

    @Override
    public synchronized void close() {
//        logger.info("Closing IndexedBlockStore...");
//        try {
//            indexDS.close();
//        } catch (Exception e) {
//            logger.warn("Problems closing indexDS", e);
//        }
//        try {
//            blocksDS.close();
//        } catch (Exception e) {
//            logger.warn("Problems closing blocksDS", e);
//        }
    }

    public static class BlockInfo implements Serializable {
        byte[] hash;
        BigInteger cummDifficulty;
        boolean mainChain;

        public byte[] getHash() {
            return hash;
        }

        public void setHash(final byte[] hash) {
            this.hash = hash;
        }

        public BigInteger getCummDifficulty() {
            return cummDifficulty;
        }

        public void setCummDifficulty(final BigInteger cummDifficulty) {
            this.cummDifficulty = cummDifficulty;
        }

        public boolean isMainChain() {
            return mainChain;
        }

        public void setMainChain(final boolean mainChain) {
            this.mainChain = mainChain;
        }
    }
}
