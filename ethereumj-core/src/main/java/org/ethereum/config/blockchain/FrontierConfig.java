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

package org.ethereum.config.blockchain;

import org.ethereum.config.Constants;
import org.ethereum.core.Transaction;

import java.math.BigInteger;

public class FrontierConfig extends OlympicConfig {

    public FrontierConfig() {
        this(new FrontierConstants());
    }

    public FrontierConfig(final Constants constants) {
        super(constants);
    }

    @Override
    public boolean acceptTransactionSignature(final Transaction tx) {
        if (!super.acceptTransactionSignature(tx)) return false;
        return tx.getSignature().validateComponents();
    }

    public static class FrontierConstants extends Constants {
        private static final BigInteger BLOCK_REWARD = new BigInteger("5000000000000000000");

        @Override
        public int getDurationLimit() {
            return 13;
        }

        @Override
        public BigInteger getBlockReward() {
            return BLOCK_REWARD;
        }

        @Override
        public int getMinGasLimit() {
            return 5000;
        }
    }

}
