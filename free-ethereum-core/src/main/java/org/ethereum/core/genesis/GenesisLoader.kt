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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.ByteStreams
import org.ethereum.config.BlockchainNetConfig
import org.ethereum.config.SystemProperties
import org.ethereum.core.AccountState
import org.ethereum.core.BlockHeader.NONCE_LENGTH
import org.ethereum.core.Genesis
import org.ethereum.core.Genesis.Companion.ZERO_HASH_2048
import org.ethereum.core.Genesis.PremineAccount
import org.ethereum.crypto.HashUtil
import org.ethereum.crypto.HashUtil.EMPTY_LIST_HASH
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.trie.SecureTrie
import org.ethereum.util.ByteUtil
import org.ethereum.util.ByteUtil.*
import org.ethereum.util.Utils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.math.BigInteger
import java.util.*

object GenesisLoader {

    /**
     * Load genesis from passed location or from classpath `genesis` directory
     */
    @Throws(RuntimeException::class)
    fun loadGenesisJson(config: SystemProperties, classLoader: ClassLoader): GenesisJson? {
        val genesisFile = config.getProperty<String>("genesisFile", null)
        val genesisResource = config.genesisInfo()

        // #1 try to find genesis at passed location
        if (genesisFile != null) {
            try {
                FileInputStream(File(genesisFile)).use { `is` -> return loadGenesisJson(`is`) }
            } catch (e: Exception) {
                showLoadError("Problem loading genesis file from " + genesisFile, genesisFile, genesisResource)
            }

        }

        // #2 fall back to old genesis location at `src/main/resources/genesis` directory
        val `is` = classLoader.getResourceAsStream("genesis/" + genesisResource)
        if (`is` != null) {
            try {
                return loadGenesisJson(`is`)
            } catch (e: Exception) {
                showLoadError("Problem loading genesis file from resource directory", genesisFile, genesisResource)
            }

        } else {
            showLoadError("Genesis file was not found in resource directory", genesisFile, genesisResource)
        }

        return null
    }

    private fun showLoadError(message: String, genesisFile: String, genesisResource: String) {
        Utils.showErrorAndExit(
                message,
                "Config option 'genesisFile': " + genesisFile,
                "Config option 'genesis': " + genesisResource)
    }

    @Throws(RuntimeException::class)
    fun parseGenesis(blockchainNetConfig: BlockchainNetConfig, genesisJson: GenesisJson): Genesis? {
        try {
            val genesis = createBlockForJson(genesisJson)

            genesis.premine = generatePreMine(blockchainNetConfig, genesisJson.alloc!!)

            val rootHash = generateRootHash(genesis.premine)
            genesis.stateRoot = rootHash

            return genesis
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.showErrorAndExit("Problem parsing genesis", e.message)
        }

        return null
    }

    /**
     * Method used much in tests.
     */
    fun loadGenesis(resourceAsStream: InputStream): Genesis {
        val genesisJson = loadGenesisJson(resourceAsStream)
        return parseGenesis(SystemProperties.getDefault()!!.blockchainConfig, genesisJson)!!
    }

    @Throws(RuntimeException::class)
    fun loadGenesisJson(genesisJsonIS: InputStream): GenesisJson {
        var json: String? = null
        try {
            json = String(ByteStreams.toByteArray(genesisJsonIS))

            val mapper = ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)

            val genesisJson = mapper.readValue(json, GenesisJson::class.java)
            return genesisJson
        } catch (e: Exception) {

            Utils.showErrorAndExit("Problem parsing genesis: " + e.message, json)

            throw RuntimeException(e.message, e)
        }

    }


    private fun createBlockForJson(genesisJson: GenesisJson): Genesis {

        val nonce = prepareNonce(ByteUtil.hexStringToBytes(genesisJson.nonce))
        val difficulty = hexStringToBytesValidate(genesisJson.difficulty!!, 32, true)
        val mixHash = hexStringToBytesValidate(genesisJson.mixhash!!, 32, false)
        val coinbase = hexStringToBytesValidate(genesisJson.coinbase!!, 20, false)

        val timestampBytes = hexStringToBytesValidate(genesisJson.timestamp!!, 8, true)
        val timestamp = ByteUtil.byteArrayToLong(timestampBytes)

        val parentHash = hexStringToBytesValidate(genesisJson.parentHash!!, 32, false)
        val extraData = hexStringToBytesValidate(genesisJson.extraData!!, 32, true)

        val gasLimitBytes = hexStringToBytesValidate(genesisJson.gasLimit!!, 8, true)
        val gasLimit = ByteUtil.byteArrayToLong(gasLimitBytes)

        return Genesis(parentHash, EMPTY_LIST_HASH, coinbase, ZERO_HASH_2048,
                difficulty, 0, gasLimit, 0, timestamp, extraData,
                mixHash, nonce)
    }

    private fun hexStringToBytesValidate(hex: String, bytes: Int, notGreater: Boolean): ByteArray {
        val ret = ByteUtil.hexStringToBytes(hex)
        if (notGreater) {
            if (ret.size > bytes) {
                throw RuntimeException("Wrong value length: $hex, expected length < $bytes bytes")
            }
        } else {
            if (ret.size != bytes) {
                throw RuntimeException("Wrong value length: $hex, expected length $bytes bytes")
            }
        }
        return ret
    }

    /**
     * Prepares nonce to be correct length
     * @param nonceUnchecked    unchecked, user-provided nonce
     * *
     * @return  correct nonce
     * *
     * @throws RuntimeException when nonce is too long
     */
    private fun prepareNonce(nonceUnchecked: ByteArray): ByteArray {
        if (nonceUnchecked.size > 8) {
            throw RuntimeException(String.format("Invalid nonce, should be %s length", NONCE_LENGTH))
        } else if (nonceUnchecked.size == 8) {
            return nonceUnchecked
        }
        val nonce = ByteArray(NONCE_LENGTH)
        val diff = NONCE_LENGTH - nonceUnchecked.size
        System.arraycopy(nonceUnchecked, 0, nonce, diff, NONCE_LENGTH - diff)
        return nonce
    }


    private fun generatePreMine(blockchainNetConfig: BlockchainNetConfig, allocs: Map<String, GenesisJson.AllocatedAccount>): MutableMap<ByteArrayWrapper, PremineAccount> {

        val premine = HashMap<ByteArrayWrapper, PremineAccount>()

        for (key in allocs.keys) {

            val address = hexStringToBytes(key)
            val alloc = allocs[key]
            val state = PremineAccount()
            var accountState = AccountState(
                    blockchainNetConfig.commonConstants.initialNonce, parseHexOrDec(alloc?.balance))

            if (alloc?.nonce != null) {
                accountState = accountState.withNonce(parseHexOrDec(alloc.nonce))
            }

            if (alloc?.code != null) {
                val codeBytes = hexStringToBytes(alloc.code)
                accountState = accountState.withCodeHash(HashUtil.sha3(codeBytes))
                state.code = codeBytes
            }

            state.accountState = accountState
            premine.put(wrap(address), state)
        }

        return premine
    }

    /**
     * @param rawValue either hex started with 0x or dec
     * * return BigInteger
     */
    private fun parseHexOrDec(rawValue: String?): BigInteger {
        if (rawValue != null) {
            return if (rawValue.startsWith("0x")) bytesToBigInteger(hexStringToBytes(rawValue)) else BigInteger(rawValue)
        } else {
            return BigInteger.ZERO
        }
    }

    fun generateRootHash(premine: Map<ByteArrayWrapper, PremineAccount>): ByteArray {

        val state = SecureTrie(null as ByteArray?)

        for (key in premine.keys) {
            state.put(key.data, premine[key]?.accountState?.encoded)
        }

        return state.rootHash
    }
}
