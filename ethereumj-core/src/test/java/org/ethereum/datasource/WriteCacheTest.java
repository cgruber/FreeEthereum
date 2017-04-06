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
 * Testing {@link WriteCache}
 */
public class WriteCacheTest {

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
        final WriteCache<byte[], byte[]> writeCache = new WriteCache.BytesKey<>(src, WriteCache.CacheType.SIMPLE);
        for (int i = 0; i < 10_000; ++i) {
            writeCache.put(intToKey(i), intToValue(i));
        }
        // Everything is cached
        assertEquals(str(intToValue(0)), str(writeCache.getCached(intToKey(0)).value()));
        assertEquals(str(intToValue(9_999)), str(writeCache.getCached(intToKey(9_999)).value()));

        // Everything is flushed
        writeCache.flush();
        assertNull(writeCache.getCached(intToKey(0)));
        assertNull(writeCache.getCached(intToKey(9_999)));
        assertEquals(str(intToValue(9_999)), str(writeCache.get(intToKey(9_999))));
        assertEquals(str(intToValue(0)), str(writeCache.get(intToKey(0))));
        // Get not caches, only write cache
        assertNull(writeCache.getCached(intToKey(0)));

        // Deleting key that is currently in cache
        writeCache.put(intToKey(0), intToValue(12345));
        assertEquals(str(intToValue(12345)), str(writeCache.getCached(intToKey(0)).value()));
        writeCache.delete(intToKey(0));
        assertTrue(null == writeCache.getCached(intToKey(0)) || null == writeCache.getCached(intToKey(0)).value());
        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        writeCache.flush();
        assertNull(src.get(intToKey(0)));

        // Deleting key that is not currently in cache
        assertTrue(null == writeCache.getCached(intToKey(1)) || null == writeCache.getCached(intToKey(1)).value());
        assertEquals(str(intToValue(1)), str(src.get(intToKey(1))));
        writeCache.delete(intToKey(1));
        assertTrue(null == writeCache.getCached(intToKey(1)) || null == writeCache.getCached(intToKey(1)).value());
        assertEquals(str(intToValue(1)), str(src.get(intToKey(1))));
        writeCache.flush();
        assertNull(src.get(intToKey(1)));
    }

    @Test
    public void testCounting() {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Source<byte[], byte[]> src = new CountingBytesSource(parentSrc);
        final WriteCache<byte[], byte[]> writeCache = new WriteCache.BytesKey<>(src, WriteCache.CacheType.COUNTING);
        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j <= i; ++j) {
                writeCache.put(intToKey(i), intToValue(i));
            }
        }
        // Everything is cached
        assertEquals(str(intToValue(0)), str(writeCache.getCached(intToKey(0)).value()));
        assertEquals(str(intToValue(99)), str(writeCache.getCached(intToKey(99)).value()));

        // Everything is flushed
        writeCache.flush();
        assertNull(writeCache.getCached(intToKey(0)));
        assertNull(writeCache.getCached(intToKey(99)));
        assertEquals(str(intToValue(99)), str(writeCache.get(intToKey(99))));
        assertEquals(str(intToValue(0)), str(writeCache.get(intToKey(0))));

        // Deleting key which has 1 ref
        writeCache.delete(intToKey(0));

        // for counting cache we return the cached value even if
        // it was deleted (once or several times) as we don't know
        // how many 'instances' are left behind

        // but when we delete entry which is not in the cache we don't
        // want to spend unnecessary time for getting the value from
        // underlying storage, so getCached may return null.
        // get() should work as expected
//        assertEquals(str(intToValue(0)), str(writeCache.getCached(intToKey(0))));

        assertEquals(str(intToValue(0)), str(src.get(intToKey(0))));
        writeCache.flush();
        assertNull(writeCache.getCached(intToKey(0)));
        assertNull(src.get(intToKey(0)));

        // Deleting key which has 2 refs
        writeCache.delete(intToKey(1));
        writeCache.flush();
        assertEquals(str(intToValue(1)), str(writeCache.get(intToKey(1))));
        writeCache.delete(intToKey(1));
        writeCache.flush();
        assertNull(writeCache.get(intToKey(1)));
    }

    @Test
    public void testWithSizeEstimator() {
        final Source<byte[], byte[]> src = new HashMapDB<>();
        final WriteCache<byte[], byte[]> writeCache = new WriteCache.BytesKey<>(src, WriteCache.CacheType.SIMPLE);
        writeCache.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
        assertEquals(0, writeCache.estimateCacheSize());

        writeCache.put(intToKey(0), intToValue(0));
        assertNotEquals(0, writeCache.estimateCacheSize());
        final long oneObjSize = writeCache.estimateCacheSize();

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j <= i; ++j) {
                writeCache.put(intToKey(i), intToValue(i));
            }
        }
        assertEquals(oneObjSize * 100, writeCache.estimateCacheSize());

        writeCache.flush();
        assertEquals(0, writeCache.estimateCacheSize());
    }
}
