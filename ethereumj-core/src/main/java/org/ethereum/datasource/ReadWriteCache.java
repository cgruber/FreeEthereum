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

import java.util.Collection;

/**
 * Facade class which encapsulates both Read and Write caches
 *
 * Created by Anton Nashatyrev on 29.11.2016.
 */
public class ReadWriteCache<Key, Value>
        extends SourceChainBox<Key, Value, Key, Value>
        implements CachedSource<Key, Value> {

    ReadCache<Key, Value> readCache;
    WriteCache<Key, Value> writeCache;

    ReadWriteCache(final Source<Key, Value> source) {
        super(source);
    }

    public ReadWriteCache(final Source<Key, Value> src, final WriteCache.CacheType cacheType) {
        super(src);
        add(writeCache = new WriteCache<>(src, cacheType));
        add(readCache = new ReadCache<>(writeCache));
        readCache.setFlushSource(true);
    }

    @Override
    public synchronized Collection<Key> getModified() {
        return writeCache.getModified();
    }

    @Override
    public boolean hasModified() {
        return writeCache.hasModified();
    }

    synchronized AbstractCachedSource.Entry<Value> getCached(final Key key) {
        AbstractCachedSource.Entry<Value> v = readCache.getCached(key);
        if (v == null) {
            v = writeCache.getCached(key);
        }
        return v;
    }

    @Override
    public synchronized long estimateCacheSize() {
        return readCache.estimateCacheSize() + writeCache.estimateCacheSize();
    }

    public static class BytesKey<V> extends ReadWriteCache<byte[], V> {
        public BytesKey(final Source<byte[], V> src, final WriteCache.CacheType cacheType) {
            super(src);
            add(this.writeCache = new WriteCache.BytesKey<>(src, cacheType));
            add(this.readCache = new ReadCache.BytesKey<>(writeCache));
            readCache.setFlushSource(true);
        }
    }
}
