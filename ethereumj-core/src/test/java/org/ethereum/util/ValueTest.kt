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

import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

class ValueTest {

    @Test
    fun testCmp() {
        val val1 = Value("hello")
        val val2 = Value("world")

        assertFalse("Expected values not to be equal", val1.cmp(val2))

        val val3 = Value("hello")
        val val4 = Value("hello")

        assertTrue("Expected values to be equal", val3.cmp(val4))
    }

    @Test
    fun testTypes() {
        val str = Value("str")
        assertEquals(str.asString(), "str")

        val num = Value(1)
        assertEquals(num.asInt().toLong(), 1)

        val inter = Value(arrayOf<Any>(1))
        val interExp = arrayOf<Any>(1)
        assertTrue(Value(inter.asObj()).cmp(Value(interExp)))

        val byt = Value(byteArrayOf(1, 2, 3, 4))
        val bytExp = byteArrayOf(1, 2, 3, 4)
        assertTrue(Arrays.equals(byt.asBytes(), bytExp))

        val bigInt = Value(BigInteger.valueOf(10))
        val bigExp = BigInteger.valueOf(10)
        assertEquals(bigInt.asBigInt(), bigExp)
    }


    @Test
    fun longListRLPBug_1() {

        val testRlp = "f7808080d387206f72726563748a626574656c676575736580d387207870726573738a70726564696361626c658080808080808080808080"

        val `val` = Value.fromRlpEncoded(Hex.decode(testRlp))

        assertEquals(testRlp, Hex.toHexString(`val`?.encode()))
    }


}
