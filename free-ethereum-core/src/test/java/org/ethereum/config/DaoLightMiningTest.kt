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

package org.ethereum.config

import org.ethereum.config.blockchain.DaoHFConfig
import org.ethereum.config.blockchain.DaoNoHFConfig
import org.ethereum.config.net.BaseNetConfig
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.Assert.assertEquals
import org.junit.Test

class DaoLightMiningTest {

    // configure
    private val FORK_BLOCK = 20
    private val FORK_BLOCK_AFFECTED = 10 // hardcoded in DAO config


    @Test
    fun testDaoExtraData() {
        val sb = createBlockchain(true)

        for (i in 0..FORK_BLOCK + 30 - 1) {
            val b = sb.createBlock()
            //            System.out.println("Created block " + b.getNumber() + " " + getData(b.getExtraData()));
        }

        assertEquals("EthereumJ powered", getData(sb, (FORK_BLOCK - 1).toLong()))
        assertEquals("dao-hard-fork", getData(sb, FORK_BLOCK.toLong()))
        assertEquals("dao-hard-fork", getData(sb, (FORK_BLOCK + FORK_BLOCK_AFFECTED - 1).toLong()))
        assertEquals("EthereumJ powered", getData(sb, (FORK_BLOCK + FORK_BLOCK_AFFECTED).toLong()))
    }

    @Test
    fun testNoDaoExtraData() {
        val sb = createBlockchain(false)

        for (i in 0..FORK_BLOCK + 30 - 1) {
            val b = sb.createBlock()
        }

        assertEquals("EthereumJ powered", getData(sb, (FORK_BLOCK - 1).toLong()))
        assertEquals("", getData(sb, FORK_BLOCK.toLong()))
        assertEquals("", getData(sb, (FORK_BLOCK + FORK_BLOCK_AFFECTED - 1).toLong()))
        assertEquals("EthereumJ powered", getData(sb, (FORK_BLOCK + FORK_BLOCK_AFFECTED).toLong()))
    }

    private fun getData(sb: StandaloneBlockchain, blockNumber: Long): String {
        return String(sb.blockchain.getBlockByNumber(blockNumber).extraData)
    }

    private fun createBlockchain(proFork: Boolean): StandaloneBlockchain {
        val netConfig = BaseNetConfig()
        val c1 = StandaloneBlockchain.getEasyMiningConfig()
        netConfig.add(0, StandaloneBlockchain.getEasyMiningConfig())
        netConfig.add(FORK_BLOCK.toLong(), if (proFork) DaoHFConfig(c1, FORK_BLOCK.toLong()) else DaoNoHFConfig(c1, FORK_BLOCK.toLong()))

        // create blockchain
        return StandaloneBlockchain()
                .withAutoblock(true)
                .withNetConfig(netConfig)
    }
}
