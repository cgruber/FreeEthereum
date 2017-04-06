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
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.util.ByteUtil.longToBytes;
import static org.junit.Assert.*;

/**
 * Test for {@link CountingBytesSource}
 */
public class CountingBytesSourceTest {

    private Source<byte[], byte[]> src;

    private byte[] intToKey(final int i) {
        return sha3(longToBytes(i));
    }

    private byte[] intToValue(final int i) {
        return (new DataWord(i)).getData();
    }

    private String str(final Object obj) {
        if (obj == null) return null;
        return Hex.toHexString((byte[]) obj);
    }


    @Before
    public void setUp() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        this.src = new CountingBytesSource(parentSrc);
    }

    @Test(expected = NullPointerException.class)
    public void testKeyNull() {
        src.put(null, null);
    }

    @Test
    public void testValueNull() {
        src.put(intToKey(0), null);
        assertNull(src.get(intToKey(0)));
    }

    @Test
    public void testDelete() {
        src.put(intToKey(0), intToValue(0));
        src.delete(intToKey(0));
        assertNull(src.get(intToKey(0)));

        src.put(intToKey(0), intToValue(0));
        src.put(intToKey(0), intToValue(0));
        src.delete(intToKey(0));
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        src.delete(intToKey(0));
        assertNull(src.get(intToKey(0)));

        src.put(intToKey(1), intToValue(1));
        src.put(intToKey(1), intToValue(1));
        src.put(intToKey(1), null);
        assertEquals(str(intToValue(1)), str(src.get(intToKey(1))));
        src.put(intToKey(1), null);
        assertNull(src.get(intToKey(1)));

        src.put(intToKey(1), intToValue(1));
        src.put(intToKey(1), intToValue(2));
        src.delete(intToKey(1));
        assertEquals(str(intToValue(2)), str(src.get(intToKey(1))));
        src.delete(intToKey(1));
        assertNull(src.get(intToKey(1)));
    }

    @Test
    public void testALotRefs() {
        for (int i = 0; i < 100_000; ++i) {
            src.put(intToKey(0), intToValue(0));
        }

        for (int i = 0; i < 99_999; ++i) {
            src.delete(intToKey(0));
            assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        }
        src.delete(intToKey(0));
        assertNull(src.get(intToKey(0)));
    }

    @Test
    public void testFlushDoNothing() {
        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j <= i; ++j) {
                src.put(intToKey(i), intToValue(i));
            }
        }
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        assertEquals(str(intToValue(99)), str(src.get(intToKey(99))));
        assertFalse(src.flush());
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        assertEquals(str(intToValue(99)), str(src.get(intToKey(99))));
    }

    @Test
    public void testEmptyValue() {
        final byte[] value = new byte[0];
        src.put(intToKey(0), value);
        src.put(intToKey(0), value);
        src.delete(intToKey(0));
        assertEquals(str(value), str(src.get(intToKey(0))));
        src.delete(intToKey(0));
        assertNull(src.get(intToKey(0)));
    }
}
