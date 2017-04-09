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

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.BigIntegers
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

class ByteUtilTest {

    @Test
    fun testAppendByte() {
        val bytes = "tes".toByteArray()
        val b: Byte = 0x74
        Assert.assertArrayEquals("test".toByteArray(), ByteUtil.appendByte(bytes, b))
    }

    @Test
    fun testBigIntegerToBytes() {
        val expecteds = byteArrayOf(0xff.toByte(), 0xec.toByte(), 0x78)
        val b = BigInteger.valueOf(16772216)
        val actuals = ByteUtil.bigIntegerToBytes(b)
        assertArrayEquals(expecteds, actuals)
    }

    @Test
    fun testBigIntegerToBytesSign() {
        run {
            val b = BigInteger.valueOf(-2)
            val actuals = ByteUtil.bigIntegerToBytesSigned(b, 8)
            assertArrayEquals(Hex.decode("fffffffffffffffe"), actuals)
        }
        run {
            val b = BigInteger.valueOf(2)
            val actuals = ByteUtil.bigIntegerToBytesSigned(b, 8)
            assertArrayEquals(Hex.decode("0000000000000002"), actuals)
        }
        run {
            val b = BigInteger.valueOf(0)
            val actuals = ByteUtil.bigIntegerToBytesSigned(b, 8)
            assertArrayEquals(Hex.decode("0000000000000000"), actuals)
        }
        run {
            val b = BigInteger("eeeeeeeeeeeeee", 16)
            val actuals = ByteUtil.bigIntegerToBytesSigned(b, 8)
            assertArrayEquals(Hex.decode("00eeeeeeeeeeeeee"), actuals)
        }
        run {
            val b = BigInteger("eeeeeeeeeeeeeeee", 16)
            val actuals = ByteUtil.bigIntegerToBytesSigned(b, 8)
            assertArrayEquals(Hex.decode("eeeeeeeeeeeeeeee"), actuals)
        }
    }

    @Test
    fun testBigIntegerToBytesNegative() {
        val expecteds = byteArrayOf(0xff.toByte(), 0x0, 0x13, 0x88.toByte())
        val b = BigInteger.valueOf(-16772216)
        val actuals = ByteUtil.bigIntegerToBytes(b)
        assertArrayEquals(expecteds, actuals)
    }

    @Test
    fun testBigIntegerToBytesZero() {
        val expecteds = byteArrayOf(0x00)
        val b = BigInteger.ZERO
        val actuals = ByteUtil.bigIntegerToBytes(b)
        assertArrayEquals(expecteds, actuals)
    }

    @Test
    fun testToHexString() {
        assertEquals("", ByteUtil.toHexString(null))
    }

    @Test
    fun testCalcPacketLength() {
        val test = byteArrayOf(0x0f, 0x10, 0x43)
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x03)
        assertArrayEquals(expected, ByteUtil.calcPacketLength(test))
    }

    @Test
    fun testByteArrayToInt() {
        assertEquals(0, ByteUtil.byteArrayToInt(null).toLong())
        assertEquals(0, ByteUtil.byteArrayToInt(ByteArray(0)).toLong())

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
    fun testNumBytes() {
        val test1 = "0"
        val test2 = "1"
        val test3 = "1000000000" //3B9ACA00
        val expected1 = 1
        val expected2 = 1
        val expected3 = 4
        assertEquals(expected1.toLong(), ByteUtil.numBytes(test1).toLong())
        assertEquals(expected2.toLong(), ByteUtil.numBytes(test2).toLong())
        assertEquals(expected3.toLong(), ByteUtil.numBytes(test3).toLong())
    }

    @Test
    fun testStripLeadingZeroes() {
        val test1: ByteArray? = null
        val test2 = byteArrayOf()
        val test3 = byteArrayOf(0x00)
        val test4 = byteArrayOf(0x00, 0x01)
        val test5 = byteArrayOf(0x00, 0x00, 0x01)
        val expected1: ByteArray? = null
        val expected2 = byteArrayOf(0)
        val expected3 = byteArrayOf(0)
        val expected4 = byteArrayOf(0x01)
        val expected5 = byteArrayOf(0x01)
        assertArrayEquals(expected1, ByteUtil.stripLeadingZeroes(test1))
        assertArrayEquals(expected2, ByteUtil.stripLeadingZeroes(test2))
        assertArrayEquals(expected3, ByteUtil.stripLeadingZeroes(test3))
        assertArrayEquals(expected4, ByteUtil.stripLeadingZeroes(test4))
        assertArrayEquals(expected5, ByteUtil.stripLeadingZeroes(test5))
    }

    @Test
    fun testMatchingNibbleLength1() {
        // a larger than b
        val a = byteArrayOf(0x00, 0x01)
        val b = byteArrayOf(0x00)
        val result = ByteUtil.matchingNibbleLength(a, b)
        assertEquals(1, result.toLong())
    }

    @Test
    fun testMatchingNibbleLength2() {
        // b larger than a
        val a = byteArrayOf(0x00)
        val b = byteArrayOf(0x00, 0x01)
        val result = ByteUtil.matchingNibbleLength(a, b)
        assertEquals(1, result.toLong())
    }

    @Test
    fun testMatchingNibbleLength3() {
        // a and b the same length equal
        val a = byteArrayOf(0x00)
        val b = byteArrayOf(0x00)
        val result = ByteUtil.matchingNibbleLength(a, b)
        assertEquals(1, result.toLong())
    }

    @Test
    fun testMatchingNibbleLength4() {
        // a and b the same length not equal
        val a = byteArrayOf(0x01)
        val b = byteArrayOf(0x00)
        val result = ByteUtil.matchingNibbleLength(a, b)
        assertEquals(0, result.toLong())
    }

    @Test
    fun testNiceNiblesOutput_1() {
        val test = byteArrayOf(7, 0, 7, 5, 7, 0, 7, 0, 7, 9)
        val result = "\\x07\\x00\\x07\\x05\\x07\\x00\\x07\\x00\\x07\\x09"
        assertEquals(result, ByteUtil.nibblesToPrettyString(test))
    }

    @Test
    fun testNiceNiblesOutput_2() {
        val test = byteArrayOf(7, 0, 7, 0xf, 7, 0, 0xa, 0, 7, 9)
        val result = "\\x07\\x00\\x07\\x0f\\x07\\x00\\x0a\\x00\\x07\\x09"
        assertEquals(result, ByteUtil.nibblesToPrettyString(test))
    }

    @Test(expected = NullPointerException::class)
    fun testMatchingNibbleLength5() {
        // a == null
        val a: ByteArray? = null
        val b = byteArrayOf(0x00)
        ByteUtil.matchingNibbleLength(a, b)
    }

    @Test(expected = NullPointerException::class)
    fun testMatchingNibbleLength6() {
        // b == null
        val a = byteArrayOf(0x00)
        val b: ByteArray? = null
        ByteUtil.matchingNibbleLength(a, b)
    }

    @Test
    fun testMatchingNibbleLength7() {
        // a or b is empty
        val a = ByteArray(0)
        val b = byteArrayOf(0x00)
        val result = ByteUtil.matchingNibbleLength(a, b)
        assertEquals(0, result.toLong())
    }

    /**
     * This test shows the difference between iterating over,
     * and comparing byte[] vs BigInteger value.

     * Results indicate that the former has ~15x better performance.
     * Therefore this is used in the Miner.mine() method.
     */
    @Test
    fun testIncrementPerformance() {
        val testEnabled = false

        if (testEnabled) {
            val counter1 = ByteArray(4)
            val max = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE).array()
            val start1 = System.currentTimeMillis()
            while (ByteUtil.increment(counter1)) {
                if (FastByteComparisons.compareTo(counter1, 0, 4, max, 0, 4) == 0) {
                    break
                }
            }
            println((System.currentTimeMillis() - start1).toString() + "ms to reach: " + Hex.toHexString(counter1))

            var counter2 = BigInteger.ZERO
            val start2 = System.currentTimeMillis()
            while (true) {
                if (counter2.compareTo(BigInteger.valueOf(Integer.MAX_VALUE.toLong())) == 0) {
                    break
                }
                counter2 = counter2.add(BigInteger.ONE)
            }
            println((System.currentTimeMillis() - start2).toString() + "ms to reach: " + Hex.toHexString(BigIntegers.asUnsignedByteArray(4, counter2)))
        }
    }


    @Test
    fun firstNonZeroByte_1() {

        val data = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")
        val result = ByteUtil.firstNonZeroByte(data)

        assertEquals(-1, result.toLong())
    }

    @Test
    fun firstNonZeroByte_2() {

        val data = Hex.decode("0000000000000000000000000000000000000000000000000000000000332211")
        val result = ByteUtil.firstNonZeroByte(data)

        assertEquals(29, result.toLong())
    }

    @Test
    fun firstNonZeroByte_3() {

        val data = Hex.decode("2211009988776655443322110099887766554433221100998877665544332211")
        val result = ByteUtil.firstNonZeroByte(data)

        assertEquals(0, result.toLong())
    }

    @Test
    fun setBitTest() {
        /*
            Set on
         */
        val data = ByteBuffer.allocate(4).putInt(0).array()
        var posBit = 24
        var expected = 16777216
        var result = -1
        var ret = ByteUtil.setBit(data, posBit, 1)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)

        posBit = 25
        expected = 50331648
        ret = ByteUtil.setBit(data, posBit, 1)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)

        posBit = 2
        expected = 50331652
        ret = ByteUtil.setBit(data, posBit, 1)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)

        /*
            Set off
         */
        posBit = 24
        expected = 33554436
        ret = ByteUtil.setBit(data, posBit, 0)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)

        posBit = 25
        expected = 4
        ret = ByteUtil.setBit(data, posBit, 0)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)

        posBit = 2
        expected = 0
        ret = ByteUtil.setBit(data, posBit, 0)
        result = ByteUtil.byteArrayToInt(ret)
        assertTrue(expected == result)
    }

    @Test
    fun getBitTest() {
        val data = ByteBuffer.allocate(4).putInt(0).array()
        ByteUtil.setBit(data, 24, 1)
        ByteUtil.setBit(data, 25, 1)
        ByteUtil.setBit(data, 2, 1)

        val found = ArrayList<Int>()
        for (i in 0..data.size * 8 - 1) {
            val res = ByteUtil.getBit(data, i)
            if (res == 1)
                if (i != 24 && i != 25 && i != 2)
                    assertTrue(false)
                else
                    found.add(i)
            else {
                if (i == 24 || i == 25 || i == 2)
                    assertTrue(false)
            }
        }

        if (found.size != 3)
            assertTrue(false)
        assertTrue(found[0] === 2)
        assertTrue(found[1] === 24)
        assertTrue(found[2] === 25)
    }

    @Test
    fun numToBytesTest() {
        var bytes = ByteUtil.intToBytesNoLeadZeroes(-1)
        assertArrayEquals(bytes, Hex.decode("ffffffff"))
        bytes = ByteUtil.intToBytesNoLeadZeroes(1)
        assertArrayEquals(bytes, Hex.decode("01"))
        bytes = ByteUtil.intToBytesNoLeadZeroes(255)
        assertArrayEquals(bytes, Hex.decode("ff"))
        bytes = ByteUtil.intToBytesNoLeadZeroes(256)
        assertArrayEquals(bytes, Hex.decode("0100"))
        bytes = ByteUtil.intToBytesNoLeadZeroes(0)
        assertArrayEquals(bytes, ByteArray(0))

        bytes = ByteUtil.intToBytes(-1)
        assertArrayEquals(bytes, Hex.decode("ffffffff"))
        bytes = ByteUtil.intToBytes(1)
        assertArrayEquals(bytes, Hex.decode("00000001"))
        bytes = ByteUtil.intToBytes(255)
        assertArrayEquals(bytes, Hex.decode("000000ff"))
        bytes = ByteUtil.intToBytes(256)
        assertArrayEquals(bytes, Hex.decode("00000100"))
        bytes = ByteUtil.intToBytes(0)
        assertArrayEquals(bytes, Hex.decode("00000000"))

        bytes = ByteUtil.longToBytesNoLeadZeroes(-1)
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"))
        bytes = ByteUtil.longToBytesNoLeadZeroes(1)
        assertArrayEquals(bytes, Hex.decode("01"))
        bytes = ByteUtil.longToBytesNoLeadZeroes(255)
        assertArrayEquals(bytes, Hex.decode("ff"))
        bytes = ByteUtil.longToBytesNoLeadZeroes(1L shl 32)
        assertArrayEquals(bytes, Hex.decode("0100000000"))
        bytes = ByteUtil.longToBytesNoLeadZeroes(0)
        assertArrayEquals(bytes, ByteArray(0))

        bytes = ByteUtil.longToBytes(-1)
        assertArrayEquals(bytes, Hex.decode("ffffffffffffffff"))
        bytes = ByteUtil.longToBytes(1)
        assertArrayEquals(bytes, Hex.decode("0000000000000001"))
        bytes = ByteUtil.longToBytes(255)
        assertArrayEquals(bytes, Hex.decode("00000000000000ff"))
        bytes = ByteUtil.longToBytes(256)
        assertArrayEquals(bytes, Hex.decode("0000000000000100"))
        bytes = ByteUtil.longToBytes(0)
        assertArrayEquals(bytes, Hex.decode("0000000000000000"))
    }

    @Test
    fun testHexStringToBytes() {
        run {
            val str = "0000"
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(0, 0)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0x0000"
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(0, 0)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0x45a6"
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(69, -90)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "1963093cee500c081443e1045c40264b670517af"
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = Hex.decode(str)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0x" // Empty
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf()
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0" // Same as 0x00
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(0)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0x00" // This case shouldn't be empty array
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(0)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0xd" // Should work with odd length, adding leading 0
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(13)
            assertArrayEquals(expected, actuals)
        }
        run {
            val str = "0xd0d" // Should work with odd length, adding leading 0
            val actuals = ByteUtil.hexStringToBytes(str)
            val expected = byteArrayOf(13, 13)
            assertArrayEquals(expected, actuals)
        }
    }

    @Test
    fun testIpConversion() {
        val ip1 = "0.0.0.0"
        val ip1Bytes = ByteUtil.hostToBytes(ip1)
        assertEquals(ip1, ByteUtil.bytesToIp(ip1Bytes))

        val ip2 = "35.36.37.138"
        val ip2Bytes = ByteUtil.hostToBytes(ip2)
        assertEquals(ip2, ByteUtil.bytesToIp(ip2Bytes))

        val ip3 = "255.255.255.255"
        val ip3Bytes = ByteUtil.hostToBytes(ip3)
        assertEquals(ip3, ByteUtil.bytesToIp(ip3Bytes))

        // Fallback case
        val ip4 = "255.255.255.256"
        val ip4Bytes = ByteUtil.hostToBytes(ip4)
        assertEquals("0.0.0.0", ByteUtil.bytesToIp(ip4Bytes))
    }
}
