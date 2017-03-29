package org.ethereum.db;

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.datasource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Anton Nashatyrev on 29.11.2016.
 */
public class StateSource extends SourceChainBox<byte[], byte[], byte[], byte[]>
        implements HashedKeySource<byte[], byte[]> {
    private static final Logger logger = LoggerFactory.getLogger("db");

    // for debug purposes
    private static StateSource INST;
    private final CountingBytesSource countingSource;
    private final ReadCache<byte[], byte[]> readCache;
    private final AbstractCachedSource<byte[], byte[]> writeCache;
    private final BloomedSource bloomedSource;
    private JournalSource<byte[]> journalSource;
    private NoDeleteSource<byte[], byte[]> noDeleteSource;

    public StateSource(Source<byte[], byte[]> src, boolean pruningEnabled) {
        this(src, pruningEnabled, 0);
    }

    public StateSource(Source<byte[], byte[]> src, boolean pruningEnabled, int maxBloomSize) {
        super(src);
        INST = this;
        add(bloomedSource = new BloomedSource(src, maxBloomSize));
        bloomedSource.setFlushSource(false);
        add(readCache = new ReadCache.BytesKey<>(bloomedSource).withMaxCapacity(16 * 1024 * 1024 / 512)); // 512 - approx size of a node
        readCache.setFlushSource(true);
        add(countingSource = new CountingBytesSource(readCache, true));
        countingSource.setFlushSource(true);
        writeCache = new AsyncWriteCache<byte[], byte[]>(countingSource) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(Source<byte[], byte[]> source) {
                WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(source, WriteCache.CacheType.COUNTING);
                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
                ret.setFlushSource(true);
                return ret;
            }
        }.withName("state");

        add(writeCache);

        if (pruningEnabled) {
            add(journalSource = new JournalSource<>(writeCache));
        } else {
            add(noDeleteSource = new NoDeleteSource<>(writeCache));
        }
    }

    @Autowired
    public void setConfig(SystemProperties config) {
        int size = config.getConfig().getInt("cache.stateCacheSize");
        readCache.withMaxCapacity(size * 1024 * 1024 / 512); // 512 - approx size of a node
    }

    @Autowired
    public void setCommonConfig(CommonConfig commonConfig) {
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
