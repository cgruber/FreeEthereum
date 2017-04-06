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
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubTestNetTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private val shacommit = "9ed33d7440f13c09ce7f038f92abd02d23b26f0d"

    @Before
    fun setup() {
        SystemProperties.getDefault()!!.setGenesisInfo("frontier.json")
        SystemProperties.getDefault()!!.blockchainConfig = object : BaseNetConfig() {
            init {
                add(0, FrontierConfig())
                add(5, HomesteadConfig())
                add(8, DaoHFConfig(HomesteadConfig(), 8))
                add(10, Eip150HFConfig(DaoHFConfig(HomesteadConfig(), 8)))

            }
        }
    }

    @After
    fun clean() {
        SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun bcEIP150Test() {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/TestNetwork/bcEIP150Test.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun bcSimpleTransitionTest() {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/TestNetwork/bcSimpleTransitionTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
    }

    @Test
    @Throws(ParseException::class, IOException::class)
    fun bcTheDaoTest() {
        val json = JSONReader.loadJSONFromCommit("BlockchainTests/TestNetwork/bcTheDaoTest.json", shacommit)
        GitHubJSONTestSuite.runGitHubJsonBlockTest(json, Collections.emptySet())
    }
}
