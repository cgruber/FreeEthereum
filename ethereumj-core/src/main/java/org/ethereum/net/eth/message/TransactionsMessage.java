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

import org.ethereum.core.Transaction;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum Transactions message on the network
 *
 * @see EthMessageCodes#TRANSACTIONS
 */
public class TransactionsMessage extends EthMessage {

    private List<Transaction> transactions;

    public TransactionsMessage(final byte[] encoded) {
        super(encoded);
    }

    public TransactionsMessage(final Transaction transaction) {

        transactions = new ArrayList<>();
        transactions.add(transaction);
        parsed = true;
    }

    public TransactionsMessage(final List<Transaction> transactionList) {
        this.transactions = transactionList;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        transactions = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            final RLPList rlpTxData = (RLPList) aParamsList;
            final Transaction tx = new Transaction(rlpTxData.getRLPData());
            transactions.add(tx);
        }
        parsed = true;
    }

    private void encode() {
        final List<byte[]> encodedElements = new ArrayList<>();
        for (final Transaction tx : transactions)
            encodedElements.add(tx.getEncoded());
        final byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }


    public List<Transaction> getTransactions() {
        parse();
        return transactions;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.TRANSACTIONS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        parse();
        final StringBuilder sb = new StringBuilder();
        if (transactions.size() < 4) {
            for (final Transaction transaction : transactions)
                sb.append("\n   ").append(transaction.toString(128));
        } else {
            for (int i = 0; i < 3; i++) {
                sb.append("\n   ").append(transactions.get(i).toString(128));
            }
            sb.append("\n   ").append("[Skipped ").append(transactions.size() - 3).append(" transactions]");
        }
        return "[" + getCommand().name() + " num:"
                + transactions.size() + " " + sb.toString() + "]";
    }
}