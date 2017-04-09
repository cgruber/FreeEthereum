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

package org.ethereum.jsontestsuite.suite.validators;

import org.ethereum.core.Transaction;

import java.util.ArrayList;

import static org.ethereum.util.ByteUtil.toHexString;

public class TransactionValidator {


    public static ArrayList<String> valid(final Transaction orig, final Transaction valid) {

        final ArrayList<String> outputSummary = new ArrayList<>();

        if (orig == null && valid == null) {
            return outputSummary;
        }

        if (orig != null && valid == null) {

            final String output = "Transaction expected to be not valid";

            outputSummary.add(output);
            return outputSummary;
        }

        if (orig == null && valid != null) {

            final String output = "Transaction expected to be valid";

            outputSummary.add(output);
            return outputSummary;
        }

        if (!toHexString(orig.getEncoded())
                .equals(toHexString(valid.getEncoded()))) {

            final String output =
                    String.format("Wrong transaction.encoded: \n expected: %s \n got: %s",
                            toHexString(valid.getEncoded()),
                            toHexString(orig.getEncoded())
                    );

            outputSummary.add(output);
        }

        return outputSummary;
    }
}
