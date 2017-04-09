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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Created by Anton Nashatyrev on 08.12.2016.
 */
class MinMaxMapTest {

    @Test
    fun test1() {
        val map = MinMaxMap<Int>()
        assertNull(map.min)
        assertNull(map.max)
        map.clearAllAfter(100)
        map.clearAllBefore(100)

        map.put(100L, 100)
        assertEquals(100, map.min!!.toLong())
        assertEquals(100, map.max!!.toLong())
        map.clearAllAfter(100)
        assertEquals(1, map.size.toLong())
        map.clearAllBefore(100)
        assertEquals(1, map.size.toLong())
        map.clearAllBefore(101)
        assertEquals(0, map.size.toLong())

        map.put(100L, 100)
        assertEquals(1, map.size.toLong())
        map.clearAllAfter(99)
        assertEquals(0, map.size.toLong())

        map.put(100L, 100)
        map.put(110L, 100)
        map.put(90L, 100)
        assertEquals(90, map.min!!.toLong())
        assertEquals(110, map.max!!.toLong())

        map.remove(100L)
        assertEquals(90, map.min!!.toLong())
        assertEquals(110, map.max!!.toLong())

        map.remove(110L)
        assertEquals(90, map.min!!.toLong())
        assertEquals(90, map.max!!.toLong())
    }
}
