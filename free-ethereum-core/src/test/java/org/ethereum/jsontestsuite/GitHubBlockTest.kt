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
import org.ethereum.config.blockchain.DaoHFConfig
import org.ethereum.config.blockchain.Eip150HFConfig
import org.ethereum.config.blockchain.FrontierConfig
import org.ethereum.config.blockchain.HomesteadConfig
import org.ethereum.config.net.BaseNetConfig
import org.ethereum.config.net.MainNetConfig
import org.ethereum.jsontestsuite.suite.JSONReader
import org.json.simple.parser.ParseException
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubBlockTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private val shacommit = "289b3e4524786618c7ec253b516bc8e76350f947"

    @Ignore // test for conveniently running a single test
    @Test
    @Throws(ParseException::class, IOException::class)
    fun runSingleTest() {
        SystemProperties.getDefault()!!.setGenesisInfo("frontier.json")
        SystemProperties.getDefault()!!.blockchainConfig = HomesteadConfig()

        val json = JSONReader.loadJSONFromCommit("BlockchainTests/Homestead/bcTotalDifficultyTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonSingleBlockTest(json, "sideChainWithNewMaxDifficultyStartingFromBlock3AfterBlock4")
    }

    @Throws(IOException::class, ParseException::class)
    private fun runFrontier(name: String) {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/$name.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
    }

    @Throws(IOException::class, ParseException::class)
    private fun runHomestead(name: String) {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/Homestead/$name.json", shacommit)
        SystemProperties.getDefault()!!.blockchainConfig = HomesteadConfig()
        try {
            GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
        } finally {
            SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
        }
    }

    @Throws(IOException::class, ParseException::class)
    private fun runEIP150(name: String) {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/EIP150/$name.json", shacommit)
        SystemProperties.getDefault()!!.blockchainConfig = Eip150HFConfig(DaoHFConfig())
        try {
            GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
        } finally {
            SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
        }
    }

    @Throws(IOException::class, ParseException::class)
    private fun run(name: String, frontier: Boolean, homestead: Boolean, eip150: Boolean) {
        if (frontier) runFrontier(name)
        if (homestead) runHomestead(name)
        if (eip150) runEIP150(name)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCInvalidHeaderTest() {
        run("bcInvalidHeaderTest", true, true, true)
    }


    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCInvalidRLPTest() {
        run("bcInvalidRLPTest", true, false, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCRPCAPITest() {
        run("bcRPC_API_Test", true, true, true)
    }


    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCUncleHeaderValidityTest() {
        run("bcUncleHeaderValiditiy", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCUncleTest() {
        run("bcUncleTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCValidBlockTest() {
        SystemProperties.getDefault()!!.setGenesisInfo("frontier.json")
        run("bcValidBlockTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCBlockGasLimitTest() {
        run("bcBlockGasLimitTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCForkBlockTest() {
        run("bcForkBlockTest", true, false, false)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCForkUncleTest() {
        run("bcForkUncle", true, false, false)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCForkStressTest() {
        run("bcForkStressTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCStateTest() {
        run("bcStateTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCGasPricerTest() {
        run("bcGasPricerTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCTotalDifficultyTest() {
        run("bcTotalDifficultyTest", false, true, true)
    }

    @Test
    @Throws(Exception::class)
    fun runBCWalletTest() {
        run("bcWalletTest", true, true, true)
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun runBCMultiChainTest() {
        run("bcMultiChainTest", true, true, true)
    }


    @Test
    @Throws(Exception::class)
    fun runDaoHardForkTest() {
        val json = JSONReader.getFromUrl("https://raw.githubusercontent.com/ethereum/tests/hardfork/BlockchainTests/TestNetwork/bcTheDaoTest.json")

        val testConfig = object : BaseNetConfig() {
            init {
                add(0, FrontierConfig())
                add(5, HomesteadConfig())
                add(8, DaoHFConfig(HomesteadConfig(), 8))
            }
        }

        SystemProperties.getDefault()!!.setGenesisInfo("frontier.json")
        SystemProperties.getDefault()!!.blockchainConfig = testConfig

        GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
    }
}
