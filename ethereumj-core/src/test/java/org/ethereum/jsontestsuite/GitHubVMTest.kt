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
import org.ethereum.config.net.MainNetConfig
import org.ethereum.jsontestsuite.suite.JSONReader
import org.ethereum.jsontestsuite.suite.JSONReader.getFileNamesForTreeSha
import org.json.simple.parser.ParseException
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubVMTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private val shacommit = "289b3e4524786618c7ec253b516bc8e76350f947"

    @After
    fun recover() {
        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig()
    }

    @Ignore
    @Test
    @Throws(ParseException::class)
    fun runSingle() {
        val json = JSONReader.loadJSONFromCommit("VMTests/vmEnvironmentalInfoTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, "ExtCodeSizeAddressInputTooBigRightMyAddress")
    }

    @Test
    @Throws(ParseException::class)
    fun testArithmeticFromGitHub() {
        val excluded = HashSet<String>()
        // TODO: these are excluded due to bad wrapping behavior in ADDMOD/DataWord.add
        val json = JSONReader.loadJSONFromCommit("VMTests/vmArithmeticTest.json", shacommit)
        //String json = JSONReader.getTestBlobForTreeSha(shacommit, "vmArithmeticTest.json");
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testBitwiseLogicOperationFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmBitwiseLogicOperationTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testBlockInfoFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmBlockInfoTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Ignore
    @Test // testing full suite
    @Throws(ParseException::class)
    fun testEnvironmentalInfoFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmEnvironmentalInfoTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testIOandFlowOperationsFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmIOandFlowOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Ignore  //FIXME - 60M - need new fast downloader
    @Test
    @Throws(ParseException::class)
    fun testvmInputLimitsTest1FromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmInputLimits1.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Ignore //FIXME - 50M - need to handle large filesizes
    @Test
    @Throws(ParseException::class)
    fun testvmInputLimitsTest2FromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmInputLimits2.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Ignore //FIXME - 20M - possibly provide percentage indicator
    @Test
    @Throws(ParseException::class)
    fun testvmInputLimitsLightTestFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmInputLimitsLight.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testVMLogGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmLogTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testPerformanceFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmPerformanceTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testPushDupSwapFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmPushDupSwapTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testShaFromGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmSha3Test.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testvmSystemOperationsTestGitHub() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("VMTests/vmSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testVMGitHub() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("VMTests/vmtests.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonVMTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class)
    fun testRandomVMGitHub() {

        val shacommit = "c5eafb85390eee59b838a93ae31bc16a5fd4f7b1"
        val fileNames = getFileNamesForTreeSha(shacommit)
        val excludedFiles = listOf("")

        for (fileName in fileNames) {

            if (excludedFiles.contains(fileName)) continue
            println("Running: " + fileName)
            val json = JSONReader.loadJSON("VMTests//RandomTests/" + fileName)
            GitHubJSONTestSuite.runGitHubJsonVMTest(json)
        }

    }
}
