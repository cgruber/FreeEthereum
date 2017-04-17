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

package org.ethereum.net.rlpx

import org.ethereum.crypto.ECKey
import org.ethereum.util.ByteUtil
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.util.encoders.Hex

/**
 * Auth Response message defined by EIP-8

 * @author mkalinin
 * *
 * @since 17.02.2016
 */
class AuthResponseMessageV4 {

    var ephemeralPublicKey: ECPoint? = null // 64 bytes - uncompressed and no type byte
    var nonce: ByteArray? = null // 32 bytes
    internal var version = 4 // 4 bytes

    fun encode(): ByteArray {

        val publicKey = ByteArray(64)
        System.arraycopy(ephemeralPublicKey!!.getEncoded(false), 1, publicKey, 0, publicKey.size)

        val publicBytes = RLP.encode(publicKey)
        val nonceBytes = RLP.encode(nonce)
        val versionBytes = RLP.encodeInt(version)

        return RLP.encodeList(publicBytes, nonceBytes, versionBytes)
    }

    override fun toString(): String {
        return "AuthResponseMessage{" +
                "\n  ephemeralPublicKey=" + ephemeralPublicKey +
                "\n  nonce=" + Hex.toHexString(nonce) +
                "\n  version=" + version +
                '}'
    }

    companion object {

        fun decode(wire: ByteArray): AuthResponseMessageV4 {

            val message = AuthResponseMessageV4()

            val params = RLP.decode2OneItem(wire, 0) as RLPList

            val pubKeyBytes = params[0].rlpData

            val bytes = ByteArray(65)
            System.arraycopy(pubKeyBytes, 0, bytes, 1, 64)
            bytes[0] = 0x04 // uncompressed
            message.ephemeralPublicKey = ECKey.CURVE.curve.decodePoint(bytes)

            message.nonce = params[1].rlpData

            val versionBytes = params[2].rlpData
            message.version = ByteUtil.byteArrayToInt(versionBytes)

            return message
        }
    }
}
