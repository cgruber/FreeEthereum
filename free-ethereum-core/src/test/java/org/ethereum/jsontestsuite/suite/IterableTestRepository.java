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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.util.ByteArrayMap;
import org.ethereum.util.ByteArraySet;
import org.ethereum.vm.DataWord;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

public class IterableTestRepository implements Repository {
    final Repository src;
    boolean environmental;
    private Set<byte[]> accounts = new ByteArraySet();
    private Map<byte[], Set<DataWord>> storageKeys = new ByteArrayMap<>();

    private IterableTestRepository(final Repository src, final IterableTestRepository parent) {
        this.src = src;
        if (parent != null) {
            this.accounts = parent.accounts;
            this.storageKeys = parent.storageKeys;
            this.environmental = parent.environmental;
        }
    }

    public IterableTestRepository(final Repository src) {
        this(src, null);
    }

    void addAccount(final byte[] addr) {
        accounts.add(addr);
    }

    private void addStorageKey(final byte[] acct, final DataWord key) {
        addAccount(acct);
        final Set<DataWord> keys = storageKeys.computeIfAbsent(acct, k -> new HashSet<>());
        keys.add(key);
    }

    @Override
    public Repository startTracking() {
        return new IterableTestRepository(src.startTracking(), this);
    }

    @Override
    public Repository getSnapshotTo(final byte[] root) {
        return new IterableTestRepository(src.getSnapshotTo(root), this);
    }

    @Override
    public AccountState createAccount(final byte[] addr) {
        addAccount(addr);
        return src.createAccount(addr);
    }

    @Override
    public boolean isExist(final byte[] addr) {
        return src.isExist(addr);
    }

    @Override
    public AccountState getAccountState(final byte[] addr) {
        return src.getAccountState(addr);
    }

    @Override
    public void delete(final byte[] addr) {
        addAccount(addr);
        src.delete(addr);
    }

    @Override
    public BigInteger increaseNonce(final byte[] addr) {
        addAccount(addr);
        return src.increaseNonce(addr);
    }

    @Override
    public BigInteger setNonce(final byte[] addr, final BigInteger nonce) {
        return src.setNonce(addr, nonce);
    }

    @Override
    public BigInteger getNonce(final byte[] addr) {
        return src.getNonce(addr);
    }

    @Override
    public ContractDetails getContractDetails(final byte[] addr) {
        return new IterableContractDetails(src.getContractDetails(addr));
    }

    @Override
    public boolean hasContractDetails(final byte[] addr) {
        return src.hasContractDetails(addr);
    }

    @Override
    public void saveCode(final byte[] addr, final byte[] code) {
        addAccount(addr);
        src.saveCode(addr, code);
    }

    @Override
    public byte[] getCode(final byte[] addr) {
        if (environmental) {
            if (!src.isExist(addr)) {
                createAccount(addr);
            }
        }
        return src.getCode(addr);
    }

    @Override
    public byte[] getCodeHash(final byte[] addr) {
        return src.getCodeHash(addr);
    }

    @Override
    public void addStorageRow(final byte[] addr, final DataWord key, final DataWord value) {
        addStorageKey(addr, key);
        src.addStorageRow(addr, key, value);
    }

    @Override
    public DataWord getStorageValue(final byte[] addr, final DataWord key) {
        return src.getStorageValue(addr, key);
    }

    @Override
    public BigInteger getBalance(final byte[] addr) {
        if (environmental) {
            if (!src.isExist(addr)) {
                createAccount(addr);
            }
        }
        return src.getBalance(addr);
    }

    @Override
    public BigInteger addBalance(final byte[] addr, final BigInteger value) {
        addAccount(addr);
        return src.addBalance(addr, value);
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        final Set<byte[]> ret = new ByteArraySet();
        for (final byte[] account : accounts) {
            if (isExist(account)) {
                ret.add(account);
            }
        }
        return ret;
    }

    @Override
    public void dumpState(final Block block, final long gasUsed, final int txNumber, final byte[] txHash) {
        src.dumpState(block, gasUsed, txNumber, txHash);
    }

    @Override
    public void flush() {
        src.flush();
    }

    @Override
    public void flushNoReconnect() {
        src.flushNoReconnect();
    }

    @Override
    public void commit() {
        src.commit();
    }

    @Override
    public void rollback() {
        src.rollback();
    }

    @Override
    public void syncToRoot(final byte[] root) {
        src.syncToRoot(root);
    }

    @Override
    public boolean isClosed() {
        return src.isClosed();
    }

    @Override
    public void close() {
        src.close();
    }

    @Override
    public void reset() {
        src.reset();
    }

    @Override
    public void updateBatch(final HashMap<ByteArrayWrapper, AccountState> accountStates, final HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
        src.updateBatch(accountStates, contractDetailes);
        for (final ByteArrayWrapper wrapper : accountStates.keySet()) {
            addAccount(wrapper.getData());
        }

        for (final Map.Entry<ByteArrayWrapper, ContractDetails> entry : contractDetailes.entrySet()) {
            for (final DataWord key : entry.getValue().getStorageKeys()) {
                addStorageKey(entry.getKey().getData(), key);
            }
        }
    }

    @Override
    public byte[] getRoot() {
        return src.getRoot();
    }

    @Override
    public void loadAccount(final byte[] addr, final HashMap<ByteArrayWrapper, AccountState> cacheAccounts, final HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
        src.loadAccount(addr, cacheAccounts, cacheDetails);
    }

    @Override
    public int getStorageSize(final byte[] addr) {
        return src.getStorageSize(addr);
    }

    @Override
    public Set<DataWord> getStorageKeys(final byte[] addr) {
        return src.getStorageKeys(addr);
    }

    @Override
    public Map<DataWord, DataWord> getStorage(final byte[] addr, @Nullable final Collection<DataWord> keys) {
        return src.getStorage(addr, keys);
    }

    private class IterableContractDetails implements ContractDetails {
        final ContractDetails src;

        public IterableContractDetails(final ContractDetails src) {
            this.src = src;
        }

        @Override
        public void put(final DataWord key, final DataWord value) {
            addStorageKey(getAddress(), key);
            src.put(key, value);
        }

        @Override
        public DataWord get(final DataWord key) {
            return src.get(key);
        }

        @Override
        public byte[] getCode() {
            return src.getCode();
        }

        @Override
        public void setCode(final byte[] code) {
            addAccount(getAddress());
            src.setCode(code);
        }

        @Override
        public byte[] getCode(final byte[] codeHash) {
            return src.getCode(codeHash);
        }

        @Override
        public byte[] getStorageHash() {
            return src.getStorageHash();
        }

        @Override
        public void decode(final byte[] rlpCode) {
            src.decode(rlpCode);
        }

        @Override
        public boolean isDirty() {
            return src.isDirty();
        }

        @Override
        public void setDirty(final boolean dirty) {
            src.setDirty(dirty);
        }

        @Override
        public boolean isDeleted() {
            return src.isDeleted();
        }

        @Override
        public void setDeleted(final boolean deleted) {
            src.setDeleted(deleted);
        }

        @Override
        public byte[] getEncoded() {
            return src.getEncoded();
        }

        @Override
        public int getStorageSize() {
            final Set<DataWord> set = storageKeys.get(getAddress());
            return set == null ? 0 : set.size();
        }

        @Override
        public Set<DataWord> getStorageKeys() {
            return getStorage().keySet();
        }

        @Override
        public Map<DataWord, DataWord> getStorage(@Nullable final Collection<DataWord> keys) {
            throw new RuntimeException();
        }

        @Override
        public Map<DataWord, DataWord> getStorage() {
            final Map<DataWord, DataWord> ret = new HashMap<>();
            final Set<DataWord> set = storageKeys.get(getAddress());

            if (set == null) return Collections.emptyMap();

            for (final DataWord key : set) {
                final DataWord val = get(key);
                if (val != null && !val.isZero()) {
                    ret.put(key, get(key));
                }
            }
            return ret;
        }

        @Override
        public void setStorage(final Map<DataWord, DataWord> storage) {
            src.setStorage(storage);
        }

        @Override
        public void setStorage(final List<DataWord> storageKeys, final List<DataWord> storageValues) {
            src.setStorage(storageKeys, storageValues);
        }

        @Override
        public byte[] getAddress() {
            return src.getAddress();
        }

        @Override
        public void setAddress(final byte[] address) {
            src.setAddress(address);
        }

        @Override
        public ContractDetails clone() {
            return new IterableContractDetails(src.clone());
        }

        @Override
        public String toString() {
            return src.toString();
        }

        @Override
        public void syncStorage() {
            src.syncStorage();
        }

        @Override
        public ContractDetails getSnapshotTo(final byte[] hash) {
            return new IterableContractDetails(src.getSnapshotTo(hash));
        }
    }
}
