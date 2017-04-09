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

import org.ethereum.crypto.ECIESCoder
import org.ethereum.crypto.ECKey
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger


/**
 * @author Roman Mandeleil
 * *
 * @since 08.02.2015
 */
class CryptoTestCase {

    var decryption_type = ""
    var key = ""
    var cipher = ""
    var payload = ""


    fun execute() {

        val key = Hex.decode(this.key)
        val cipher = Hex.decode(this.cipher)

        val ecKey = ECKey.fromPrivate(key)

        var resultPayload = ByteArray(0)
        if (decryption_type == "aes_ctr")
            resultPayload = ecKey.decryptAES(cipher)

        if (decryption_type == "ecies_sec1_altered")
            try {
                resultPayload = ECIESCoder.decrypt(BigInteger(Hex.toHexString(key), 16), cipher)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

        if (Hex.toHexString(resultPayload) != payload) {
            val error = String.format("payload should be: %s, but got that result: %s  ",
                    payload, Hex.toHexString(resultPayload))
            logger.info(error)

            System.exit(-1)
        }
    }

    override fun toString(): String {
        return "CryptoTestCase{" +
                "decryption_type='" + decryption_type + '\'' +
                ", key='" + key + '\'' +
                ", cipher='" + cipher + '\'' +
                ", payload='" + payload + '\'' +
                '}'
    }

    companion object {

        private val logger = LoggerFactory.getLogger("TCK-Test")
    }
}
