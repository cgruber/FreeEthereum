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

package org.ethereum.core;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.ethereum.listener.EthereumListener.PendingTransactionState.*;
import static org.ethereum.util.blockchain.EtherUtil.Unit.ETHER;
import static org.ethereum.util.blockchain.EtherUtil.convert;

/**
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
public class PendingStateTest {

    @BeforeClass
    public static void setup() {
        SystemProperties.getDefault().setBlockchainConfig(StandaloneBlockchain.getEasyMiningConfig());
    }

    @AfterClass
    public static void cleanup() {
        SystemProperties.resetToDefault();
    }

    @Test
    public void testSimple() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();

        bc.sendEther(new byte[20], BigInteger.valueOf(100000));
        bc.sendEther(new byte[20], BigInteger.valueOf(100000));

        bc.createBlock();
        l.onBlock.poll(5, SECONDS);

        final Transaction tx1 = bc.createTransaction(100, new byte[32], 1000, new byte[0]);
        pendingState.addPendingTransaction(tx1);
        // dropped due to large nonce
        Assert.assertEquals(l.pollTxUpdateState(tx1), DROPPED);

        final Transaction tx1_ = bc.createTransaction(0, new byte[32], 1000, new byte[0]);
        pendingState.addPendingTransaction(tx1_);
        // dropped due to low nonce
        Assert.assertEquals(l.pollTxUpdateState(tx1_), DROPPED);

        final Transaction tx2 = bc.createTransaction(2, alice.getAddress(), 1000000, new byte[0]);
        final Transaction tx3 = bc.createTransaction(3, alice.getAddress(), 1000000, new byte[0]);
        pendingState.addPendingTransaction(tx2);
        pendingState.addPendingTransaction(tx3);

        txUpd = l.pollTxUpdate(tx2);
        Assert.assertEquals(txUpd.getMiddle(), NEW_PENDING);
        Assert.assertTrue(txUpd.getLeft().isValid());
        txUpd = l.pollTxUpdate(tx3);
        Assert.assertEquals(txUpd.getMiddle(), NEW_PENDING);
        Assert.assertTrue(txUpd.getLeft().isValid());
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000 - 100000)) > 0);

        pendingState.addPendingTransaction(tx2);  // double transaction submit
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());

        bc.createBlock();

        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING);
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING);

        bc.submitTransaction(tx2);
        final Block b3 = bc.createBlock();

        txUpd = l.pollTxUpdate(tx2);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertEquals(txUpd.getRight(), b3);
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING);

        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000 - 100000)) > 0);

        for (int i = 0; i < SystemProperties.getDefault().txOutdatedThreshold() + 1; i++) {
            bc.createBlock();
            txUpd = l.pollTxUpdate(tx3);
            if (txUpd.getMiddle() != PENDING) break;
        }

        // tx3 dropped due to timeout
        Assert.assertEquals(txUpd.getMiddle(), DROPPED);
        Assert.assertEquals(txUpd.getLeft().getTransaction(), tx3);
        Assert.assertFalse(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000 - 100000)) > 0);
    }

    @Test
    public void testRebranch1() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd = null;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        bc.sendEther(charlie.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);
        final Transaction tx2 = bc.createTransaction(charlie, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx2);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);
        Assert.assertEquals(l.pollTxUpdateState(tx2), NEW_PENDING);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);

        bc.submitTransaction(tx1);
        final Block b2 = bc.createBlock();

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED);
        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);

        bc.submitTransaction(tx2);
        final Block b3 = bc.createBlock();

        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);

        final Block b2_ = bc.createForkBlock(b1);
        final Block b3_ = bc.createForkBlock(b2_);

        bc.submitTransaction(tx2);
        final Block b4_ = bc.createForkBlock(b3_);

        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED);
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);

        bc.submitTransaction(tx1);
        final Block b5_ = bc.createForkBlock(b4_);

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);
    }

    @Test
    public void testRebranch2() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        bc.sendEther(charlie.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);
        final Transaction tx2 = bc.createTransaction(charlie, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx2);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);
        Assert.assertEquals(l.pollTxUpdateState(tx2), NEW_PENDING);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(2000000)) == 0);

        bc.submitTransaction(tx1);
        bc.sendEther(alice.getAddress(), BigInteger.valueOf(1000000));
        final Block b2 = bc.createBlock();
        final Transaction tx3 = b2.getTransactionsList().get(1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED);
        Assert.assertEquals(l.pollTxUpdateState(tx2), PENDING);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(3000000)) == 0);

        bc.sendEther(alice.getAddress(), BigInteger.valueOf(1000000));
        bc.submitTransaction(tx2);
        final Block b3 = bc.createBlock();
        final Transaction tx4 = b3.getTransactionsList().get(0);

        Assert.assertEquals(l.pollTxUpdateState(tx2), INCLUDED);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(4000000)) == 0);

        bc.submitTransaction(tx2);
        final Block b2_ = bc.createForkBlock(b1);
        bc.submitTransaction(tx1);
        final Block b3_ = bc.createForkBlock(b2_);

        final Block b4_ = bc.createForkBlock(b3_); // becoming the best branch

        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertEquals(txUpd.getRight(), b3_);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        txUpd = l.pollTxUpdate(tx2);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertEquals(txUpd.getRight(), b2_);
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());
        Assert.assertEquals(l.pollTxUpdateState(tx3), PENDING);
        Assert.assertEquals(l.pollTxUpdateState(tx4), PENDING);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(4000000)) == 0);

        // rebranching back
        final Block b4 = bc.createForkBlock(b3);
        final Block b5 = bc.createForkBlock(b4);

        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertEquals(txUpd.getRight(), b2);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        txUpd = l.pollTxUpdate(tx2);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertEquals(txUpd.getRight(), b3);
        Assert.assertTrue(l.getQueueFor(tx2).isEmpty());
        Assert.assertEquals(l.pollTxUpdateState(tx3), INCLUDED);
        Assert.assertEquals(l.pollTxUpdateState(tx4), INCLUDED);
        Assert.assertTrue(pendingState.getRepository().getBalance(alice.getAddress()).
                compareTo(BigInteger.valueOf(4000000)) == 0);
    }

    @Test
    public void testRebranch3() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        bc.sendEther(charlie.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);

        bc.submitTransaction(tx1);
        final Block b2 = bc.createBlock();

        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        final Block b3 = bc.createBlock();
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        bc.submitTransaction(tx1);
        final Block b2_ = bc.createForkBlock(b1);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        final Block b3_ = bc.createForkBlock(b2_);
        final Block b4_ = bc.createForkBlock(b3_);
        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertArrayEquals(txUpd.getRight().getHash(), b2_.getHash());

        final Block b4 = bc.createForkBlock(b3);
        final Block b5 = bc.createForkBlock(b4);
        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertArrayEquals(txUpd.getRight().getHash(), b2.getHash());
    }

    @Test
    public void testOldBlockIncluded() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        for (int i = 0; i < 16; i++) {
            bc.createBlock();
        }

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);
        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);

        bc.submitTransaction(tx1);
        final Block b2_ = bc.createForkBlock(b1);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        bc.submitTransaction(tx1);
        final Block b18 = bc.createBlock();
        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertArrayEquals(txUpd.getRight().getHash(), b18.getHash());
    }

    @Test
    public void testBlockOnlyIncluded() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        bc.submitTransaction(tx1);
        final Block b2 = bc.createBlock();

        final Block b2_ = bc.createForkBlock(b1);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
        final Block b3_ = bc.createForkBlock(b2_);
        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), PENDING);
    }

    @Test
    public void testTrackTx1() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();
        final Block b2 = bc.createBlock();
        final Block b3 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        bc.submitTransaction(tx1);
        final Block b2_ = bc.createForkBlock(b1);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        pendingState.trackTransaction(tx1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);

        final Block b3_ = bc.createForkBlock(b2_);
        final Block b4_ = bc.createForkBlock(b3_);
        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertArrayEquals(txUpd.getRight().getHash(), b2_.getHash());
    }

    @Test
    public void testPrevBlock() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();

        final SolidityContract contract = bc.submitNewContract("contract A {" +
                "  function getPrevBlockHash() returns (bytes32) {" +
                "    return block.blockhash(block.number - 1);" +
                "  }" +
                "}");

        bc.sendEther(bob.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();
        final Block b2 = bc.createBlock();
        final Block b3 = bc.createBlock();

        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;

        contract.callFunction("getPrevBlockHash");
        bc.generatePendingTransactions();
        txUpd = l.onPendingTransactionUpdate.values().iterator().next().poll();

        Assert.assertArrayEquals(txUpd.getLeft().getExecutionResult(), b3.getHash());
    }

    @Test
    public void testTrackTx2() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        bc.submitTransaction(tx1);
        final Block b2 = bc.createBlock();
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        pendingState.trackTransaction(tx1);

        txUpd = l.pollTxUpdate(tx1);
        Assert.assertEquals(txUpd.getMiddle(), INCLUDED);
        Assert.assertArrayEquals(txUpd.getRight().getHash(), b2.getHash());

        final Block b2_ = bc.createForkBlock(b1);
        final Block b3_ = bc.createForkBlock(b2_);
        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING);
    }

    @Test
    public void testRejected1() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd = null;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        bc.sendEther(charlie.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);

        bc.submitTransaction(tx1);
        final Block b2_ = bc.createForkBlock(b1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED);

        final Block b2 = bc.createForkBlock(b1);
        final Block b3 = bc.createForkBlock(b2);
        Assert.assertEquals(l.pollTxUpdateState(tx1), PENDING);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());

        for (int i = 0; i < 16; i++) {
            bc.createBlock();
            final EthereumListener.PendingTransactionState state = l.pollTxUpdateState(tx1);
            if (state == EthereumListener.PendingTransactionState.DROPPED) {
                break;
            }
            if (i == 15) {
                throw new RuntimeException("Transaction was not dropped");
            }
        }
    }

    @Test
    public void testIncludedRejected() throws InterruptedException {
        // check INCLUDED => DROPPED state transition when a new (long) fork without
        // the transaction becomes the main chain
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final PendingListener l = new PendingListener();
        bc.addEthereumListener(l);
        final Triple<TransactionReceipt, EthereumListener.PendingTransactionState, Block> txUpd = null;
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final ECKey bob = new ECKey();
        final ECKey charlie = new ECKey();

        bc.sendEther(bob.getAddress(), convert(100, ETHER));
        bc.sendEther(charlie.getAddress(), convert(100, ETHER));

        final Block b1 = bc.createBlock();

        final Transaction tx1 = bc.createTransaction(bob, 0, alice.getAddress(), BigInteger.valueOf(1000000), new byte[0]);
        pendingState.addPendingTransaction(tx1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), NEW_PENDING);

        bc.submitTransaction(tx1);
        final Block b2 = bc.createForkBlock(b1);

        Assert.assertEquals(l.pollTxUpdateState(tx1), INCLUDED);

        for (int i = 0; i < 10; i++) {
            bc.createBlock();
        }

        Block b_ = bc.createForkBlock(b1);

        for (int i = 0; i < 11; i++) {
            b_ = bc.createForkBlock(b_);
        }

        Assert.assertEquals(l.pollTxUpdateState(tx1), DROPPED);
        Assert.assertTrue(l.getQueueFor(tx1).isEmpty());
    }

    @Test
    public void testInvalidTransaction() throws InterruptedException {
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final CountDownLatch txHandle = new CountDownLatch(1);
        final PendingListener l = new PendingListener() {
            @Override
            public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {
                assert !txReceipt.isSuccessful();
                assert txReceipt.getError().toLowerCase().contains("invalid");
                assert txReceipt.getError().toLowerCase().contains("receive address");
                txHandle.countDown();
            }
        };
        bc.addEthereumListener(l);
        final PendingStateImpl pendingState = (PendingStateImpl) bc.getBlockchain().getPendingState();

        final ECKey alice = new ECKey();
        final Random rnd = new Random();
        final Block b1 = bc.createBlock();
        final byte[] b = new byte[21];
        rnd.nextBytes(b);

        final Transaction tx1 = bc.createTransaction(alice, 0, b, BigInteger.ONE, new byte[0]);
        pendingState.addPendingTransaction(tx1);

        assert txHandle.await(3, TimeUnit.SECONDS);
    }

    static class PendingListener extends EthereumListenerAdapter {
        public final BlockingQueue<Pair<Block, List<TransactionReceipt>>> onBlock = new LinkedBlockingQueue<>();
        public final BlockingQueue<Object> onPendingStateChanged = new LinkedBlockingQueue<>();
//        public BlockingQueue<Triple<TransactionReceipt, PendingTransactionState, Block>> onPendingTransactionUpdate = new LinkedBlockingQueue<>();

        final Map<ByteArrayWrapper, BlockingQueue<Triple<TransactionReceipt, PendingTransactionState, Block>>>
                onPendingTransactionUpdate = new HashMap<>();

        @Override
        public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
            System.out.println("PendingStateTest.onBlock:" + "block = [" + block.getShortDescr() + "]");
            onBlock.add(Pair.of(block, receipts));
        }

        @Override
        public void onPendingStateChanged(final PendingState pendingState) {
            System.out.println("PendingStateTest.onPendingStateChanged.");
            onPendingStateChanged.add(new Object());
        }

        @Override
        public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {
            System.out.println("PendingStateTest.onPendingTransactionUpdate:" + "txReceipt.err = [" + txReceipt.getError() + "], state = [" + state + "], block: " + block.getShortDescr());
            getQueueFor(txReceipt.getTransaction()).add(Triple.of(txReceipt, state, block));
        }

        public synchronized BlockingQueue<Triple<TransactionReceipt, PendingTransactionState, Block>> getQueueFor(final Transaction tx) {
            final ByteArrayWrapper hashW = new ByteArrayWrapper(tx.getHash());
            final BlockingQueue<Triple<TransactionReceipt, PendingTransactionState, Block>> queue = onPendingTransactionUpdate.computeIfAbsent(hashW, k -> new LinkedBlockingQueue<>());
            return queue;
        }

        public PendingTransactionState pollTxUpdateState(final Transaction tx) throws InterruptedException {
            return getQueueFor(tx).poll(5, SECONDS).getMiddle();
        }

        public Triple<TransactionReceipt, PendingTransactionState, Block> pollTxUpdate(final Transaction tx) throws InterruptedException {
            return getQueueFor(tx).poll(5, SECONDS);
        }
    }
}
