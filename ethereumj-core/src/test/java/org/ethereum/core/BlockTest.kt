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

package org.ethereum.core

import org.ethereum.config.SystemProperties
import org.ethereum.core.genesis.GenesisLoader
import org.ethereum.trie.SecureTrie
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class BlockTest {

    // https://github.com/ethereum/tests/blob/71d80bd63aaf7cee523b6ca9d12a131698d41e98/BasicTests/genesishashestest.json
    private val GENESIS_RLP = "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a09178d0f23c965d81f0834a4c72c6253ce6830f4022b1359aaebfc1ecba442d4ea056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0"
    private val GENESIS_HASH = "fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdca"
    private val MESSY_NONCE_GENESIS_RLP = "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a0da3d5bd4c2f8443fbca1f12c0b9eaa4996825e9d32d239ffb302b8f98f202c97a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008301000080832fefd8808080a00000000000000000000000000000000000000000000000000000000000000000880000000000000000c0c0"
    private val MESSY_NONCE_GENESIS_HASH = "b096cfdeb2a3c0abd3ce9f77cf5adc92a8cead34aa4d2be54c004373e3986788"

    @Test
    fun testGenesisFromRLP() {
        // from RLP encoding
        val genesisBytes = Hex.decode(GENESIS_RLP)
        val genesisFromRLP = Block(genesisBytes)
        val genesis = GenesisLoader.loadGenesis(javaClass.getResourceAsStream("/genesis/olympic.json"))
        assertEquals(Hex.toHexString(genesis.hash), Hex.toHexString(genesisFromRLP.hash))
        assertEquals(Hex.toHexString(genesis.parentHash), Hex.toHexString(genesisFromRLP.parentHash))
        assertEquals(Hex.toHexString(genesis.stateRoot), Hex.toHexString(genesisFromRLP.stateRoot))
    }

    private fun loadGenesisFromFile(resPath: String): Block {
        val genesis = GenesisLoader.loadGenesis(javaClass.getResourceAsStream(resPath))
        logger.info(genesis.toString())

        logger.info("genesis hash: [{}]", Hex.toHexString(genesis.hash))
        logger.info("genesis rlp: [{}]", Hex.toHexString(genesis.encoded))

        return genesis
    }

    @Test
    fun testGenesisFromNew() {
        val genesis = loadGenesisFromFile("/genesis/olympic.json")

        assertEquals(GENESIS_HASH, Hex.toHexString(genesis.hash))
        assertEquals(GENESIS_RLP, Hex.toHexString(genesis.encoded))
    }

    /**
     * Test genesis loading from JSON with some
     * freedom for user like odd length of hex values etc.
     */
    @Test
    fun testGenesisFromNewMessy() {
        val genesis = loadGenesisFromFile("/genesis/olympic-messy.json")

        assertEquals(GENESIS_HASH, Hex.toHexString(genesis.hash))
        assertEquals(GENESIS_RLP, Hex.toHexString(genesis.encoded))
    }

    /**
     * Test genesis with empty nonce
     * + alloc addresses with 0x
     */
    @Test
    fun testGenesisEmptyNonce() {
        val genesis = loadGenesisFromFile("/genesis/nonce-messy.json")

        assertEquals(MESSY_NONCE_GENESIS_HASH, Hex.toHexString(genesis.hash))
        assertEquals(MESSY_NONCE_GENESIS_RLP, Hex.toHexString(genesis.encoded))
    }

    /**
     * Test genesis with short nonce
     * + alloc addresses with 0x
     */
    @Test
    fun testGenesisShortNonce() {
        val genesis = loadGenesisFromFile("/genesis/nonce-messy2.json")

        assertEquals(MESSY_NONCE_GENESIS_HASH, Hex.toHexString(genesis.hash))
        assertEquals(MESSY_NONCE_GENESIS_RLP, Hex.toHexString(genesis.encoded))
    }

    @Test
    fun testGenesisPremineData() {
        val genesis = GenesisLoader.loadGenesis(javaClass.getResourceAsStream("/genesis/olympic.json"))
        val accounts = genesis.premine.values
        assertTrue(accounts.size == 12)
    }


    @Test
    @Throws(ParseException::class)
    fun testPremineFromJSON() {

        val parser = JSONParser()
        val genesisMap = parser.parse(TEST_GENESIS) as JSONObject

        val keys = genesisMap.keys

        val state = SecureTrie(null as ByteArray?)

        for (key in keys) {

            val `val` = genesisMap[key] as JSONObject
            val denom = `val`.keys.toTypedArray()[0] as String
            val value = `val`.values.toTypedArray()[0] as String

            val wei = Denomination.valueOf(denom.toUpperCase()).value().multiply(BigInteger(value))

            val acctState = AccountState(BigInteger.ZERO, wei)
            state.put(Hex.decode(key.toString()), acctState.encoded)
        }

        logger.info("root: " + Hex.toHexString(state.rootHash))
        val GENESIS_STATE_ROOT = "9178d0f23c965d81f0834a4c72c6253ce6830f4022b1359aaebfc1ecba442d4e"
        assertEquals(GENESIS_STATE_ROOT, Hex.toHexString(state.rootHash))
    }


    @Test
    fun testFrontierGenesis() {
        val config = SystemProperties()
        config.setGenesisInfo("frontier.json")

        val genesis = config.genesis

        val hash = Hex.toHexString(genesis.hash)
        val root = Hex.toHexString(genesis.stateRoot)

        assertEquals("d7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544", root)
        assertEquals("d4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3", hash)
    }

    @Test
    fun testZeroPrecedingDifficultyGenesis() {
        val config = SystemProperties()
        config.setGenesisInfo("genesis-low-difficulty.json")

        val genesis = config.genesis

        val hash = Hex.toHexString(genesis.hash)
        val root = Hex.toHexString(genesis.stateRoot)

        assertEquals("8028c28b55eab8be08883e921f20d1b6cc9f2aa02cc6cd90cfaa9b0462ff6d3e", root)
        assertEquals("05b2dc41ade973d26db921052bcdaf54e2e01b308c9e90723b514823a0923592", hash)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
        private var TEST_GENESIS =
                "{" +
                        "'0000000000000000000000000000000000000001': { 'wei': '1' }" +
                        "'0000000000000000000000000000000000000002': { 'wei': '1' }" +
                        "'0000000000000000000000000000000000000003': { 'wei': '1' }" +
                        "'0000000000000000000000000000000000000004': { 'wei': '1' }" +
                        "'dbdbdb2cbd23b783741e8d7fcf51e459b497e4a6': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'e6716f9544a56c530d868e4bfbacb172315bdead': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'b9c015918bdaba24b4ff057a92a3873d6eb201be': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'1a26338f0d905e295fccb71fa9ea849ffa12aaf4': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'2ef47100e0787b915105fd5e3f4ff6752079d5cb': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'cd2a3d9f938e13cd947ec05abc7fe734df8dd826': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'6c386a4b26f73c802f34673f7248bb118f97424a': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "'e4157b34ea9615cfbde6b4fda419828124b70c78': { 'wei': '1606938044258990275541962092341162602522202993782792835301376' }" +
                        "}"

        init {
            TEST_GENESIS = TEST_GENESIS.replace("'", "\"")
        }
    }
}