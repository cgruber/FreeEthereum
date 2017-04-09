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
package org.ethereum.crypto.cryptohash

import org.junit.Assert.assertArrayEquals
import org.spongycastle.util.encoders.Hex

/**
 * Generic test utility class that gets extended from the digest test
 * classes.
 * @author Stephan Fuhrmann &lt;stephan@tynne.de&gt;
 */
open class AbstractCryptoTest {

    /** Does the comparison using the digest and some calls on it.
     * @param digest the digest to operate on.
     * *
     * @param message the input data to pass to the digest.
     * *
     * @param expected the expected data out of the digest.
     */
    private fun testFrom(digest: Digest, message: ByteArray, expected: ByteArray) {
        /*
         * First test the hashing itself.
         */
        val out = digest.digest(message)
        assertArrayEquals(expected, out)

        /*
         * Now the update() API; this also exercises auto-reset.
         */
        for (aMessage in message) {
            digest.update(aMessage)
        }
        assertArrayEquals(expected, digest.digest())

        /*
         * The cloning API.
         */
        val blen = message.size
        digest.update(message, 0, blen / 2)
        val dig2 = digest.copy()
        digest.update(message, blen / 2, blen - blen / 2)
        assertArrayEquals(expected, digest.digest())
        dig2.update(message, blen / 2, blen - blen / 2)
        assertArrayEquals(expected, dig2.digest())
    }

    internal fun testKatHex(dig: Digest, data: String, ref: String) {
        testFrom(dig, Hex.decode(data), Hex.decode(ref))
    }
}