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

import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.datasource.*;
import org.ethereum.trie.SecureTrie;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.vm.DataWord;

/**
 * Created by Anton Nashatyrev on 07.10.2016.
 */
public class RepositoryRoot extends RepositoryImpl {

    private final Source<byte[], byte[]> stateDS;
    private final CachedSource.BytesKey<byte[]> trieCache;
    private final Trie<byte[]> stateTrie;

    public RepositoryRoot(final Source<byte[], byte[]> stateDS) {
        this(stateDS, null);
    }
    /**
     * Building the following structure for snapshot Repository:
     *
     * stateDS --> trieCacheCodec --> trieCache --> stateTrie --> accountStateCodec --> accountStateCache
     *  \                               \
     *   \                               \-->>>  contractStorageTrie --> storageCodec --> StorageCache
     *    \--> codeCache
     *
     *
     * @param stateDS
     * @param root
     */
    public RepositoryRoot(final Source<byte[], byte[]> stateDS, final byte[] root) {
        this.stateDS = stateDS;

        trieCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        stateTrie = new SecureTrie(trieCache, root);

        final SourceCodec.BytesKey<AccountState, byte[]> accountStateCodec = new SourceCodec.BytesKey<>(stateTrie, Serializers.AccountStateSerializer);
        final ReadWriteCache.BytesKey<AccountState> accountStateCache = new ReadWriteCache.BytesKey<>(accountStateCodec, WriteCache.CacheType.SIMPLE);

        final MultiCache<StorageCache> storageCache = new MultiStorageCache();

        // counting as there can be 2 contracts with the same code, 1 can suicide
        final Source<byte[], byte[]> codeCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);

        init(accountStateCache, codeCache, storageCache);
    }

    @Override
    public synchronized void commit() {
        super.commit();

        stateTrie.flush();
        trieCache.flush();
    }

    @Override
    public synchronized byte[] getRoot() {
        storageCache.flush();
        accountStateCache.flush();

        return stateTrie.getRootHash();
    }

    @Override
    public synchronized void flush() {
        commit();
    }

    @Override
    public Repository getSnapshotTo(final byte[] root) {
        return new RepositoryRoot(stateDS, root);
    }

    @Override
    public synchronized String dumpStateTrie() {
        return ((TrieImpl) stateTrie).dumpTrie();
    }

    @Override
    public synchronized void syncToRoot(final byte[] root) {
        stateTrie.setRoot(root);
    }

    private TrieImpl createTrie(final CachedSource.BytesKey<byte[]> trieCache, final byte[] root) {
        return new SecureTrie(trieCache, root);
    }

    private static class StorageCache extends ReadWriteCache<DataWord, DataWord> {
        final Trie<byte[]> trie;

        public StorageCache(final Trie<byte[]> trie) {
            super(new SourceCodec<>(trie, Serializers.StorageKeySerializer, Serializers.StorageValueSerializer), WriteCache.CacheType.SIMPLE);
            this.trie = trie;
        }
    }

    private class MultiStorageCache extends MultiCache<StorageCache> {
        public MultiStorageCache() {
            super(null);
        }

        @Override
        protected synchronized StorageCache create(final byte[] key, final StorageCache srcCache) {
            final AccountState accountState = accountStateCache.get(key);
            final TrieImpl storageTrie = createTrie(trieCache, accountState == null ? null : accountState.getStateRoot());
            return new StorageCache(storageTrie);
        }

        @Override
        protected synchronized boolean flushChild(final byte[] key, final StorageCache childCache) {
            if (super.flushChild(key, childCache)) {
                if (childCache != null) {
                    final AccountState storageOwnerAcct = accountStateCache.get(key);
                    // need to update account storage root
                    childCache.trie.flush();
                    final byte[] rootHash = childCache.trie.getRootHash();
                    accountStateCache.put(key, storageOwnerAcct.withStateRoot(rootHash));
                    return true;
                } else {
                    // account was deleted
                    return true;
                }
            } else {
                // no storage changes
                return false;
            }
        }
    }

}
