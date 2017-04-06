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

package org.ethereum.jsonrpc;

import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

/**
 * Created by Ruben on 19/11/2015.
 */
class TypeConverter {

    public static byte[] StringNumberAsBytes(final String input) {
        return ByteUtil.bigIntegerToBytes(StringDecimalToBigInteger(input));
    }

    public static BigInteger StringNumberAsBigInt(final String input) throws Exception {
        if (input.startsWith("0x"))
            return TypeConverter.StringHexToBigInteger(input);
        else
            return TypeConverter.StringDecimalToBigInteger(input);
    }

    public static BigInteger StringHexToBigInteger(final String input) {
        final String hexa = input.startsWith("0x") ? input.substring(2) : input;
        return new BigInteger(hexa, 16);
    }

    private static BigInteger StringDecimalToBigInteger(final String input) {
        return new BigInteger(input);
    }

    public static byte[] StringHexToByteArray(String x) throws Exception {
        if (x.startsWith("0x")) {
            x = x.substring(2);
        }
        if (x.length() % 2 != 0) x = "0" + x;
        return Hex.decode(x);
    }

    public static String toJsonHex(final byte[] x) {
        return "0x"+Hex.toHexString(x);
    }

    public static String toJsonHex(final String x) {
        return x.startsWith("0x") ? x : "0x" + x;
    }

    public static String toJsonHex(final long n) {
        return "0x"+Long.toHexString(n);
    }

    public static String toJsonHex(final BigInteger n) {
        return "0x"+ n.toString(16);
    }
}
