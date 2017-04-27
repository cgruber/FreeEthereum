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

import org.ethereum.config.CommonConfig
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.db.IndexedBlockStore
import org.ethereum.db.RepositoryRoot
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.util.BIUtil
import org.ethereum.validator.DependentBlockHeaderRuleAdapter
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * @author Mikhail Kalinin
 * *
 * @since 24.09.2015
 */
@Ignore
class PendingStateLongRunTest {

    private var blockchain: Blockchain? = null

    private var pendingState: PendingState? = null

    private var strData: List<String>? = null

    @Before
    @Throws(URISyntaxException::class, IOException::class, InterruptedException::class)
    fun setup() {

        blockchain = createBlockchain(Genesis.instance as Genesis)
        pendingState = (blockchain as BlockchainImpl).pendingState

        val blocks = ClassLoader.getSystemResource("state/47250.dmp")
        val file = File(blocks.toURI())
        strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)

        for (i in 0..45999) {
            val b = Block(Hex.decode(strData!![i]))
            blockchain!!.tryToConnect(b)
        }
    }

    @Test // test with real data from the frontier net
    fun test_1() {

        val b46169 = Block(Hex.decode(strData!![46169]))
        val b46170 = Block(Hex.decode(strData!![46170]))

        val tx46169 = b46169.transactionsList[0]
        val tx46170 = b46170.transactionsList[0]

        var pending = pendingState!!.repository

        val balanceBefore46169 = pending.getAccountState(tx46169.receiveAddress).balance
        val balanceBefore46170 = pending.getAccountState(tx46170.receiveAddress).balance

        pendingState!!.addPendingTransaction(tx46169)
        pendingState!!.addPendingTransaction(tx46170)

        for (i in 46000..46168) {
            val b = Block(Hex.decode(strData!![i]))
            blockchain!!.tryToConnect(b)
        }

        pending = pendingState!!.repository

        val balanceAfter46169 = balanceBefore46169.add(BIUtil.toBI(tx46169.value))

        assertEquals(pendingState!!.pendingTransactions.size.toLong(), 2)
        assertEquals(balanceAfter46169, pending.getAccountState(tx46169.receiveAddress).balance)

        blockchain!!.tryToConnect(b46169)
        pending = pendingState!!.repository

        assertEquals(balanceAfter46169, pending.getAccountState(tx46169.receiveAddress).balance)
        assertEquals(pendingState!!.pendingTransactions.size.toLong(), 1)

        val balanceAfter46170 = balanceBefore46170.add(BIUtil.toBI(tx46170.value))

        assertEquals(balanceAfter46170, pending.getAccountState(tx46170.receiveAddress).balance)

        blockchain!!.tryToConnect(b46170)
        pending = pendingState!!.repository

        assertEquals(balanceAfter46170, pending.getAccountState(tx46170.receiveAddress).balance)
        assertEquals(pendingState!!.pendingTransactions.size.toLong(), 0)
    }

    private fun createBlockchain(genesis: Genesis): Blockchain {
        val blockStore = IndexedBlockStore()
        blockStore.init(HashMapDB<ByteArray>(), HashMapDB<ByteArray>())

        val repository = RepositoryRoot(HashMapDB())

        val programInvokeFactory = ProgramInvokeFactoryImpl()

        val blockchain = BlockchainImpl(blockStore, repository)
                .withParentBlockHeaderValidator(CommonConfig().parentHeaderValidator())
        blockchain.setParentHeaderValidator(DependentBlockHeaderRuleAdapter())
        blockchain.programInvokeFactory = programInvokeFactory

        blockchain.byTest = true

        val pendingState = PendingStateImpl(EthereumListenerAdapter(), blockchain)

        pendingState.setBlockchain(blockchain)
        blockchain.pendingState = pendingState

        val track = repository.startTracking()
        Genesis.populateRepository(track, genesis)

        track.commit()

        blockStore.saveBlock(Genesis.instance, Genesis.instance.cumulativeDifficulty, true)

        blockchain.bestBlock = Genesis.instance
        blockchain.totalDifficulty = Genesis.instance.cumulativeDifficulty

        return blockchain
    }
}
