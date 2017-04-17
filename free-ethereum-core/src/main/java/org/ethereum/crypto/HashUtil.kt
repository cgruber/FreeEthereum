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

package org.ethereum.crypto

import org.ethereum.config.SystemProperties
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY
import org.ethereum.util.RLP
import org.ethereum.util.Utils
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.digests.RIPEMD160Digest
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Provider
import java.security.Security
import java.util.*
import java.util.Arrays.copyOfRange

object HashUtil {

    val EMPTY_DATA_HASH: ByteArray
    val EMPTY_LIST_HASH: ByteArray
    val EMPTY_TRIE_HASH: ByteArray
    private val LOG = LoggerFactory.getLogger(HashUtil::class.java)
    private val CRYPTO_PROVIDER: Provider

    private val HASH_256_ALGORITHM_NAME: String
    private val HASH_512_ALGORITHM_NAME: String

    private val sha256digest: MessageDigest

    init {
        val props = SystemProperties.getDefault()
        Security.addProvider(SpongyCastleProvider.instance)
        CRYPTO_PROVIDER = Security.getProvider(props!!.cryptoProviderName)
        HASH_256_ALGORITHM_NAME = props.hash256AlgName
        HASH_512_ALGORITHM_NAME = props.hash512AlgName
        try {
            sha256digest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Can't initialize HashUtils", e)
            throw RuntimeException(e) // Can't happen.
        }

        EMPTY_DATA_HASH = sha3(EMPTY_BYTE_ARRAY)
        EMPTY_LIST_HASH = sha3(RLP.encodeList())
        EMPTY_TRIE_HASH = sha3(RLP.encodeElement(EMPTY_BYTE_ARRAY))
    }

    /**
     * @param input
     * *            - data for hashing
     * *
     * @return - sha256 hash of the data
     */
    fun sha256(input: ByteArray): ByteArray {
        return sha256digest.digest(input)
    }

    fun sha3(input: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Can't find such algorithm", e)
            throw RuntimeException(e)
        }

    }

    fun sha3(input1: ByteArray, input2: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input1, 0, input1.size)
            digest.update(input2, 0, input2.size)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Can't find such algorithm", e)
            throw RuntimeException(e)
        }

    }

    /**
     * hashing chunk of the data

     * @param input
     * *            - data for hash
     * *
     * @param start
     * *            - start of hashing chunk
     * *
     * @param length
     * *            - length of hashing chunk
     * *
     * @return - keccak hash of the chunk
     */
    fun sha3(input: ByteArray, start: Int, length: Int): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input, start, length)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Can't find such algorithm", e)
            throw RuntimeException(e)
        }

    }

    fun sha512(input: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_512_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Can't find such algorithm", e)
            throw RuntimeException(e)
        }

    }

    /**
     * @param data
     * *            - message to hash
     * *
     * @return - reipmd160 hash of the message
     */
    fun ripemd160(data: ByteArray?): ByteArray {
        val digest = RIPEMD160Digest()
        if (data != null) {
            val resBuf = ByteArray(digest.digestSize)
            digest.update(data, 0, data.size)
            digest.doFinal(resBuf, 0)
            return resBuf
        }
        throw NullPointerException("Can't hash a NULL value")
    }

    /**
     * Calculates RIGTMOST160(SHA3(input)). This is used in address
     * calculations. *

     * @param input
     * *            - data
     * *
     * @return - 20 right bytes of the hash keccak of the data
     */
    fun sha3omit12(input: ByteArray): ByteArray {
        val hash = sha3(input)
        return copyOfRange(hash, 12, hash.size)
    }

    /**
     * The way to calculate new address inside ethereum

     * @param addr
     * *            - creating addres
     * *
     * @param nonce
     * *            - nonce of creating address
     * *
     * @return new address
     */
    fun calcNewAddr(addr: ByteArray, nonce: ByteArray): ByteArray {

        val encSender = RLP.encodeElement(addr)
        val encNonce = RLP.encodeBigInteger(BigInteger(1, nonce))

        return sha3omit12(RLP.encodeList(encSender, encNonce))
    }

    /**
     * @see .doubleDigest
     * @param input
     * *            -
     * *
     * @return -
     */
    fun doubleDigest(input: ByteArray): ByteArray {
        return doubleDigest(input, 0, input.size)
    }

    /**
     * Calculates the SHA-256 hash of the given byte range, and then hashes the
     * resulting hash again. This is standard procedure in Bitcoin. The
     * resulting hash is in big endian form.

     * @param input
     * *            -
     * *
     * @param offset
     * *            -
     * *
     * @param length
     * *            -
     * *
     * @return -
     */
    private fun doubleDigest(input: ByteArray, offset: Int, length: Int): ByteArray {
        synchronized(sha256digest) {
            sha256digest.reset()
            sha256digest.update(input, offset, length)
            val first = sha256digest.digest()
            return sha256digest.digest(first)
        }
    }

    /**
     * @return generates random peer id for the HelloMessage
     */
    fun randomPeerId(): ByteArray {

        val peerIdBytes = BigInteger(512, Utils.getRandom()).toByteArray()

        val peerId: String
        if (peerIdBytes.size > 64)
            peerId = Hex.toHexString(peerIdBytes, 1, 64)
        else
            peerId = Hex.toHexString(peerIdBytes)

        return Hex.decode(peerId)
    }

    /**
     * @return - generate random 32 byte hash
     */
    fun randomHash(): ByteArray {

        val randomHash = ByteArray(32)
        val random = Random()
        random.nextBytes(randomHash)
        return randomHash
    }

    fun shortHash(hash: ByteArray): String {
        return Hex.toHexString(hash).substring(0, 6)
    }
}
