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

import org.ethereum.jsontestsuite.suite.Env
import org.ethereum.jsontestsuite.suite.Utils.*
import org.ethereum.jsontestsuite.suite.model.EnvTck

object EnvBuilder {

    fun build(envTck: EnvTck): Env {
        val coinbase = parseData(envTck.currentCoinbase)
        val difficulty = parseVarData(envTck.currentDifficulty)
        val gasLimit = parseVarData(envTck.currentGasLimit)
        val number = parseNumericData(envTck.currentNumber)
        val timestamp = parseNumericData(envTck.currentTimestamp)
        val hash = parseData(envTck.previousHash)

        return Env(coinbase, difficulty, gasLimit, number, timestamp, hash)
    }

}
