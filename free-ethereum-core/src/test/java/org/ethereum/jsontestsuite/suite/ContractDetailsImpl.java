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

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.DbSource;
import org.ethereum.datasource.Source;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.trie.SecureTrie;
import org.ethereum.util.RLP;
import org.ethereum.vm.DataWord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.util.ByteUtil.wrap;

/**
 * @author Roman Mandeleil
 * @since 24.06.2014
 */
@Component
@Scope("prototype")
public class ContractDetailsImpl extends AbstractContractDetails {
    private static final Logger logger = LoggerFactory.getLogger("general");

    private CommonConfig commonConfig = CommonConfig.getDefault();

    private SystemProperties config = SystemProperties.getDefault();

    private DbSource dataSource;
    private boolean externalStorage;
    private byte[] address = EMPTY_BYTE_ARRAY;
    private Set<ByteArrayWrapper> keys = new HashSet<>();
    private SecureTrie storageTrie = new SecureTrie((byte[]) null);
    private DbSource externalStorageDataSource;

    /** Tests only **/
    public ContractDetailsImpl() {
    }

    private ContractDetailsImpl(final byte[] address, final SecureTrie storageTrie, final Map<ByteArrayWrapper, byte[]> codes) {
        this.address = address;
        this.storageTrie = storageTrie;
        setCodes(codes);
    }

    private void addKey(final byte[] key) {
        keys.add(wrap(key));
    }

    private void removeKey(final byte[] key) {
//        keys.remove(wrap(key)); // TODO: we can't remove keys , because of fork branching
    }

    @Override
    public void put(final DataWord key, final DataWord value) {
        if (value.equals(DataWord.ZERO)) {
            storageTrie.delete(key.getData());
            removeKey(key.getData());
        } else {
            storageTrie.put(key.getData(), RLP.encodeElement(value.getNoLeadZeroesData()));
            addKey(key.getData());
        }

        this.setDirty(true);
    }

    @Override
    public DataWord get(final DataWord key) {
        DataWord result = null;

        final byte[] data = storageTrie.get(key.getData());
        if (data.length > 0) {
            final byte[] dataDecoded = RLP.decode2(data).get(0).getRLPData();
            result = new DataWord(dataDecoded);
        }

        return result;
    }

    @Override
    public byte[] getStorageHash() {
        return storageTrie.getRootHash();
    }

    @Override
    public void decode(final byte[] rlpCode) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public byte[] getEncoded() {
        throw new RuntimeException("Not supported");
    }

//    @Override
//    public Map<DataWord, DataWord> getStorage(final Collection<DataWord> keys) {
//        final Map<DataWord, DataWord> storage = new HashMap<>();
//        if (keys == null) {
//            for (final ByteArrayWrapper keyBytes : this.keys) {
//                final DataWord key = new DataWord(keyBytes);
//                final DataWord value = get(key);
//
//                // we check if the value is not null,
//                // cause we keep all historical keys
//                if (value != null)
//                    storage.put(key, value);
//            }
//        } else {
//            for (final DataWord key : keys) {
//                final DataWord value = get(key);
//
//                // we check if the value is not null,
//                // cause we keep all historical keys
//                if (value != null)
//                    storage.put(key, value);
//            }
//        }
//
//        return storage;
//    }

    @Override
    public Map<DataWord, DataWord> getStorage() {
        return getStorage(null);
    }

//    @Override
//    public void setStorage(final Map<DataWord, DataWord> storage) {
//        for (final DataWord key : storage.keySet()) {
//            put(key, storage.get(key));
//        }
//    }

    @Override
    public void setStorage(@NotNull Map<DataWord, ? extends DataWord> map) {
        for (final DataWord key : map.keySet()) {
            put(key, map.get(key));
        }

    }

    @Override
    public int getStorageSize() {
        return keys.size();
    }

//    @Override
//    public void setStorage(final List<DataWord> storageKeys, final List<DataWord> storageValues) {
//
//        for (int i = 0; i < storageKeys.size(); ++i)
//            put(storageKeys.get(i), storageValues.get(i));
//    }

    @Override
    public Set<DataWord> getStorageKeys() {
        final Set<DataWord> result = new HashSet<>();
        for (final ByteArrayWrapper key : keys) {
            result.add(new DataWord(key));
        }
        return result;
    }

    @Override
    public byte[] getAddress() {
        return address;
    }

    @Override
    public void setAddress(final byte[] address) {
        this.address = address;
    }

    public SecureTrie getStorageTrie() {
        return storageTrie;
    }

    @Override
    public void syncStorage() {
    }

    @Override
    public ContractDetails clone() {

        // FIXME: clone is not working now !!!
        // FIXME: should be fixed

//        storageTrie.getRoot();

        return new ContractDetailsImpl(address, null, getCodes());
    }

    @Override
    public ContractDetails getSnapshotTo(final byte[] hash) {

        final Source<byte[], byte[]> cache = this.storageTrie.getCache();

        final SecureTrie snapStorage = wrap(hash).equals(wrap(HashUtil.INSTANCE.getEMPTY_TRIE_HASH())) ?
            new SecureTrie(cache, "".getBytes()):
            new SecureTrie(cache, hash);

        final ContractDetailsImpl details = new ContractDetailsImpl(this.address, snapStorage, getCodes());
        details.externalStorage = this.externalStorage;
        details.externalStorageDataSource = this.externalStorageDataSource;
        details.keys = this.keys;
        details.config = config;
        details.commonConfig = commonConfig;
        details.dataSource = dataSource;

        return details;
    }

    @NotNull
    @Override
    public Map<DataWord, DataWord> getStorage(@Nullable Collection<? extends DataWord> keys) {
        final Map<DataWord, DataWord> storage = new HashMap<>();
        if (keys == null) {
            for (final ByteArrayWrapper keyBytes : this.keys) {
                final DataWord key = new DataWord(keyBytes);
                final DataWord value = get(key);

                // we check if the value is not null,
                // cause we keep all historical keys
                if (value != null)
                    storage.put(key, value);
            }
        } else {
            for (final DataWord key : keys) {
                final DataWord value = get(key);

                // we check if the value is not null,
                // cause we keep all historical keys
                if (value != null)
                    storage.put(key, value);
            }
        }

        return storage;

    }

    @Override
    public void setStorage(@NotNull List<? extends DataWord> storageKeys, @NotNull List<? extends DataWord> storageValues) {
        for (int i = 0; i < storageKeys.size(); ++i)
            put(storageKeys.get(i), storageValues.get(i));

    }
}

