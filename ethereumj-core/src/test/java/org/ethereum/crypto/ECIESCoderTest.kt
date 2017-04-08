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

package org.ethereum.crypto

import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

class ECIESCoderTest {


    @Test // decrypt cpp data
    fun test1() {
        val privKey = BigInteger("5e173f6ac3c669587538e7727cf19b782a4f2fda07c1eaa662c593e5e85e3051", 16)
        val cipher = Hex.decode("049934a7b2d7f9af8fd9db941d9da281ac9381b5740e1f64f7092f3588d4f87f5ce55191a6653e5e80c1c5dd538169aa123e70dc6ffc5af1827e546c0e958e42dad355bcc1fcb9cdf2cf47ff524d2ad98cbf275e661bf4cf00960e74b5956b799771334f426df007350b46049adb21a6e78ab1408d5e6ccde6fb5e69f0f4c92bb9c725c02f99fa72b9cdc8dd53cff089e0e73317f61cc5abf6152513cb7d833f09d2851603919bf0fbe44d79a09245c6e8338eb502083dc84b846f2fee1cc310d2cc8b1b9334728f97220bb799376233e113")

        var payload = ByteArray(0)
        try {
            payload = ECIESCoder.decrypt(privKey, cipher)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        Assert.assertEquals("802b052f8b066640bba94a4fc39d63815c377fced6fcb84d27f791c9921ddf3e9bf0108e298f490812847109cbd778fae393e80323fd643209841a3b7f110397f37ec61d84cea03dcc5e8385db93248584e8af4b4d1c832d8c7453c0089687a700",
                Hex.toHexString(payload))
    }


    @Test // encrypt decrypt round trip
    fun test2() {

        val privKey = BigInteger("5e173f6ac3c669587538e7727cf19b782a4f2fda07c1eaa662c593e5e85e3051", 16)

        val payload = Hex.decode("1122334455")

        val ecKey = ECKey.fromPrivate(privKey)
        val pubKeyPoint = ecKey.pubKeyPoint

        var cipher = ByteArray(0)
        try {
            cipher = ECIESCoder.encrypt(pubKeyPoint, payload)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        println(Hex.toHexString(cipher))

        var decrypted_payload = ByteArray(0)
        try {
            decrypted_payload = ECIESCoder.decrypt(privKey, cipher)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        println(Hex.toHexString(decrypted_payload))
    }

}
