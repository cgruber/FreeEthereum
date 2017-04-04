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

package org.ethereum.core

import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.db.BlockStoreDummy
import org.ethereum.db.RepositoryRoot
import org.junit.Test
import java.util.*

/**
 * Testing [BlockchainImpl.getListOfHeadersStartFrom]
 */
class BlockchainGetHeadersTest {

    private val blockchain: BlockchainImpl

    init {
        blockchain = BlockchainImplTester()
    }

    @Test
    fun singleHeader() {
        // Get by number
        val blockNumber = 2L
        val identifier = BlockIdentifier(null, blockNumber)
        val headers = blockchain.getListOfHeadersStartFrom(identifier, 0, 1, false)

        assert(headers.size == 1)
        assert(headers[0].number == blockNumber)

        // Get by hash
        val hash = headers[0].hash
        val hashIdentifier = BlockIdentifier(hash, 0L)
        val headersByHash = blockchain.getListOfHeadersStartFrom(hashIdentifier, 0, 1, false)

        assert(headersByHash.size == 1)
        assert(headersByHash[0].number == blockNumber)

        // Reverse doesn't matter for single block
        val headersReverse = blockchain.getListOfHeadersStartFrom(hashIdentifier, 0, 1, true)
        assert(headersReverse.size == 1)
        assert(headersReverse[0].number == blockNumber)

        // Skip doesn't matter for single block
        val headersSkip = blockchain.getListOfHeadersStartFrom(hashIdentifier, 15, 1, false)
        assert(headersReverse.size == 1)
        assert(headersReverse[0].number == blockNumber)
    }

    @Test
    fun continuousHeaders() {
        // Get by number
        val blockNumber = 2L
        val identifier = BlockIdentifier(null, blockNumber)
        val headers = blockchain.getListOfHeadersStartFrom(identifier, 0, 3, false)

        assert(headers.size == 3)
        assert(headers[0].number == blockNumber)
        assert(headers[1].number == blockNumber + 1)
        assert(headers[2].number == blockNumber + 2)

        val headersReverse = blockchain.getListOfHeadersStartFrom(identifier, 0, 3, true)
        assert(headersReverse.size == 3)
        assert(headersReverse[0].number == blockNumber)
        assert(headersReverse[1].number == blockNumber - 1)
        assert(headersReverse[2].number == blockNumber - 2)

        // Requesting more than we have
        val identifierMore = BlockIdentifier(null, 8L)
        val headersMore = blockchain.getListOfHeadersStartFrom(identifierMore, 0, 3, false)
        assert(headersMore.size == 2)
        assert(headersMore[0].number == 8L)
        assert(headersMore[1].number == 9L)
    }

    @Test
    fun gapedHeaders() {
        val skip = 2
        val identifier = BlockIdentifier(null, 2L)
        val headers = blockchain.getListOfHeadersStartFrom(identifier, skip, 3, false)

        assert(headers.size == 3)
        assert(headers[0].number == 2L)
        assert(headers[1].number == 5L) // 2, [3, 4], 5 - skipping []
        assert(headers[2].number == 8L) // 5, [6, 7], 8 - skipping []

        // Same for reverse
        val identifierReverse = BlockIdentifier(null, 8L)
        val headersReverse = blockchain.getListOfHeadersStartFrom(identifierReverse, skip, 3, true)
        assert(headersReverse.size == 3)
        assert(headersReverse[0].number == 8L)
        assert(headersReverse[1].number == 5L)
        assert(headersReverse[2].number == 2L)

        // Requesting more than we have
        val identifierMore = BlockIdentifier(null, 8L)
        val headersMore = blockchain.getListOfHeadersStartFrom(identifierMore, skip, 3, false)
        assert(headersMore.size == 1)
        assert(headersMore[0].number == 8L)
    }

    private inner class BlockStoreMock : BlockStoreDummy() {

        private val dummyBlocks = ArrayList<Block>()

        init {
            val emptyArray = ByteArray(0)
            var recentHash = emptyArray

            for (i in 0..9) {
                val blockHeader = BlockHeader(recentHash, emptyArray, emptyArray, emptyArray, emptyArray,
                        i.toLong(), emptyArray, 0L, 0L, emptyArray, emptyArray, emptyArray)
                recentHash = blockHeader.hash
                val block = Block(blockHeader, ArrayList<Transaction>(), ArrayList<BlockHeader>())
                dummyBlocks.add(block)
            }
        }

        override fun getBlockByHash(hash: ByteArray): Block? {
            for (block in dummyBlocks) {
                if (Arrays.equals(block.hash, hash)) {
                    return block
                }
            }

            return null
        }

        override fun getChainBlockByNumber(blockNumber: Long): Block? {
            return if (blockNumber < dummyBlocks.size) dummyBlocks[blockNumber.toInt()] else null
        }

        override fun getListHeadersEndWith(hash: ByteArray, qty: Long): List<BlockHeader> {
            val headers = ArrayList<BlockHeader>()
            val start = getBlockByHash(hash)
            if (start != null) {
                var i = start.number
                while (i >= 0 && headers.size < qty) {
                    headers.add(getChainBlockByNumber(i)!!.header)
                    --i
                }
            }

            return headers
        }

        override fun getBestBlock(): Block {
            return dummyBlocks[dummyBlocks.size - 1]
        }
    }

    private inner class BlockchainImplTester : BlockchainImpl() {
        init {
            blockStore = BlockStoreMock()
            repository = RepositoryRoot(HashMapDB<ByteArray>())
            bestBlock = blockStore.getChainBlockByNumber(9)
        }
    }
}
