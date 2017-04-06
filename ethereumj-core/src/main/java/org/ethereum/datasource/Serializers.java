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

package org.ethereum.datasource;

import org.ethereum.core.AccountState;
import org.ethereum.core.BlockHeader;
import org.ethereum.util.RLP;
import org.ethereum.util.Value;
import org.ethereum.vm.DataWord;

/**
 * Collection of common Serializers
 * Created by Anton Nashatyrev on 08.11.2016.
 */
public class Serializers {

    /**
     * Serializes/Deserializes AccountState instances from the State Trie (part of Ethereum spec)
     */
    public final static Serializer<AccountState, byte[]> AccountStateSerializer = new Serializer<AccountState, byte[]>() {
        @Override
        public byte[] serialize(final AccountState object) {
            return object.getEncoded();
        }

        @Override
        public AccountState deserialize(final byte[] stream) {
            return stream == null || stream.length == 0 ? null : new AccountState(stream);
        }
    };
    /**
     * Contract storage key serializer
     */
    public final static Serializer<DataWord, byte[]> StorageKeySerializer = new Serializer<DataWord, byte[]>() {
        @Override
        public byte[] serialize(final DataWord object) {
            return object.getData();
        }

        @Override
        public DataWord deserialize(final byte[] stream) {
            return new DataWord(stream);
        }
    };
    /**
     * Contract storage value serializer (part of Ethereum spec)
     */
    public final static Serializer<DataWord, byte[]> StorageValueSerializer = new Serializer<DataWord, byte[]>() {
        @Override
        public byte[] serialize(final DataWord object) {
            return RLP.encodeElement(object.getNoLeadZeroesData());
        }

        @Override
        public DataWord deserialize(final byte[] stream) {
            if (stream == null || stream.length == 0) return null;
            final byte[] dataDecoded = RLP.decode2(stream).get(0).getRLPData();
            return new DataWord(dataDecoded);
        }
    };
    /**
     * Trie node serializer (part of Ethereum spec)
     */
    public final static Serializer<Value, byte[]> TrieNodeSerializer = new Serializer<Value, byte[]>() {
        @Override
        public byte[] serialize(final Value object) {
            return object.encode();
        }

        @Override
        public Value deserialize(final byte[] stream) {
            return Value.fromRlpEncoded(stream);
        }
    };
    /**
     * Trie node serializer (part of Ethereum spec)
     */
    public final static Serializer<BlockHeader, byte[]> BlockHeaderSerializer = new Serializer<BlockHeader, byte[]>() {
        @Override
        public byte[] serialize(final BlockHeader object) {
            return object == null ? null : object.getEncoded();
        }

        @Override
        public BlockHeader deserialize(final byte[] stream) {
            return stream == null ? null : new BlockHeader(stream);
        }
    };

    /**
     * No conversion
     */
    public static class Identity<T> implements Serializer<T, T> {
        @Override
        public T serialize(final T object) {
            return object;
        }

        @Override
        public T deserialize(final T stream) {
            return stream;
        }
    }
}
