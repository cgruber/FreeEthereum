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
import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Repository;
import org.ethereum.vm.DataWord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Repository delegating all calls to the last Repository
 *
 * Created by Anton Nashatyrev on 22.12.2016.
 */
@Component
public class RepositoryWrapper implements Repository {

    @Autowired
    private
    BlockchainImpl blockchain;

    public RepositoryWrapper() {
    }

    @Override
    public AccountState createAccount(final byte[] addr) {
        return blockchain.getRepository().createAccount(addr);
    }

    @Override
    public boolean isExist(final byte[] addr) {
        return blockchain.getRepository().isExist(addr);
    }

    @Override
    public AccountState getAccountState(final byte[] addr) {
        return blockchain.getRepository().getAccountState(addr);
    }

    @Override
    public void delete(final byte[] addr) {
        blockchain.getRepository().delete(addr);
    }

    @Override
    public BigInteger increaseNonce(final byte[] addr) {
        return blockchain.getRepository().increaseNonce(addr);
    }

    @Override
    public BigInteger setNonce(final byte[] addr, final BigInteger nonce) {
        return blockchain.getRepository().setNonce(addr, nonce);
    }

    @Override
    public BigInteger getNonce(final byte[] addr) {
        return blockchain.getRepository().getNonce(addr);
    }

    @Override
    public ContractDetails getContractDetails(final byte[] addr) {
        return blockchain.getRepository().getContractDetails(addr);
    }

    @Override
    public boolean hasContractDetails(final byte[] addr) {
        return blockchain.getRepository().hasContractDetails(addr);
    }

    @Override
    public void saveCode(final byte[] addr, final byte[] code) {
        blockchain.getRepository().saveCode(addr, code);
    }

    @Override
    public byte[] getCode(final byte[] addr) {
        return blockchain.getRepository().getCode(addr);
    }

    @Override
    public byte[] getCodeHash(final byte[] addr) {
        return blockchain.getRepository().getCodeHash(addr);
    }

    @Override
    public void addStorageRow(final byte[] addr, final DataWord key, final DataWord value) {
        blockchain.getRepository().addStorageRow(addr, key, value);
    }

    @Override
    public DataWord getStorageValue(final byte[] addr, final DataWord key) {
        return blockchain.getRepository().getStorageValue(addr, key);
    }

    @Override
    public BigInteger getBalance(final byte[] addr) {
        return blockchain.getRepository().getBalance(addr);
    }

    @Override
    public BigInteger addBalance(final byte[] addr, final BigInteger value) {
        return blockchain.getRepository().addBalance(addr, value);
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        return blockchain.getRepository().getAccountsKeys();
    }

    @Override
    public void dumpState(final Block block, final long gasUsed, final int txNumber, final byte[] txHash) {
        blockchain.getRepository().dumpState(block, gasUsed, txNumber, txHash);
    }

    @Override
    public Repository startTracking() {
        return blockchain.getRepository().startTracking();
    }

    @Override
    public void flush() {
        blockchain.getRepository().flush();
    }

    @Override
    public void flushNoReconnect() {
        blockchain.getRepository().flushNoReconnect();
    }

    @Override
    public void commit() {
        blockchain.getRepository().commit();
    }

    @Override
    public void rollback() {
        blockchain.getRepository().rollback();
    }

    @Override
    public void syncToRoot(final byte[] root) {
        blockchain.getRepository().syncToRoot(root);
    }

    @Override
    public boolean isClosed() {
        return blockchain.getRepository().isClosed();
    }

    @Override
    public void close() {
        blockchain.getRepository().close();
    }

    @Override
    public void reset() {
        blockchain.getRepository().reset();
    }

    @Override
    public void updateBatch(final HashMap<ByteArrayWrapper, AccountState> accountStates, final HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
        blockchain.getRepository().updateBatch(accountStates, contractDetailes);
    }

    @Override
    public byte[] getRoot() {
        return blockchain.getRepository().getRoot();
    }

    @Override
    public void loadAccount(final byte[] addr, final HashMap<ByteArrayWrapper, AccountState> cacheAccounts, final HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
        blockchain.getRepository().loadAccount(addr, cacheAccounts, cacheDetails);
    }

    @Override
    public Repository getSnapshotTo(final byte[] root) {
        return blockchain.getRepository().getSnapshotTo(root);
    }

    @Override
    public int getStorageSize(final byte[] addr) {
        return blockchain.getRepository().getStorageSize(addr);
    }

    @Override
    public Set<DataWord> getStorageKeys(final byte[] addr) {
        return blockchain.getRepository().getStorageKeys(addr);
    }

    @Override
    public Map<DataWord, DataWord> getStorage(final byte[] addr, @Nullable final Collection<DataWord> keys) {
        return blockchain.getRepository().getStorage(addr, keys);
    }
}
