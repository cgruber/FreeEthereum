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

import org.ethereum.config.SystemProperties
import org.ethereum.core.BlockHeader
import org.ethereum.core.ImportLightTest1
import org.ethereum.core.ImportResult
import org.ethereum.core.Transaction
import org.ethereum.core.genesis.GenesisLoader
import org.ethereum.crypto.ECKey
import org.ethereum.db.PruneManager
import org.ethereum.util.ByteUtil
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.spongycastle.util.encoders.Hex
import java.util.*
import javax.annotation.Resource

/**
 * Created by Anton Nashatyrev on 10.12.2015.
 */
class MineBlock {

    @Mock
    internal var pruneManager: PruneManager? = null

    @InjectMocks
    @Resource
    private val blockchain = ImportLightTest1.createBlockchain(GenesisLoader.loadGenesis(
            javaClass.getResourceAsStream("/genesis/genesis-light.json")))

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Initialize mocks created above
        MockitoAnnotations.initMocks(this)
    }


    @Test
    @Throws(Exception::class)
    fun mine1() {

        blockchain.minerCoinbase = Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f")
        val parent = blockchain.bestBlock

        val pendingTx = ArrayList<Transaction>()

        val senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"))
        val receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c")
        val tx = Transaction(byteArrayOf(0), byteArrayOf(1), ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(77), ByteArray(0))
        tx.sign(senderKey)
        pendingTx.add(tx)

        val b = blockchain.createNewBlock(parent, pendingTx, emptyList<BlockHeader>())

        println("Mining...")
        Ethash.getForBlock(SystemProperties.getDefault(), b.number).mineLight(b).get()
        println("Validating...")
        val valid = Ethash.getForBlock(SystemProperties.getDefault(), b.number).validate(b.header)
        Assert.assertTrue(valid)

        println("Connecting...")
        val importResult = blockchain.tryToConnect(b)

        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)
        println(Hex.toHexString(blockchain.repository.root))
    }

    companion object {

        @BeforeClass
        fun setup() {
            SystemProperties.getDefault()!!.blockchainConfig = StandaloneBlockchain.getEasyMiningConfig()
        }

        @AfterClass
        fun cleanup() {
            SystemProperties.resetToDefault()
        }
    }
}
