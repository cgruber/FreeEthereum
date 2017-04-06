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

package org.ethereum.jsontestsuite.suite.runners

import org.ethereum.config.BlockchainConfig
import org.ethereum.config.SystemProperties
import org.ethereum.core.Transaction
import org.ethereum.jsontestsuite.suite.TransactionTestCase
import org.ethereum.jsontestsuite.suite.Utils
import org.ethereum.jsontestsuite.suite.builder.TransactionBuilder
import org.ethereum.jsontestsuite.suite.validators.TransactionValidator
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

class TransactionTestRunner private constructor(private val transactionTestCase: TransactionTestCase) {
    private var transaction: Transaction? = null
    private var expectedTransaction: Transaction? = null
    private var blockNumber: Long = 0
    private var blockchainConfig: BlockchainConfig? = null

    private fun runImpl(): List<String> {

        blockNumber = if (transactionTestCase.blocknumber == null) 0 else Utils.parseLong(transactionTestCase.blocknumber)
        logger.info("Block number: {}", blockNumber)
        this.blockchainConfig = SystemProperties.getDefault()!!.blockchainConfig.getConfigForBlock(blockNumber)

        try {
            val rlp = Utils.parseData(transactionTestCase.rlp)
            transaction = Transaction(rlp)
            transaction!!.verify()
        } catch (e: Exception) {
            transaction = null
        }

        if (transaction == null || transaction!!.encoded.size < 10000) {
            logger.info("Transaction: {}", transaction)
        } else {
            logger.info("Transaction data skipped because it's too big", transaction)
        }

        expectedTransaction = if (transactionTestCase.transaction == null) null else TransactionBuilder.build(transactionTestCase.transaction)
        if (expectedTransaction == null || expectedTransaction!!.encoded.size < 10000) {
            logger.info("Expected transaction: {}", expectedTransaction)
        } else {
            logger.info("Expected transaction data skipped because it's too big", transaction)
        }

        // Not enough GAS
        if (transaction != null) {
            val basicTxCost = blockchainConfig!!.getTransactionCost(transaction!!)
            if (BigInteger(1, transaction!!.gasLimit).compareTo(BigInteger.valueOf(basicTxCost)) < 0) {
                transaction = null
            }
        }

        // Transaction signature verification
        var acceptFail: String? = null
        val shouldAccept = transaction != null && blockchainConfig!!.acceptTransactionSignature(transaction!!)
        if (!shouldAccept) transaction = null
        if (shouldAccept != (expectedTransaction != null)) {
            acceptFail = "Transaction shouldn't be accepted"
        }

        var wrongSender: String? = null
        var wrongHash: String? = null
        if (transaction != null && expectedTransaction != null) {
            // Verifying sender
            if (Hex.toHexString(transaction!!.sender) != transactionTestCase.sender)
                wrongSender = "Sender is incorrect in parsed transaction"
            // Verifying hash
            // NOTE: "hash" is not required field in test case
            if (transactionTestCase.hash != null && Hex.toHexString(transaction!!.hash) != transactionTestCase.hash)
                wrongHash = "Hash is incorrect in parsed transaction"
        }

        logger.info("--------- POST Validation---------")
        val results = ArrayList<String>()

        val outputSummary = TransactionValidator.valid(transaction, expectedTransaction)

        results.addAll(outputSummary)
        if (acceptFail != null) results.add(acceptFail)
        if (wrongSender != null) results.add(wrongSender)
        if (wrongHash != null) results.add(wrongHash)

        for (result in results) {
            logger.error(result)
        }

        logger.info("\n\n")
        return results
    }

    companion object {

        private val logger = LoggerFactory.getLogger("TCK-Test")

        fun run(transactionTestCase2: TransactionTestCase): List<String> {
            return TransactionTestRunner(transactionTestCase2).runImpl()
        }
    }
}
