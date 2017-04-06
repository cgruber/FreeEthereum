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

import org.ethereum.core.BlockIdentifier
import org.ethereum.net.eth.message.NewBlockHashesMessage
import org.ethereum.net.server.Channel
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.util.*

/**
 * Testing [Eth62.processNewBlockHashes]
 */
class ProcessNewBlockHashesTest {
    private val ethHandler: Eth62Tester

    init {
        ethHandler = Eth62Tester()
    }

    @Test
    fun testSingleHashHandling() {
        val blockIdentifiers = ArrayList<BlockIdentifier>()
        val blockHash = byteArrayOf(2, 3, 4)
        val blockNumber: Long = 123
        blockIdentifiers.add(BlockIdentifier(blockHash, blockNumber))
        val msg = NewBlockHashesMessage(blockIdentifiers)

        ethHandler.setGetNewBlockHeadersParams(blockHash, 1, 0, false)
        ethHandler.processNewBlockHashes(msg)
        assert(ethHandler.wasCalled)
    }

    @Test
    fun testSeveralHashesHandling() {
        val blockIdentifiers = ArrayList<BlockIdentifier>()
        val blockHash1 = byteArrayOf(2, 3, 4)
        val blockNumber1: Long = 123
        val blockHash2 = byteArrayOf(5, 3, 4)
        val blockNumber2: Long = 124
        val blockHash3 = byteArrayOf(2, 6, 4)
        val blockNumber3: Long = 125
        blockIdentifiers.add(BlockIdentifier(blockHash1, blockNumber1))
        blockIdentifiers.add(BlockIdentifier(blockHash2, blockNumber2))
        blockIdentifiers.add(BlockIdentifier(blockHash3, blockNumber3))
        val msg = NewBlockHashesMessage(blockIdentifiers)

        ethHandler.setGetNewBlockHeadersParams(blockHash1, 3, 0, false)
        ethHandler.processNewBlockHashes(msg)
        assert(ethHandler.wasCalled)
    }

    @Test
    fun testSeveralHashesMixedOrderHandling() {
        val blockIdentifiers = ArrayList<BlockIdentifier>()
        val blockHash1 = byteArrayOf(5, 3, 4)
        val blockNumber1: Long = 124
        val blockHash2 = byteArrayOf(2, 3, 4)
        val blockNumber2: Long = 123
        val blockHash3 = byteArrayOf(2, 6, 4)
        val blockNumber3: Long = 125
        blockIdentifiers.add(BlockIdentifier(blockHash1, blockNumber1))
        blockIdentifiers.add(BlockIdentifier(blockHash2, blockNumber2))
        blockIdentifiers.add(BlockIdentifier(blockHash3, blockNumber3))
        val msg = NewBlockHashesMessage(blockIdentifiers)

        ethHandler.setGetNewBlockHeadersParams(blockHash2, 3, 0, false)
        ethHandler.processNewBlockHashes(msg)
        assert(ethHandler.wasCalled)
    }

    private inner class Eth62Tester internal constructor() : Eth62() {

        private var blockHash: ByteArray? = null
        private var maxBlockAsk: Int = 0
        private var skip: Int = 0
        private var reverse: Boolean = false

        var wasCalled = false

        init {
            this.syncDone = true
            this.channel = Channel()
        }

        internal fun setGetNewBlockHeadersParams(blockHash: ByteArray, maxBlocksAsk: Int, skip: Int, reverse: Boolean) {
            this.blockHash = blockHash
            this.maxBlockAsk = maxBlocksAsk
            this.skip = skip
            this.reverse = reverse
            this.wasCalled = false
        }

        @Synchronized override fun sendGetNewBlockHeaders(blockHash: ByteArray, maxBlocksAsk: Int, skip: Int, reverse: Boolean) {
            this.wasCalled = true
            Eth62.logger.error("Request for sending new headers: hash {}, max {}, skip {}, reverse {}",
                    Hex.toHexString(blockHash), maxBlocksAsk, skip, reverse)
            assert(Arrays.equals(blockHash, this.blockHash) &&
                    maxBlocksAsk == this.maxBlockAsk && skip == this.skip && reverse == this.reverse)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("test")
    }
}
