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

import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.config.Constants;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Transaction;

import java.math.BigInteger;

public class HomesteadConfig extends FrontierConfig {

    static final BigInteger SECP256K1N_HALF = Constants.Companion.getSecP256K1N().divide(BigInteger.valueOf(2));

    public HomesteadConfig() {
        this(new HomesteadConstants());
    }

    public HomesteadConfig(final Constants constants) {
        super(constants);
    }

    @Override
    protected BigInteger getCalcDifficultyMultiplier(final BlockHeader curBlock, final BlockHeader parent) {
        return BigInteger.valueOf(Math.max(1 - (curBlock.getTimestamp() - parent.getTimestamp()) / 10, -99));
    }

    @Override
    public long getTransactionCost(final Transaction tx) {
        final long nonZeroes = tx.nonZeroDataBytes();
        final long zeroVals = ArrayUtils.getLength(tx.getData()) - nonZeroes;

        return (tx.isContractCreation() ? getGasCost().getTRANSACTION_CREATE_CONTRACT() : getGasCost().getTRANSACTION())
                + zeroVals * getGasCost().getTX_ZERO_DATA() + nonZeroes * getGasCost().getTX_NO_ZERO_DATA();
    }

    @Override
    public boolean acceptTransactionSignature(final Transaction tx) {
        if (!super.acceptTransactionSignature(tx)) return false;
        return tx.getSignature().s.compareTo(SECP256K1N_HALF) <= 0;
    }

    public static class HomesteadConstants extends FrontierConstants {
        @Override
        public boolean createEmptyContractOnOOG() {
            return false;
        }

        @Override
        public boolean hasDelegateCallOpcode() {
            return true;
        }
    }
}
