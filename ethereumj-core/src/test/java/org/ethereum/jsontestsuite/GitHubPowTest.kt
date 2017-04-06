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

package org.ethereum.jsontestsuite

import org.ethereum.jsontestsuite.suite.EthashTestSuite
import org.ethereum.jsontestsuite.suite.JSONReader
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * @author Mikhail Kalinin
 * *
 * @since 03.09.2015
 */
class GitHubPowTest {
    private val shacommit = "92bb72cccf4b5a2d29d74248fdddfe8b43baddda"

    @Test
    @Throws(IOException::class)
    fun runEthashTest() {

        val json = JSONReader.loadJSONFromCommit("PoWTests/ethash_tests.json", shacommit)

        val testSuite = EthashTestSuite(json)

        for (testCase in testSuite.testCases) {

            logger.info("Running {}\n", testCase.name)

            val header = testCase.blockHeader

            assertArrayEquals(testCase.resultBytes, header.calcPowValue())
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger("TCK-Test")
    }
}
