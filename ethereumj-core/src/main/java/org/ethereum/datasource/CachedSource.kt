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

/**
 * Source which internally caches underlying Source key-value pairs

 * Created by Anton Nashatyrev on 21.10.2016.
 */
interface CachedSource<Key, Value> : Source<Key, Value> {

    /**
     * @return The underlying Source
     */
    val source: Source<Key, Value>

    /**
     * @return Modified entry keys if this is a write cache
     */
    val modified: Collection<Key>

    /**
     * @return indicates the cache has modified entries
     */
    fun hasModified(): Boolean

    /**
     * Estimates the size of cached entries in bytes.
     * This value shouldn't be precise size of Java objects
     * @return cache size in bytes
     */
    fun estimateCacheSize(): Long

    /**
     * Just a convenient shortcut to the most popular Sources with byte[] key
     */
    interface BytesKey<Value> : CachedSource<ByteArray, Value>
}
