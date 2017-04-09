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

import java.security.*
import java.security.spec.ECGenParameterSpec

object ECKeyPairGenerator {

    private val ALGORITHM = "EC"
    private val CURVE_NAME = "secp256k1"

    private val algorithmAssertionMsg = "Assumed JRE supports EC key pair generation"

    private val keySpecAssertionMsg = "Assumed correct key spec statically"

    private val SECP256K1_CURVE = ECGenParameterSpec(CURVE_NAME)

    fun generateKeyPair(): KeyPair {
        return Holder.INSTANCE.generateKeyPair()
    }

    @Throws(NoSuchProviderException::class)
    fun getInstance(provider: String, random: SecureRandom): KeyPairGenerator {
        try {
            val gen = KeyPairGenerator.getInstance(ALGORITHM, provider)
            gen.initialize(SECP256K1_CURVE, random)
            return gen
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        } catch (ex: InvalidAlgorithmParameterException) {
            throw AssertionError(keySpecAssertionMsg, ex)
        }

    }

    fun getInstance(provider: Provider, random: SecureRandom): KeyPairGenerator {
        try {
            val gen = KeyPairGenerator.getInstance(ALGORITHM, provider)
            gen.initialize(SECP256K1_CURVE, random)
            return gen
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        } catch (ex: InvalidAlgorithmParameterException) {
            throw AssertionError(keySpecAssertionMsg, ex)
        }

    }

    private object Holder {
        val INSTANCE: KeyPairGenerator

        init {
            try {
                INSTANCE = KeyPairGenerator.getInstance(ALGORITHM)
                INSTANCE.initialize(SECP256K1_CURVE)
            } catch (ex: NoSuchAlgorithmException) {
                throw AssertionError(algorithmAssertionMsg, ex)
            } catch (ex: InvalidAlgorithmParameterException) {
                throw AssertionError(keySpecAssertionMsg, ex)
            }

        }
    }
}
