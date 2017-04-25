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

import org.ethereum.core.AccountState
import org.ethereum.core.BlockHeader
import org.ethereum.util.RLP
import org.ethereum.util.Value
import org.ethereum.vm.DataWord

/**
 * Collection of common Serializers
 * Created by Anton Nashatyrev on 08.11.2016.
 */
object Serializers {

    /**
     * Serializes/Deserializes AccountState instances from the State Trie (part of Ethereum spec)
     */
    val AccountStateSerializer: Serializer<AccountState, ByteArray> = object : Serializer<AccountState, ByteArray> {
        override fun serialize(`object`: AccountState): ByteArray {
            return `object`.encoded
        }

        override fun deserialize(stream: ByteArray?): AccountState? {
            return if (stream == null || stream.isEmpty()) null else AccountState(stream)
        }
    }
    /**
     * Contract storage key serializer
     */
    val StorageKeySerializer: Serializer<DataWord, ByteArray> = object : Serializer<DataWord, ByteArray> {
        override fun serialize(`object`: DataWord): ByteArray {
            return `object`.data
        }

        override fun deserialize(stream: ByteArray): DataWord {
            return DataWord(stream)
        }
    }
    /**
     * Contract storage value serializer (part of Ethereum spec)
     */
    val StorageValueSerializer: Serializer<DataWord, ByteArray> = object : Serializer<DataWord, ByteArray> {
        override fun serialize(`object`: DataWord): ByteArray {
            return RLP.encodeElement(`object`.noLeadZeroesData)
        }

        override fun deserialize(stream: ByteArray?): DataWord? {
            if (stream == null || stream.isEmpty()) return null
            val dataDecoded = RLP.decode2(stream)[0].rlpData
            return DataWord(dataDecoded)
        }
    }
    /**
     * Trie node serializer (part of Ethereum spec)
     */
    val TrieNodeSerializer: Serializer<Value, ByteArray> = object : Serializer<Value, ByteArray> {
        override fun serialize(`object`: Value): ByteArray {
            return `object`.encode()
        }

        override fun deserialize(stream: ByteArray): Value {
            return Value.fromRlpEncoded(stream)!!
        }
    }
    /**
     * Trie node serializer (part of Ethereum spec)
     */
    val BlockHeaderSerializer: Serializer<BlockHeader, ByteArray> = object : Serializer<BlockHeader, ByteArray> {
        override fun serialize(`object`: BlockHeader?): ByteArray? {
            return `object`?.encoded
        }

        override fun deserialize(stream: ByteArray?): BlockHeader? {
            return if (stream == null) null else BlockHeader(stream)
        }
    }

    /**
     * No conversion
     */
    class Identity<T> : Serializer<T, T> {
        override fun serialize(`object`: T): T {
            return `object`
        }

        override fun deserialize(stream: T): T {
            return stream
        }
    }
}
