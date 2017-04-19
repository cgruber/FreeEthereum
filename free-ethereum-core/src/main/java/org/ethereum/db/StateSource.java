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

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.datasource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class StateSource extends SourceChainBox<byte[], byte[], byte[], byte[]>
        implements HashedKeySource<byte[], byte[]> {
    private static final Logger logger = LoggerFactory.getLogger("db");

    private final ReadCache<byte[], byte[]> readCache;
    private final AbstractCachedSource<byte[], byte[]> writeCache;
    private final BloomedSource bloomedSource;
    private JournalSource<byte[]> journalSource;

    public StateSource(final Source<byte[], byte[]> src, final boolean pruningEnabled) {
        this(src, pruningEnabled, 0);
    }

    public StateSource(final Source<byte[], byte[]> src, final boolean pruningEnabled, final int maxBloomSize) {
        super(src);
        final StateSource INST = this;
        add(bloomedSource = new BloomedSource(src, maxBloomSize));
        bloomedSource.setFlushSource(false);
        add(readCache = new ReadCache.BytesKey<>(bloomedSource).withMaxCapacity(16 * 1024 * 1024 / 512)); // 512 - approx size of a node
        readCache.setFlushSource(true);
        final CountingBytesSource countingSource;
        add(countingSource = new CountingBytesSource(readCache, true));
        countingSource.setFlushSource(true);
        writeCache = new AsyncWriteCache<byte[], byte[]>(countingSource) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(final Source<byte[], byte[]> source) {
                final WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(source, WriteCache.CacheType.COUNTING);
                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
                ret.setFlushSource(true);
                return ret;
            }
        }.withName("state");

        add(writeCache);

        if (pruningEnabled) {
            add(journalSource = new JournalSource<>(writeCache));
        } else {
            final NoDeleteSource<byte[], byte[]> noDeleteSource;
            add(noDeleteSource = new NoDeleteSource<>(writeCache));
        }
    }

    @Autowired
    public void setConfig(final SystemProperties config) {
        final int size = config.getConfig().getInt("cache.stateCacheSize");
        readCache.withMaxCapacity(size * 1024 * 1024 / 512); // 512 - approx size of a node
    }

    @Autowired
    public void setCommonConfig(final CommonConfig commonConfig) {
        if (journalSource != null) {
            journalSource.setJournalStore(commonConfig.cachedDbSource("journal"));
        }
    }

    public JournalSource<byte[]> getJournalSource() {
        return journalSource;
    }

    public BloomedSource getBloomedSource() {
        return bloomedSource;
    }

    /**
     * Returns the source behind JournalSource
     */
    public Source<byte[], byte[]> getNoJournalSource() {
        return writeCache;
    }

    public AbstractCachedSource<byte[], byte[]> getWriteCache() {
        return writeCache;
    }

    public ReadCache<byte[], byte[]> getReadCache() {
        return readCache;
    }
}
