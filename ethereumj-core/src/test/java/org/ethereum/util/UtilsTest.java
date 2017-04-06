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
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Roman Mandeleil
 * @since 17.05.14
 */
public class UtilsTest {

    @Test
    public void testGetValueShortString1() {

        int aaa;
        final String expected = "123\u00b7(10^24)";
        final String result = Utils.getValueShortString(new BigInteger("123456789123445654363653463"));

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueShortString2() {

        final String expected = "123\u00b7(10^3)";
        final String result = Utils.getValueShortString(new BigInteger("123456"));

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueShortString3() {

        final String expected = "1\u00b7(10^3)";
        final String result = Utils.getValueShortString(new BigInteger("1234"));

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueShortString4() {

        final String expected = "123\u00b7(10^0)";
        final String result = Utils.getValueShortString(new BigInteger("123"));

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueShortString5() {

        final byte[] decimal = Hex.decode("3913517ebd3c0c65000000");
        final String expected = "69\u00b7(10^24)";
        final String result = Utils.getValueShortString(new BigInteger(decimal));

        assertEquals(expected, result);
    }

    @Test
    public void testAddressStringToBytes() {
        // valid address
        String HexStr = "6c386a4b26f73c802f34673f7248bb118f97424a";
        byte[] expected = Hex.decode(HexStr);
        byte[] result = Utils.addressStringToBytes(HexStr);
        assertEquals(Arrays.areEqual(expected, result), true);

        // invalid address, we removed the last char so it cannot decode
        HexStr = "6c386a4b26f73c802f34673f7248bb118f97424";
        expected = null;
        result = Utils.addressStringToBytes(HexStr);
        assertEquals(expected, result);

        // invalid address, longer than 20 bytes
        HexStr = new String(Hex.encode("I am longer than 20 bytes, i promise".getBytes()));
        expected = null;
        result = Utils.addressStringToBytes(HexStr);
        assertEquals(expected, result);

        // invalid address, shorter than 20 bytes
        HexStr = new String(Hex.encode("I am short".getBytes()));
        expected = null;
        result = Utils.addressStringToBytes(HexStr);
        assertEquals(expected, result);
    }
}
