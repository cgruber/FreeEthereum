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

package org.ethereum.net.eth

import java.util.*

/**
 * Represents supported Eth versions

 * @author Mikhail Kalinin
 * *
 * @since 14.08.2015
 */
enum class EthVersion constructor(val code: Byte) {

    V62(62.toByte()),
    V63(63.toByte());

    fun isCompatible(version: EthVersion): Boolean {

        if (version.code >= V62.code) {
            return this.code >= V62.code
        } else {
            return this.code < V62.code
        }
    }

    companion object {

        val UPPER = V63.code
        private val LOWER = V62.code

        fun fromCode(code: Int): EthVersion? {
            for (v in values()) {
                if (v.code.toInt() == code) {
                    return v
                }
            }

            return null
        }

        private fun isSupported(code: Byte): Boolean {
            return code >= LOWER && code <= UPPER
        }

        fun supported(): List<EthVersion> {
            val supported = ArrayList<EthVersion>()
            for (v in values()) {
                if (isSupported(v.code)) {
                    supported.add(v)
                }
            }

            return supported
        }
    }
}
