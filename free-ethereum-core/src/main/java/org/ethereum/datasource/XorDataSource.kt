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

package org.ethereum.datasource

import org.ethereum.util.ByteUtil

/**
 * When propagating changes to the backing Source XORs keys
 * with the specified value

 * May be useful for merging several Sources into a single

 * Created by Anton Nashatyrev on 18.02.2016.
 */
class XorDataSource<V>
/**
 * Creates instance with a value all keys are XORed with
 */
(source: Source<ByteArray, V>, private val subKey: ByteArray) : AbstractChainedSource<ByteArray, V, ByteArray, V>(source) {

    private fun convertKey(key: ByteArray): ByteArray {
        return ByteUtil.xorAlignRight(key, subKey)
    }

    override fun get(key: ByteArray): V {
        return source[convertKey(key)]
    }

    override fun put(key: ByteArray, value: V) {
        source.put(convertKey(key), value)
    }

    override fun delete(key: ByteArray) {
        source.delete(convertKey(key))
    }

    override fun flushImpl(): Boolean {
        return false
    }
}
