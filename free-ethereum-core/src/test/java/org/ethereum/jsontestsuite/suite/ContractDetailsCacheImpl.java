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

import org.ethereum.db.ContractDetails;
import org.ethereum.trie.SecureTrie;
import org.ethereum.util.RLP;
import org.ethereum.vm.DataWord;

import java.util.*;

import static java.util.Collections.unmodifiableMap;

/**
 * @author Roman Mandeleil
 * @since 24.06.2014
 */
public class ContractDetailsCacheImpl extends AbstractContractDetails {

    private final ContractDetails origContract;
    private Map<DataWord, DataWord> storage = new HashMap<>();

    public ContractDetailsCacheImpl(final ContractDetails origContract) {
        this.origContract = origContract;
        if (origContract != null) {
            if (origContract instanceof AbstractContractDetails) {
                setCodes(((AbstractContractDetails) this.origContract).getCodes());
            } else {
                setCode(origContract.getCode());
            }
        }
    }

    @Override
    public void put(final DataWord key, final DataWord value) {
        storage.put(key, value);
        this.setDirty(true);
    }

    @Override
    public DataWord get(final DataWord key) {

        DataWord value = storage.get(key);
        if (value != null)
            value = value.clone();
        else{
            if (origContract == null) return null;
            value = origContract.get(key);
            storage.put(key.clone(), value == null ? DataWord.ZERO.clone() : value.clone());
        }

        if (value == null || value.isZero())
            return null;
        else
            return value;
    }

    @Override
    public byte[] getStorageHash() { // todo: unsupported

        final SecureTrie storageTrie = new SecureTrie((byte[]) null);

        for (final DataWord key : storage.keySet()) {

            final DataWord value = storage.get(key);

            storageTrie.put(key.getData(),
                    RLP.encodeElement(value.getNoLeadZeroesData()));
        }

        return storageTrie.getRootHash();
    }

    @Override
    public void decode(final byte[] rlpCode) {
        throw new RuntimeException("Not supported by this implementation.");
    }

    @Override
    public byte[] getEncoded() {
        throw new RuntimeException("Not supported by this implementation.");
    }

    @Override
    public Map<DataWord, DataWord> getStorage() {
        return unmodifiableMap(storage);
    }

    @Override
    public void setStorage(final Map<DataWord, DataWord> storage) {
        this.storage = storage;
    }

    @Override
    public Map<DataWord, DataWord> getStorage(final Collection<DataWord> keys) {
        if (keys == null) return getStorage();

        final Map<DataWord, DataWord> result = new HashMap<>();
        for (final DataWord key : keys) {
            result.put(key, storage.get(key));
        }
        return unmodifiableMap(result);
    }

    @Override
    public int getStorageSize() {
        return (origContract == null)
                ? storage.size()
                : origContract.getStorageSize();
    }

    @Override
    public Set<DataWord> getStorageKeys() {
        return (origContract == null)
                ? storage.keySet()
                : origContract.getStorageKeys();
    }

    @Override
    public void setStorage(final List<DataWord> storageKeys, final List<DataWord> storageValues) {

        for (int i = 0; i < storageKeys.size(); ++i){

            final DataWord key = storageKeys.get(i);
            final DataWord value = storageValues.get(i);

            if (value.isZero())
                storage.put(key, null);
        }

    }

    @Override
    public byte[] getAddress() {
         return (origContract == null) ? null : origContract.getAddress();
    }

    @Override
    public void setAddress(final byte[] address) {
        if (origContract != null) origContract.setAddress(address);
    }

    @Override
    public ContractDetails clone() {

        final ContractDetailsCacheImpl contractDetails = new ContractDetailsCacheImpl(origContract);

        final Object storageClone = ((HashMap<DataWord, DataWord>) storage).clone();

        contractDetails.setCode(this.getCode());
        contractDetails.setStorage( (HashMap<DataWord, DataWord>) storageClone);
        return contractDetails;
    }

    @Override
    public void syncStorage() {
        if (origContract != null) origContract.syncStorage();
    }

    public void commit(){

        if (origContract == null) return;

        for (final DataWord key : storage.keySet()) {
            origContract.put(key, storage.get(key));
        }

        if (origContract instanceof AbstractContractDetails) {
            ((AbstractContractDetails) origContract).appendCodes(getCodes());
        } else {
            origContract.setCode(getCode());
        }
        origContract.setDirty(this.isDirty() || origContract.isDirty());
    }


    @Override
    public ContractDetails getSnapshotTo(final byte[] hash) {
        throw new UnsupportedOperationException("No snapshot option during cache state");
    }
}

