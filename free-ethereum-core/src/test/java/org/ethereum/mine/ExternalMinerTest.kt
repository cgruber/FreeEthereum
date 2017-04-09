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

package org.ethereum.mine

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.FrontierConfig
import org.ethereum.core.Block
import org.ethereum.core.BlockHeader
import org.ethereum.core.ImportResult
import org.ethereum.core.Transaction
import org.ethereum.facade.EthereumImpl
import org.ethereum.listener.CompositeEthereumListener
import org.ethereum.util.ByteUtil
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.math.BigInteger
import javax.annotation.Resource


/**
 * Creates an instance
 */
class ExternalMinerTest {

    private val bc = StandaloneBlockchain().withAutoblock(false)

    private val listener = CompositeEthereumListener()

    @Mock
    private val ethereum: EthereumImpl? = null

    @InjectMocks
    @Resource
    private val blockMiner = BlockMiner(SystemProperties.getDefault(), listener, bc.blockchain,
            bc.blockchain.blockStore, bc.pendingState)

    @Before
    fun setup() {
        SystemProperties.getDefault()!!.blockchainConfig = FrontierConfig(object : FrontierConfig.FrontierConstants() {
            override val minimumDifficulty: BigInteger
                get() = BigInteger.ONE
        })

        // Initialize mocks created above
        MockitoAnnotations.initMocks(this)

        `when`<ImportResult>(ethereum!!.addNewMinedBlock(ArgumentMatchers.any(Block::class.java))).thenAnswer { invocation ->
            val block = invocation.arguments[0] as Block
            bc.blockchain.tryToConnect(block)
        }
    }

    @Test
    @Throws(Exception::class)
    fun externalMiner_shouldWork() {

        val startBestBlock = bc.blockchain.bestBlock

        val futureBlock = SettableFuture.create<MinerIfc.MiningResult>()

        blockMiner.setExternalMiner(object : MinerIfc {
            override fun mine(block: Block): ListenableFuture<MinerIfc.MiningResult> {
                //                System.out.print("Mining requested");
                return futureBlock
            }

            override fun validate(blockHeader: BlockHeader): Boolean {
                return true
            }
        })
        val b = bc.blockchain.createNewBlock(startBestBlock, emptyList<Transaction>(), emptyList<BlockHeader>())
        Ethash.getForBlock(SystemProperties.getDefault(), b.number).mineLight(b).get()
        futureBlock.set(MinerIfc.MiningResult(ByteUtil.byteArrayToLong(b.nonce), b.mixHash, b))

        assertThat(bc.blockchain.bestBlock.number, `is`(startBestBlock.number + 1))
    }
}
