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

package org.ethereum.jsontestsuite.suite

import org.json.simple.JSONObject

import org.spongycastle.util.BigIntegers
import org.spongycastle.util.encoders.Hex

/**
 * @author Roman Mandeleil
 * *
 * @since 28.06.2014
 */
class Env {

    val currentCoinbase: ByteArray
    val currentDifficulty: ByteArray
    val currentGasLimit: ByteArray
    val currentNumber: ByteArray
    val currentTimestamp: ByteArray
    val previousHash: ByteArray


    constructor(currentCoinbase: ByteArray, currentDifficulty: ByteArray, currentGasLimit: ByteArray, currentNumber: ByteArray, currentTimestamp: ByteArray, previousHash: ByteArray) {
        this.currentCoinbase = currentCoinbase
        this.currentDifficulty = currentDifficulty
        this.currentGasLimit = currentGasLimit
        this.currentNumber = currentNumber
        this.currentTimestamp = currentTimestamp
        this.previousHash = previousHash
    }

    /*
                e.g:
                    "currentCoinbase" : "2adc25665018aa1fe0e6bc666dac8fc2697ff9ba",
                    "currentDifficulty" : "256",
                    "currentGasLimit" : "1000000",
                    "currentNumber" : "0",
                    "currentTimestamp" : 1,
                    "previousHash" : "5e20a0453cecd065ea59c37ac63e079ee08998b6045136a8ce6635c7912ec0b6"
          */
    constructor(env: JSONObject) {

        val coinbase = env["currentCoinbase"].toString()
        val difficulty = env["currentDifficulty"].toString()
        val timestamp = env["currentTimestamp"].toString()
        val number = env["currentNumber"].toString()
        val gasLimit = Utils.parseUnidentifiedBase(env["currentGasLimit"].toString())
        val previousHash = env["previousHash"]
        val prevHash = previousHash?.toString() ?: ""

        this.currentCoinbase = Hex.decode(coinbase)
        this.currentDifficulty = BigIntegers.asUnsignedByteArray(TestCase.toBigInt(difficulty))
        this.currentGasLimit = BigIntegers.asUnsignedByteArray(TestCase.toBigInt(gasLimit))
        this.currentNumber = TestCase.toBigInt(number).toByteArray()
        this.currentTimestamp = TestCase.toBigInt(timestamp).toByteArray()
        this.previousHash = Hex.decode(prevHash)

    }

    override fun toString(): String {
        return "Env{" +
                "currentCoinbase=" + Hex.toHexString(currentCoinbase) +
                ", currentDifficulty=" + Hex.toHexString(currentDifficulty) +
                ", currentGasLimit=" + Hex.toHexString(currentGasLimit) +
                ", currentNumber=" + Hex.toHexString(currentNumber) +
                ", currentTimestamp=" + Hex.toHexString(currentTimestamp) +
                ", previousHash=" + Hex.toHexString(previousHash) +
                '}'
    }
}
