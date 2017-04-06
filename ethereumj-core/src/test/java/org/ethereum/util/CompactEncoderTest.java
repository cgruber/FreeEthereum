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

package org.ethereum.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class CompactEncoderTest {

    private final static byte T = 16; // terminator

    @Test
    public void testCompactEncodeOddCompact() {
        final byte[] test = new byte[]{1, 2, 3, 4, 5};
        final byte[] expectedData = new byte[]{0x11, 0x23, 0x45};
        assertArrayEquals("odd compact encode fail", expectedData, CompactEncoder.packNibbles(test));
    }

    @Test
    public void testCompactEncodeEvenCompact() {
        final byte[] test = new byte[]{0, 1, 2, 3, 4, 5};
        final byte[] expectedData = new byte[]{0x00, 0x01, 0x23, 0x45};
        assertArrayEquals("even compact encode fail", expectedData, CompactEncoder.packNibbles(test));
    }

    @Test
    public void testCompactEncodeEvenTerminated() {
        final byte[] test = new byte[]{0, 15, 1, 12, 11, 8, T};
        final byte[] expectedData = new byte[]{0x20, 0x0f, 0x1c, (byte) 0xb8};
        assertArrayEquals("even terminated compact encode fail", expectedData, CompactEncoder.packNibbles(test));
    }

    @Test
    public void testCompactEncodeOddTerminated() {
        final byte[] test = new byte[]{15, 1, 12, 11, 8, T};
        final byte[] expectedData = new byte[]{0x3f, 0x1c, (byte) 0xb8};
        assertArrayEquals("odd terminated compact encode fail", expectedData, CompactEncoder.packNibbles(test));
    }

    @Test
    public void testCompactDecodeOddCompact() {
        final byte[] test = new byte[]{0x11, 0x23, 0x45};
        final byte[] expected = new byte[]{1, 2, 3, 4, 5};
        assertArrayEquals("odd compact decode fail", expected, CompactEncoder.unpackToNibbles(test));
    }

    @Test
    public void testCompactDecodeEvenCompact() {
        final byte[] test = new byte[]{0x00, 0x01, 0x23, 0x45};
        final byte[] expected = new byte[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals("even compact decode fail", expected, CompactEncoder.unpackToNibbles(test));
    }

    @Test
    public void testCompactDecodeEvenTerminated() {
        final byte[] test = new byte[]{0x20, 0x0f, 0x1c, (byte) 0xb8};
        final byte[] expected = new byte[]{0, 15, 1, 12, 11, 8, T};
        assertArrayEquals("even terminated compact decode fail", expected, CompactEncoder.unpackToNibbles(test));
    }

    @Test
    public void testCompactDecodeOddTerminated() {
        final byte[] test = new byte[]{0x3f, 0x1c, (byte) 0xb8};
        final byte[] expected = new byte[]{15, 1, 12, 11, 8, T};
        assertArrayEquals("odd terminated compact decode fail", expected, CompactEncoder.unpackToNibbles(test));
    }

    @Test
    public void testCompactHexEncode_1() {
        final byte[] test = "stallion".getBytes();
        final byte[] result = new byte[]{7, 3, 7, 4, 6, 1, 6, 12, 6, 12, 6, 9, 6, 15, 6, 14, T};
        assertArrayEquals(result, CompactEncoder.binToNibbles(test));
    }

    @Test
    public void testCompactHexEncode_2() {
        final byte[] test = "verb".getBytes();
        final byte[] result = new byte[]{7, 6, 6, 5, 7, 2, 6, 2, T};
        assertArrayEquals(result, CompactEncoder.binToNibbles(test));
    }

    @Test
    public void testCompactHexEncode_3() {
        final byte[] test = "puppy".getBytes();
        final byte[] result = new byte[]{7, 0, 7, 5, 7, 0, 7, 0, 7, 9, T};
        assertArrayEquals(result, CompactEncoder.binToNibbles(test));
    }
}
