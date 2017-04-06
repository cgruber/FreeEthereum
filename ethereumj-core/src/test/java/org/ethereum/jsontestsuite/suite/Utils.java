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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.util.Utils.unifiedNumericToBigInteger;

/**
 * @author Roman Mandeleil
 * @since 15.12.2014
 */
public class Utils {

    public static byte[] parseVarData(String data){
        if (data == null || data.equals("")) return EMPTY_BYTE_ARRAY;
        if (data.startsWith("0x")) {
            data = data.substring(2);
            if (data.equals("")) return EMPTY_BYTE_ARRAY;

            if (data.length() % 2 == 1) data = "0" + data;

            return Hex.decode(data);
        }

        return parseNumericData(data);
    }


    public static byte[] parseData(String data) {
        if (data == null) return EMPTY_BYTE_ARRAY;
        if (data.startsWith("0x")) data = data.substring(2);
        if (data.length() % 2 != 0) data = "0" + data;
        return Hex.decode(data);
    }

    public static byte[] parseNumericData(final String data) {

        if (data == null || data.equals("")) return EMPTY_BYTE_ARRAY;
        final byte[] dataB = unifiedNumericToBigInteger(data).toByteArray();
        return ByteUtil.stripLeadingZeroes(dataB);
    }

    public static long parseLong(String data) {
        final boolean hex = data.startsWith("0x");
        if (hex) data = data.substring(2);
        if (data.equals("")) return 0;
        return new BigInteger(data, hex ? 16 : 10).longValue();
    }

    public static byte parseByte(String data) {
        if (data.startsWith("0x")) {
            data = data.substring(2);
            return data.equals("") ? 0 : Byte.parseByte(data, 16);
        } else
            return data.equals("") ? 0 : Byte.parseByte(data);
    }


    public static String parseUnidentifiedBase(String number) {
        if (number.startsWith("0x"))
          number = new BigInteger(number.substring(2), 16).toString(10);
        return number;
    }
}
