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

package org.ethereum.vm;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataWordTest {

    private static BigInteger pow(final BigInteger x, final BigInteger y) {
        if (y.compareTo(BigInteger.ZERO) < 0)
            throw new IllegalArgumentException();
        BigInteger z = x; // z will successively become x^2, x^4, x^8, x^16,
        // x^32...
        BigInteger result = BigInteger.ONE;
        final byte[] bytes = y.toByteArray();
        for (int i = bytes.length - 1; i >= 0; i--) {
            byte bits = bytes[i];
            for (int j = 0; j < 8; j++) {
                if ((bits & 1) != 0)
                    result = result.multiply(z);
                // short cut out if there are no more bits to handle:
                if ((bits >>= 1) == 0 && i == 0)
                    return result;
                z = z.multiply(z);
            }
        }
        return result;
    }

    @Test
    public void testAddPerformance() {
        final boolean enabled = false;

        if (enabled) {
            final byte[] one = new byte[]{0x01, 0x31, 0x54, 0x41, 0x01, 0x31, 0x54,
                    0x41, 0x01, 0x31, 0x54, 0x41, 0x01, 0x31, 0x54, 0x41, 0x01,
                    0x31, 0x54, 0x41, 0x01, 0x31, 0x54, 0x41, 0x01, 0x31, 0x54,
                    0x41, 0x01, 0x31, 0x54, 0x41}; // Random value

            final int ITERATIONS = 10000000;

            final long now1 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                final DataWord x = new DataWord(one);
                x.add(x);
            }
            System.out.println("Add1: " + (System.currentTimeMillis() - now1) + "ms");

            final long now2 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                final DataWord x = new DataWord(one);
                x.add2(x);
            }
            System.out.println("Add2: " + (System.currentTimeMillis() - now2) + "ms");
        } else {
            System.out.println("ADD performance test is disabled.");
        }
    }

    @Test
    public void testAdd2() {
        final byte[] two = new byte[32];
        two[31] = (byte) 0xff; // 0x000000000000000000000000000000000000000000000000000000000000ff

        final DataWord x = new DataWord(two);
        x.add(new DataWord(two));
        System.out.println(Hex.toHexString(x.getData()));

        final DataWord y = new DataWord(two);
        y.add2(new DataWord(two));
        System.out.println(Hex.toHexString(y.getData()));
    }

    @Test
    public void testAdd3() {
        final byte[] three = new byte[32];
        for (int i = 0; i < three.length; i++) {
            three[i] = (byte) 0xff;
        }

        final DataWord x = new DataWord(three);
        x.add(new DataWord(three));
        assertEquals(32, x.getData().length);
        System.out.println(Hex.toHexString(x.getData()));

        // FAIL
//      DataWord y = new DataWord(three);
//      y.add2(new DataWord(three));
//      System.out.println(Hex.toHexString(y.getData()));
    }

    @Test
    public void testMod() {
        final String expected = "000000000000000000000000000000000000000000000000000000000000001a";

        final byte[] one = new byte[32];
        one[31] = 0x1e; // 0x000000000000000000000000000000000000000000000000000000000000001e

        final byte[] two = new byte[32];
        for (int i = 0; i < two.length; i++) {
            two[i] = (byte) 0xff;
        }
        two[31] = 0x56; // 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff56

        final DataWord x = new DataWord(one);// System.out.println(x.value());
        final DataWord y = new DataWord(two);// System.out.println(y.value());
        y.mod(x);
        assertEquals(32, y.getData().length);
        assertEquals(expected, Hex.toHexString(y.getData()));
    }

    @Test
    public void testMul() {
        final byte[] one = new byte[32];
        one[31] = 0x1; // 0x0000000000000000000000000000000000000000000000000000000000000001

        final byte[] two = new byte[32];
        two[11] = 0x1; // 0x0000000000000000000000010000000000000000000000000000000000000000

        final DataWord x = new DataWord(one);// System.out.println(x.value());
        final DataWord y = new DataWord(two);// System.out.println(y.value());
        x.mul(y);
        assertEquals(32, y.getData().length);
        assertEquals("0000000000000000000000010000000000000000000000000000000000000000", Hex.toHexString(y.getData()));
    }

    @Test
    public void testMulOverflow() {

        final byte[] one = new byte[32];
        one[30] = 0x1; // 0x0000000000000000000000000000000000000000000000000000000000000100

        final byte[] two = new byte[32];
        two[0] = 0x1; //  0x1000000000000000000000000000000000000000000000000000000000000000

        final DataWord x = new DataWord(one);// System.out.println(x.value());
        final DataWord y = new DataWord(two);// System.out.println(y.value());
        x.mul(y);
        assertEquals(32, y.getData().length);
        assertEquals("0100000000000000000000000000000000000000000000000000000000000000", Hex.toHexString(y.getData()));
    }

    @Test
    public void testDiv() {
        final byte[] one = new byte[32];
        one[30] = 0x01;
        one[31] = 0x2c; // 0x000000000000000000000000000000000000000000000000000000000000012c

        final byte[] two = new byte[32];
        two[31] = 0x0f; // 0x000000000000000000000000000000000000000000000000000000000000000f

        final DataWord x = new DataWord(one);
        final DataWord y = new DataWord(two);
        x.div(y);

        assertEquals(32, x.getData().length);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000014", Hex.toHexString(x.getData()));
    }

    @Test
    public void testDivZero() {
        final byte[] one = new byte[32];
        one[30] = 0x05; // 0x0000000000000000000000000000000000000000000000000000000000000500

        final byte[] two = new byte[32];

        final DataWord x = new DataWord(one);
        final DataWord y = new DataWord(two);
        x.div(y);

        assertEquals(32, x.getData().length);
        assertTrue(x.isZero());
    }

    @Test
    public void testSDivNegative() {

        // one is -300 as 256-bit signed integer:
        final byte[] one = Hex.decode("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed4");

        final byte[] two = new byte[32];
        two[31] = 0x0f;

        final DataWord x = new DataWord(one);
        final DataWord y = new DataWord(two);
        x.sDiv(y);

        assertEquals(32, x.getData().length);
        assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffec", x.toString());
    }

    @Test
    public void testPow() {

        final BigInteger x = BigInteger.valueOf(Integer.MAX_VALUE);
        final BigInteger y = BigInteger.valueOf(1000);

        final BigInteger result1 = x.modPow(x, y);
        final BigInteger result2 = pow(x, y);
        System.out.println(result1);
        System.out.println(result2);
    }

    @Test
    public void testSignExtend1() {

        final DataWord x = new DataWord(Hex.decode("f2"));
        final byte k = 0;
        final String expected = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff2";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend2() {
        final DataWord x = new DataWord(Hex.decode("f2"));
        final byte k = 1;
        final String expected = "00000000000000000000000000000000000000000000000000000000000000f2";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend3() {

        final byte k = 1;
        final DataWord x = new DataWord(Hex.decode("0f00ab"));
        final String expected = "00000000000000000000000000000000000000000000000000000000000000ab";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend4() {

        final byte k = 1;
        final DataWord x = new DataWord(Hex.decode("ffff"));
        final String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend5() {

        final byte k = 3;
        final DataWord x = new DataWord(Hex.decode("ffffffff"));
        final String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend6() {

        final byte k = 3;
        final DataWord x = new DataWord(Hex.decode("ab02345678"));
        final String expected = "0000000000000000000000000000000000000000000000000000000002345678";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend7() {

        final byte k = 3;
        final DataWord x = new DataWord(Hex.decode("ab82345678"));
        final String expected = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff82345678";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test
    public void testSignExtend8() {

        final byte k = 30;
        final DataWord x = new DataWord(Hex.decode("ff34567882345678823456788234567882345678823456788234567882345678"));
        final String expected = "0034567882345678823456788234567882345678823456788234567882345678";

        x.signExtend(k);
        System.out.println(x.toString());
        assertEquals(expected, x.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSignExtendException1() {

        final byte k = -1;
        final DataWord x = new DataWord();

        x.signExtend(k); // should throw an exception
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSignExtendException2() {

        final byte k = 32;
        final DataWord x = new DataWord();

        x.signExtend(k); // should throw an exception
    }

    @Test
    public void testAddModOverflow() {
        testAddMod("9999999999999999999999999999999999999999999999999999999999999999",
                "8888888888888888888888888888888888888888888888888888888888888888",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        testAddMod("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    }

    private void testAddMod(final String v1, final String v2, final String v3) {
        final DataWord dv1 = new DataWord(Hex.decode(v1));
        final DataWord dv2 = new DataWord(Hex.decode(v2));
        final DataWord dv3 = new DataWord(Hex.decode(v3));
        final BigInteger bv1 = new BigInteger(v1, 16);
        final BigInteger bv2 = new BigInteger(v2, 16);
        final BigInteger bv3 = new BigInteger(v3, 16);

        dv1.addmod(dv2, dv3);
        final BigInteger br = bv1.add(bv2).mod(bv3);
        assertEquals(dv1.value(), br);
    }

    @Test
    public void testMulMod1() {
        final DataWord wr = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));
        final DataWord w1 = new DataWord(Hex.decode("01"));
        final DataWord w2 = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999998"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000001", Hex.toHexString(wr.getData()));
    }

    @Test
    public void testMulMod2() {
        final DataWord wr = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));
        final DataWord w1 = new DataWord(Hex.decode("01"));
        final DataWord w2 = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertTrue(wr.isZero());
    }

    @Test
    public void testMulModZero() {
        final DataWord wr = new DataWord(Hex.decode("00"));
        final DataWord w1 = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));
        final DataWord w2 = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertTrue(wr.isZero());
    }

    @Test
    public void testMulModZeroWord1() {
        final DataWord wr = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));
        final DataWord w1 = new DataWord(Hex.decode("00"));
        final DataWord w2 = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertTrue(wr.isZero());
    }

    @Test
    public void testMulModZeroWord2() {
        final DataWord wr = new DataWord(Hex.decode("9999999999999999999999999999999999999999999999999999999999999999"));
        final DataWord w1 = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        final DataWord w2 = new DataWord(Hex.decode("00"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertTrue(wr.isZero());
    }

    @Test
    public void testMulModOverflow() {
        final DataWord wr = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        final DataWord w1 = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        final DataWord w2 = new DataWord(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        wr.mulmod(w1, w2);

        assertEquals(32, wr.getData().length);
        assertTrue(wr.isZero());
    }
}
