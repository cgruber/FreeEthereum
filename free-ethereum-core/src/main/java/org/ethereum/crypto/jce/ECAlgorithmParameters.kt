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

import java.io.IOException
import java.security.AlgorithmParameters
import java.security.NoSuchAlgorithmException
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.InvalidParameterSpecException

internal object ECAlgorithmParameters {

    private val ALGORITHM = "EC"
    private val CURVE_NAME = "secp256k1"

    val parameterSpec: ECParameterSpec
        get() {
            try {
                return Holder.INSTANCE.getParameterSpec(ECParameterSpec::class.java)
            } catch (ex: InvalidParameterSpecException) {
                throw AssertionError(
                        "Assumed correct key spec statically", ex)
            }

        }

    val asN1Encoding: ByteArray
        get() {
            try {
                return Holder.INSTANCE.encoded
            } catch (ex: IOException) {
                throw AssertionError(
                        "Assumed algo params has been initialized", ex)
            }

        }

    private object Holder {
        val INSTANCE: AlgorithmParameters

        private val SECP256K1_CURVE = ECGenParameterSpec(CURVE_NAME)

        init {
            try {
                INSTANCE = AlgorithmParameters.getInstance(ALGORITHM)
                INSTANCE.init(SECP256K1_CURVE)
            } catch (ex: NoSuchAlgorithmException) {
                throw AssertionError(
                        "Assumed the JRE supports EC algorithm params", ex)
            } catch (ex: InvalidParameterSpecException) {
                throw AssertionError(
                        "Assumed correct key spec statically", ex)
            }

        }
    }
}
