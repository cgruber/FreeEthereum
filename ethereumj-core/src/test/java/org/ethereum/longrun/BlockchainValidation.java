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

package org.ethereum.longrun;

import org.ethereum.config.CommonConfig;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.DataSourceArray;
import org.ethereum.datasource.Source;
import org.ethereum.db.BlockStore;
import org.ethereum.facade.Ethereum;
import org.ethereum.trie.SecureTrie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.util.FastByteComparisons;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ethereum.core.BlockchainImpl.calcReceiptsTrie;

/**
 * Validation for all kind of blockchain data
 */
class BlockchainValidation {

    private static final Logger testLogger = LoggerFactory.getLogger("TestLogger");

    private static Integer getReferencedTrieNodes(final Source<byte[], byte[]> stateDS, final boolean includeAccounts,
                                                  final byte[]... roots) {
        final AtomicInteger ret = new AtomicInteger(0);
        for (final byte[] root : roots) {
            final SecureTrie trie = new SecureTrie(stateDS, root);
            trie.scanTree(new TrieImpl.ScanAction() {
                @Override
                public void doOnNode(final byte[] hash, final TrieImpl.Node node) {
                    ret.incrementAndGet();
                }

                @Override
                public void doOnValue(final byte[] nodeHash, final TrieImpl.Node node, final byte[] key, final byte[] value) {
                    if (includeAccounts) {
                        final AccountState accountState = new AccountState(value);
                        if (!FastByteComparisons.equal(accountState.getCodeHash(), HashUtil.EMPTY_DATA_HASH)) {
                            ret.incrementAndGet();
                        }
                        if (!FastByteComparisons.equal(accountState.getStateRoot(), HashUtil.EMPTY_TRIE_HASH)) {
                            ret.addAndGet(getReferencedTrieNodes(stateDS, false, accountState.getStateRoot()));
                        }
                    }
                }
            });
        }
        return ret.get();
    }

    public static void checkNodes(final Ethereum ethereum, final CommonConfig commonConfig, final AtomicInteger fatalErrors) {
        try {
            final Source<byte[], byte[]> stateDS = commonConfig.stateSource();
            final byte[] stateRoot = ethereum.getBlockchain().getBestBlock().getHeader().getStateRoot();
            final Integer rootsSize = getReferencedTrieNodes(stateDS, true, stateRoot);
            testLogger.info("Node validation successful");
            testLogger.info("Non-unique node size: {}", rootsSize);
        } catch (Exception | AssertionError ex) {
            testLogger.error("Node validation error", ex);
            fatalErrors.incrementAndGet();
        }
    }

    private static void checkHeaders(final Ethereum ethereum, final AtomicInteger fatalErrors) {
        int blockNumber = (int) ethereum.getBlockchain().getBestBlock().getHeader().getNumber();
        byte[] lastParentHash = null;
        testLogger.info("Checking headers from best block: {}", blockNumber);

        try {
            while (blockNumber >= 0) {
                final Block currentBlock = ethereum.getBlockchain().getBlockByNumber(blockNumber);
                if (lastParentHash != null) {
                    assert FastByteComparisons.equal(currentBlock.getHash(), lastParentHash);
                }
                lastParentHash = currentBlock.getHeader().getParentHash();
                assert lastParentHash != null;
                blockNumber--;
            }

            testLogger.info("Checking headers successful, ended on block: {}", blockNumber + 1);
        } catch (Exception | AssertionError ex) {
            testLogger.error(String.format("Block header validation error on block #%s", blockNumber), ex);
            fatalErrors.incrementAndGet();
        }
    }

    public static void checkFastHeaders(final Ethereum ethereum, final CommonConfig commonConfig, final AtomicInteger fatalErrors) {
        final DataSourceArray<BlockHeader> headerStore = commonConfig.headerSource();
        int blockNumber = headerStore.size() - 1;
        byte[] lastParentHash = null;

        try {
            testLogger.info("Checking fast headers from best block: {}", blockNumber);
            while (blockNumber > 0) {
                final BlockHeader header = headerStore.get(blockNumber);
                if (lastParentHash != null) {
                    assert FastByteComparisons.equal(header.getHash(), lastParentHash);
                }
                lastParentHash = header.getParentHash();
                assert lastParentHash != null;
                blockNumber--;
            }

            final Block genesis = ethereum.getBlockchain().getBlockByNumber(0);
            assert FastByteComparisons.equal(genesis.getHash(), lastParentHash);

            testLogger.info("Checking fast headers successful, ended on block: {}", blockNumber + 1);
        } catch (Exception | AssertionError ex) {
            testLogger.error(String.format("Fast header validation error on block #%s", blockNumber), ex);
            fatalErrors.incrementAndGet();
        }
    }

    private static void checkBlocks(final Ethereum ethereum, final AtomicInteger fatalErrors) {
        Block currentBlock = ethereum.getBlockchain().getBestBlock();
        int blockNumber = (int) currentBlock.getHeader().getNumber();

        try {
            final BlockStore blockStore = ethereum.getBlockchain().getBlockStore();
            testLogger.info("Checking blocks from best block: {}", blockNumber);
            BigInteger curTotalDiff = blockStore.getTotalDifficultyForHash(currentBlock.getHash());

            while (blockNumber > 0) {
                currentBlock = ethereum.getBlockchain().getBlockByNumber(blockNumber);

                // Validate uncles
                assert ((BlockchainImpl) ethereum.getBlockchain()).validateUncles(currentBlock);
                // Validate total difficulty
                Assert.assertTrue(String.format("Total difficulty, count %s == %s blockStore",
                        curTotalDiff, blockStore.getTotalDifficultyForHash(currentBlock.getHash())),
                        curTotalDiff.compareTo(blockStore.getTotalDifficultyForHash(currentBlock.getHash())) == 0);
                curTotalDiff = curTotalDiff.subtract(currentBlock.getDifficultyBI());

                blockNumber--;
            }

            // Checking total difficulty for genesis
            currentBlock = ethereum.getBlockchain().getBlockByNumber(0);
            Assert.assertTrue(String.format("Total difficulty for genesis, count %s == %s blockStore",
                    curTotalDiff, blockStore.getTotalDifficultyForHash(currentBlock.getHash())),
                    curTotalDiff.compareTo(blockStore.getTotalDifficultyForHash(currentBlock.getHash())) == 0);
            Assert.assertTrue(String.format("Total difficulty, count %s == %s genesis",
                    curTotalDiff, currentBlock.getDifficultyBI()),
                    curTotalDiff.compareTo(currentBlock.getDifficultyBI()) == 0);

            testLogger.info("Checking blocks successful, ended on block: {}", blockNumber + 1);
        } catch (Exception | AssertionError ex) {
            testLogger.error(String.format("Block validation error on block #%s", blockNumber), ex);
            fatalErrors.incrementAndGet();
        }
    }

    private static void checkTransactions(final Ethereum ethereum, final AtomicInteger fatalErrors) {
        int blockNumber = (int) ethereum.getBlockchain().getBestBlock().getHeader().getNumber();
        testLogger.info("Checking block transactions from best block: {}", blockNumber);

        try {
            while (blockNumber > 0) {
                final Block currentBlock = ethereum.getBlockchain().getBlockByNumber(blockNumber);

                final List<TransactionReceipt> receipts = new ArrayList<>();
                for (final Transaction tx : currentBlock.getTransactionsList()) {
                    final TransactionInfo txInfo = ((BlockchainImpl) ethereum.getBlockchain()).getTransactionInfo(tx.getHash());
                    assert txInfo != null;
                    receipts.add(txInfo.getReceipt());
                }

                final Bloom logBloom = new Bloom();
                for (final TransactionReceipt receipt : receipts) {
                    logBloom.or(receipt.getBloomFilter());
                }
                assert FastByteComparisons.equal(currentBlock.getLogBloom(), logBloom.getData());
                assert FastByteComparisons.equal(currentBlock.getReceiptsRoot(), calcReceiptsTrie(receipts));

                blockNumber--;
            }

            testLogger.info("Checking block transactions successful, ended on block: {}", blockNumber + 1);
        } catch (Exception | AssertionError ex) {
            testLogger.error(String.format("Transaction validation error on block #%s", blockNumber), ex);
            fatalErrors.incrementAndGet();
        }
    }

    public static void fullCheck(final Ethereum ethereum, final CommonConfig commonConfig, final AtomicInteger fatalErrors) {

        // nodes
        testLogger.info("Validating nodes: Start");
        BlockchainValidation.checkNodes(ethereum, commonConfig, fatalErrors);
        testLogger.info("Validating nodes: End");

        // headers
        testLogger.info("Validating block headers: Start");
        BlockchainValidation.checkHeaders(ethereum, fatalErrors);
        testLogger.info("Validating block headers: End");

        // blocks
        testLogger.info("Validating blocks: Start");
        BlockchainValidation.checkBlocks(ethereum, fatalErrors);
        testLogger.info("Validating blocks: End");

        // receipts
        testLogger.info("Validating transaction receipts: Start");
        BlockchainValidation.checkTransactions(ethereum, fatalErrors);
        testLogger.info("Validating transaction receipts: End");
    }
}
