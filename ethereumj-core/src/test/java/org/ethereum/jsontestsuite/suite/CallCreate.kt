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
import org.json.simple.JSONObject
import org.spongycastle.util.encoders.Hex

/**
 * @author Roman Mandeleil
 * *
 * @since 28.06.2014
 */
internal class CallCreate
/* e.g.
        "data" : [
                ],
        "destination" : "cd1722f3947def4cf144679da39c4c32bdc35681",
        "gasLimit" : 9792,
        "value" : 74
*/
(callCreateJSON: JSONObject) {

    val data: ByteArray
    val destination: ByteArray
    val gasLimit: ByteArray
    val value: ByteArray

    init {

        val data = callCreateJSON["data"].toString()
        val destination = callCreateJSON["destination"].toString()
        val gasLimit = callCreateJSON["gasLimit"].toString()
        val value = callCreateJSON["value"].toString()

        if (data != null && data.length > 2)
            this.data = Hex.decode(data.substring(2))
        else
            this.data = ByteUtil.EMPTY_BYTE_ARRAY

        this.destination = Hex.decode(destination)
        this.gasLimit = ByteUtil.bigIntegerToBytes(TestCase.toBigInt(gasLimit))
        this.value = ByteUtil.bigIntegerToBytes(TestCase.toBigInt(value))
    }

    override fun toString(): String {
        return "CallCreate{" +
                "data=" + Hex.toHexString(data) +
                ", destination=" + Hex.toHexString(destination) +
                ", gasLimit=" + Hex.toHexString(gasLimit) +
                ", value=" + Hex.toHexString(value) +
                '}'
    }
}
