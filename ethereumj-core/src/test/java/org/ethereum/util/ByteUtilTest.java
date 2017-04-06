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

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ByteUtilTest {

    @Test
    public void testAppendByte() {
        final byte[] bytes = "tes".getBytes();
        final byte b = 0x74;
        Assert.assertArrayEquals("test".getBytes(), ByteUtil.appendByte(bytes, b));
    }

    @Test
    public void testBigIntegerToBytes() {
        final byte[] expecteds = new byte[]{(byte) 0xff, (byte) 0xec, 0x78};
        final BigInteger b = BigInteger.valueOf(16772216);
        final byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBigIntegerToBytesSign() {
        {
            final BigInteger b = BigInteger.valueOf(-2);
            final byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("fffffffffffffffe"), actuals);
        }
        {
            final BigInteger b = BigInteger.valueOf(2);
            final byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("0000000000000002"), actuals);
        }
        {
            final BigInteger b = BigInteger.valueOf(0);
            final byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("0000000000000000"), actuals);
        }
        {
            final BigInteger b = new BigInteger("eeeeeeeeeeeeee", 16);
            final byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("00eeeeeeeeeeeeee"), actuals);
        }
        {
            final BigInteger b = new BigInteger("eeeeeeeeeeeeeeee", 16);
            final byte[] actuals = ByteUtil.bigIntegerToBytesSigned(b, 8);
            assertArrayEquals(Hex.decode("eeeeeeeeeeeeeeee"), actuals);
        }
    }

    @Test
    public void testBigIntegerToBytesNegative() {
        final byte[] expecteds = new byte[]{(byte) 0xff, 0x0, 0x13, (byte) 0x88};
        final BigInteger b = BigInteger.valueOf(-16772216);
        final byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBigIntegerToBytesZero() {
        final byte[] expecteds = new byte[]{0x00};
        final BigInteger b = BigInteger.ZERO;
        final byte[] actuals = ByteUtil.bigIntegerToBytes(b);
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testToHexString() {
        assertEquals("", ByteUtil.toHexString(null));
    }

    @Test
    public void testCalcPacketLength() {
        final byte[] test = new byte[]{0x0f, 0x10, 0x43};
        final byte[] expected = new byte[]{0x00, 0x00, 0x00, 0x03};
        assertArrayEquals(expected, ByteUtil.calcPacketLength(test));
    }

    @Test
    public void testByteArrayToInt() {
        assertEquals(0, ByteUtil.byteArrayToInt(null));
        assertEquals(0, ByteUtil.byteArrayToInt(new byte[0]));

//      byte[] x = new byte[] { 5,1,7,0,8 };
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < 100000000; i++) {
//           ByteArray.read32bit(x, 0);
//      }
//      long end = System.currentTimeMillis();
//      System.out.println(end - start + "ms");
//
//      long start1 = System.currentTimeMillis();
//      for (int i = 0; i < 100000000; i++) {
//          new BigInteger(1, x).intValue();
//      }
//      long end1 = System.currentTimeMillis();
//      System.out.println(end1 - start1 + "ms");

    }

    @Test
    public void testNumBytes() {
        final String test1 = "0";
        final String test2 = "1";
        final String test3 = "1000000000"; //3B9ACA00
        final int expected1 = 1;
        final int expected2 = 1;
        final int expected3 = 4;
        assertEquals(expected1, ByteUtil.numBytes(test1));
        assertEquals(expected2, ByteUtil.numBytes(test2));
        assertEquals(expected3, ByteUtil.numBytes(test3));
    }

    @Test
    public void testStripLeadingZeroes() {
        final byte[] test1 = null;
        final byte[] test2 = new byte[]{};
        final byte[] test3 = new byte[]{0x00};
        final byte[] test4 = new byte[]{0x00, 0x01};
        final byte[] test5 = new byte[]{0x00, 0x00, 0x01};
        final byte[] expected1 = null;
        final byte[] expected2 = new byte[]{0};
        final byte[] expected3 = new byte[]{0};
        final byte[] expected4 = new byte[]{0x01};
        final byte[] expected5 = new byte[]{0x01};
        assertArrayEquals(expected1, ByteUtil.stripLeadingZeroes(test1));
        assertArrayEquals(expected2, ByteUtil.stripLeadingZeroes(test2));
        assertArrayEquals(expected3, ByteUtil.stripLeadingZeroes(test3));
        assertArrayEquals(expected4, ByteUtil.stripLeadingZeroes(test4));
        assertArrayEquals(expected5, ByteUtil.stripLeadingZeroes(test5));
    }

    @Test
    public void testMatchingNibbleLength1() {
        // a larger than b
        final byte[] a = new byte[]{0x00, 0x01};
        final byte[] b = new byte[]{0x00};
        final int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength2() {
        // b larger than a
        final byte[] a = new byte[]{0x00};
        final byte[] b = new byte[]{0x00, 0x01};
        final int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength3() {
        // a and b the same length equal
        final byte[] a = new byte[]{0x00};
        final byte[] b = new byte[]{0x00};
        final int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(1, result);
    }

    @Test
    public void testMatchingNibbleLength4() {
        // a and b the same length not equal
        final byte[] a = new byte[]{0x01};
        final byte[] b = new byte[]{0x00};
        final int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(0, result);
    }

    @Test
    public void testNiceNiblesOutput_1() {
        final byte[] test = {7, 0, 7, 5, 7, 0, 7, 0, 7, 9};
        final String result = "\\x07\\x00\\x07\\x05\\x07\\x00\\x07\\x00\\x07\\x09";
        assertEquals(result, ByteUtil.nibblesToPrettyString(test));
    }

    @Test
    public void testNiceNiblesOutput_2() {
        final byte[] test = {7, 0, 7, 0xf, 7, 0, 0xa, 0, 7, 9};
        final String result = "\\x07\\x00\\x07\\x0f\\x07\\x00\\x0a\\x00\\x07\\x09";
        assertEquals(result, ByteUtil.nibblesToPrettyString(test));
    }

    @Test(expected = NullPointerException.class)
    public void testMatchingNibbleLength5() {
        // a == null
        final byte[] a = null;
        final byte[] b = new byte[]{0x00};
        ByteUtil.matchingNibbleLength(a, b);
    }

    @Test(expected = NullPointerException.class)
    public void testMatchingNibbleLength6() {
        // b == null
        final byte[] a = new byte[]{0x00};
        final byte[] b = null;
        ByteUtil.matchingNibbleLength(a, b);
    }

    @Test
    public void testMatchingNibbleLength7() {
        // a or b is empty
        final byte[] a = new byte[0];
        final byte[] b = new byte[]{0x00};
        final int result = ByteUtil.matchingNibbleLength(a, b);
        assertEquals(0, result);
    }

    /**
     * This test shows the difference between iterating over,
     * and comparing byte[] vs BigInteger value.
     *
     * Results indicate that the former has ~15x better performance.
     * Therefore this is used in the Miner.mine() method.
     */
    @Test
    public void testIncrementPerformance() {
        final boolean testEnabled = false;

        if (testEnabled) {
            final byte[] counter1 = new byte[4];
            final byte[] max = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE).array();
            final long start1 = System.currentTimeMillis();
            while (ByteUtil.increment(counter1)) {
                if (FastByteComparisons.compareTo(counter1, 0, 4, max, 0, 4) == 0) {
                    break;
                }
            }
            System.out.println(System.currentTimeMillis() - start1 + "ms to reach: " + Hex.toHexString(counter1));

            BigInteger counter2 = BigInteger.ZERO;
            final long start2 = System.currentTimeMillis();
            while (true) {
                if (counter2.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 0) {
                    break;
                }
                counter2 = counter2.add(BigInteger.ONE);
            }
            System.out.println(System.currentTimeMillis() - start2 + "ms to reach: " + Hex.toHexString(BigIntegers.asUnsignedByteArray(4, counter2)));
        }
    }


    @Test
    public void firstNonZeroByte_1() {

        final byte[] data = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000");
        final int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(-1, result);
    }

    @Test
    public void firstNonZeroByte_2() {

        final byte[] data = Hex.decode("0000000000000000000000000000000000000000000000000000000000332211");
        final int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(29, result);
    }

    @Test
    public void firstNonZeroByte_3() {

        final byte[] data = Hex.decode("2211009988776655443322110099887766554433221100998877665544332211");
        final int result = ByteUtil.firstNonZeroByte(data);

        assertEquals(0, result);
    }

    @Test
    public void setBitTest() {
        /*
            Set on
         */
        final byte[] data = ByteBuffer.allocate(4).putInt(0).array();
        int posBit = 24;
        int expected = 16777216;
        int result = -1;
        byte[] ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);

        posBit = 25;
        expected = 50331648;
        ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);

        posBit = 2;
        expected = 50331652;
        ret = ByteUtil.setBit(data, posBit, 1);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);

        /*
            Set off
         */
        posBit = 24;
        expected = 33554436;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);

        posBit = 25;
        expected = 4;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);

        posBit = 2;
        expected = 0;
        ret = ByteUtil.setBit(data, posBit, 0);
        result = ByteUtil.byteArrayToInt(ret);
        assertTrue(expected == result);
    }

    @Test
    public void getBitTest() {
        final byte[] data = ByteBuffer.allocate(4).putInt(0).array();
        ByteUtil.setBit(data, 24, 1);
        ByteUtil.setBit(data, 25, 1);
        ByteUtil.setBit(data, 2, 1);

        final List<Integer> found = new ArrayList<>();
        for (int i = 0; i < (data.length * 8); i++) {
            final int res = ByteUtil.getBit(data, i);
            if (res == 1)
                if (i != 24 && i != 25 && i != 2)
                    assertTrue(false);
                else
                    found.add(i);
            else {
                if (i == 24 || i == 25 || i == 2)
                    assertTrue(false);
            }
        }

        if (found.size() != 3)
            assertTrue(false);
        assertTrue(found.get(0) == 2);
        assertTrue(found.get(1) == 24);
        assertTrue(found.get(2) == 25);
    }

    @Test
    public void numToBytesTest() {
        byte[] bytes = ByteUtil.intToBytesNoLeadZeroes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffff"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(1);
        assertArrayEquals(bytes, Hex.decode("01"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(255);
        assertArrayEquals(bytes, Hex.decode("ff"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(256);
        assertArrayEquals(bytes, Hex.decode("0100"));
        bytes = ByteUtil.intToBytesNoLeadZeroes(0);
        assertArrayEquals(bytes, new byte[0]);

        bytes = ByteUtil.intToBytes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffff"));
        bytes = ByteUtil.intToBytes(1);
        assertArrayEquals(bytes, Hex.decode("00000001"));
        bytes = ByteUtil.intToBytes(255);
        assertArrayEquals(bytes, Hex.decode("000000ff"));
        bytes = ByteUtil.intToBytes(256);
        assertArrayEquals(bytes, Hex.decode("00000100"));
        bytes = ByteUtil.intToBytes(0);
        assertArrayEquals(bytes, Hex.decode("00000000"));

        bytes = ByteUtil.longToBytesNoLeadZeroes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(1);
        assertArrayEquals(bytes, Hex.decode("01"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(255);
        assertArrayEquals(bytes, Hex.decode("ff"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(1L << 32);
        assertArrayEquals(bytes, Hex.decode("0100000000"));
        bytes = ByteUtil.longToBytesNoLeadZeroes(0);
        assertArrayEquals(bytes, new byte[0]);

        bytes = ByteUtil.longToBytes(-1);
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"));
        bytes = ByteUtil.longToBytes(1);
        assertArrayEquals(bytes, Hex.decode("0000000000000001"));
        bytes = ByteUtil.longToBytes(255);
        assertArrayEquals(bytes, Hex.decode("00000000000000ff"));
        bytes = ByteUtil.longToBytes(256);
        assertArrayEquals(bytes, Hex.decode("0000000000000100"));
        bytes = ByteUtil.longToBytes(0);
        assertArrayEquals(bytes, Hex.decode("0000000000000000"));
    }

    @Test
    public void testHexStringToBytes() {
        {
            final String str = "0000";
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{0, 0};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0x0000";
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{0, 0};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0x45a6";
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{69, -90};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "1963093cee500c081443e1045c40264b670517af";
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = Hex.decode(str);
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0x"; // Empty
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0"; // Same as 0x00
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{0};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0x00"; // This case shouldn't be empty array
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{0};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0xd"; // Should work with odd length, adding leading 0
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{13};
            assertArrayEquals(expected, actuals);
        }
        {
            final String str = "0xd0d"; // Should work with odd length, adding leading 0
            final byte[] actuals = ByteUtil.hexStringToBytes(str);
            final byte[] expected = new byte[]{13, 13};
            assertArrayEquals(expected, actuals);
        }
    }

    @Test
    public void testIpConversion() {
        final String ip1 = "0.0.0.0";
        final byte[] ip1Bytes = ByteUtil.hostToBytes(ip1);
        assertEquals(ip1, ByteUtil.bytesToIp(ip1Bytes));

        final String ip2 = "35.36.37.138";
        final byte[] ip2Bytes = ByteUtil.hostToBytes(ip2);
        assertEquals(ip2, ByteUtil.bytesToIp(ip2Bytes));

        final String ip3 = "255.255.255.255";
        final byte[] ip3Bytes = ByteUtil.hostToBytes(ip3);
        assertEquals(ip3, ByteUtil.bytesToIp(ip3Bytes));

        // Fallback case
        final String ip4 = "255.255.255.256";
        final byte[] ip4Bytes = ByteUtil.hostToBytes(ip4);
        assertEquals("0.0.0.0", ByteUtil.bytesToIp(ip4Bytes));
    }
}
