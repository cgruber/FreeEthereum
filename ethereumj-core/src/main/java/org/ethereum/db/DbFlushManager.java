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

package org.ethereum.db;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.ethereum.config.SystemProperties;
import org.ethereum.datasource.AbstractCachedSource;
import org.ethereum.datasource.AsyncFlushable;
import org.ethereum.datasource.DbSource;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Anton Nashatyrev on 01.12.2016.
 */
public class DbFlushManager {
    private static final Logger logger = LoggerFactory.getLogger("db");
    private final BlockingQueue<Runnable> executorQueue = new ArrayBlockingQueue<>(1);
    private final ExecutorService flushThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            executorQueue, new ThreadFactoryBuilder().setNameFormat("DbFlushManagerThread-%d").build());
    private final List<AbstractCachedSource<byte[], byte[]>> writeCaches = new ArrayList<>();
    private final AbstractCachedSource<byte[], byte[]> stateDbCache;
    private final int commitsCountThreshold;
    private final boolean flushAfterSyncDone;
    private final SystemProperties config;
    private Set<DbSource> dbSources = new HashSet<>();
    private long sizeThreshold;
    private boolean syncDone = false;
    private int commitCount = 0;
    private Future<Boolean> lastFlush = Futures.immediateFuture(false);

    public DbFlushManager(final SystemProperties config, final Set<DbSource> dbSources, final AbstractCachedSource<byte[], byte[]> stateDbCache) {
        this.config = config;
        this.dbSources = dbSources;
        sizeThreshold = config.getConfig().getInt("cache.flush.writeCacheSize") * 1024 * 1024;
        commitsCountThreshold = config.getConfig().getInt("cache.flush.blocks");
        flushAfterSyncDone = config.getConfig().getBoolean("cache.flush.shortSyncFlush");
        this.stateDbCache = stateDbCache;
    }

    @Autowired
    public void setEthereumListener(final CompositeEthereumListener listener) {
        if (!flushAfterSyncDone) return;
        listener.addListener(new EthereumListenerAdapter() {
            @Override
            public void onSyncDone(final SyncState state) {
                if (state == SyncState.COMPLETE) {
                    logger.info("DbFlushManager: long sync done, flushing each block now");
                    syncDone = true;
                }
            }
        });
    }

    public void setSizeThreshold(final long sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public void addCache(final AbstractCachedSource<byte[], byte[]> cache) {
        writeCaches.add(cache);
    }

    private long getCacheSize() {
        long ret = 0;
        for (final AbstractCachedSource<byte[], byte[]> writeCache : writeCaches) {
            ret += writeCache.estimateCacheSize();
        }
        return ret;
    }

    public synchronized void commit(final Runnable atomicUpdate) {
        atomicUpdate.run();
        commit();
    }

    public synchronized void commit() {
        final long cacheSize = getCacheSize();
        if (sizeThreshold >= 0 && cacheSize >= sizeThreshold) {
            logger.info("DbFlushManager: flushing db due to write cache size (" + cacheSize + ") reached threshold (" + sizeThreshold + ")");
            flush();
        } else if (commitsCountThreshold > 0 && commitCount >= commitsCountThreshold) {
            logger.info("DbFlushManager: flushing db due to commits (" + commitCount + ") reached threshold (" + commitsCountThreshold + ")");
            flush();
            commitCount = 0;
        } else if (flushAfterSyncDone && syncDone) {
            logger.debug("DbFlushManager: flushing db due to short sync");
            flush();
        }
        commitCount++;
    }

    public synchronized void flushSync() {
        try {
            flush().get();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Future<Boolean> flush() {
        if (!lastFlush.isDone()) {
            logger.info("Waiting for previous flush to complete...");
            try {
                lastFlush.get();
            } catch (final Exception e) {
                logger.error("Error during last flush", e);
            }
        }
        logger.debug("Flipping async storages");
        for (final AbstractCachedSource<byte[], byte[]> writeCache : writeCaches) {
            try {
                if (writeCache instanceof AsyncFlushable) {
                    ((AsyncFlushable) writeCache).flipStorage();
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        logger.debug("Submitting flush task");
        return lastFlush = flushThread.submit(() -> {
            boolean ret = false;
            final long s = System.nanoTime();
            logger.info("Flush started");

            for (final AbstractCachedSource<byte[], byte[]> writeCache : writeCaches) {
                if (writeCache instanceof AsyncFlushable) {
                    try {
                        ret |= ((AsyncFlushable) writeCache).flushAsync().get();
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    ret |= writeCache.flush();
                }
            }
            if (stateDbCache != null) {
                logger.debug("Flushing to DB");
                stateDbCache.flush();
            }
            logger.info("Flush completed in " + (System.nanoTime() - s) / 1000000 + " ms");

            return ret;
        });
    }

    /**
     * Flushes all caches and closes all databases
     */
    public synchronized void close() {
        logger.info("Flushing DBs...");
        flushSync();
        logger.info("Flush done.");
        for (final DbSource dbSource : dbSources) {
            logger.info("Closing DB: {}", dbSource.getName());
            try {
                dbSource.close();
            } catch (final Exception ex) {
                logger.error(String.format("Caught error while closing DB: %s", dbSource.getName()), ex);
            }
        }
    }
}

