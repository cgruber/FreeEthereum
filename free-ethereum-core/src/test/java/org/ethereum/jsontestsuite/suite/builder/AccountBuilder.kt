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

import org.ethereum.config.SystemProperties
import org.ethereum.core.AccountState
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.jsontestsuite.suite.ContractDetailsImpl
import org.ethereum.jsontestsuite.suite.Utils.parseData
import org.ethereum.jsontestsuite.suite.model.AccountTck
import org.ethereum.util.Utils.unifiedNumericToBigInteger
import org.ethereum.vm.DataWord
import java.util.*

object AccountBuilder {

    fun build(account: AccountTck): StateWrap {

        val details = ContractDetailsImpl()
        details.code = parseData(account.code)
        details.storage = convertStorage(account.storage)

        val state = AccountState(SystemProperties.getDefault())
                .withBalanceIncrement(unifiedNumericToBigInteger(account.balance))
                .withNonce(unifiedNumericToBigInteger(account.nonce))
                .withStateRoot(details.storageHash)
                .withCodeHash(sha3(details.code))

        return StateWrap(state, details)
    }


    private fun convertStorage(storageTck: Map<String, String>): Map<DataWord, DataWord> {

        val storage = HashMap<DataWord, DataWord>()

        for (keyTck in storageTck.keys) {
            val valueTck = storageTck[keyTck]

            val key = DataWord(parseData(keyTck))
            val value = DataWord(parseData(valueTck))

            storage.put(key, value)
        }

        return storage
    }


    class StateWrap(val accountState: AccountState, val contractDetails: ContractDetailsImpl)
}
