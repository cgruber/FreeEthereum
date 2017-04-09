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

package org.ethereum.crypto.jce

import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Provider
import javax.crypto.KeyAgreement

object ECKeyAgreement {

    private val ALGORITHM = "ECDH"

    private val algorithmAssertionMsg = "Assumed the JRE supports EC key agreement"

    val instance: KeyAgreement
        get() {
            try {
                return KeyAgreement.getInstance(ALGORITHM)
            } catch (ex: NoSuchAlgorithmException) {
                throw AssertionError(algorithmAssertionMsg, ex)
            }

        }

    @Throws(NoSuchProviderException::class)
    fun getInstance(provider: String): KeyAgreement {
        try {
            return KeyAgreement.getInstance(ALGORITHM, provider)
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        }

    }

    fun getInstance(provider: Provider): KeyAgreement {
        try {
            return KeyAgreement.getInstance(ALGORITHM, provider)
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        }

    }
}
