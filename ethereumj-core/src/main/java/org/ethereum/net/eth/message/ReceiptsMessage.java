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

package org.ethereum.net.eth.message;

import org.ethereum.core.TransactionReceipt;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum Receipts message on the network
 * Tx Receipts grouped by blocks
 *
 * @see EthMessageCodes#RECEIPTS
 */
public class ReceiptsMessage extends EthMessage {

    private List<List<TransactionReceipt>> receipts;

    public ReceiptsMessage(final byte[] encoded) {
        super(encoded);
    }

    public ReceiptsMessage(final List<List<TransactionReceipt>> receiptList) {
        this.receipts = receiptList;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        this.receipts = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            final RLPList blockRLP = (RLPList) aParamsList;

            final List<TransactionReceipt> blockReceipts = new ArrayList<>();
            for (final RLPElement txReceipt : blockRLP) {
                final RLPList receiptRLP = (RLPList) txReceipt;
                if (receiptRLP.size() != 4) {
                    continue;
                }
                final TransactionReceipt receipt = new TransactionReceipt(receiptRLP);
                blockReceipts.add(receipt);
            }
            this.receipts.add(blockReceipts);
        }
        this.parsed = true;
    }

    private void encode() {
        final List<byte[]> blocks = new ArrayList<>();

        for (final List<TransactionReceipt> blockReceipts : receipts) {

            final List<byte[]> encodedBlockReceipts = new ArrayList<>();
            for (final TransactionReceipt txReceipt : blockReceipts) {
                encodedBlockReceipts.add(txReceipt.getEncoded(true));
            }
            final byte[][] encodedElementArray = encodedBlockReceipts.toArray(new byte[encodedBlockReceipts.size()][]);
            final byte[] blockReceiptsEncoded = RLP.encodeList(encodedElementArray);

            blocks.add(blockReceiptsEncoded);
        }

        final byte[][] encodedElementArray = blocks.toArray(new byte[blocks.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }


    public List<List<TransactionReceipt>> getReceipts() {
        parse();
        return receipts;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.RECEIPTS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        parse();
        final StringBuilder sb = new StringBuilder();
        if (receipts.size() < 4) {
            for (final List<TransactionReceipt> blockReceipts : receipts)
                sb.append("\n   ").append(blockReceipts.size()).append(" receipts in block");
        } else {
            for (int i = 0; i < 3; i++) {
                sb.append("\n   ").append(receipts.get(i).size()).append(" receipts in block");
            }
            sb.append("\n   ").append("[Skipped ").append(receipts.size() - 3).append(" blocks]");
        }
        return "[" + getCommand().name() + " num:"
                + receipts.size() + " " + sb.toString() + "]";
    }
}