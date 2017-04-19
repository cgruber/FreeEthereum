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

import org.spongycastle.crypto.DataLengthException
import org.spongycastle.crypto.DerivationFunction
import org.spongycastle.crypto.DerivationParameters
import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.params.MGFParameters

/**
 * This class is borrowed from spongycastle project
 * The only change made is addition of 'counterStart' parameter to
 * conform to Crypto++ capabilities
 */
internal class MGF1BytesGeneratorExt(val digest: Digest, private val counterStart: Int) : DerivationFunction {
    private val hLen: Int = digest.digestSize
    private var seed: ByteArray? = null

    override fun init(param: DerivationParameters) {
        if (param !is MGFParameters) {
            throw IllegalArgumentException("MGF parameters required for MGF1Generator")
        } else {
            this.seed = param.seed
        }
    }

    private fun ItoOSP(i: Int, sp: ByteArray) {
        sp[0] = i.ushr(24).toByte()
        sp[1] = i.ushr(16).toByte()
        sp[2] = i.ushr(8).toByte()
        sp[3] = i.toByte()
    }

    @Throws(DataLengthException::class, IllegalArgumentException::class)
    override fun generateBytes(out: ByteArray, outOff: Int, len: Int): Int {
        if (out.size - len < outOff) {
            throw DataLengthException("output buffer too small")
        } else {
            val hashBuf = ByteArray(this.hLen)
            val C = ByteArray(4)
            var counter = 0
            var hashCounter = counterStart
            this.digest.reset()
            if (len > this.hLen) {
                do {
                    this.ItoOSP(hashCounter++, C)
                    this.digest.update(this.seed, 0, this.seed!!.size)
                    this.digest.update(C, 0, C.size)
                    this.digest.doFinal(hashBuf, 0)
                    System.arraycopy(hashBuf, 0, out, outOff + counter * this.hLen, this.hLen)
                    ++counter
                } while (counter < len / this.hLen)
            }

            if (counter * this.hLen < len) {
                this.ItoOSP(hashCounter, C)
                this.digest.update(this.seed, 0, this.seed!!.size)
                this.digest.update(C, 0, C.size)
                this.digest.doFinal(hashBuf, 0)
                System.arraycopy(hashBuf, 0, out, outOff + counter * this.hLen, len - counter * this.hLen)
            }

            return len
        }
    }
}
