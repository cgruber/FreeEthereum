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
class Exec/*
     e.g:
            "address" : "0f572e5295c57f15886f9b263e2f6d2d6c7b5ec6",
            "caller" : "cd1722f3947def4cf144679da39c4c32bdc35681",
            "data" : [
            ],

            "code" : [ 96,0,96,0,96,0,96,0,96,74,51,96,200,92,3,241 ],

            "gas" : 10000,
            "gasPrice" : 100000000000000,
            "origin" : "cd1722f3947def4cf144679da39c4c32bdc35681",
            "value" : 1000000000000000000
   */
(exec: JSONObject) {

    val address: ByteArray
    val caller: ByteArray
    val data: ByteArray
    val code: ByteArray

    val gas: ByteArray
    val gasPrice: ByteArray

    val origin: ByteArray
    val value: ByteArray

    init {

        val address = exec["address"].toString()
        val caller = exec["caller"].toString()

        val code = exec["code"].toString()
        val data = exec["data"].toString()

        val gas = exec["gas"].toString()
        val gasPrice = exec["gasPrice"].toString()
        val origin = exec["origin"].toString()

        val value = exec["value"].toString()

        this.address = Hex.decode(address)
        this.caller = Hex.decode(caller)

        if (code != null && code.length > 2)
            this.code = Hex.decode(code.substring(2))
        else
            this.code = ByteUtil.EMPTY_BYTE_ARRAY

        if (data != null && data.length > 2)
            this.data = Hex.decode(data.substring(2))
        else
            this.data = ByteUtil.EMPTY_BYTE_ARRAY

        this.gas = ByteUtil.bigIntegerToBytes(TestCase.toBigInt(gas))
        this.gasPrice = ByteUtil.bigIntegerToBytes(TestCase.toBigInt(gasPrice))

        this.origin = Hex.decode(origin)
        this.value = ByteUtil.bigIntegerToBytes(TestCase.toBigInt(value))
    }


    override fun toString(): String {
        return "Exec{" +
                "address=" + Hex.toHexString(address) +
                ", caller=" + Hex.toHexString(caller) +
                ", data=" + Hex.toHexString(data) +
                ", code=" + Hex.toHexString(data) +
                ", gas=" + Hex.toHexString(gas) +
                ", gasPrice=" + Hex.toHexString(gasPrice) +
                ", origin=" + Hex.toHexString(origin) +
                ", value=" + Hex.toHexString(value) +
                '}'
    }
}
