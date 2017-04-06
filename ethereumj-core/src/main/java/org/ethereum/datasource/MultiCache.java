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

/**
 * Cache of Caches (child caches)
 * When a child cache is not found in the local cache it is looked up in the backing Source
 * Based on this child backing cache (or null if not found) the new local cache is created
 * via create() method
 *
 * When flushing children, each child is just flushed if it has backing Source or the whole
 * child cache is put to the MultiCache backing source
 *
 * The primary goal if for caching contract storages in the child repositories (tracks)
 *
 * Created by Anton Nashatyrev on 07.10.2016.
 */
public abstract class MultiCache<V extends CachedSource> extends ReadWriteCache.BytesKey<V> {

    public MultiCache(final Source<byte[], V> src) {
        super(src, WriteCache.CacheType.SIMPLE);
    }

    /**
     * When a child cache is not found in the local cache it is looked up in the backing Source
     * Based on this child backing cache (or null if not found) the new local cache is created
     * via create() method
     */
    @Override
    public synchronized V get(final byte[] key) {
        final AbstractCachedSource.Entry<V> ownCacheEntry = getCached(key);
        V ownCache = ownCacheEntry == null ? null : ownCacheEntry.value();
        if (ownCache == null) {
            final V v = getSource() != null ? super.get(key) : null;
            ownCache = create(key, v);
            put(key, ownCache);
        }
        return ownCache;
    }

    /**
     * each child is just flushed if it has backing Source or the whole
     * child cache is put to the MultiCache backing source
     */
    @Override
    public synchronized boolean flushImpl() {
        boolean ret = false;
        for (final byte[] key : writeCache.getModified()) {
            final V value = super.get(key);
            if (value == null) {
                // cache was deleted
                ret |= flushChild(key, value);
                if (getSource() != null) {
                    getSource().delete(key);
                }
            } else if (value.getSource() != null){
                ret |= flushChild(key, value);
            } else {
                getSource().put(key, value);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Is invoked to flush child cache if it has backing Source
     * Some additional tasks may be performed by subclasses here
     */
    protected boolean flushChild(final byte[] key, final V childCache) {
        return childCache == null || childCache.flush();
    }

    /**
     * Creates a local child cache instance based on the child cache instance
     * (or null) from the MultiCache backing Source
     */
    protected abstract V create(byte[] key, V srcCache);
}
