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

package org.ethereum.jsontestsuite.suite.builder

import org.ethereum.core.Transaction
import org.ethereum.jsontestsuite.suite.Utils.*
import org.ethereum.jsontestsuite.suite.model.TransactionTck

object TransactionBuilder {

    fun build(transactionTck: TransactionTck): Transaction {

        val transaction: Transaction
        if (transactionTck.secretKey != null) {

            transaction = Transaction(
                    parseVarData(transactionTck.nonce),
                    parseVarData(transactionTck.gasPrice),
                    parseVarData(transactionTck.gasLimit),
                    parseData(transactionTck.to),
                    parseVarData(transactionTck.value),
                    parseData(transactionTck.data))
            transaction.sign(parseData(transactionTck.secretKey))

        } else {

            transaction = Transaction(
                    parseNumericData(transactionTck.nonce),
                    parseNumericData(transactionTck.gasPrice),
                    parseVarData(transactionTck.gasLimit),
                    parseData(transactionTck.to),
                    parseNumericData(transactionTck.value),
                    parseData(transactionTck.data),
                    parseData(transactionTck.r),
                    parseData(transactionTck.s),
                    parseByte(transactionTck.v)
            )
        }

        return transaction
    }
}
