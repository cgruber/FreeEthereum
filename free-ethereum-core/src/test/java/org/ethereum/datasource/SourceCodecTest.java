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

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.vm.DataWord;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.ethereum.util.ByteUtil.longToBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for {@link SourceCodec}
 */
public class SourceCodecTest {

    private byte[] intToKey(final int i) {
        return HashUtil.INSTANCE.sha3(longToBytes(i));
    }

    private byte[] intToValue(final int i) {
        return (new DataWord(i)).getData();
    }

    private DataWord intToDataWord(final int i) {
        return new DataWord(i);
    }

    private DataWord intToDataWordKey(final int i) {
        return new DataWord(intToKey(i));
    }

    private String str(final Object obj) {
        if (obj == null) return null;

        final byte[] data;
        if (obj instanceof DataWord) {
            data = ((DataWord) obj).getData();
        } else {
            data = (byte[]) obj;
        }

        return Hex.toHexString(data);
    }

    @Test
    public void testDataWordKeySerializer() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Serializer<DataWord, byte[]> keySerializer = Serializers.INSTANCE.getStorageKeySerializer();
        final Serializer<byte[], byte[]> valueSerializer = new Serializers.Identity<>();
        final SourceCodec<DataWord, byte[], byte[], byte[]> src = new SourceCodec<>(parentSrc, keySerializer, valueSerializer);

        for (int i = 0; i < 10_000; ++i) {
            src.put(intToDataWordKey(i), intToValue(i));
        }

        // Everything is in src
        assertEquals(str(intToValue(0)), str(src.get(intToDataWordKey(0))));
        assertEquals(str(intToValue(9_999)), str(src.get(intToDataWordKey(9_999))));

        // Modifying key
        src.put(intToDataWordKey(0), intToValue(12345));
        assertEquals(str(intToValue(12345)), str(src.get(intToDataWordKey(0))));

        // Testing there is no cache
        assertEquals(str(intToValue(9_990)), str(src.get(intToDataWordKey(9_990))));
        parentSrc.delete(keySerializer.serialize(intToDataWordKey(9_990)));
        assertNull(src.get(intToDataWordKey(9_990)));
    }

    @Test
    public void testDataWordKeyValueSerializer() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Serializer<DataWord, byte[]> keySerializer = Serializers.INSTANCE.getStorageKeySerializer();
        final Serializer<DataWord, byte[]> valueSerializer = Serializers.INSTANCE.getStorageValueSerializer();
        final SourceCodec<DataWord, DataWord, byte[], byte[]> src = new SourceCodec<>(parentSrc, keySerializer, valueSerializer);

        for (int i = 0; i < 10_000; ++i) {
            src.put(intToDataWordKey(i), intToDataWord(i));
        }

        // Everything is in src
        assertEquals(str(intToDataWord(0)), str(src.get(intToDataWordKey(0))));
        assertEquals(str(intToDataWord(9_999)), str(src.get(intToDataWordKey(9_999))));

        // Modifying key
        src.put(intToDataWordKey(0), intToDataWord(12345));
        assertEquals(str(intToDataWord(12345)), str(src.get(intToDataWordKey(0))));
    }
}
