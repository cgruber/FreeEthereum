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

import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.vm.DataWord;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.util.ByteUtil.longToBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for {@link ObjectDataSource}
 */
public class ObjectDataSourceTest {

    private byte[] intToKey(final int i) {
        return sha3(longToBytes(i));
    }

    private byte[] intToValue(final int i) {
        return (new DataWord(i)).getData();
    }

    private DataWord intToDataWord(final int i) {
        return new DataWord(i);
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
    public void testDummySerializer() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Serializer<byte[], byte[]> serializer = new Serializers.Identity<>();
        final ObjectDataSource<byte[]> src = new ObjectDataSource<>(parentSrc, serializer, 256);

        for (int i = 0; i < 10_000; ++i) {
            src.put(intToKey(i), intToValue(i));
        }
        // Everything is in src and parentSrc w/o flush
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        assertEquals(str(intToValue(9_999)), str(src.get(intToKey(9_999))));
        assertEquals(str(intToValue(0)), str(parentSrc.get(intToKey(0))));
        assertEquals(str(intToValue(9_999)), str(parentSrc.get(intToKey(9_999))));

        // Testing read cache is available
        parentSrc.delete(intToKey(9_999));
        assertEquals(str(intToValue(9_999)), str(src.get(intToKey(9_999))));
        src.delete(intToKey(9_999));

        // Testing src delete invalidates read cache
        src.delete(intToKey(9_998));
        assertNull(src.get(intToKey(9_998)));

        // Modifying key
        src.put(intToKey(0), intToValue(12345));
        assertEquals(str(intToValue(12345)), str(src.get(intToKey(0))));
        assertEquals(str(intToValue(12345)), str(parentSrc.get(intToKey(0))));
    }

    @Test
    public void testDataWordValueSerializer() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Serializer<DataWord, byte[]> serializer = Serializers.StorageValueSerializer;
        final ObjectDataSource<DataWord> src = new ObjectDataSource<>(parentSrc, serializer, 256);

        for (int i = 0; i < 10_000; ++i) {
            src.put(intToKey(i), intToDataWord(i));
        }

        // Everything is in src
        assertEquals(str(intToDataWord(0)), str(src.get(intToKey(0))));
        assertEquals(str(intToDataWord(9_999)), str(src.get(intToKey(9_999))));

        // Modifying key
        src.put(intToKey(0), intToDataWord(12345));
        assertEquals(str(intToDataWord(12345)), str(src.get(intToKey(0))));
    }
}
