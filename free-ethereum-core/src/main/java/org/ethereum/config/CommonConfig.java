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

package org.ethereum.config;

import org.ethereum.core.BlockHeader;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.*;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.datasource.leveldb.LevelDbDataSource;
import org.ethereum.db.*;
import org.ethereum.listener.EthereumListener;
import org.ethereum.sync.FastSyncManager;
import org.ethereum.validator.*;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.program.ProgramPrecompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

@Configuration
@EnableTransactionManagement
@ComponentScan(
        basePackages = "org.ethereum",
        excludeFilters = @ComponentScan.Filter(NoAutoscan.class))
public class CommonConfig {
    private static final Logger logger = LoggerFactory.getLogger("general");
    private static CommonConfig defaultInstance;
    private final Set<DbSource> dbSources = new HashSet<>();

    public static CommonConfig getDefault() {
        if (defaultInstance == null && !SystemProperties.isUseOnlySpringConfig()) {
            defaultInstance = new CommonConfig() {
                @Override
                public Source<byte[], ProgramPrecompile> precompileSource() {
                    return null;
                }
            };
        }
        return defaultInstance;
    }

    @Bean
    public SystemProperties systemProperties() {
        return SystemProperties.getSpringDefault();
    }

    @Bean
    BeanPostProcessor initializer() {
        return new Initializer();
    }


    @Bean @Primary
    public Repository repository() {
        return new RepositoryWrapper();
    }

    @Bean
    public Repository defaultRepository() {
        return new RepositoryRoot(stateSource(), null);
    }

    @Bean @Scope("prototype")
    public Repository repository(final byte[] stateRoot) {
        return new RepositoryRoot(stateSource(), stateRoot);
    }


    @Bean
    public StateSource stateSource() {
        fastSyncCleanUp();
        final StateSource stateSource = new StateSource(blockchainSource("state"),
                systemProperties().databasePruneDepth() >= 0, systemProperties().getConfig().getInt("cache.maxStateBloomSize") << 20);

        dbFlushManager().addCache(stateSource.getWriteCache());

        return stateSource;
    }

    @Bean
    @Scope("prototype")
    public Source<byte[], byte[]> cachedDbSource(final String name) {
        final AbstractCachedSource<byte[], byte[]> writeCache = new AsyncWriteCache<byte[], byte[]>(blockchainSource(name)) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(final Source<byte[], byte[]> source) {
                final WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(source, WriteCache.CacheType.SIMPLE);
                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
                ret.setFlushSource(true);
                return ret;
            }
        }.withName(name);
        dbFlushManager().addCache(writeCache);
        return writeCache;
    }

    @Bean
    @Scope("prototype")
    public Source<byte[], byte[]> blockchainSource(final String name) {
        return new XorDataSource<>(blockchainDbCache(), HashUtil.INSTANCE.sha3(name.getBytes()));
    }

    @Bean
    public AbstractCachedSource<byte[], byte[]> blockchainDbCache() {
        final WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(
                new BatchSourceWriter<>(blockchainDB()), WriteCache.CacheType.SIMPLE);
        ret.setFlushSource(true);
        return ret;
    }

    @Bean
    @Scope("prototype")
    @Primary
    public DbSource<byte[]> keyValueDataSource(final String name) {
        String dataSource = systemProperties().getKeyValueDataSource();
        try {
            final DbSource<byte[]> dbSource;
            if ("inmem".equals(dataSource)) {
                dbSource = new HashMapDB<>();
            } else {
                dataSource = "leveldb";
                dbSource = levelDbDataSource();
            }
            dbSource.setName(name);
            dbSource.init();
            dbSources.add(dbSource);
            return dbSource;
        } finally {
            logger.info(dataSource + " key-value data source created: " + name);
        }
    }

    @Bean
    @Scope("prototype")
    protected LevelDbDataSource levelDbDataSource() {
        return new LevelDbDataSource();
    }

    public void fastSyncCleanUp() {
        final byte[] fastsyncStageBytes = blockchainDB().get(FastSyncManager.FASTSYNC_DB_KEY_SYNC_STAGE);
        if (fastsyncStageBytes == null) return; // no uncompleted fast sync

        final EthereumListener.SyncState syncStage = EthereumListener.SyncState.values()[fastsyncStageBytes[0]];

        if (!systemProperties().isFastSyncEnabled() || syncStage == EthereumListener.SyncState.UNSECURE) {
            // we need to cleanup state/blocks/tranasaction DBs when previous fast sync was not complete:
            // - if we now want to do regular sync
            // - if the first fastsync stage was not complete (thus DBs are not in consistent state)

            logger.warn("Last fastsync was interrupted. Removing inconsistent DBs...");

            final DbSource bcSource = blockchainDB();
            resetDataSource(bcSource);
        }
    }

    private void resetDataSource(final Source source) {
        if (source instanceof LevelDbDataSource) {
            ((LevelDbDataSource) source).reset();
        } else {
            throw new Error("Cannot cleanup non-LevelDB database");
        }
    }

    @Bean
    @Lazy
    public DataSourceArray<BlockHeader> headerSource() {
        final DbSource<byte[]> dataSource = keyValueDataSource("headers");
        final BatchSourceWriter<byte[], byte[]> batchSourceWriter = new BatchSourceWriter<>(dataSource);
        final WriteCache.BytesKey<byte[]> writeCache = new WriteCache.BytesKey<>(batchSourceWriter, WriteCache.CacheType.SIMPLE);
        writeCache.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
        writeCache.setFlushSource(true);
        final ObjectDataSource<BlockHeader> objectDataSource = new ObjectDataSource<>(dataSource, Serializers.INSTANCE.getBlockHeaderSerializer(), 0);
        final DataSourceArray<BlockHeader> dataSourceArray = new DataSourceArray<>(objectDataSource);
        return dataSourceArray;
    }

    @Bean
    public Source<byte[], ProgramPrecompile> precompileSource() {

        final StateSource source = stateSource();
        return new SourceCodec<>(source,
                new Serializer<byte[], byte[]>() {
                    public byte[] serialize(final byte[] object) {
                        final DataWord ret = new DataWord(object);
                        ret.add(new DataWord(1));
                        return ret.getLast20Bytes();
                    }

                    public byte[] deserialize(final byte[] stream) {
                        throw new RuntimeException("Shouldn't be called");
                    }
                }, new Serializer<ProgramPrecompile, byte[]>() {
            public byte[] serialize(final ProgramPrecompile object) {
                return object == null ? null : object.serialize();
            }

            public ProgramPrecompile deserialize(final byte[] stream) {
                return stream == null ? null : ProgramPrecompile.Companion.deserialize(stream);
            }
        });
    }

    @Bean
    public DbSource<byte[]> blockchainDB() {
        return keyValueDataSource("blockchain");
    }

    @Bean
    public DbFlushManager dbFlushManager() {
        return new DbFlushManager(systemProperties(), dbSources, blockchainDbCache());
    }

    @Bean
    public BlockHeaderValidator headerValidator() {

        final List<BlockHeaderRule> rules = new ArrayList<>(asList(
                new GasValueRule(),
                new ExtraDataRule(systemProperties()),
                new ProofOfWorkRule(),
                new GasLimitRule(systemProperties()),
                new BlockHashRule(systemProperties())
        ));

        return new BlockHeaderValidator(rules);
    }

    @Bean
    public ParentBlockHeaderValidator parentHeaderValidator() {

        final List<DependentBlockHeaderRule> rules = new ArrayList<>(asList(
                new ParentNumberRule(),
                new DifficultyRule(systemProperties()),
                new ParentGasLimitRule(systemProperties())
        ));

        return new ParentBlockHeaderValidator(rules);
    }

    @Bean
    @Lazy
    public PeerSource peerSource() {
        final DbSource<byte[]> dbSource = keyValueDataSource("peers");
        dbSources.add(dbSource);
        return new PeerSource(dbSource);
    }
}
