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

package org.ethereum.longrun

import org.ethereum.config.CommonConfig
import org.ethereum.core.AccountState
import org.ethereum.core.BlockchainImpl
import org.ethereum.core.BlockchainImpl.calcReceiptsTrie
import org.ethereum.core.Bloom
import org.ethereum.crypto.HashUtil
import org.ethereum.datasource.Source
import org.ethereum.facade.Ethereum
import org.ethereum.trie.SecureTrie
import org.ethereum.trie.TrieImpl
import org.ethereum.util.FastByteComparisons
import org.junit.Assert
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validation for all kind of blockchain data
 */
internal object BlockchainValidation {

    private val testLogger = LoggerFactory.getLogger("TestLogger")

    private fun getReferencedTrieNodes(stateDS: Source<ByteArray, ByteArray>, includeAccounts: Boolean,
                                       vararg roots: ByteArray): Int {
        val ret = AtomicInteger(0)
        roots
                .map { SecureTrie(stateDS, it) }
                .forEach {
                    it.scanTree(object : TrieImpl.ScanAction {
                        override fun doOnNode(hash: ByteArray, node: TrieImpl.Node) {
                            ret.incrementAndGet()
                        }

                        override fun doOnValue(nodeHash: ByteArray, node: TrieImpl.Node, key: ByteArray, value: ByteArray) {
                            if (includeAccounts) {
                                val accountState = AccountState(value)
                                if (!FastByteComparisons.equal(accountState.codeHash, HashUtil.EMPTY_DATA_HASH)) {
                                    ret.incrementAndGet()
                                }
                                if (!FastByteComparisons.equal(accountState.stateRoot, HashUtil.EMPTY_TRIE_HASH)) {
                                    ret.addAndGet(getReferencedTrieNodes(stateDS, false, accountState.stateRoot))
                                }
                            }
                        }
                    })
                }
        return ret.get()
    }

    fun checkNodes(ethereum: Ethereum, commonConfig: CommonConfig, fatalErrors: AtomicInteger) {
        try {
            val stateDS = commonConfig.stateSource()
            val stateRoot = ethereum.blockchain.bestBlock.header.stateRoot
            val rootsSize = getReferencedTrieNodes(stateDS, true, stateRoot)
            testLogger.info("Node validation successful")
            testLogger.info("Non-unique node size: {}", rootsSize)
        } catch (ex: Exception) {
            testLogger.error("Node validation error", ex)
            fatalErrors.incrementAndGet()
        } catch (ex: AssertionError) {
            testLogger.error("Node validation error", ex)
            fatalErrors.incrementAndGet()
        }

    }

    private fun checkHeaders(ethereum: Ethereum, fatalErrors: AtomicInteger) {
        var blockNumber = ethereum.blockchain.bestBlock.header.number.toInt()
        var lastParentHash: ByteArray? = null
        testLogger.info("Checking headers from best block: {}", blockNumber)

        try {
            while (blockNumber >= 0) {
                val currentBlock = ethereum.blockchain.getBlockByNumber(blockNumber.toLong())
                if (lastParentHash != null) {
                    assert(FastByteComparisons.equal(currentBlock.hash, lastParentHash))
                }
                lastParentHash = currentBlock.header.parentHash
                assert(lastParentHash != null)
                blockNumber--
            }

            testLogger.info("Checking headers successful, ended on block: {}", blockNumber + 1)
        } catch (ex: Exception) {
            testLogger.error(String.format("Block header validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        } catch (ex: AssertionError) {
            testLogger.error(String.format("Block header validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        }

    }

    fun checkFastHeaders(ethereum: Ethereum, commonConfig: CommonConfig, fatalErrors: AtomicInteger) {
        val headerStore = commonConfig.headerSource()
        var blockNumber = headerStore.size - 1
        var lastParentHash: ByteArray? = null

        try {
            testLogger.info("Checking fast headers from best block: {}", blockNumber)
            while (blockNumber > 0) {
                val header = headerStore[blockNumber]
                if (lastParentHash != null) {
                    assert(FastByteComparisons.equal(header.hash, lastParentHash))
                }
                lastParentHash = header.parentHash
                assert(lastParentHash != null)
                blockNumber--
            }

            val genesis = ethereum.blockchain.getBlockByNumber(0)
            assert(FastByteComparisons.equal(genesis.hash, lastParentHash))

            testLogger.info("Checking fast headers successful, ended on block: {}", blockNumber + 1)
        } catch (ex: Exception) {
            testLogger.error(String.format("Fast header validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        } catch (ex: AssertionError) {
            testLogger.error(String.format("Fast header validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        }

    }

    private fun checkBlocks(ethereum: Ethereum, fatalErrors: AtomicInteger) {
        var currentBlock = ethereum.blockchain.bestBlock
        var blockNumber = currentBlock.header.number.toInt()

        try {
            val blockStore = ethereum.blockchain.blockStore
            testLogger.info("Checking blocks from best block: {}", blockNumber)
            var curTotalDiff = blockStore.getTotalDifficultyForHash(currentBlock.hash)

            while (blockNumber > 0) {
                currentBlock = ethereum.blockchain.getBlockByNumber(blockNumber.toLong())

                // Validate uncles
                assert((ethereum.blockchain as BlockchainImpl).validateUncles(currentBlock))
                // Validate total difficulty
                Assert.assertTrue(String.format("Total difficulty, count %s == %s blockStore",
                        curTotalDiff, blockStore.getTotalDifficultyForHash(currentBlock.hash)),
                        curTotalDiff.compareTo(blockStore.getTotalDifficultyForHash(currentBlock.hash)) == 0)
                curTotalDiff = curTotalDiff.subtract(currentBlock.difficultyBI)

                blockNumber--
            }

            // Checking total difficulty for genesis
            currentBlock = ethereum.blockchain.getBlockByNumber(0)
            Assert.assertTrue(String.format("Total difficulty for genesis, count %s == %s blockStore",
                    curTotalDiff, blockStore.getTotalDifficultyForHash(currentBlock.hash)),
                    curTotalDiff.compareTo(blockStore.getTotalDifficultyForHash(currentBlock.hash)) == 0)
            Assert.assertTrue(String.format("Total difficulty, count %s == %s genesis",
                    curTotalDiff, currentBlock.difficultyBI),
                    curTotalDiff.compareTo(currentBlock.difficultyBI) == 0)

            testLogger.info("Checking blocks successful, ended on block: {}", blockNumber + 1)
        } catch (ex: Exception) {
            testLogger.error(String.format("Block validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        } catch (ex: AssertionError) {
            testLogger.error(String.format("Block validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        }

    }

    private fun checkTransactions(ethereum: Ethereum, fatalErrors: AtomicInteger) {
        var blockNumber = ethereum.blockchain.bestBlock.header.number.toInt()
        testLogger.info("Checking block transactions from best block: {}", blockNumber)

        try {
            while (blockNumber > 0) {
                val currentBlock = ethereum.blockchain.getBlockByNumber(blockNumber.toLong())

                val receipts = currentBlock.transactionsList
                        .map { (ethereum.blockchain as BlockchainImpl).getTransactionInfo(it.hash)!! }
                        .map { it.receipt }

                val logBloom = Bloom()
                for (receipt in receipts) {
                    logBloom.or(receipt.bloomFilter)
                }
                assert(FastByteComparisons.equal(currentBlock.logBloom, logBloom.data))
                assert(FastByteComparisons.equal(currentBlock.receiptsRoot, calcReceiptsTrie(receipts)))

                blockNumber--
            }

            testLogger.info("Checking block transactions successful, ended on block: {}", blockNumber + 1)
        } catch (ex: Exception) {
            testLogger.error(String.format("Transaction validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        } catch (ex: AssertionError) {
            testLogger.error(String.format("Transaction validation error on block #%s", blockNumber), ex)
            fatalErrors.incrementAndGet()
        }

    }

    fun fullCheck(ethereum: Ethereum, commonConfig: CommonConfig, fatalErrors: AtomicInteger) {

        // nodes
        testLogger.info("Validating nodes: Start")
        BlockchainValidation.checkNodes(ethereum, commonConfig, fatalErrors)
        testLogger.info("Validating nodes: End")

        // headers
        testLogger.info("Validating block headers: Start")
        BlockchainValidation.checkHeaders(ethereum, fatalErrors)
        testLogger.info("Validating block headers: End")

        // blocks
        testLogger.info("Validating blocks: Start")
        BlockchainValidation.checkBlocks(ethereum, fatalErrors)
        testLogger.info("Validating blocks: End")

        // receipts
        testLogger.info("Validating transaction receipts: Start")
        BlockchainValidation.checkTransactions(ethereum, fatalErrors)
        testLogger.info("Validating transaction receipts: End")
    }
}
