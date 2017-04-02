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

package org.ethereum

import org.spongycastle.crypto.DataLengthException
import org.spongycastle.crypto.DerivationParameters
import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.DigestDerivationFunction
import org.spongycastle.crypto.params.ISO18033KDFParameters
import org.spongycastle.crypto.params.KDFParameters
import org.spongycastle.util.Pack

/**
 * Basic KDF generator for derived keys and ivs as defined by NIST SP 800-56A.
 */
class ConcatKDFBytesGenerator
/**
 * Construct a KDF Parameters generator.
 *
 *

 * @param counterStart
 * *            value of counter.
 * *
 * @param digest
 * *            the digest to be used as the source of derived keys.
 */
private constructor(private val counterStart: Int, private val digest: Digest) : DigestDerivationFunction {
    private var shared: ByteArray? = null
    private var iv: ByteArray? = null

    constructor(digest: Digest) : this(1, digest)

    override fun init(param: DerivationParameters) {
        if (param is KDFParameters) {
            val p = param

            shared = p.sharedSecret
            iv = p.iv
        } else if (param is ISO18033KDFParameters) {

            shared = param.seed
            iv = null
        } else {
            throw IllegalArgumentException("KDF parameters required for KDF2Generator")
        }
    }

    /**
     * return the underlying digest.
     */
    override fun getDigest(): Digest {
        return digest
    }

    /**
     * fill len bytes of the output buffer with bytes generated from the
     * derivation function.

     * @throws IllegalArgumentException
     * *             if the size of the request will cause an overflow.
     * *
     * @throws DataLengthException
     * *             if the out buffer is too small.
     */
    @Throws(DataLengthException::class, IllegalArgumentException::class)
    override fun generateBytes(out: ByteArray, outOff: Int, len: Int): Int {
        var outOff = outOff
        var len = len
        if (out.size - len < outOff) {
            throw DataLengthException("output buffer too small")
        }

        val oBytes = len.toLong()
        val outLen = digest.digestSize

        //
        // this is at odds with the standard implementation, the
        // maximum value should be hBits * (2^32 - 1) where hBits
        // is the digest output size in bits. We can't have an
        // array with a long index at the moment...
        //
        if (oBytes > (2L shl 32) - 1) {
            throw IllegalArgumentException("Output length too large")
        }

        val cThreshold = ((oBytes + outLen - 1) / outLen).toInt()

        val dig = ByteArray(digest.digestSize)

        val C = ByteArray(4)
        Pack.intToBigEndian(counterStart, C, 0)

        var counterBase = counterStart and 0xFF.inv()

        for (i in 0..cThreshold - 1) {
            digest.update(C, 0, C.size)
            digest.update(shared, 0, shared!!.size)

            if (iv != null) {
                digest.update(iv, 0, iv!!.size)
            }

            digest.doFinal(dig, 0)

            if (len > outLen) {
                System.arraycopy(dig, 0, out, outOff, outLen)
                outOff += outLen
                len -= outLen
            } else {
                System.arraycopy(dig, 0, out, outOff, len)
            }

            if ((++C[3]).toInt() == 0) {
                counterBase += 0x100
                Pack.intToBigEndian(counterBase, C, 0)
            }
        }

        digest.reset()

        return oBytes.toInt()
    }
}
