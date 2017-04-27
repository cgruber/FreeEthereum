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

import org.apache.commons.lang3.tuple.Pair
import org.apache.commons.lang3.tuple.Triple
import org.ethereum.config.SystemProperties
import org.ethereum.crypto.ECKey
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListener.PendingTransactionState.*
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.util.blockchain.EtherUtil.Unit.ETHER
import org.ethereum.util.blockchain.EtherUtil.convert
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.math.BigInteger
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Mikhail Kalinin
 * *
 * @since 28.09.2015
 */
class PendingStateTest {

    @Test
    @Throws(InterruptedException::class)
    fun testSimple() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        var txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()

        bc.sendEther(ByteArray(20), BigInteger.valueOf(100000))
        bc.sendEther(ByteArray(20), BigInteger.valueOf(100000))

        bc.createBlock()
        l.onBlock.poll(5, SECONDS)

        val tx1 = bc.createTransaction(100, ByteArray(32), 1000, ByteArray(0))
        pendingState.addPendingTransaction(tx1)
        // dropped due to large nonce
        Assert.assertEquals(l.pollTxUpdateState(tx1), DROPPED)

        val tx1_ = bc.createTransaction(0, ByteArray(32), 1000, ByteArray(0))
        pendingState.addPendingTransaction(tx1_)
        // dropped due to low nonce
        Assert.assertEquals(l.pollTxUpdateState(tx1_), DROPPED)

        val tx2 = bc.createTransaction(2, alice.address, 1000000, ByteArray(0))
        val tx3 = bc.createTransaction(3, alice.address, 1000000, ByteArray(0))
        pendingState.addPendingTransaction(tx2)
        pendingState.addPendingTransaction(tx3)

        txUpd = l.pollTxUpdate(tx2)
        Assert.assertEquals(txUpd.middle, NEW_PENDING)
        Assert.assertTrue(txUpd.left.isValid)
        txUpd = l.pollTxUpdate(tx3)
        Assert.assertEquals(txUpd.middle, NEW_PENDING)
        Assert.assertTrue(txUpd.left.isValid)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf((2000000 - 100000).toLong())) > 0)

        pendingState.addPendingTransaction(tx2)  // double transaction submit
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())

        bc.createBlock()

        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING)
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING)

        bc.submitTransaction(tx2)
        val b3 = bc.createBlock()

        txUpd = l.pollTxUpdate(tx2)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertEquals(txUpd.right, b3)
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING)

        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf((2000000 - 100000).toLong())) > 0)

        for (i in 0..SystemProperties.getDefault()!!.txOutdatedThreshold() + 1 - 1) {
            bc.createBlock()
            txUpd = l.pollTxUpdate(tx3)
            if (txUpd.middle !== PENDING) break
        }

        // tx3 dropped due to timeout
        Assert.assertEquals(txUpd.middle, DROPPED)
        Assert.assertEquals(txUpd.left.transaction, tx3)
        Assert.assertFalse(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf((2000000 - 100000).toLong())) > 0)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRebranch1() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>? = null
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        bc.sendEther(charlie.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)
        val tx2 = bc.createTransaction(charlie, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx2)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)
        Assert.assertEquals(l.pollTxUpdateState(tx2), NEW_PENDING)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)

        bc.submitTransaction(tx1)
        val b2 = bc.createBlock()

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED)
        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)

        bc.submitTransaction(tx2)
        val b3 = bc.createBlock()

        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)

        val b2_ = bc.createForkBlock(b1)
        val b3_ = bc.createForkBlock(b2_)

        bc.submitTransaction(tx2)
        val b4_ = bc.createForkBlock(b3_)

        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED)
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)

        bc.submitTransaction(tx1)
        val b5_ = bc.createForkBlock(b4_)

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRebranch2() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        var txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        bc.sendEther(charlie.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)
        val tx2 = bc.createTransaction(charlie, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx2)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)
        Assert.assertEquals(l.pollTxUpdateState(tx2), NEW_PENDING)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(2000000)) == 0)

        bc.submitTransaction(tx1)
        bc.sendEther(alice.address, BigInteger.valueOf(1000000))
        val b2 = bc.createBlock()
        val tx3 = b2.transactionsList[1]

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED)
        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(3000000)) == 0)

        bc.sendEther(alice.address, BigInteger.valueOf(1000000))
        bc.submitTransaction(tx2)
        val b3 = bc.createBlock()
        val tx4 = b3.transactionsList[0]

        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(4000000)) == 0)

        bc.submitTransaction(tx2)
        val b2_ = bc.createForkBlock(b1)
        bc.submitTransaction(tx1)
        val b3_ = bc.createForkBlock(b2_)

        val b4_ = bc.createForkBlock(b3_) // becoming the best branch

        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertEquals(txUpd.right, b3_)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        txUpd = l.pollTxUpdate(tx2)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertEquals(txUpd.right, b2_)
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING)
        Assert.assertEquals(l.pollTxUpdateState(tx4), PENDING)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(4000000)) == 0)

        // rebranching back
        val b4 = bc.createForkBlock(b3)
        val b5 = bc.createForkBlock(b4)

        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertEquals(txUpd.right, b2)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        txUpd = l.pollTxUpdate(tx2)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertEquals(txUpd.right, b3)
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty())
        Assert.assertEquals(l.pollTxUpdateState(tx3), INCLUDED)
        Assert.assertEquals(l.pollTxUpdateState(tx4), INCLUDED)
        Assert.assertTrue(pendingState.repository.getBalance(alice.address).compareTo(BigInteger.valueOf(4000000)) == 0)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRebranch3() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        var txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        bc.sendEther(charlie.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)

        bc.submitTransaction(tx1)
        val b2 = bc.createBlock()

        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        val b3 = bc.createBlock()
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        bc.submitTransaction(tx1)
        val b2_ = bc.createForkBlock(b1)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        val b3_ = bc.createForkBlock(b2_)
        val b4_ = bc.createForkBlock(b3_)
        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertArrayEquals(txUpd.right.hash, b2_.hash)

        val b4 = bc.createForkBlock(b3)
        val b5 = bc.createForkBlock(b4)
        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertArrayEquals(txUpd.right.hash, b2.hash)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testOldBlockIncluded() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        for (i in 0..15) {
            bc.createBlock()
        }

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)
        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)

        bc.submitTransaction(tx1)
        val b2_ = bc.createForkBlock(b1)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        bc.submitTransaction(tx1)
        val b18 = bc.createBlock()
        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertArrayEquals(txUpd.right.hash, b18.hash)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBlockOnlyIncluded() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        bc.submitTransaction(tx1)
        val b2 = bc.createBlock()

        val b2_ = bc.createForkBlock(b1)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
        val b3_ = bc.createForkBlock(b2_)
        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, PENDING)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTrackTx1() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))

        val b1 = bc.createBlock()
        val b2 = bc.createBlock()
        val b3 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        bc.submitTransaction(tx1)
        val b2_ = bc.createForkBlock(b1)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        pendingState.trackTransaction(tx1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)

        val b3_ = bc.createForkBlock(b2_)
        val b4_ = bc.createForkBlock(b3_)
        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertArrayEquals(txUpd.right.hash, b2_.hash)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPrevBlock() {
        val bc = StandaloneBlockchain()
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()

        val contract = bc.submitNewContract("contract A {" +
                "  function getPrevBlockHash() returns (bytes32) {" +
                "    return block.blockhash(block.number - 1);" +
                "  }" +
                "}")

        bc.sendEther(bob.address, convert(100, ETHER))

        val b1 = bc.createBlock()
        val b2 = bc.createBlock()
        val b3 = bc.createBlock()

        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>

        contract.callFunction("getPrevBlockHash")
        bc.generatePendingTransactions()
        txUpd = l.onPendingTransactionUpdate.values.iterator().next().poll()

        Assert.assertArrayEquals(txUpd.left.executionResult, b3.hash)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTrackTx2() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        bc.submitTransaction(tx1)
        val b2 = bc.createBlock()
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        pendingState.trackTransaction(tx1)

        txUpd = l.pollTxUpdate(tx1)
        Assert.assertEquals(txUpd.middle, INCLUDED)
        Assert.assertArrayEquals(txUpd.right.hash, b2.hash)

        val b2_ = bc.createForkBlock(b1)
        val b3_ = bc.createForkBlock(b2_)
        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRejected1() {
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>? = null
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        bc.sendEther(charlie.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)

        bc.submitTransaction(tx1)
        val b2_ = bc.createForkBlock(b1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED)

        val b2 = bc.createForkBlock(b1)
        val b3 = bc.createForkBlock(b2)
        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())

        for (i in 0..15) {
            bc.createBlock()
            val state = l.pollTxUpdateState(tx1)
            if (state === EthereumListener.PendingTransactionState.DROPPED) {
                break
            }
            if (i == 15) {
                throw RuntimeException("Transaction was not dropped")
            }
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIncludedRejected() {
        // check INCLUDED => DROPPED state transition when a new (long) fork without
        // the transaction becomes the main chain
        val bc = StandaloneBlockchain()
        val l = PendingListener()
        bc.addEthereumListener(l)
        val txUpd: Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>? = null
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val bob = ECKey()
        val charlie = ECKey()

        bc.sendEther(bob.address, convert(100, ETHER))
        bc.sendEther(charlie.address, convert(100, ETHER))

        val b1 = bc.createBlock()

        val tx1 = bc.createTransaction(bob, 0, alice.address, BigInteger.valueOf(1000000), ByteArray(0))
        pendingState.addPendingTransaction(tx1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING)

        bc.submitTransaction(tx1)
        val b2 = bc.createForkBlock(b1)

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED)

        for (i in 0..9) {
            bc.createBlock()
        }

        var b_ = bc.createForkBlock(b1)

        for (i in 0..10) {
            b_ = bc.createForkBlock(b_)
        }

        Assert.assertEquals(l.pollTxUpdateState(tx1), DROPPED)
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testInvalidTransaction() {
        val bc = StandaloneBlockchain()
        val txHandle = CountDownLatch(1)
        val l = object : PendingListener() {
            override fun onPendingTransactionUpdate(txReceipt: TransactionReceipt, state: EthereumListener.PendingTransactionState, block: Block) {
                assert(!txReceipt.isSuccessful)
                assert(txReceipt.error.toLowerCase().contains("invalid"))
                assert(txReceipt.error.toLowerCase().contains("receive address"))
                txHandle.countDown()
            }
        }
        bc.addEthereumListener(l)
        val pendingState = bc.blockchain.pendingState as PendingStateImpl

        val alice = ECKey()
        val rnd = Random()
        val b1 = bc.createBlock()
        val b = ByteArray(21)
        rnd.nextBytes(b)

        val tx1 = bc.createTransaction(alice, 0, b, BigInteger.ONE, ByteArray(0))
        pendingState.addPendingTransaction(tx1)

        assert(txHandle.await(3, TimeUnit.SECONDS))
    }

    internal open class PendingListener : EthereumListenerAdapter() {
        val onBlock: BlockingQueue<Pair<Block, List<TransactionReceipt>>> = LinkedBlockingQueue()
        val onPendingStateChanged: BlockingQueue<Any> = LinkedBlockingQueue()
        //        public BlockingQueue<Triple<TransactionReceipt, PendingTransactionState, Block>> onPendingTransactionUpdate = new LinkedBlockingQueue<>();

        val onPendingTransactionUpdate: MutableMap<ByteArrayWrapper, BlockingQueue<Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>>> = HashMap()

        override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
            println("PendingStateTest.onBlock:" + "block = [" + block.shortDescr + "]")
            onBlock.add(Pair.of(block, receipts))
        }

        override fun onPendingStateChanged(pendingState: PendingState) {
            println("PendingStateTest.onPendingStateChanged.")
            onPendingStateChanged.add(Any())
        }

        override fun onPendingTransactionUpdate(txReceipt: TransactionReceipt, state: EthereumListener.PendingTransactionState, block: Block) {
            println("PendingStateTest.onPendingTransactionUpdate:" + "txReceipt.err = [" + txReceipt.error + "], state = [" + state + "], block: " + block.shortDescr)
            getQueueFor(txReceipt.transaction).add(Triple.of<TransactionReceipt, EthereumListener.PendingTransactionState, Block>(txReceipt, state, block))
        }

        @Synchronized fun getQueueFor(tx: Transaction): BlockingQueue<Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>> {
            val hashW = ByteArrayWrapper(tx.hash)
            val queue = (onPendingTransactionUpdate as java.util.Map<ByteArrayWrapper, BlockingQueue<Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>>>).computeIfAbsent(hashW) { k -> LinkedBlockingQueue<Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block>>() }
            return queue
        }

        @Throws(InterruptedException::class)
        fun pollTxUpdateState(tx: Transaction): EthereumListener.PendingTransactionState {
            return getQueueFor(tx).poll(5, SECONDS).middle
        }

        @Throws(InterruptedException::class)
        fun pollTxUpdate(tx: Transaction): Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> {
            return getQueueFor(tx).poll(5, SECONDS)
        }
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
