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

package org.ethereum.samples;

import org.ethereum.core.Block;
import org.ethereum.core.PendingState;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.ByteUtil;
import org.jetbrains.annotations.NotNull;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.math.BigInteger;
import java.util.*;

/**
 * PendingState is the ability to track the changes made by transaction immediately and not wait for
 * the block containing that transaction.
 *
 * This sample connects to the test network (it has a lot of free Ethers) and starts periodically
 * transferring funds to a random address. The pending state is monitored and you may see that
 * while the actual receiver balance remains the same right after transaction sent the pending
 * state reflects balance change immediately.
 *
 * While new blocks are arrived the sample monitors which of our transactions are included ot those blocks.
 * After each 5 transactions the sample stops sending transactions and waits for all transactions
 * are cleared (included to blocks) so the actual and pending receiver balances become equal.
 *
 * Created by Anton Nashatyrev on 05.02.2016.
 */
public class PendingStateSample extends TestNetSample {

    // remember here what transactions have been sent
    // removing transaction from here on block arrival
    private final Map<ByteArrayWrapper, Transaction> pendingTxs = Collections.synchronizedMap(
            new HashMap<ByteArrayWrapper, Transaction>());
    // some random receiver
    private final byte[] receiverAddress = new ECKey().getAddress();
    @Autowired
    private
    PendingState pendingState;

    public static void main(final String[] args) throws Exception {
        sLogger.info("Starting EthereumJ!");

        class Config extends TestNetConfig {
            @Override
            @Bean
            public TestNetSample sampleBean() {
                return new PendingStateSample();
            }
        }

        // Based on Config class the BasicSample would be created by Spring
        // and its springInit() method would be called as an entry point
        EthereumFactory.createEthereum(Config.class);
    }

    @Override
    public void onSyncDone() {
        ethereum.addListener(new EthereumListenerAdapter() {
            // listening here when the PendingState is updated with new transactions
            @Override
//            public void onPendingTransactionsReceived(final List<Transaction> transactions) {
            public void onPendingTransactionsReceived(@NotNull List<? extends Transaction> transactions) {
                for (final Transaction tx : transactions) {
                    PendingStateSample.this.onPendingTransactionReceived(tx);
                }
            }

            // when block arrives look for our included transactions
            @Override
            public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
                PendingStateSample.this.onBlock(block, receipts);
            }
        });

        new Thread("PendingStateSampleThread") {
            @Override
            public void run() {
                try {
                    sendTransactions();
                } catch (final Exception e) {
                    logger.error("Error while sending transactions", e);
                }
            }
        }.start();
    }

    /**
     * Periodically send value transfer transactions and each 5 transactions
     * wait for all sent transactions to be included into blocks
     */
    private void sendTransactions() throws InterruptedException {
        // initial sender nonce needs to be retrieved from the repository
        // for further transactions we just do nonce++
        BigInteger nonce = ethereum.getRepository().getNonce(senderAddress);

        final int weisToSend = 100;
        int count = 0;
        while(true) {
            if (count < 5) {
                final Transaction tx = new Transaction(
                        ByteUtil.bigIntegerToBytes(nonce),
                        ByteUtil.longToBytesNoLeadZeroes(ethereum.getGasPrice()),
                        ByteUtil.longToBytesNoLeadZeroes(1_000_000),
                        receiverAddress,
                        ByteUtil.longToBytesNoLeadZeroes(weisToSend), new byte[0],
                        ethereum.getChainIdForNextBlock());
                tx.sign(ECKey.fromPrivate(receiverAddress));
                logger.info("<=== Sending transaction: " + tx);
                ethereum.submitTransaction(tx);

                nonce = nonce.add(BigInteger.ONE);
                count++;
            } else {
                if (pendingTxs.size() > 0) {
                    logger.info("Waiting for transaction clearing. " + pendingTxs.size() + " transactions remain.");
                } else {
                    logger.info("All transactions are included to blocks!");
                    count = 0;
                }
            }

            Thread.sleep(7000);
        }
    }

    /**
     *  The PendingState is updated with a new pending transactions.
     *  Prints the current receiver balance (based on blocks) and the pending balance
     *  which should immediately reflect receiver balance change
     */
    private void onPendingTransactionReceived(final Transaction tx) {
        logger.info("onPendingTransactionReceived: " + tx);
        if (Arrays.equals(tx.getSender(), senderAddress)) {
            final BigInteger receiverBalance = ethereum.getRepository().getBalance(receiverAddress);
            final BigInteger receiverBalancePending = pendingState.getRepository().getBalance(receiverAddress);
            logger.info(" + New pending transaction 0x" + Hex.toHexString(tx.getHash()).substring(0, 8));

            pendingTxs.put(new ByteArrayWrapper(tx.getHash()), tx);

            logger.info("Receiver pending/current balance: " + receiverBalancePending + " / " + receiverBalance +
                    " (" + pendingTxs.size() + " pending txs)");
        }
    }

    /**
     * For each block we are looking for our transactions and clearing them
     * The actual receiver balance is confirmed upon block arrival
     */
    private void onBlock(final Block block, final List<TransactionReceipt> receipts) {
        int cleared = 0;
        for (final Transaction tx : block.getTransactionsList()) {
            final ByteArrayWrapper txHash = new ByteArrayWrapper(tx.getHash());
            final Transaction ptx = pendingTxs.get(txHash);
            if (ptx != null) {
                logger.info(" - Pending transaction cleared 0x" + Hex.toHexString(tx.getHash()).substring(0, 8) +
                        " in block " + block.getShortDescr());

                pendingTxs.remove(txHash);
                cleared++;
            }
        }
        final BigInteger receiverBalance = ethereum.getRepository().getBalance(receiverAddress);
        final BigInteger receiverBalancePending = pendingState.getRepository().getBalance(receiverAddress);
        logger.info("" + cleared + " transactions cleared in the block " + block.getShortDescr());
        logger.info("Receiver pending/current balance: " + receiverBalancePending + " / " + receiverBalance +
                " (" + pendingTxs.size() + " pending txs)");
    }
}
