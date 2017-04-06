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

import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.HomesteadConfig
import org.ethereum.config.net.MainNetConfig
import org.ethereum.jsontestsuite.suite.DifficultyTestSuite
import org.ethereum.jsontestsuite.suite.JSONReader
import org.json.simple.parser.ParseException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * @author Mikhail Kalinin
 * *
 * @since 02.09.2015
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubBasicTest {
    private val shacommit = "92bb72cccf4b5a2d29d74248fdddfe8b43baddda"

    @After
    fun recover() {
        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun runDifficultyTest() {

        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE

        val json = JSONReader.loadJSONFromCommit("BasicTests/difficulty.json", shacommit)

        val testSuite = DifficultyTestSuite(json)

        for (testCase in testSuite.testCases) {

            logger.info("Running {}\n", testCase.name)

            val current = testCase.current
            val parent = testCase.parent

            assertEquals(testCase.expectedDifficulty, current.calcDifficulty(SystemProperties.getDefault()!!.blockchainConfig, parent))
        }
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun runDifficultyFrontierTest() {

        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE

        val json = JSONReader.loadJSONFromCommit("BasicTests/difficultyFrontier.json", shacommit)

        val testSuite = DifficultyTestSuite(json)

        for (testCase in testSuite.testCases) {

            logger.info("Running {}\n", testCase.name)

            val current = testCase.current
            val parent = testCase.parent

            assertEquals(testCase.expectedDifficulty, current.calcDifficulty(
                    SystemProperties.getDefault()!!.blockchainConfig, parent))
        }
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun runDifficultyHomesteadTest() {

        SystemProperties.getDefault()!!.blockchainConfig = HomesteadConfig()

        val json = JSONReader.loadJSONFromCommit("BasicTests/difficultyHomestead.json", shacommit)

        val testSuite = DifficultyTestSuite(json)

        for (testCase in testSuite.testCases) {

            logger.info("Running {}\n", testCase.name)

            val current = testCase.current
            val parent = testCase.parent

            assertEquals(testCase.expectedDifficulty, current.calcDifficulty(
                    SystemProperties.getDefault()!!.blockchainConfig, parent))
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger("TCK-Test")
    }
}
