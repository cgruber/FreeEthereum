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

import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Provider

object ECKeyFactory {

    private val ALGORITHM = "EC"

    private val algorithmAssertionMsg = "Assumed the JRE supports EC key factories"

    val instance: KeyFactory
        get() = Holder.INSTANCE

    @Throws(NoSuchProviderException::class)
    fun getInstance(provider: String): KeyFactory {
        try {
            return KeyFactory.getInstance(ALGORITHM, provider)
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        }

    }

    fun getInstance(provider: Provider): KeyFactory {
        try {
            return KeyFactory.getInstance(ALGORITHM, provider)
        } catch (ex: NoSuchAlgorithmException) {
            throw AssertionError(algorithmAssertionMsg, ex)
        }

    }

    private object Holder {
        val INSTANCE: KeyFactory

        init {
            try {
                INSTANCE = KeyFactory.getInstance(ALGORITHM)
            } catch (ex: NoSuchAlgorithmException) {
                throw AssertionError(algorithmAssertionMsg, ex)
            }

        }
    }
}
