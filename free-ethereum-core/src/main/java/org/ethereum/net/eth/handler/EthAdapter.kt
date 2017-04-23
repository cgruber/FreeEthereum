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

import com.google.common.util.concurrent.ListenableFuture
import org.ethereum.core.*
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.eth.EthVersion.Companion
import org.ethereum.sync.SyncStatistics
import java.math.BigInteger

/**
 * It's quite annoying to always check `if (eth != null)` before accessing it. <br></br>

 * This adapter helps to avoid such checks. It provides meaningful answers to Eth client
 * assuming that Eth hasn't been initialized yet. <br></br>

 * Check [org.ethereum.net.server.Channel] for example.

 * @author Mikhail Kalinin
 * *
 * @since 20.08.2015
 */
class EthAdapter : Eth {

    private val syncStats = SyncStatistics()

    override fun hasStatusPassed(): Boolean {
        return false
    }

    override fun hasStatusSucceeded(): Boolean {
        return false
    }

    override fun onShutdown() {}

    override fun getSyncStats(): String {
        return ""
    }

    override fun isHashRetrievingDone(): Boolean {
        return false
    }

    override fun isHashRetrieving(): Boolean {
        return false
    }

    override fun isIdle(): Boolean {
        return true
    }

    override fun getStats(): SyncStatistics {
        return syncStats
    }

    override fun disableTransactions() {}

    override fun enableTransactions() {}

    override fun sendTransaction(tx: List<Transaction>) {}

    override fun sendGetBlockHeaders(blockNumber: Long, maxBlocksAsk: Int, reverse: Boolean): ListenableFuture<List<BlockHeader>>? {
        return null
    }

    override fun sendGetBlockHeaders(blockHash: ByteArray, maxBlocksAsk: Int, skip: Int, reverse: Boolean): ListenableFuture<List<BlockHeader>>? {
        return null
    }

    override fun sendGetBlockBodies(headers: List<BlockHeaderWrapper>): ListenableFuture<List<Block>>? {
        return null
    }

    override fun sendNewBlock(newBlock: Block) {}

    override fun sendNewBlockHashes(block: Block) {

    }

    override fun getVersion(): EthVersion {
        return Companion.fromCode(Companion.UPPER.toInt())!!
    }

    override fun onSyncDone(done: Boolean) {}

    override fun sendStatus() {}

    override fun dropConnection() {}

    override fun fetchBodies(headers: List<BlockHeaderWrapper>) {}

    override fun getBestKnownBlock(): BlockIdentifier? {
        return null
    }

    override fun getTotalDifficulty(): BigInteger {
        return BigInteger.ZERO
    }

}
