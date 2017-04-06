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

package org.ethereum.jsontestsuite.suite.runners;

import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Transaction;
import org.ethereum.jsontestsuite.suite.TransactionTestCase;
import org.ethereum.jsontestsuite.suite.Utils;
import org.ethereum.jsontestsuite.suite.builder.TransactionBuilder;
import org.ethereum.jsontestsuite.suite.validators.TransactionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TransactionTestRunner {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");
    private final TransactionTestCase transactionTestCase;
    private Transaction transaction = null;
    private Transaction expectedTransaction;
    private long blockNumber;
    private BlockchainConfig blockchainConfig;

    private TransactionTestRunner(final TransactionTestCase transactionTestCase) {
        this.transactionTestCase = transactionTestCase;
    }

    public static List<String> run(final TransactionTestCase transactionTestCase2) {
        return new TransactionTestRunner(transactionTestCase2).runImpl();
    }

    private List<String> runImpl() {

        blockNumber = transactionTestCase.getBlocknumber() == null ? 0 : Utils.parseLong(transactionTestCase.getBlocknumber());
        logger.info("Block number: {}", blockNumber);
        this.blockchainConfig = SystemProperties.getDefault().getBlockchainConfig().getConfigForBlock(blockNumber);

        try {
            final byte[] rlp = Utils.parseData(transactionTestCase.getRlp());
            transaction = new Transaction(rlp);
            transaction.verify();
        } catch (final Exception e) {
            transaction = null;
        }
        if (transaction == null || transaction.getEncoded().length < 10000) {
            logger.info("Transaction: {}", transaction);
        } else {
            logger.info("Transaction data skipped because it's too big", transaction);
        }

        expectedTransaction = transactionTestCase.getTransaction() == null ? null : TransactionBuilder.build(transactionTestCase.getTransaction());
        if (expectedTransaction == null || expectedTransaction.getEncoded().length < 10000) {
            logger.info("Expected transaction: {}", expectedTransaction);
        } else {
            logger.info("Expected transaction data skipped because it's too big", transaction);
        }

        // Not enough GAS
        if (transaction != null) {
            final long basicTxCost = blockchainConfig.getTransactionCost(transaction);
            if (new BigInteger(1, transaction.getGasLimit()).compareTo(BigInteger.valueOf(basicTxCost)) < 0) {
                transaction = null;
            }
        }

        // Transaction signature verification
        String acceptFail = null;
        final boolean shouldAccept = transaction != null && blockchainConfig.acceptTransactionSignature(transaction);
        if (!shouldAccept) transaction = null;
        if (shouldAccept != (expectedTransaction != null)) {
            acceptFail = "Transaction shouldn't be accepted";
        }

        String wrongSender = null;
        String wrongHash = null;
        if (transaction != null && expectedTransaction != null) {
            // Verifying sender
            if (!Hex.toHexString(transaction.getSender()).equals(transactionTestCase.getSender()))
                wrongSender = "Sender is incorrect in parsed transaction";
            // Verifying hash
            // NOTE: "hash" is not required field in test case
            if (transactionTestCase.getHash() != null &&
                    !Hex.toHexString(transaction.getHash()).equals(transactionTestCase.getHash()))
                wrongHash = "Hash is incorrect in parsed transaction";
        }

        logger.info("--------- POST Validation---------");
        final List<String> results = new ArrayList<>();

        final ArrayList<String> outputSummary =
                TransactionValidator.valid(transaction, expectedTransaction);

        results.addAll(outputSummary);
        if (acceptFail != null) results.add(acceptFail);
        if (wrongSender != null) results.add(wrongSender);
        if (wrongHash != null) results.add(wrongHash);

        for (final String result : results) {
            logger.error(result);
        }

        logger.info("\n\n");
        return results;
    }
}
