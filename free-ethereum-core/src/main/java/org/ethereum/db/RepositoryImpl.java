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

import org.ethereum.config.SystemProperties;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.CachedSource;
import org.ethereum.datasource.MultiCache;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.WriteCache;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.vm.DataWord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

public class RepositoryImpl implements Repository, org.ethereum.facade.Repository {

    Source<byte[], AccountState> accountStateCache;
    MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache;
    private RepositoryImpl parent;
    private Source<byte[], byte[]> codeCache;
    @Autowired
    private SystemProperties config = SystemProperties.getDefault();

    RepositoryImpl() {
    }

    private RepositoryImpl(final Source<byte[], AccountState> accountStateCache, final Source<byte[], byte[]> codeCache,
                           final MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache) {
        init(accountStateCache, codeCache, storageCache);
    }

    void init(final Source<byte[], AccountState> accountStateCache, final Source<byte[], byte[]> codeCache,
              final MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache) {
        this.accountStateCache = accountStateCache;
        this.codeCache = codeCache;
        this.storageCache = storageCache;
    }

    @Override
    public synchronized AccountState createAccount(final byte[] addr) {
        final AccountState state = new AccountState(config.getBlockchainConfig().getCommonConstants().getInitialNonce(),
                BigInteger.ZERO);
        accountStateCache.put(addr, state);
        return state;
    }

    @Override
    public synchronized boolean isExist(final byte[] addr) {
        return getAccountState(addr) != null;
    }

    @Override
    public synchronized AccountState getAccountState(final byte[] addr) {
        return accountStateCache.get(addr);
    }

    private synchronized AccountState getOrCreateAccountState(final byte[] addr) {
        AccountState ret = accountStateCache.get(addr);
        if (ret == null) {
            ret = createAccount(addr);
        }
        return ret;
    }

    @Override
    public synchronized void delete(final byte[] addr) {
        accountStateCache.delete(addr);
        storageCache.delete(addr);
    }

    @Override
    public synchronized BigInteger increaseNonce(final byte[] addr) {
        final AccountState accountState = getOrCreateAccountState(addr);
        accountStateCache.put(addr, accountState.withIncrementedNonce());
        return accountState.getNonce();
    }

    @Override
    public synchronized BigInteger setNonce(final byte[] addr, final BigInteger nonce) {
        final AccountState accountState = getOrCreateAccountState(addr);
        accountStateCache.put(addr, accountState.withNonce(nonce));
        return accountState.getNonce();
    }

    @Override
    public synchronized BigInteger getNonce(final byte[] addr) {
        final AccountState accountState = getAccountState(addr);
        return accountState == null ? config.getBlockchainConfig().getCommonConstants().getInitialNonce() :
                accountState.getNonce();
    }

    @Override
    public synchronized ContractDetails getContractDetails(final byte[] addr) {
        return new ContractDetailsImpl(addr);
    }

    @Override
    public synchronized boolean hasContractDetails(final byte[] addr) {
        return getContractDetails(addr) != null;
    }

    @Override
    public synchronized void saveCode(final byte[] addr, final byte[] code) {
        final byte[] codeHash = HashUtil.sha3(code);
        codeCache.put(codeHash, code);
        final AccountState accountState = getOrCreateAccountState(addr);
        accountStateCache.put(addr, accountState.withCodeHash(codeHash));
    }

    @Override
    public synchronized byte[] getCode(final byte[] addr) {
        final byte[] codeHash = getCodeHash(addr);
        return FastByteComparisons.equal(codeHash, HashUtil.EMPTY_DATA_HASH) ?
                ByteUtil.EMPTY_BYTE_ARRAY : codeCache.get(codeHash);
    }

    @Override
    public byte[] getCodeHash(final byte[] addr) {
        final AccountState accountState = getAccountState(addr);
        return accountState != null ? accountState.getCodeHash() : HashUtil.EMPTY_DATA_HASH;
    }

    @Override
    public synchronized void addStorageRow(final byte[] addr, final DataWord key, final DataWord value) {
        getOrCreateAccountState(addr);

        final Source<DataWord, DataWord> contractStorage = storageCache.get(addr);
        contractStorage.put(key, value.isZero() ? null : value);
    }

    @Override
    public synchronized DataWord getStorageValue(final byte[] addr, final DataWord key) {
        final AccountState accountState = getAccountState(addr);
        return accountState == null ? null : storageCache.get(addr).get(key);
    }

    @Override
    public synchronized BigInteger getBalance(final byte[] addr) {
        final AccountState accountState = getAccountState(addr);
        return accountState == null ? BigInteger.ZERO : accountState.getBalance();
    }

    @Override
    public synchronized BigInteger addBalance(final byte[] addr, final BigInteger value) {
        final AccountState accountState = getOrCreateAccountState(addr);
        accountStateCache.put(addr, accountState.withBalanceIncrement(value));
        return accountState.getBalance();
    }

    @Override
    public synchronized RepositoryImpl startTracking() {
        final Source<byte[], AccountState> trackAccountStateCache = new WriteCache.BytesKey<>(accountStateCache,
                WriteCache.CacheType.SIMPLE);
        final Source<byte[], byte[]> trackCodeCache = new WriteCache.BytesKey<>(codeCache, WriteCache.CacheType.SIMPLE);
        final MultiCache<CachedSource<DataWord, DataWord>> trackStorageCache = new MultiCache(storageCache) {
            @Override
            protected CachedSource create(final byte[] key, final CachedSource srcCache) {
                return new WriteCache<>(srcCache, WriteCache.CacheType.SIMPLE);
            }
        };

        final RepositoryImpl ret = new RepositoryImpl(trackAccountStateCache, trackCodeCache, trackStorageCache);
        ret.parent = this;
        return ret;
    }

    @Override
    public synchronized Repository getSnapshotTo(final byte[] root) {
        return parent.getSnapshotTo(root);
    }

    @Override
    public synchronized void commit() {
        final Repository parentSync = parent == null ? this : parent;
        // need to synchronize on parent since between different caches flush
        // the parent repo would not be in consistent state
        // when no parent just take this instance as a mock
        synchronized (parentSync) {
            storageCache.flush();
            codeCache.flush();
            accountStateCache.flush();
        }
    }

    @Override
    public synchronized void rollback() {
        // nothing to do, will be GCed
    }

    @Override
    public byte[] getRoot() {
        throw new RuntimeException("Not supported");
    }

    public synchronized String getTrieDump() {
        return dumpStateTrie();
    }

    String dumpStateTrie() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void dumpState(final Block block, final long gasUsed, final int txNumber, final byte[] txHash) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void flush() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void flushNoReconnect() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void syncToRoot(final byte[] root) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean isClosed() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void close() {
    }

    @Override
    public void reset() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public int getStorageSize(final byte[] addr) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Set<DataWord> getStorageKeys(final byte[] addr) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Map<DataWord, DataWord> getStorage(final byte[] addr, @Nullable final Collection<DataWord> keys) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void updateBatch(final HashMap<ByteArrayWrapper, AccountState> accountStates, final HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
        for (final Map.Entry<ByteArrayWrapper, AccountState> entry : accountStates.entrySet()) {
            accountStateCache.put(entry.getKey().getData(), entry.getValue());
        }
        for (final Map.Entry<ByteArrayWrapper, ContractDetails> entry : contractDetailes.entrySet()) {
            final ContractDetails details = getContractDetails(entry.getKey().getData());
            for (final DataWord key : entry.getValue().getStorageKeys()) {
                details.put(key, entry.getValue().get(key));
            }
            final byte[] code = entry.getValue().getCode();
            if (code != null && code.length > 0) {
                details.setCode(code);
            }
        }
    }

    @Override
    public void loadAccount(final byte[] addr, final HashMap<ByteArrayWrapper, AccountState> cacheAccounts, final HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
        throw new RuntimeException("Not supported");
    }

    class ContractDetailsImpl implements ContractDetails {
        private final byte[] address;

        public ContractDetailsImpl(final byte[] address) {
            this.address = address;
        }

        @Override
        public void put(final DataWord key, final DataWord value) {
            RepositoryImpl.this.addStorageRow(address, key, value);
        }

        @Override
        public DataWord get(final DataWord key) {
            return RepositoryImpl.this.getStorageValue(address, key);
        }

        @Override
        public byte[] getCode() {
            return RepositoryImpl.this.getCode(address);
        }

        @Override
        public void setCode(final byte[] code) {
            RepositoryImpl.this.saveCode(address, code);
        }

        @Override
        public byte[] getCode(final byte[] codeHash) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public byte[] getStorageHash() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void decode(final byte[] rlpCode) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public boolean isDirty() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setDirty(final boolean dirty) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public boolean isDeleted() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setDeleted(final boolean deleted) {
            RepositoryImpl.this.delete(address);
        }

        @Override
        public byte[] getEncoded() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public int getStorageSize() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public Set<DataWord> getStorageKeys() {
            throw new RuntimeException("Not supported");
        }

//        @Override
//        public Map<DataWord, DataWord> getStorage(@Nullable final Collection<DataWord> keys) {
//            throw new RuntimeException("Not supported");
//        }

        @Override
        public Map<DataWord, DataWord> getStorage() {
            throw new RuntimeException("Not supported");
        }

//        @Override
//        public void setStorage(final Map<DataWord, DataWord> storage) {
//            throw new RuntimeException("Not supported");
//        }
//
//        @Override
//        public void setStorage(final List<DataWord> storageKeys, final List<DataWord> storageValues) {
//            throw new RuntimeException("Not supported");
//        }

        @Override
        public void setStorage(@NotNull Map<DataWord, ? extends DataWord> map) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public byte[] getAddress() {
            return address;
        }

        @Override
        public void setAddress(final byte[] address) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public ContractDetails clone() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void syncStorage() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public ContractDetails getSnapshotTo(final byte[] hash) {
            throw new RuntimeException("Not supported");
        }

        @NotNull
        @Override
        public Map<DataWord, DataWord> getStorage(@org.jetbrains.annotations.Nullable Collection<? extends DataWord> keys) {
            return null;
        }

        @Override
        public void setStorage(@NotNull List<? extends DataWord> storageKeys, @NotNull List<? extends DataWord> storageValues) {
            throw new RuntimeException("Not supported");
        }
    }

}
