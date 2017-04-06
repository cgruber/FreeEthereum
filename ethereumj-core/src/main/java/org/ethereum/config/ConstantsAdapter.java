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

package org.ethereum.config;

import java.math.BigInteger;

public class ConstantsAdapter extends Constants {
    private final Constants delegate;

    public ConstantsAdapter(final Constants delegate) {
        this.delegate = delegate;
    }

    public static BigInteger getSecP256K1N() {
        return Constants.Companion.getSecP256K1N();
    }

    @Override
    public int getDurationLimit() {
        return delegate.getDurationLimit();
    }

    @Override
    public BigInteger getInitialNonce() {
        return delegate.getInitialNonce();
    }

    @Override
    public int getMaximumExtraDataSize() {
        return delegate.getMaximumExtraDataSize();
    }

    @Override
    public int getMinGasLimit() {
        return delegate.getMinGasLimit();
    }

    @Override
    public int getGasLimitBoundDivisor() {
        return delegate.getGasLimitBoundDivisor();
    }

    @Override
    public BigInteger getMinimumDifficulty() {
        return delegate.getMinimumDifficulty();
    }

    @Override
    public BigInteger getDifficultyBoundDivisor() {
        return delegate.getDifficultyBoundDivisor();
    }

    @Override
    public int getExpDifficultyPeriod() {
        return delegate.getExpDifficultyPeriod();
    }

    @Override
    public int getUncleGenerationLimit() {
        return delegate.getUncleGenerationLimit();
    }

    @Override
    public int getUncleListLimit() {
        return delegate.getUncleListLimit();
    }

    @Override
    public int getBestNumberDiffLimit() {
        return delegate.getBestNumberDiffLimit();
    }

    @Override
    public BigInteger getBlockReward() {
        return delegate.getBlockReward();
    }

    @Override
    public int getMaxContractSize() {
        return delegate.getMaxContractSize();
    }

    @Override
    public boolean createEmptyContractOnOOG() {
        return delegate.createEmptyContractOnOOG();
    }

    @Override
    public boolean hasDelegateCallOpcode() {
        return delegate.hasDelegateCallOpcode();
    }
}
