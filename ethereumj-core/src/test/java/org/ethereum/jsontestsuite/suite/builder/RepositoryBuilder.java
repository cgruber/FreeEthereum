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

package org.ethereum.jsontestsuite.suite.builder;

import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.datasource.NoDeleteSource;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.jsontestsuite.suite.ContractDetailsCacheImpl;
import org.ethereum.jsontestsuite.suite.IterableTestRepository;
import org.ethereum.jsontestsuite.suite.model.AccountTck;

import java.util.HashMap;
import java.util.Map;

import static org.ethereum.jsontestsuite.suite.Utils.parseData;
import static org.ethereum.util.ByteUtil.wrap;

public class RepositoryBuilder {

    public static Repository build(final Map<String, AccountTck> accounts) {
        final HashMap<ByteArrayWrapper, AccountState> stateBatch = new HashMap<>();
        final HashMap<ByteArrayWrapper, ContractDetails> detailsBatch = new HashMap<>();

        for (final String address : accounts.keySet()) {

            final AccountTck accountTCK = accounts.get(address);
            final AccountBuilder.StateWrap stateWrap = AccountBuilder.build(accountTCK);

            final AccountState state = stateWrap.getAccountState();
            final ContractDetails details = stateWrap.getContractDetails();

            stateBatch.put(wrap(parseData(address)), state);

            final ContractDetailsCacheImpl detailsCache = new ContractDetailsCacheImpl(details);
            detailsCache.setDirty(true);

            detailsBatch.put(wrap(parseData(address)), detailsCache);
        }

        final Repository repositoryDummy = new IterableTestRepository(new RepositoryRoot(new NoDeleteSource<>(new HashMapDB<>())));
        final Repository track = repositoryDummy.startTracking();

        track.updateBatch(stateBatch, detailsBatch);
        track.commit();
        repositoryDummy.commit();

        return repositoryDummy;
    }
}
