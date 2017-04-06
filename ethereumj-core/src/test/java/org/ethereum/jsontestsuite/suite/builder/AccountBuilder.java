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

import org.ethereum.config.SystemProperties;
import org.ethereum.core.AccountState;
import org.ethereum.jsontestsuite.suite.ContractDetailsImpl;
import org.ethereum.jsontestsuite.suite.model.AccountTck;
import org.ethereum.vm.DataWord;

import java.util.HashMap;
import java.util.Map;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.jsontestsuite.suite.Utils.parseData;
import static org.ethereum.util.Utils.unifiedNumericToBigInteger;

public class AccountBuilder {

    public static StateWrap build(final AccountTck account) {

        final ContractDetailsImpl details = new ContractDetailsImpl();
        details.setCode(parseData(account.getCode()));
        details.setStorage(convertStorage(account.getStorage()));

        final AccountState state = new AccountState(SystemProperties.getDefault())
                .withBalanceIncrement(unifiedNumericToBigInteger(account.getBalance()))
                .withNonce(unifiedNumericToBigInteger(account.getNonce()))
                .withStateRoot(details.getStorageHash())
                .withCodeHash(sha3(details.getCode()));

        return new StateWrap(state, details);
    }


    private static Map<DataWord, DataWord> convertStorage(final Map<String, String> storageTck) {

        final Map<DataWord, DataWord> storage = new HashMap<>();

        for (final String keyTck : storageTck.keySet()) {
            final String valueTck = storageTck.get(keyTck);

            final DataWord key = new DataWord(parseData(keyTck));
            final DataWord value = new DataWord(parseData(valueTck));

            storage.put(key, value);
        }

        return storage;
    }


    public static class StateWrap {

        final AccountState accountState;
        final ContractDetailsImpl contractDetails;

        public StateWrap(final AccountState accountState, final ContractDetailsImpl contractDetails) {
            this.accountState = accountState;
            this.contractDetails = contractDetails;
        }

        public AccountState getAccountState() {
            return accountState;
        }

        public ContractDetailsImpl getContractDetails() {
            return contractDetails;
        }
    }
}
