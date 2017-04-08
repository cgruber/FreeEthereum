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

package org.ethereum.config

import java.math.BigInteger

open class ConstantsAdapter(private val delegate: Constants) : Constants() {

    override val durationLimit: Int
        get() = delegate.durationLimit

    override val initialNonce: BigInteger
        get() = delegate.initialNonce

    override val maximumExtraDataSize: Int
        get() = delegate.maximumExtraDataSize

    override val minGasLimit: Int
        get() = delegate.minGasLimit

    override val gasLimitBoundDivisor: Int
        get() = delegate.gasLimitBoundDivisor

    override val minimumDifficulty: BigInteger
        get() = delegate.minimumDifficulty

    override val difficultyBoundDivisor: BigInteger
        get() = delegate.difficultyBoundDivisor

    override val expDifficultyPeriod: Int
        get() = delegate.expDifficultyPeriod

    override val uncleGenerationLimit: Int
        get() = delegate.uncleGenerationLimit

    override val uncleListLimit: Int
        get() = delegate.uncleListLimit

    override val bestNumberDiffLimit: Int
        get() = delegate.bestNumberDiffLimit

    override val blockReward: BigInteger
        get() = delegate.blockReward

    override val maxContractSize: Int
        get() = delegate.maxContractSize

    override fun createEmptyContractOnOOG(): Boolean {
        return delegate.createEmptyContractOnOOG()
    }

    override fun hasDelegateCallOpcode(): Boolean {
        return delegate.hasDelegateCallOpcode()
    }

    companion object {

        val secP256K1N: BigInteger
            get() = Constants.secP256K1N
    }
}
