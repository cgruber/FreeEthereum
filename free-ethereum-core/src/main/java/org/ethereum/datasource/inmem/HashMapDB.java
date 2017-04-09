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

package org.ethereum.datasource.inmem;

import org.ethereum.datasource.DbSource;
import org.ethereum.util.ALock;
import org.ethereum.util.ByteArrayMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashMapDB<V> implements DbSource<V> {

    protected final Map<byte[], V> storage;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ALock readLock = new ALock(rwLock.readLock());
    private final ALock writeLock = new ALock(rwLock.writeLock());

    public HashMapDB() {
        this(new ByteArrayMap<>());
    }

    private HashMapDB(final ByteArrayMap<V> storage) {
        this.storage = storage;
    }

    @Override
    public void put(final byte[] key, final V val) {
        if (val == null) {
            delete(key);
        } else {
            try (ALock l = writeLock.lock()) {
                storage.put(key, val);
            }
        }
    }

    @Override
    public V get(final byte[] key) {
        try (ALock l = readLock.lock()) {
            return storage.get(key);
        }
    }

    @Override
    public void delete(final byte[] key) {
        try (ALock l = writeLock.lock()) {
            storage.remove(key);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public String getName() {
        return "in-memory";
    }

    @Override
    public void setName(final String name) {
    }

    @Override
    public void init() {}

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void close() {}

    @Override
    public Set<byte[]> keys() {
        try (ALock l = readLock.lock()) {
            return getStorage().keySet();
        }
    }

    @Override
    public void updateBatch(final Map<byte[], V> rows) {
        try (ALock l = writeLock.lock()) {
            for (final Map.Entry<byte[], V> entry : rows.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map<byte[], V> getStorage() {
        return storage;
    }
}
