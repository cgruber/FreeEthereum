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

package org.ethereum.jsontestsuite.suite.builder;

import org.ethereum.core.Transaction;
import org.ethereum.jsontestsuite.suite.model.TransactionTck;

import static org.ethereum.jsontestsuite.suite.Utils.*;

public class TransactionBuilder {

    public static Transaction build(final TransactionTck transactionTck) {

        final Transaction transaction;
        if (transactionTck.getSecretKey() != null){

            transaction = new Transaction(
                    parseVarData(transactionTck.getNonce()),
                    parseVarData(transactionTck.getGasPrice()),
                    parseVarData(transactionTck.getGasLimit()),
                    parseData(transactionTck.getTo()),
                    parseVarData(transactionTck.getValue()),
                    parseData(transactionTck.getData()));
            transaction.sign(parseData(transactionTck.getSecretKey()));

        } else {

            transaction = new Transaction(
                    parseNumericData(transactionTck.getNonce()),
                    parseNumericData(transactionTck.getGasPrice()),
                    parseVarData(transactionTck.getGasLimit()),
                    parseData(transactionTck.getTo()),
                    parseNumericData(transactionTck.getValue()),
                    parseData(transactionTck.getData()),
                    parseData(transactionTck.getR()),
                    parseData(transactionTck.getS()),
                    parseByte(transactionTck.getV())
            );
        }

        return transaction;
    }
}
