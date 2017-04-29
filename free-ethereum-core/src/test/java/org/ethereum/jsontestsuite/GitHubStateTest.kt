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
import org.ethereum.jsontestsuite.suite.JSONReader.getFileNamesForTreeSha
import org.json.simple.parser.ParseException
import org.junit.*
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubStateTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private val shacommit = "289b3e4524786618c7ec253b516bc8e76350f947"


    private val oldForkValue: Long = 0

    @Before
    fun setup() {
        // TODO remove this after Homestead launch and shacommit update with actual block number
        // for this JSON test commit the Homestead block was defined as 900000
        SystemProperties.getDefault()!!.blockchainConfig = object : BaseNetConfig() {
            init {
                add(0, FrontierConfig())
                add(1150000, HomesteadConfig())
                add(2457000, Eip150HFConfig(DaoHFConfig()))
                add(2700000, Eip160HFConfig(DaoHFConfig()))

            }
        }
    }

    @After
    fun clean() {
        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
    }

    @Ignore
    @Test // this method is mostly for hands-on convenient testing
    @Throws(ParseException::class, IOException::class)
    fun stSingleTest() {
        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, "CreateHashCollision")
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stExample() {

        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("StateTests/stExample.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCallCodes() {

        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/stCallCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stCallCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stCallCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stCallCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCallDelegateCodes() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stCallDelegateCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stCallDelegateCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stCallDelegateCodes.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCallDelegateCodesCallCode() {

        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stCallDelegateCodesCallCode.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stCallDelegateCodesCallCode.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stCallDelegateCodesCallCode.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stHomeSteadSpecific() {

        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stHomeSteadSpecific.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stHomeSteadSpecific.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stHomeSteadSpecific.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCallCreateCallCodeTest() {

        val excluded = HashSet<String>()
        excluded.add("CallRecursiveBombPreCall") // Max Gas value is pending to be < 2^63

        // the test creates a contract with the same address as existing contract (which is not possible in
        // live). In this case we need to clear the storage in TransactionExecutor.create
        // return back to this case when the contract deleting will be implemented
        excluded.add("createJS_ExampleContract")

        var json = JSONReader.loadJSONFromCommit("StateTests/stCallCreateCallCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stCallCreateCallCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stCallCreateCallCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stCallCreateCallCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stDelegatecallTest() {

        val excluded = HashSet<String>()
        //        String json = JSONReader.loadJSONFromCommit("StateTests/stDelegatecallTest.json", shacommit);
        //        GitHubJSONTestSuite.runStateTest(json, excluded);

        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stDelegatecallTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stDelegatecallTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stDelegatecallTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stInitCodeTest() {
        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/stInitCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stInitCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stInitCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stInitCodeTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stLogTests() {
        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/stLogTests.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stLogTests.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stLogTests.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stLogTests.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stPreCompiledContracts() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stPreCompiledContracts.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stPreCompiledContracts.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stPreCompiledContracts.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stPreCompiledContracts.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Ignore
    @Throws(ParseException::class, IOException::class)
    fun stMemoryStressTest() {
        val excluded = HashSet<String>()
        excluded.add("mload32bitBound_return2")// The test extends memory to 4Gb which can't be handled with Java arrays
        excluded.add("mload32bitBound_return") // The test extends memory to 4Gb which can't be handled with Java arrays
        excluded.add("mload32bitBound_Msize") // The test extends memory to 4Gb which can't be handled with Java arrays
        var json = JSONReader.loadJSONFromCommit("StateTests/stMemoryStressTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stMemoryStressTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stMemoryTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stMemoryTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stMemoryTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stMemoryTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stMemoryTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stQuadraticComplexityTest() {
        val excluded = HashSet<String>()

        // leaving only Homestead version since the test runs too long
        //        String json = JSONReader.loadJSONFromCommit("StateTests/stQuadraticComplexityTest.json", shacommit);
        //        GitHubJSONTestSuite.runStateTest(json, excluded);

        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stQuadraticComplexityTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stQuadraticComplexityTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stQuadraticComplexityTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stSolidityTest() {
        val excluded = HashSet<String>()
        val json = JSONReader.loadJSONFromCommit("StateTests/stSolidityTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stRecursiveCreate() {
        val excluded = HashSet<String>()
        var json = JSONReader.loadJSONFromCommit("StateTests/stRecursiveCreate.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stRecursiveCreate.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stRecursiveCreate.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stRecursiveCreate.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stRefundTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stRefundTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stRefundTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stRefundTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stRefundTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stSpecialTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stSpecialTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stSpecialTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stSpecialTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stSpecialTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stBlockHashTest() {
        val json = JSONReader.loadJSONFromCommit("StateTests/stBlockHashTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json)
    }

    @Test
    @Throws(IOException::class)
    fun stSystemOperationsTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stSystemOperationsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stTransactionTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stTransactionTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stTransitionTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/stTransitionTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stWalletTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/stWalletTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stWalletTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stWalletTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stWalletTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stBoundsTest() {
        val excluded = HashSet<String>()

        var json = JSONReader.loadJSONFromCommit("StateTests/Homestead/stBoundsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP150/Homestead/stBoundsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)

        json = JSONReader.loadJSONFromCommit("StateTests/EIP158/Homestead/stBoundsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stEIPSpecificTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP150/stEIPSpecificTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stChangedTests() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP150/stChangedTests.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stEIPsingleCodeGasPrices() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP150/stEIPsingleCodeGasPrices.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stMemExpandingEIPCalls() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP150/stMemExpandingEIPCalls.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCreateTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/stCreateTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stEIP158SpecificTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/stEIP158SpecificTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stNonZeroCallsTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/stNonZeroCallsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stZeroCallsTest() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/stZeroCallsTest.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun stCodeSizeLimit() {
        val excluded = HashSet<String>()

        val json = JSONReader.loadJSONFromCommit("StateTests/EIP158/stCodeSizeLimit.json", shacommit)
        GitHubJSONTestSuite.runStateTest(json, excluded)
    }

    @Test // testing full suite
    @Throws(ParseException::class, IOException::class)
    @Ignore
    fun testRandomStateGitHub() {

        val sha = "99db6f4f5fea3aa5cfbe8436feba8e213d06d1e8"
        val fileNames = getFileNamesForTreeSha(sha)
        val includedFiles = Arrays.asList(
                "st201504081841JAVA.json",
                "st201504081842JAVA.json",
                "st201504081843JAVA.json"
        )

        for (fileName in fileNames) {
            if (includedFiles.contains(fileName)) {
                println("Running: " + fileName)
                val json = JSONReader.loadJSON("StateTests//RandomTests/" + fileName)
                GitHubJSONTestSuite.runStateTest(json)
            }
        }

    }
}

