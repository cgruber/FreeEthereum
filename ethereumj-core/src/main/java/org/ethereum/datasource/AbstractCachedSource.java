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
 * Abstract cache implementation which tracks the cache size with
 * supplied key and value MemSizeEstimator's
 *
 * Created by Anton Nashatyrev on 01.12.2016.
 */
public abstract class AbstractCachedSource <Key, Value>
        extends AbstractChainedSource<Key, Value, Key, Value>
        implements CachedSource<Key, Value> {

    private final Object lock = new Object();
    MemSizeEstimator<Key> keySizeEstimator;
    MemSizeEstimator<Value> valueSizeEstimator;
    private int size = 0;

    AbstractCachedSource(final Source<Key, Value> source) {
        super(source);
    }

    /**
     * Returns the cached value if exist.
     * Method doesn't look into the underlying storage
     * @return The value Entry if it cached (Entry may has null value if null value is cached),
     *        or null if no information in the cache for this key
     */
    abstract Entry<Value> getCached(Key key);

    /**
     * Needs to be called by the implementation when cache entry is added
     * Only new entries should be accounted for accurate size tracking
     * If the value for the key is changed the {@link #cacheRemoved}
     * needs to be called first
     */
    void cacheAdded(final Key key, final Value value) {
        synchronized (lock) {
            if (keySizeEstimator != null) {
                size += keySizeEstimator.estimateSize(key);
            }
            if (valueSizeEstimator != null) {
                size += valueSizeEstimator.estimateSize(value);
            }
        }
    }

    /**
     * Needs to be called by the implementation when cache entry is removed
     */
    void cacheRemoved(final Key key, final Value value) {
        synchronized (lock) {
            if (keySizeEstimator != null) {
                size -= keySizeEstimator.estimateSize(key);
            }
            if (valueSizeEstimator != null) {
                size -= valueSizeEstimator.estimateSize(value);
            }
        }
    }

    /**
     * Needs to be called by the implementation when cache is cleared
     */
    void cacheCleared() {
        synchronized (lock) {
            size = 0;
        }
    }

    /**
     * Sets the key/value size estimators
     */
    public AbstractCachedSource<Key, Value> withSizeEstimators(final MemSizeEstimator<Key> keySizeEstimator, final MemSizeEstimator<Value> valueSizeEstimator) {
        this.keySizeEstimator = keySizeEstimator;
        this.valueSizeEstimator = valueSizeEstimator;
        return this;
    }

    @Override
    public long estimateCacheSize() {
        return size;
    }

    /**
     * Like the Optional interface represents either the value cached
     * or null cached (i.e. cache knows that underlying storage contain null)
     */
    public interface Entry<V> {
        V value();
    }

    static final class SimpleEntry<V> implements Entry<V> {
        private final V val;

        public SimpleEntry(final V val) {
            this.val = val;
        }

        public V value() {
            return val;
        }
    }
}
