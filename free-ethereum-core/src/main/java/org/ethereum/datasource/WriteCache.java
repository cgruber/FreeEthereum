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

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import org.ethereum.util.ALock;
import org.ethereum.util.ByteArrayMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Collects changes and propagate them to the backing Source when flush() is called
 *
 * The WriteCache can be of two types: Simple and Counting
 *
 * Simple acts as regular Map: single and double adding of the same entry has the same effect
 * Source entries (key/value pairs) may have arbitrary nature
 *
 * Counting counts the resulting number of inserts (+1) and deletes (-1) and when flushed
 * does the resulting number of inserts (if sum > 0) or deletes (if sum < 0)
 * Counting Source acts like {@link HashedKeySource} and makes sense only for data
 * where a single key always corresponds to a single value
 * Counting cache normally used as backing store for Trie data structure
 *
 * Created by Anton Nashatyrev on 11.11.2016.
 */
public class WriteCache<Key, Value> extends AbstractCachedSource<Key, Value> {

    private final boolean isCounting;
    private final ReadWriteUpdateLock rwuLock = new ReentrantReadWriteUpdateLock();
    private final ALock readLock = new ALock(rwuLock.readLock());
    private final ALock writeLock = new ALock(rwuLock.writeLock());
    private final ALock updateLock = new ALock(rwuLock.updateLock());
    volatile Map<Key, CacheEntry<Value>> cache = new HashMap<>();
    private boolean checked = false;

    public WriteCache(final Source<Key, Value> src, final CacheType cacheType) {
        super(src);
        this.isCounting = cacheType == CacheType.COUNTING;
    }

    WriteCache<Key, Value> withCache(final Map<Key, CacheEntry<Value>> cache) {
        this.cache = cache;
        return this;
    }

    @Override
    public Collection<Key> getModified() {
        try (ALock l = readLock.lock()){
            return cache.keySet();
        }
    }

    @Override
    public boolean hasModified() {
        return !cache.isEmpty();
    }

    private CacheEntry<Value> createCacheEntry(final Value val) {
        if (isCounting) {
            return new CountCacheEntry<>(val);
        } else {
            return new SimpleCacheEntry<>(val);
        }
    }

    @Override
    public void put(final Key key, final Value val) {
        checkByteArrKey(key);
        if (val == null)  {
            delete(key);
            return;
        }


        try (ALock l = writeLock.lock()){
            CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(val);
                final CacheEntry<Value> oldVal = cache.put(key, curVal);
                if (oldVal != null) {
                    cacheRemoved(key, oldVal.value == unknownValue() ? null : oldVal.value);
                }
                cacheAdded(key, curVal.value);
            }
            // assigning for non-counting cache only
            // for counting cache the value should be immutable (see HashedKeySource)
            curVal.value = val;
            curVal.added();
        }
    }

    @Override
    public Value get(final Key key) {
        checkByteArrKey(key);
        try (ALock l = readLock.lock()){
            final CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                return getSource() == null ? null : getSource().get(key);
            } else {
                final Value value = curVal.getValue();
                if (value == unknownValue()) {
                    return getSource() == null ? null : getSource().get(key);
                } else {
                    return value;
                }
            }
        }
    }

    @Override
    public void delete(final Key key) {
        checkByteArrKey(key);
        try (ALock l = writeLock.lock()){
            CacheEntry<Value> curVal = cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(getSource() == null ? null : unknownValue());
                final CacheEntry<Value> oldVal = cache.put(key, curVal);
                if (oldVal != null) {
                    cacheRemoved(key, oldVal.value);
                }
                cacheAdded(key, curVal.value == unknownValue() ? null : curVal.value);
            }
            curVal.deleted();
        }
    }

    @Override
    public boolean flush() {
        boolean ret = false;
        try (ALock l = updateLock.lock()){
            for (final Map.Entry<Key, CacheEntry<Value>> entry : cache.entrySet()) {
                if (entry.getValue().counter > 0) {
                    for (int i = 0; i < entry.getValue().counter; i++) {
                        getSource().put(entry.getKey(), entry.getValue().value);
                    }
                    ret = true;
                } else if (entry.getValue().counter < 0) {
                    for (int i = 0; i > entry.getValue().counter; i--) {
                        getSource().delete(entry.getKey());
                    }
                    ret = true;
                }
            }
            if (flushSource) {
                getSource().flush();
            }
            try (ALock l1 = writeLock.lock()){
                cache.clear();
                cacheCleared();
            }
            return ret;
        }
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }

    private Value unknownValue() {
        return (Value) CacheEntry.UNKNOWN_VALUE;
    }

    public Entry<Value> getCached(final Key key) {
        try (ALock l = readLock.lock()){
            final CacheEntry<Value> entry = cache.get(key);
            if (entry == null || entry.value == unknownValue()) {
                return null;
            }else {
                return entry;
            }
        }
    }

    // Guard against wrong cache Map
    // if a regular Map is accidentally used for byte[] type keys
    // the situation might be tricky to debug
    private void checkByteArrKey(final Key key) {
        if (checked) return;

        if (key instanceof byte[]) {
            if (!(cache instanceof ByteArrayMap)) {
                throw new RuntimeException("Wrong map/set for byte[] key");
            }
        }
        checked = true;
    }

    public long debugCacheSize() {
        long ret = 0;
        for (final Map.Entry<Key, CacheEntry<Value>> entry : cache.entrySet()) {
            ret += keySizeEstimator.estimateSize(entry.getKey());
            ret += valueSizeEstimator.estimateSize(entry.getValue().value());
        }
        return ret;
    }

    /**
     * Type of the write cache
     */
    public enum CacheType {
        /**
         * Simple acts as regular Map: single and double adding of the same entry has the same effect
         * Source entries (key/value pairs) may have arbitrary nature
         */
        SIMPLE,
        /**
         * Counting counts the resulting number of inserts (+1) and deletes (-1) and when flushed
         * does the resulting number of inserts (if sum > 0) or deletes (if sum < 0)
         * Counting Source acts like {@link HashedKeySource} and makes sense only for data
         * where a single key always corresponds to a single value
         * Counting cache normally used as backing store for Trie data structure
         */
        COUNTING
    }

    private static abstract class CacheEntry<V> implements Entry<V> {
        // dedicated value instance which indicates that the entry was deleted
        // (ref counter decremented) but we don't know actual value behind it
        static final Object UNKNOWN_VALUE = new Object();

        V value;
        int counter = 0;

        CacheEntry(final V value) {
            this.value = value;
        }

        protected abstract void deleted();

        protected abstract void added();

        protected abstract V getValue();

        @Override
        public V value() {
            final V v = getValue();
            return v == UNKNOWN_VALUE ? null : v;
        }
    }

    private static final class SimpleCacheEntry<V> extends CacheEntry<V> {
        public SimpleCacheEntry(final V value) {
            super(value);
        }

        public void deleted() {
            counter = -1;
        }

        public void added() {
            counter = 1;
        }

        @Override
        public V getValue() {
            return counter < 0 ? null : value;
        }
    }

    private static final class CountCacheEntry<V> extends CacheEntry<V> {
        public CountCacheEntry(final V value) {
            super(value);
        }

        public void deleted() {
            counter--;
        }

        public void added() {
            counter++;
        }

        @Override
        public V getValue() {
            // for counting cache we return the cached value even if
            // it was deleted (once or several times) as we don't know
            // how many 'instances' are left behind
            return value;
        }
    }

    /**
     * Shortcut for WriteCache with byte[] keys. Also prevents accidental
     * usage of regular Map implementation (non byte[])
     */
    public static class BytesKey<V> extends WriteCache<byte[], V> implements CachedSource.BytesKey<V> {

        public BytesKey(final Source<byte[], V> src, final CacheType cacheType) {
            super(src, cacheType);
            withCache(new ByteArrayMap<>());
        }
    }
}
