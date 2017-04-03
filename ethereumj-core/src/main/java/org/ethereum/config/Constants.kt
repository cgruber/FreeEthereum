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

/**
 * Describes different constants specific for a blockchain
 */
open class Constants {

    open val durationLimit: Int
        get() = 8

    open val initialNonce: BigInteger
        get() = BigInteger.ZERO

    open val maximumExtraDataSize: Int
        get() = MAXIMUM_EXTRA_DATA_SIZE

    open val minGasLimit: Int
        get() = MIN_GAS_LIMIT

    open val gasLimitBoundDivisor: Int
        get() = GAS_LIMIT_BOUND_DIVISOR

    open val minimumDifficulty: BigInteger
        get() = MINIMUM_DIFFICULTY

    open val difficultyBoundDivisor: BigInteger
        get() = DIFFICULTY_BOUND_DIVISOR

    open val expDifficultyPeriod: Int
        get() = EXP_DIFFICULTY_PERIOD

    open val uncleGenerationLimit: Int
        get() = UNCLE_GENERATION_LIMIT

    open val uncleListLimit: Int
        get() = UNCLE_LIST_LIMIT

    open val bestNumberDiffLimit: Int
        get() = BEST_NUMBER_DIFF_LIMIT

    open val blockReward: BigInteger
        get() = BLOCK_REWARD

    open val maxContractSize: Int
        get() = Integer.MAX_VALUE

    /**
     * Introduced in the Homestead release
     */
    open fun createEmptyContractOnOOG(): Boolean {
        return true
    }

    /**
     * New DELEGATECALL opcode introduced in the Homestead release. Before Homestead this opcode should generate
     * exception
     */
    open fun hasDelegateCallOpcode(): Boolean {
        return false
    }

    companion object {
        private val MAXIMUM_EXTRA_DATA_SIZE = 32
        private val MIN_GAS_LIMIT = 125000
        private val GAS_LIMIT_BOUND_DIVISOR = 1024
        private val MINIMUM_DIFFICULTY = BigInteger.valueOf(131072)
        private val DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(2048)
        private val EXP_DIFFICULTY_PERIOD = 100000

        private val UNCLE_GENERATION_LIMIT = 7
        private val UNCLE_LIST_LIMIT = 2

        private val BEST_NUMBER_DIFF_LIMIT = 100

        private val BLOCK_REWARD = BigInteger("1500000000000000000")

        /**
         * Introduced in the Homestead release
         */
        val secP256K1N = BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16)
    }
}
