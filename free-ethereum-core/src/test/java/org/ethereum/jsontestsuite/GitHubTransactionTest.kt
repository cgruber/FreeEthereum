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
import org.ethereum.config.blockchain.*
import org.ethereum.config.net.BaseNetConfig
import org.ethereum.config.net.MainNetConfig
import org.ethereum.jsontestsuite.suite.JSONReader
import org.ethereum.jsontestsuite.suite.TransactionTestSuite
import org.ethereum.jsontestsuite.suite.runners.TransactionTestRunner
import org.json.simple.parser.ParseException
import org.junit.*
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubTransactionTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private val shacommit = "289b3e4524786618c7ec253b516bc8e76350f947"

    @Before
    fun setup() {
        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig()
    }

    @After
    fun recover() {
        SystemProperties.resetToDefault()
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testEIP155TransactionTestFromGitHub() {
        val excluded = HashSet<String>()
        SystemProperties.getDefault()!!.blockchainConfig = object : BaseNetConfig() {
            init {
                add(0, FrontierConfig())
                add(1150000, HomesteadConfig())
                add(2457000, Eip150HFConfig(DaoHFConfig()))
                add(2675000, object : Eip160HFConfig(DaoHFConfig()) {
                    override val chainId: Int?
                        get() = null
                })
            }
        }
        val json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json, excluded)
    }

    @Ignore
    @Test
    @Throws(Exception::class)
    fun runsingleTest() {
        val json = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTest.json", shacommit)
        val testSuite = TransactionTestSuite(json)
        val res = TransactionTestRunner.run(testSuite.testCases["V_overflow64bitPlus28"]!!)
        println(res)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testHomesteadTestsFromGitHub() {
        val excluded = HashSet<String>()
        val json1 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/tt10mbDataField.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json1, excluded)

        val json2 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json2, excluded)

        val json3 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTestEip155VitaliksTests.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json3, excluded)

        val json4 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttWrongRLPTransaction.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json4, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testRandomTestFromGitHub() {
        val excluded = HashSet<String>()
        // pre-EIP155 wrong chain id (negative)
        val json = JSONReader.loadJSONFromCommit("TransactionTests/RandomTests/tr201506052141PYTHON.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testGeneralTestsFromGitHub() {
        val excluded = HashSet<String>()
        val json1 = JSONReader.loadJSONFromCommit("TransactionTests/tt10mbDataField.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json1, excluded)

        val json2 = JSONReader.loadJSONFromCommit("TransactionTests/ttTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json2, excluded)
    }

    @Ignore // Few tests fails, RLPWrongByteEncoding and RLPLength preceding 0s errors left
    @Test
    @Throws(ParseException::class, IOException::class)
    fun testWrongRLPTestsFromGitHub() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("TransactionTests/ttWrongRLPTransaction.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testEip155VitaliksTestFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTestEip155VitaliksTests.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun testEip155VRuleTestFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTestVRule.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonTransactionTest(json, excluded)
    }
}
