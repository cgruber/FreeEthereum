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

package org.ethereum.jsontestsuite.suite

import org.ethereum.util.ByteUtil
import org.json.simple.JSONArray
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.regex.Pattern

/**
 * @author Roman Mandeleil
 * *
 * @since 28.06.2014
 */
internal object Helper {

    private val logger = LoggerFactory.getLogger("misc")

    fun parseDataArray(valArray: JSONArray): ByteArray {

        // value can be:
        //   1. 324234 number
        //   2. "0xAB3F23A" - hex string
        //   3. "239472398472" - big number

        val bos = ByteArrayOutputStream()
        for (`val` in valArray) {

            if (`val` is String) {

                // Hex num
                val hexVal = Pattern.matches("0[xX][0-9a-fA-F]+", `val`.toString())
                if (hexVal) {
                    var number = `val`.substring(2)
                    if (number.length % 2 == 1) number = "0" + number
                    val data = Hex.decode(number)
                    try {
                        bos.write(data)
                    } catch (e: IOException) {
                        logger.error("should not happen", e)
                    }

                } else {

                    // BigInt num
                    val isNumeric = Pattern.matches("[0-9a-fA-F]+", `val`.toString())
                    if (!isNumeric)
                        throw Error("Wrong test case JSON format")
                    else {
                        val value = BigInteger(`val`.toString())
                        try {
                            bos.write(value.toByteArray())
                        } catch (e: IOException) {
                            logger.error("should not happen", e)
                        }

                    }
                }
            } else if (`val` is Long) {

                // Simple long
                val data = ByteUtil.bigIntegerToBytes(BigInteger.valueOf(`val`))
                try {
                    bos.write(data)
                } catch (e: IOException) {
                    logger.error("should not happen", e)
                }

            } else {
                throw Error("Wrong test case JSON format")
            }
        }
        return bos.toByteArray()
    }
}
