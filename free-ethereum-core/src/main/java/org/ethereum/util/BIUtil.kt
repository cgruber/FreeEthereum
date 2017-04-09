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

package org.ethereum.util

import org.ethereum.core.Repository

import java.math.BigInteger

object BIUtil {


    /**
     * @param value - not null
     * *
     * @return true - if the param is zero
     */
    fun isZero(value: BigInteger): Boolean {
        return value.compareTo(BigInteger.ZERO) == 0
    }

    /**
     * @param valueA - not null
     * *
     * @param valueB - not null
     * *
     * @return true - if the valueA is equal to valueB is zero
     */
    fun isEqual(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA.compareTo(valueB) == 0
    }

    /**
     * @param valueA - not null
     * *
     * @param valueB - not null
     * *
     * @return true - if the valueA is not equal to valueB is zero
     */
    fun isNotEqual(valueA: BigInteger, valueB: BigInteger): Boolean {
        return !isEqual(valueA, valueB)
    }

    /**
     * @param valueA - not null
     * *
     * @param valueB - not null
     * *
     * @return true - if the valueA is less than valueB is zero
     */
    fun isLessThan(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA < valueB
    }

    /**
     * @param valueA - not null
     * *
     * @param valueB - not null
     * *
     * @return true - if the valueA is more than valueB is zero
     */
    fun isMoreThan(valueA: BigInteger, valueB: BigInteger): Boolean {
        return valueA > valueB
    }


    /**
     * @param valueA - not null
     * *
     * @param valueB - not null
     * *
     * @return sum - valueA + valueB
     */
    fun sum(valueA: BigInteger, valueB: BigInteger): BigInteger {
        return valueA.add(valueB)
    }


    /**
     * @param data = not null
     * *
     * @return new positive BigInteger
     */
    fun toBI(data: ByteArray): BigInteger {
        return BigInteger(1, data)
    }

    /**
     * @param data = not null
     * *
     * @return new positive BigInteger
     */
    fun toBI(data: Long): BigInteger {
        return BigInteger.valueOf(data)
    }


    fun isPositive(value: BigInteger): Boolean {
        return value.signum() > 0
    }

    fun isCovers(covers: BigInteger, value: BigInteger): Boolean {
        return !isNotCovers(covers, value)
    }

    fun isNotCovers(covers: BigInteger, value: BigInteger): Boolean {
        return covers < value
    }


    fun transfer(repository: Repository, fromAddr: ByteArray, toAddr: ByteArray, value: BigInteger) {
        repository.addBalance(fromAddr, value.negate())
        repository.addBalance(toAddr, value)
    }

    fun exitLong(value: BigInteger): Boolean {

        return value.compareTo(BigInteger(java.lang.Long.MAX_VALUE.toString() + "")) > -1
    }

    fun isIn20PercentRange(first: BigInteger, second: BigInteger): Boolean {
        val five = BigInteger.valueOf(5)
        val limit = first.add(first.divide(five))
        return !isMoreThan(second, limit)
    }

    fun max(first: BigInteger, second: BigInteger): BigInteger {
        return if (first < second) second else first
    }
}
