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

package org.ethereum.util

import org.ethereum.crypto.HashUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.spongycastle.util.encoders.Hex

class HashUtilTest {

    @Test
    fun testSha256_EmptyString() {
        val expected1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val result1 = Hex.toHexString(HashUtil.sha256(ByteArray(0)))
        assertEquals(expected1, result1)
    }

    @Test
    fun testSha256_Test() {
        val expected2 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
        val result2 = Hex.toHexString(HashUtil.sha256("test".toByteArray()))
        assertEquals(expected2, result2)
    }

    @Test
    fun testSha256_Multiple() {
        val expected1 = "1b4f0e9851971998e732078544c96b36c3d01cedf7caa332359d6f1d83567014"
        val result1 = Hex.toHexString(HashUtil.sha256("test1".toByteArray()))
        assertEquals(expected1, result1)

        val expected2 = "60303ae22b998861bce3b28f33eec1be758a213c86c93c076dbe9f558c11c752"
        val result2 = Hex.toHexString(HashUtil.sha256("test2".toByteArray()))
        assertEquals(expected2, result2)
    }

    @Test
    fun testSha3_EmptyString() {
        val expected1 = "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
        val result1 = Hex.toHexString(HashUtil.sha3(ByteArray(0)))
        assertEquals(expected1, result1)
    }

    @Test
    fun testSha3_Test() {
        val expected2 = "9c22ff5f21f0b81b113e63f7db6da94fedef11b2119b4088b89664fb9a3cb658"
        val result2 = Hex.toHexString(HashUtil.sha3("test".toByteArray()))
        assertEquals(expected2, result2)
    }

    @Test
    fun testSha3_Multiple() {
        val expected1 = "6d255fc3390ee6b41191da315958b7d6a1e5b17904cc7683558f98acc57977b4"
        val result1 = Hex.toHexString(HashUtil.sha3("test1".toByteArray()))
        assertEquals(expected1, result1)

        val expected2 = "4da432f1ecd4c0ac028ebde3a3f78510a21d54087b161590a63080d33b702b8d"
        val result2 = Hex.toHexString(HashUtil.sha3("test2".toByteArray()))
        assertEquals(expected2, result2)
    }

    @Test
    fun testRIPEMD160_EmptyString() {
        val expected1 = "9c1185a5c5e9fc54612808977ee8f548b2258d31"
        val result1 = Hex.toHexString(HashUtil.ripemd160(ByteArray(0)))
        assertEquals(expected1, result1)
    }

    @Test
    fun testRIPEMD160_Test() {
        val expected2 = "5e52fee47e6b070565f74372468cdc699de89107"
        val result2 = Hex.toHexString(HashUtil.ripemd160("test".toByteArray()))
        assertEquals(expected2, result2)
    }

    @Test
    fun testRIPEMD160_Multiple() {
        val expected1 = "9295fac879006ff44812e43b83b515a06c2950aa"
        val result1 = Hex.toHexString(HashUtil.ripemd160("test1".toByteArray()))
        assertEquals(expected1, result1)

        val expected2 = "80b85ebf641abccdd26e327c5782353137a0a0af"
        val result2 = Hex.toHexString(HashUtil.ripemd160("test2".toByteArray()))
        assertEquals(expected2, result2)
    }
}
