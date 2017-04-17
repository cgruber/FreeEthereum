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
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.util.encoders.Hex

/**
 * Authentication response message, to be wrapped inside

 * Created by devrandom on 2015-04-07.
 */
class AuthResponseMessage {
    var ephemeralPublicKey: ECPoint? = null // 64 bytes - uncompressed and no type byte
    var nonce: ByteArray? = null // 32 bytes
    var isTokenUsed: Boolean = false // 1 byte - 0x00 or 0x01

    fun encode(): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        val publicBytes = ephemeralPublicKey?.getEncoded(false)
        System.arraycopy(publicBytes, 1, buffer, offset, publicBytes?.size!! - 1)
        offset += publicBytes.size - 1
        System.arraycopy(nonce, 0, buffer, offset, nonce?.size!!)
        offset += nonce!!.size
        buffer[offset] = (if (isTokenUsed) 0x01 else 0x00).toByte()
        offset += 1
        return buffer
    }

    override fun toString(): String {
        return "AuthResponseMessage{" +
                "\n  ephemeralPublicKey=" + ephemeralPublicKey +
                "\n  nonce=" + Hex.toHexString(nonce) +
                "\n  isTokenUsed=" + isTokenUsed +
                '}'
    }

    companion object {

        fun decode(wire: ByteArray): AuthResponseMessage {
            var offset = 0
            val message = AuthResponseMessage()
            val bytes = ByteArray(65)
            System.arraycopy(wire, offset, bytes, 1, 64)
            offset += 64
            bytes[0] = 0x04 // uncompressed
            message.ephemeralPublicKey = ECKey.CURVE.curve.decodePoint(bytes)
            message.nonce = ByteArray(32)
            System.arraycopy(wire, offset, message.nonce, 0, 32)
            offset += message.nonce!!.size
            val tokenUsed = wire[offset]
            offset += 1
            if (tokenUsed.toInt() != 0x00 && tokenUsed.toInt() != 0x01)
                throw RuntimeException("invalid boolean") // TODO specific exception
            message.isTokenUsed = tokenUsed.toInt() == 0x01
            return message
        }

        val length: Int
            get() = 64 + 32 + 1
    }
}
