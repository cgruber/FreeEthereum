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
import static org.junit.Assert.*;

/**
 * Testing {@link ReadWriteCache}
 */
public class ReadWriteCacheTest {

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

    @Test
    public void testSimple() {
        final Source<byte[], byte[]> src = new HashMapDB<>();
        final ReadWriteCache<byte[], byte[]> cache = new ReadWriteCache.BytesKey<>(src, WriteCache.CacheType.SIMPLE);

        for (int i = 0; i < 10_000; ++i) {
            cache.put(intToKey(i), intToValue(i));
        }

        // Everything is cached
        assertEquals(str(intToValue(0)), str(cache.getCached(intToKey(0)).value()));
        assertEquals(str(intToValue(9_999)), str(cache.getCached(intToKey(9_999)).value()));

        // Source is empty
        assertNull(src.get(intToKey(0)));
        assertNull(src.get(intToKey(9_999)));

        // After flush src is filled
        cache.flush();
        assertEquals(str(intToValue(9_999)), str(src.get(intToKey(9_999))));
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));

        // Deleting key that is currently in cache
        cache.put(intToKey(0), intToValue(12345));
        assertEquals(str(intToValue(12345)), str(cache.getCached(intToKey(0)).value()));
        cache.delete(intToKey(0));
        assertTrue(null == cache.getCached(intToKey(0)) || null == cache.getCached(intToKey(0)).value());
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        cache.flush();
        assertNull(src.get(intToKey(0)));

        // No size estimators
        assertEquals(0, cache.estimateCacheSize());
    }
}
