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

package org.ethereum.net.eth.handler

import org.ethereum.core.BlockHeader
import org.ethereum.net.eth.message.BlockHeadersMessage
import org.ethereum.net.eth.message.GetBlockHeadersMessage
import org.junit.Test
import java.util.*

/**
 * Testing [org.ethereum.net.eth.handler.Eth62.isValid]
 */
class HeaderMessageValidationTest {

    private val EMPTY_ARRAY = ByteArray(0)
    private val ethHandler: Eth62Tester

    init {
        ethHandler = Eth62Tester()
    }

    @Test
    fun testSingleBlockResponse() {
        val blockNumber = 0L
        val blockHeader = BlockHeader(byteArrayOf(11, 12), EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        val blockHeaders = ArrayList<BlockHeader>()
        blockHeaders.add(blockHeader)
        val msg = BlockHeadersMessage(blockHeaders)

        val hash = blockHeader.hash
        // Block number doesn't matter when hash is provided in request
        val requestHash = GetBlockHeadersMessage(123L, hash, 1, 0, false)
        val wrapperHash = GetBlockHeadersMessageWrapper(requestHash)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperHash))

        // Getting same with block number request
        val requestNumber = GetBlockHeadersMessage(blockNumber, null, 1, 0, false)
        val wrapperNumber = GetBlockHeadersMessageWrapper(requestNumber)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperNumber))

        // Getting same with reverse request
        val requestReverse = GetBlockHeadersMessage(blockNumber, null, 1, 0, true)
        val wrapperReverse = GetBlockHeadersMessageWrapper(requestReverse)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperReverse))

        // Getting same with skip request
        val requestSkip = GetBlockHeadersMessage(blockNumber, null, 1, 10, false)
        val wrapperSkip = GetBlockHeadersMessageWrapper(requestSkip)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperSkip))
    }

    @Test
    fun testFewBlocksNoSkip() {
        val blockHeaders = ArrayList<BlockHeader>()

        val blockNumber1 = 0L
        val blockHeader1 = BlockHeader(byteArrayOf(11, 12), EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber1, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        val hash1 = blockHeader1.hash
        blockHeaders.add(blockHeader1)

        val blockNumber2 = 1L
        val blockHeader2 = BlockHeader(hash1, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber2, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        val hash2 = blockHeader2.hash
        blockHeaders.add(blockHeader2)

        val msg = BlockHeadersMessage(blockHeaders)

        // Block number doesn't matter when hash is provided in request
        val requestHash = GetBlockHeadersMessage(123L, hash1, 2, 0, false)
        val wrapperHash = GetBlockHeadersMessageWrapper(requestHash)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperHash))

        // Getting same with block number request
        val requestNumber = GetBlockHeadersMessage(blockNumber1, null, 2, 0, false)
        val wrapperNumber = GetBlockHeadersMessageWrapper(requestNumber)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperNumber))

        // Reverse list
        Collections.reverse(blockHeaders)
        val requestReverse = GetBlockHeadersMessage(blockNumber2, null, 2, 0, true)
        val wrapperReverse = GetBlockHeadersMessageWrapper(requestReverse)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperReverse))
    }

    @Test
    fun testFewBlocksWithSkip() {
        val blockHeaders = ArrayList<BlockHeader>()

        val blockNumber1 = 0L
        val blockHeader1 = BlockHeader(byteArrayOf(11, 12), EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber1, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        blockHeaders.add(blockHeader1)

        val blockNumber2 = 16L
        val blockHeader2 = BlockHeader(byteArrayOf(12, 13), EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber2, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        blockHeaders.add(blockHeader2)

        val blockNumber3 = 32L
        val blockHeader3 = BlockHeader(byteArrayOf(14, 15), EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber3, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY)
        blockHeaders.add(blockHeader3)

        val msg = BlockHeadersMessage(blockHeaders)

        val requestNumber = GetBlockHeadersMessage(blockNumber1, null, 3, 15, false)
        val wrapperNumber = GetBlockHeadersMessageWrapper(requestNumber)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperNumber))

        // Requesting more than we have
        val requestMore = GetBlockHeadersMessage(blockNumber1, null, 4, 15, false)
        val wrapperMore = GetBlockHeadersMessageWrapper(requestMore)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperMore))

        // Reverse list
        Collections.reverse(blockHeaders)
        val requestReverse = GetBlockHeadersMessage(blockNumber3, null, 3, 15, true)
        val wrapperReverse = GetBlockHeadersMessageWrapper(requestReverse)
        assert(ethHandler.blockHeaderMessageValid(msg, wrapperReverse))
    }

    private inner class Eth62Tester : Eth62() {

        internal fun blockHeaderMessageValid(msg: BlockHeadersMessage, request: GetBlockHeadersMessageWrapper): Boolean {
            return super.isValid(msg, request)
        }
    }
}
