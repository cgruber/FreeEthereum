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

package org.ethereum.core.genesis

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.ethereum.config.BlockchainConfig
import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.*
import org.ethereum.util.FastByteComparisons
import org.ethereum.util.FastByteComparisons.equal
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.math.BigInteger
import java.net.URISyntaxException

/**
 * Testing system exit
 * http://stackoverflow.com/questions/309396/java-how-to-test-methods-that-call-system-exit
 */
class GenesisLoadTest {

    @Test
    fun shouldLoadGenesis_whenShortWay() {
        loadGenesis(null, "frontier-test.json")
        assertTrue(true)
    }

    @Test
    @Throws(URISyntaxException::class)
    fun shouldLoadGenesis_whenFullPathSpecified() {
        val url = GenesisLoadTest::class.java.classLoader.getResource("genesis/frontier-test.json")

        // full path
        println("url.getPath() " + url!!.path)
        loadGenesis(url.path, null)

        val path = File(url.toURI()).toPath()
        val curPath = File("").absoluteFile.toPath()
        val relPath = curPath.relativize(path).toFile().path
        println("Relative path: " + relPath)
        loadGenesis(relPath, null)
        assertTrue(true)
    }

    @Test
    fun shouldLoadGenesisFromFile_whenBothSpecified() {
        val url = GenesisLoadTest::class.java.classLoader.getResource("genesis/frontier-test.json")

        // full path
        println("url.getPath() " + url!!.path)
        loadGenesis(url.path, "NOT_EXIST")
        assertTrue(true)
    }

    @Test(expected = RuntimeException::class)
    fun shouldError_whenWrongPath() {
        loadGenesis("NON_EXISTED_PATH", null)
        assertTrue(false)
    }

    @Test
    fun shouldLoadGenesis_whenManyOrderedConfigs() {
        val properties = loadGenesis(null, "genesis-with-config.json")
        properties.genesis
        val bnc = properties.blockchainConfig

        assertThat<BlockchainConfig>(bnc.getConfigForBlock(0), instanceOf<BlockchainConfig>(FrontierConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(149), instanceOf<BlockchainConfig>(FrontierConfig::class.java))

        assertThat<BlockchainConfig>(bnc.getConfigForBlock(150), instanceOf<BlockchainConfig>(HomesteadConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(299), instanceOf<BlockchainConfig>(HomesteadConfig::class.java))

        assertThat<BlockchainConfig>(bnc.getConfigForBlock(300), instanceOf<BlockchainConfig>(DaoHFConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(300), instanceOf<BlockchainConfig>(DaoHFConfig::class.java))
        val daoHFConfig = bnc.getConfigForBlock(300) as DaoHFConfig

        assertThat<BlockchainConfig>(bnc.getConfigForBlock(450), instanceOf<BlockchainConfig>(Eip150HFConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(10000000), instanceOf<BlockchainConfig>(Eip150HFConfig::class.java))
    }

    @Test
    fun shouldLoadGenesis_withCodeAndNonceInAlloc() {
        val genesis = GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/genesis-alloc.json"))
        val bc = StandaloneBlockchain()

        bc.withGenesis(genesis)

        val account = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826")
        val expectedCode = Hex.decode("00ff00")
        val expectedNonce: Long = 255       //FF

        val actualNonce = bc.blockchain.repository.getNonce(account)
        val actualCode = bc.blockchain.repository.getCode(account)

        assertEquals(BigInteger.valueOf(expectedNonce), actualNonce)
        assertTrue(equal(expectedCode, actualCode))
    }

    @Test
    fun shouldLoadGenesis_withSameBlockManyConfigs() {
        val properties = loadGenesis(null, "genesis-alloc.json")
        properties.genesis
        val bnc = properties.blockchainConfig

        assertThat<BlockchainConfig>(bnc.getConfigForBlock(0), instanceOf<BlockchainConfig>(FrontierConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(1999), instanceOf<BlockchainConfig>(FrontierConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(2000), instanceOf<BlockchainConfig>(Eip160HFConfig::class.java))
        assertThat<BlockchainConfig>(bnc.getConfigForBlock(10000000), instanceOf<BlockchainConfig>(Eip160HFConfig::class.java))

        // check DAO extradata for mining
        val SOME_EXTRA_DATA = "some-extra-data".toByteArray()
        val inDaoForkExtraData = bnc.getConfigForBlock(2000).getExtraData(SOME_EXTRA_DATA, 2000)
        val pastDaoForkExtraData = bnc.getConfigForBlock(2200).getExtraData(SOME_EXTRA_DATA, 2200)

        assertTrue(FastByteComparisons.equal(AbstractDaoConfig.DAO_EXTRA_DATA, inDaoForkExtraData))
        assertTrue(FastByteComparisons.equal(SOME_EXTRA_DATA, pastDaoForkExtraData))
    }


    private fun loadGenesis(genesisFile: String?, genesisResource: String?): SystemProperties {
        var config = ConfigFactory.empty()

        if (genesisResource != null) {
            config = config.withValue("genesis",
                    ConfigValueFactory.fromAnyRef(genesisResource))
        }
        if (genesisFile != null) {
            config = config.withValue("genesisFile",
                    ConfigValueFactory.fromAnyRef(genesisFile))
        }

        val properties = SystemProperties(config)
        properties.genesis
        return properties
    }
}
