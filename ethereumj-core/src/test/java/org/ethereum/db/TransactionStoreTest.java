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

package org.ethereum.db;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionInfo;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.ethereum.vm.DataWord;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by Anton Nashatyrev on 08.04.2016.
 */
public class TransactionStoreTest {

    @AfterClass
    public static void cleanup() {
        SystemProperties.resetToDefault();
    }

    @Test
    public void simpleTest() {
        final String contractSrc =
                "contract Adder {" +
                "  function add(int a, int b) returns (int) {return a + b;}" +
                "}";
        final HashMapDB<byte[]> txDb = new HashMapDB<>();

        final StandaloneBlockchain bc = new StandaloneBlockchain();
        bc.getBlockchain().withTransactionStore(new TransactionStore(txDb));
        final SolidityContract contract = bc.submitNewContract(contractSrc);
        bc.createBlock();
        contract.callFunction("add", 555, 222);
        final Block b2 = bc.createBlock();
        contract.callFunction("add", 333, 333);
        final Block b3 = bc.createBlock();
        final Transaction tx1 = b2.getTransactionsList().get(0);
        final TransactionInfo tx1Info = bc.getBlockchain().getTransactionInfo(tx1.getHash());
        byte[] executionResult = tx1Info.getReceipt().getExecutionResult();
        Assert.assertArrayEquals(new DataWord(777).getData(), executionResult);

        System.out.println(txDb.keys().size());
        bc.getBlockchain().flush();
        System.out.println(txDb.keys().size());

        final TransactionStore txStore = new TransactionStore(txDb);
        final TransactionInfo tx1Info_ = txStore.get(tx1.getHash()).get(0);
        executionResult = tx1Info_.getReceipt().getExecutionResult();
        Assert.assertArrayEquals(new DataWord(777).getData(), executionResult);

        final TransactionInfo highIndex = new TransactionInfo(tx1Info.getReceipt(), tx1Info.getBlockHash(), 255);
        final TransactionInfo highIndexCopy = new TransactionInfo(highIndex.getEncoded());
        Assert.assertArrayEquals(highIndex.getBlockHash(), highIndexCopy.getBlockHash());
        Assert.assertEquals(highIndex.getIndex(), highIndexCopy.getIndex());
    }

    @Test
    public void forkTest() {
        // check that TransactionInfo is always returned from the main chain for
        // transaction which included into blocks from different forks

        final String contractSrc =
                "contract Adder {" +
                "  int public lastResult;" +
                "  function add(int a, int b) returns (int) {lastResult = a + b; return lastResult; }" +
                "}";
        final HashMapDB txDb = new HashMapDB();

        final StandaloneBlockchain bc = new StandaloneBlockchain();
        final TransactionStore transactionStore = new TransactionStore(txDb);
        bc.getBlockchain().withTransactionStore(transactionStore);
        final SolidityContract contract = bc.submitNewContract(contractSrc);
        final Block b1 = bc.createBlock();
        contract.callFunction("add", 555, 222);
        final Block b2 = bc.createBlock();
        final Transaction tx1 = b2.getTransactionsList().get(0);
        TransactionInfo txInfo = bc.getBlockchain().getTransactionInfo(tx1.getHash());
        Assert.assertTrue(Arrays.equals(txInfo.getBlockHash(), b2.getHash()));

        final Block b2_ = bc.createForkBlock(b1);
        contract.callFunction("add", 555, 222); // tx with the same hash as before
        final Block b3_ = bc.createForkBlock(b2_);
        final TransactionInfo txInfo_ = bc.getBlockchain().getTransactionInfo(tx1.getHash());
        Assert.assertTrue(Arrays.equals(txInfo_.getBlockHash(), b3_.getHash()));

        final Block b3 = bc.createForkBlock(b2);
        final Block b4 = bc.createForkBlock(b3);
        txInfo = bc.getBlockchain().getTransactionInfo(tx1.getHash());
        Assert.assertTrue(Arrays.equals(txInfo.getBlockHash(), b2.getHash()));
    }

    @Test
    public void backwardCompatibleDbTest() {
        // check that we can read previously saved entries (saved with legacy code)

        final HashMapDB txDb = new HashMapDB();
        final TransactionStore transactionStore = new TransactionStore(txDb);
        final StandaloneBlockchain bc = new StandaloneBlockchain();
        bc.getBlockchain().withTransactionStore(transactionStore);

        bc.sendEther(new byte[20], BigInteger.valueOf(1000));
        final Block b1 = bc.createBlock();
        final Transaction tx = b1.getTransactionsList().get(0);
        final TransactionInfo info = transactionStore.get(tx.getHash()).get(0);

        final HashMapDB<byte[]> txDb1 = new HashMapDB<>();
        txDb1.put(tx.getHash(), info.getEncoded()); // legacy serialization
        final TransactionStore transactionStore1 = new TransactionStore(txDb1);
        final TransactionInfo info1 = transactionStore1.get(tx.getHash()).get(0);
        Assert.assertArrayEquals(info1.getReceipt().getPostTxState(), info.getReceipt().getPostTxState());
    }
}
