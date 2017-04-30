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

package org.ethereum.mine

import org.apache.commons.lang3.tuple.Pair
import org.ethereum.crypto.HashUtil
import org.ethereum.util.ByteUtil.*
import org.spongycastle.util.Arrays
import org.spongycastle.util.Arrays.reverse
import java.lang.System.arraycopy
import java.math.BigInteger
import java.math.BigInteger.valueOf
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * The Ethash algorithm described in https://github.com/ethereum/wiki/wiki/Ethash

 * Created by Anton Nashatyrev on 27.11.2015.
 */
internal class EthashAlgo @JvmOverloads constructor(val params: EthashParams = EthashParams()) {

    private fun makeCacheBytes(cacheSize: Long, seed: ByteArray): Array<ByteArray> {
        val n = (cacheSize / params.hasH_BYTES).toInt()
        val o = arrayOfNulls<ByteArray>(n)
        o[0] = HashUtil.sha512(seed)
        for (i in 1..n - 1) {
            o[i] = HashUtil.sha512(o[i - 1]!!)
        }

        for (cacheRound in 0..params.cachE_ROUNDS - 1) {
            for (i in 0..n - 1) {
                val v = remainderUnsigned(getWord(o[i]!!, 0), n)
                o[i] = HashUtil.sha512(xor(o[(i - 1 + n) % n], o[v]))
            }
        }
        return o as Array<ByteArray>
    }

    fun makeCache(cacheSize: Long, seed: ByteArray): IntArray {
        val bytes = makeCacheBytes(cacheSize, seed)
        val ret = IntArray(bytes.size * bytes[0].size / 4)
        val ints = IntArray(bytes[0].size / 4)
        for (i in bytes.indices) {
            bytesToInts(bytes[i], ints, false)
            arraycopy(ints, 0, ret, i * ints.size, ints.size)
        }
        return ret
    }

    private fun sha512(arr: IntArray, bigEndian: Boolean): IntArray {
        var bytesTmp = ByteArray(arr.size shl 2)
        intsToBytes(arr, bytesTmp, bigEndian)
        bytesTmp = HashUtil.sha512(bytesTmp)
        bytesToInts(bytesTmp, arr, bigEndian)
        return arr
    }

    fun calcDatasetItem(cache: IntArray, i: Int): IntArray {
        val r = params.hasH_BYTES / params.worD_BYTES
        val n = cache.size / r
        var mix = Arrays.copyOfRange(cache, i % n * r, (i % n + 1) * r)

        mix[0] = i xor mix[0]
        mix = sha512(mix, false)
        val dsParents = params.dataseT_PARENTS.toInt()
        val mixLen = mix.size
        for (j in 0..dsParents - 1) {
            var cacheIdx = fnv(i xor j, mix[j % r])
            cacheIdx = remainderUnsigned(cacheIdx, n)
            val off = cacheIdx * r
            for (k in 0..mixLen - 1) {
                mix[k] = fnv(mix[k], cache[off + k])
            }
        }
        return sha512(mix, false)
    }

    fun calcDataset(fullSize: Long, cache: IntArray): IntArray {
        val hashesCount = (fullSize / params.hasH_BYTES).toInt()
        val ret = IntArray(hashesCount * (params.hasH_BYTES / 4))
        for (i in 0..hashesCount - 1) {
            val item = calcDatasetItem(cache, i)
            arraycopy(item, 0, ret, i * (params.hasH_BYTES / 4), item.size)
        }
        return ret
    }

    private fun hashimoto(blockHeaderTruncHash: ByteArray, nonce: ByteArray, fullSize: Long,
                          cacheOrDataset: IntArray, full: Boolean): Pair<ByteArray, ByteArray> {
        if (nonce.size != 8) throw RuntimeException("nonce.length != 8")

        val hashWords = params.hasH_BYTES / 4
        val w = params.miX_BYTES / params.worD_BYTES
        val mixhashes = params.miX_BYTES / params.hasH_BYTES
        val s = bytesToInts(HashUtil.sha512(merge(blockHeaderTruncHash, reverse(nonce))), false)
        val mix = IntArray(params.miX_BYTES / 4)
        for (i in 0..mixhashes - 1) {
            arraycopy(s, 0, mix, i * s.size, s.size)
        }

        val numFullPages = (fullSize / params.miX_BYTES).toInt()
        for (i in 0..params.accesses - 1) {
            val p = remainderUnsigned(fnv((i xor s[0].toLong()).toInt(), mix[i.toInt() % w]), numFullPages)
            val newData = IntArray(mix.size)
            val off = p * mixhashes
            for (j in 0..mixhashes - 1) {
                val itemIdx = off + j
                if (!full) {
                    val lookup1 = calcDatasetItem(cacheOrDataset, itemIdx)
                    arraycopy(lookup1, 0, newData, j * lookup1.size, lookup1.size)
                } else {
                    arraycopy(cacheOrDataset, itemIdx * hashWords, newData, j * hashWords, hashWords)
                }
            }
            for (i1 in mix.indices) {
                mix[i1] = fnv(mix[i1], newData[i1])
            }
        }

        val cmix = IntArray(mix.size / 4)
        var i = 0
        while (i < mix.size) {
            val fnv1 = fnv(mix[i], mix[i + 1])
            val fnv2 = fnv(fnv1, mix[i + 2])
            val fnv3 = fnv(fnv2, mix[i + 3])
            cmix[i shr 2] = fnv3
            i += 4 /* ? */
        }

        return Pair.of(intsToBytes(cmix, false), HashUtil.sha3(merge(intsToBytes(s, false), intsToBytes(cmix, false))))
    }

    fun hashimotoLight(fullSize: Long, cache: IntArray, blockHeaderTruncHash: ByteArray,
                       nonce: ByteArray): Pair<ByteArray, ByteArray> {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, cache, false)
    }

    fun hashimotoFull(fullSize: Long, dataset: IntArray, blockHeaderTruncHash: ByteArray,
                      nonce: ByteArray): Pair<ByteArray, ByteArray> {
        return hashimoto(blockHeaderTruncHash, nonce, fullSize, dataset, true)
    }

    @JvmOverloads fun mine(fullSize: Long, dataset: IntArray, blockHeaderTruncHash: ByteArray, difficulty: Long, startNonce: Long = Random().nextLong()): Long {
        var nonce = startNonce
        val target = valueOf(2).pow(256).divide(valueOf(difficulty))
        while (!Thread.currentThread().isInterrupted) {
            nonce++
            val pair = hashimotoFull(fullSize, dataset, blockHeaderTruncHash, longToBytes(nonce))
            val h = BigInteger(1, pair.right /* ?? */)
            if (h.compareTo(target) < 0) break
        }
        return nonce
    }

    @JvmOverloads fun mineLight(fullSize: Long, cache: IntArray, blockHeaderTruncHash: ByteArray, difficulty: Long, startNonce: Long = Random().nextLong()): Long {
        var nonce = startNonce
        val target = valueOf(2).pow(256).divide(valueOf(difficulty))
        while (!Thread.currentThread().isInterrupted) {
            nonce++
            val pair = hashimotoLight(fullSize, cache, blockHeaderTruncHash, longToBytes(nonce))
            val h = BigInteger(1, pair.right /* ?? */)
            if (h.compareTo(target) < 0) break
        }
        return nonce
    }

    fun getSeedHash(blockNumber: Long): ByteArray {
        var ret = ByteArray(32)
        for (i in 0..blockNumber / params.epocH_LENGTH - 1) {
            ret = HashUtil.sha3(ret)
        }
        return ret
    }

    companion object {
        private val FNV_PRIME = 0x01000193

        // Little-Endian !
        private fun getWord(arr: ByteArray, wordOff: Int): Int {
            return ByteBuffer.wrap(arr, wordOff * 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        }

        fun setWord(arr: ByteArray, wordOff: Int, `val`: Long) {
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(`val`.toInt())
            bb.rewind()
            bb.get(arr, wordOff * 4, 4)
        }

        private fun remainderUnsigned(dividend: Int, divisor: Int): Int {
            var dividend = dividend
            if (divisor >= 0) {
                if (dividend >= 0) {
                    return dividend % divisor
                }
                // The implementation is a Java port of algorithm described in the book
                // "Hacker's Delight" (section "Unsigned short division from signed division").
                val q = dividend.ushr(1) / divisor shl 1
                dividend -= q * divisor
                if (dividend < 0 || dividend >= divisor) {
                    dividend -= divisor
                }
                return dividend
            }
            return if (dividend >= 0 || dividend < divisor) dividend else dividend - divisor
        }

        private fun fnv(v1: Int, v2: Int): Int {
            return v1 * FNV_PRIME xor v2
        }
    }
}
/**
 * This the slower miner version which uses only cache thus taking much less memory than
 * regular [.mine] method
 */
