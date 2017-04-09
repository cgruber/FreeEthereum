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

import com.google.common.util.concurrent.*;
import org.ethereum.util.ALock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Anton Nashatyrev on 18.01.2017.
 */
public abstract class AsyncWriteCache<Key, Value> extends AbstractCachedSource<Key, Value> implements AsyncFlushable {
    private static final Logger logger = LoggerFactory.getLogger("db");

    private static final ListeningExecutorService flushExecutor = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("AsyncWriteCacheThread-%d").build()));
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ALock rLock = new ALock(rwLock.readLock());
    private final ALock wLock = new ALock(rwLock.writeLock());
    private final WriteCache<Key, Value> flushingCache;
    private volatile WriteCache<Key, Value> curCache;
    private ListenableFuture<Boolean> lastFlush = Futures.immediateFuture(false);
    private String name = "<null>";

    public AsyncWriteCache(final Source<Key, Value> source) {
        super(source);
        flushingCache = createCache(source);
        flushingCache.setFlushSource(true);
        curCache = createCache(flushingCache);
    }

    protected abstract WriteCache<Key, Value> createCache(Source<Key, Value> source);

    @Override
    public Collection<Key> getModified() {
        try (ALock l = rLock.lock()) {
            return curCache.getModified();
        }
    }

    @Override
    public boolean hasModified() {
        try (ALock l = rLock.lock()) {
            return curCache.hasModified();
        }
    }

    @Override
    public void put(final Key key, final Value val) {
        try (ALock l = rLock.lock()) {
            curCache.put(key, val);
        }
    }

    @Override
    public void delete(final Key key) {
        try (ALock l = rLock.lock()) {
            curCache.delete(key);
        }
    }

    @Override
    public Value get(final Key key) {
        try (ALock l = rLock.lock()) {
            return curCache.get(key);
        }
    }

    @Override
    public synchronized boolean flush() {
        try {
            flipStorage();
            flushAsync();
            return flushingCache.hasModified();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    Entry<Value> getCached(final Key key) {
        return curCache.getCached(key);
    }

    @Override
    public synchronized void flipStorage() throws InterruptedException {
        // if previous flush still running
        try {
            if (!lastFlush.isDone()) logger.debug("AsyncWriteCache (" + name + "): waiting for previous flush to complete");
            lastFlush.get();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }

        try (ALock l = wLock.lock()) {
            flushingCache.cache = curCache.cache;
            curCache = createCache(flushingCache);
        }
    }

    public synchronized ListenableFuture<Boolean> flushAsync() throws InterruptedException {
        logger.debug("AsyncWriteCache (" + name + "): flush submitted");
        lastFlush = flushExecutor.submit(() -> {
            logger.debug("AsyncWriteCache (" + name + "): flush started");
            final long s = System.currentTimeMillis();
            final boolean ret = flushingCache.flush();
            logger.debug("AsyncWriteCache (" + name + "): flush completed in " + (System.currentTimeMillis() - s) + " ms");
            return ret;
        });
        return lastFlush;
    }

    @Override
    public long estimateCacheSize() {
        // 2.0 is upper cache size estimation to take into account there are two
        // caches may exist simultaneously up to doubling cache size
        return (long) (curCache.estimateCacheSize() * 2.0);
    }

    @Override
    protected synchronized boolean flushImpl() {
        return false;
    }

    public AsyncWriteCache<Key, Value> withName(final String name) {
        this.name = name;
        return this;
    }
}
