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
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListenerAdapter;
import org.spongycastle.util.encoders.Hex;

import java.util.Collections;
import java.util.List;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class TransactionBomb extends EthereumListenerAdapter {


    private Ethereum ethereum = null;
    private boolean startedTxBomb = false;

    private TransactionBomb(final Ethereum ethereum) {
        this.ethereum = ethereum;
    }

    public static void main(final String[] args) {

        final Ethereum ethereum = EthereumFactory.createEthereum();
        ethereum.addListener(new TransactionBomb(ethereum));
    }


    @Override
    public void onSyncDone(final SyncState state) {

        // We will send transactions only
        // after we have the full chain syncs
        // - in order to prevent old nonce usage
        startedTxBomb = true;
        System.err.println(" ~~~ SYNC DONE ~~~ ");
    }

    @Override
    public void onBlock(final Block block, final List<TransactionReceipt> receipts) {

        if (startedTxBomb){
            final byte[] sender = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
            long nonce = ethereum.getRepository().getNonce(sender).longValue();

            for (int i=0; i < 20; ++i){
                sendTx(nonce);
                ++nonce;
                sleep(10);
            }
        }
    }

    private void sendTx(final long nonce) {

        final byte[] gasPrice = longToBytesNoLeadZeroes(1_000_000_000_000L);
        final byte[] gasLimit = longToBytesNoLeadZeroes(21000);

        final byte[] toAddress = Hex.decode("9f598824ffa7068c1f2543f04efb58b6993db933");
        final byte[] value = longToBytesNoLeadZeroes(10_000);

        final Transaction tx = new Transaction(longToBytesNoLeadZeroes(nonce),
                gasPrice,
                gasLimit,
                toAddress,
                value,
                null,
                ethereum.getChainIdForNextBlock());

        final byte[] privKey = sha3("cow".getBytes());
        tx.sign(privKey);

        ethereum.getChannelManager().sendTransaction(Collections.singletonList(tx), null);
        System.err.println("Sending tx: " + Hex.toHexString(tx.getHash()));
    }

    private void sleep(final int millis) {
        try {Thread.sleep(millis);} catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
