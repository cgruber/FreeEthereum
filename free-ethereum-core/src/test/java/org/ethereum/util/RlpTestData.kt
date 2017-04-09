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

import java.math.BigInteger

internal object RlpTestData {

    /***********************************
     * https://github.com/ethereum/tests/blob/master/rlptest.txt
     */
    val test01 = 0
    val result01 = "80"

    val test02 = ""
    val result02 = "80"

    val test03 = "d"
    val result03 = "64"

    val test04 = "cat"
    val result04 = "83636174"

    val test05 = "dog"
    val result05 = "83646f67"

    val test06 = arrayOf("cat", "dog")
    val result06 = "c88363617483646f67"

    val test07 = arrayOf("dog", "god", "cat")
    val result07 = "cc83646f6783676f6483636174"

    val test08 = 1
    val result08 = "01"

    val test09 = 10
    val result09 = "0a"

    val test10 = 100
    val result10 = "64"

    val test11 = 1000
    val result11 = "8203e8"

    val test12 = BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935")
    val result12 = "a0ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

    val test13 = BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936")
    val result13 = "a1010000000000000000000000000000000000000000000000000000000000000000"

    val test14 = arrayOf<Any>(1, 2, arrayOf<Any>())
    val result14 = "c30102c0"
    val expected14 = arrayOf(byteArrayOf(1), byteArrayOf(2), arrayOf<Any>())

    val test15 = arrayOf<Any>(arrayOf<Any>(arrayOf<Any>(), arrayOf<Any>()), arrayOf<Any>())
    val result15 = "c4c2c0c0c0"

    val test16 = arrayOf<Any>("zw", arrayOf<Any>(4), "wz")
    val result16 = "c8827a77c10482777a"
    val expected16 = arrayOf(byteArrayOf(122, 119), arrayOf<Any>(byteArrayOf(4)), byteArrayOf(119, 122))
}
