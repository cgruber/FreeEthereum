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

package org.ethereum.jsontestsuite.suite.builder

import org.ethereum.core.AccountState
import org.ethereum.core.Repository
import org.ethereum.datasource.NoDeleteSource
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.db.ContractDetails
import org.ethereum.db.RepositoryRoot
import org.ethereum.jsontestsuite.suite.ContractDetailsCacheImpl
import org.ethereum.jsontestsuite.suite.IterableTestRepository
import org.ethereum.jsontestsuite.suite.Utils.parseData
import org.ethereum.jsontestsuite.suite.model.AccountTck
import org.ethereum.util.ByteUtil.wrap
import java.util.*

object RepositoryBuilder {

    fun build(accounts: Map<String, AccountTck>): Repository {
        val stateBatch = HashMap<ByteArrayWrapper, AccountState>()
        val detailsBatch = HashMap<ByteArrayWrapper, ContractDetails>()

        for (address in accounts.keys) {

            val accountTCK = accounts[address]
            val stateWrap = AccountBuilder.build(accountTCK!!)

            val state = stateWrap.accountState
            val details = stateWrap.contractDetails

            stateBatch.put(wrap(parseData(address)), state)

            val detailsCache = ContractDetailsCacheImpl(details)
            detailsCache.isDirty = true

            detailsBatch.put(wrap(parseData(address)), detailsCache)
        }

        val repositoryDummy = IterableTestRepository(RepositoryRoot(NoDeleteSource(HashMapDB<ByteArray>())))
        val track = repositoryDummy.startTracking()

        track.updateBatch(stateBatch, detailsBatch)
        track.commit()
        repositoryDummy.commit()

        return repositoryDummy
    }
}
