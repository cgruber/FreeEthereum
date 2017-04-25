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
 * Source for converting between different key/value types
 * Has no own state and immediately propagate all changes
 * to the backing Source with key/value conversion

 * Created by Anton Nashatyrev on 03.11.2016.
 */
open class SourceCodec<Key, Value, SourceKey, SourceValue>
/**
 * Instantiates class
 * @param src  Backing Source
 * *
 * @param keySerializer  Key codec Key <=> SourceKey
 * *
 * @param valSerializer  Value codec Value <=> SourceValue
 */
(src: Source<SourceKey, SourceValue>, private val keySerializer: Serializer<Key, SourceKey>, private val valSerializer: Serializer<Value, SourceValue>) : AbstractChainedSource<Key, Value, SourceKey, SourceValue>(src) {

    init {
        setFlushSource(true)
    }

    override fun put(key: Key, `val`: Value) {
        source.put(keySerializer.serialize(key), valSerializer.serialize(`val`))
    }

    override fun get(key: Key): Value {
        return valSerializer.deserialize(source[keySerializer.serialize(key)])
    }

    override fun delete(key: Key) {
        source.delete(keySerializer.serialize(key))
    }

    public override fun flushImpl(): Boolean {
        return false
    }

    /**
     * Shortcut class when only value conversion is required
     */
    open class ValueOnly<Key, Value, SourceValue>(src: Source<Key, SourceValue>, valSerializer: Serializer<Value, SourceValue>) : SourceCodec<Key, Value, Key, SourceValue>(src, Serializers.Identity<Key>(), valSerializer)

    /**
     * Shortcut class when only value conversion is required and keys are of byte[] type
     */
    class BytesKey<Value, SourceValue>(src: Source<ByteArray, SourceValue>, valSerializer: Serializer<Value, SourceValue>) : ValueOnly<ByteArray, Value, SourceValue>(src, valSerializer)
}
