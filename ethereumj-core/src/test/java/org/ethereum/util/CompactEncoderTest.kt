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

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class CompactEncoderTest {

    @Test
    fun testCompactEncodeOddCompact() {
        val test = byteArrayOf(1, 2, 3, 4, 5)
        val expectedData = byteArrayOf(0x11, 0x23, 0x45)
        assertArrayEquals("odd compact encode fail", expectedData, CompactEncoder.packNibbles(test))
    }

    @Test
    fun testCompactEncodeEvenCompact() {
        val test = byteArrayOf(0, 1, 2, 3, 4, 5)
        val expectedData = byteArrayOf(0x00, 0x01, 0x23, 0x45)
        assertArrayEquals("even compact encode fail", expectedData, CompactEncoder.packNibbles(test))
    }

    @Test
    fun testCompactEncodeEvenTerminated() {
        val test = byteArrayOf(0, 15, 1, 12, 11, 8, T)
        val expectedData = byteArrayOf(0x20, 0x0f, 0x1c, 0xb8.toByte())
        assertArrayEquals("even terminated compact encode fail", expectedData, CompactEncoder.packNibbles(test))
    }

    @Test
    fun testCompactEncodeOddTerminated() {
        val test = byteArrayOf(15, 1, 12, 11, 8, T)
        val expectedData = byteArrayOf(0x3f, 0x1c, 0xb8.toByte())
        assertArrayEquals("odd terminated compact encode fail", expectedData, CompactEncoder.packNibbles(test))
    }

    @Test
    fun testCompactDecodeOddCompact() {
        val test = byteArrayOf(0x11, 0x23, 0x45)
        val expected = byteArrayOf(1, 2, 3, 4, 5)
        assertArrayEquals("odd compact decode fail", expected, CompactEncoder.unpackToNibbles(test))
    }

    @Test
    fun testCompactDecodeEvenCompact() {
        val test = byteArrayOf(0x00, 0x01, 0x23, 0x45)
        val expected = byteArrayOf(0, 1, 2, 3, 4, 5)
        assertArrayEquals("even compact decode fail", expected, CompactEncoder.unpackToNibbles(test))
    }

    @Test
    fun testCompactDecodeEvenTerminated() {
        val test = byteArrayOf(0x20, 0x0f, 0x1c, 0xb8.toByte())
        val expected = byteArrayOf(0, 15, 1, 12, 11, 8, T)
        assertArrayEquals("even terminated compact decode fail", expected, CompactEncoder.unpackToNibbles(test))
    }

    @Test
    fun testCompactDecodeOddTerminated() {
        val test = byteArrayOf(0x3f, 0x1c, 0xb8.toByte())
        val expected = byteArrayOf(15, 1, 12, 11, 8, T)
        assertArrayEquals("odd terminated compact decode fail", expected, CompactEncoder.unpackToNibbles(test))
    }

    @Test
    fun testCompactHexEncode_1() {
        val test = "stallion".toByteArray()
        val result = byteArrayOf(7, 3, 7, 4, 6, 1, 6, 12, 6, 12, 6, 9, 6, 15, 6, 14, T)
        assertArrayEquals(result, CompactEncoder.binToNibbles(test))
    }

    @Test
    fun testCompactHexEncode_2() {
        val test = "verb".toByteArray()
        val result = byteArrayOf(7, 6, 6, 5, 7, 2, 6, 2, T)
        assertArrayEquals(result, CompactEncoder.binToNibbles(test))
    }

    @Test
    fun testCompactHexEncode_3() {
        val test = "puppy".toByteArray()
        val result = byteArrayOf(7, 0, 7, 5, 7, 0, 7, 0, 7, 9, T)
        assertArrayEquals(result, CompactEncoder.binToNibbles(test))
    }

    companion object {

        private val T: Byte = 16 // terminator
    }
}
