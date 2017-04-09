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

package org.ethereum.jsontestsuite.suite.validators;

import org.ethereum.core.AccountState;
import org.ethereum.db.ContractDetails;
import org.ethereum.vm.DataWord;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

class AccountValidator {


    public static List<String> valid(final String address, final AccountState expectedState, final ContractDetails expectedDetails,
                                     final AccountState currentState, final ContractDetails currentDetails) {

        final List<String> results = new ArrayList<>();

        if (currentState == null || currentDetails == null){
            final String formattedString = String.format("Account: %s: expected but doesn't exist",
                    address);
            results.add(formattedString);
            return results;
        }

        if (expectedState == null || expectedDetails == null){
            final String formattedString = String.format("Account: %s: unexpected account in the repository",
                    address);
            results.add(formattedString);
            return results;
        }


        final BigInteger expectedBalance = expectedState.getBalance();
        if (currentState.getBalance().compareTo(expectedBalance) != 0) {
            final String formattedString = String.format("Account: %s: has unexpected balance, expected balance: %s found balance: %s",
                    address, expectedBalance.toString(), currentState.getBalance().toString());
            results.add(formattedString);
        }

        final BigInteger expectedNonce = expectedState.getNonce();
        if (currentState.getNonce().compareTo(expectedNonce) != 0) {
            final String formattedString = String.format("Account: %s: has unexpected nonce, expected nonce: %s found nonce: %s",
                    address, expectedNonce.toString(), currentState.getNonce().toString());
            results.add(formattedString);
        }

        final byte[] code = currentDetails.getCode();
        if (!Arrays.equals(expectedDetails.getCode(), code)) {
            final String formattedString = String.format("Account: %s: has unexpected code, expected code: %s found code: %s",
                    address, Hex.toHexString(expectedDetails.getCode()), Hex.toHexString(currentDetails.getCode()));
            results.add(formattedString);
        }


        // compare storage
        final Set<DataWord> expectedKeys = expectedDetails.getStorage().keySet();

        for (final DataWord key : expectedKeys) {
            // force to load known keys to cache to enumerate them
            currentDetails.get(key);
        }

        final Set<DataWord> currentKeys = currentDetails.getStorage().keySet();
        final Set<DataWord> checked = new HashSet<>();

        for (final DataWord key : currentKeys) {

            final DataWord currentValue = currentDetails.getStorage().get(key);
            final DataWord expectedValue = expectedDetails.getStorage().get(key);
            if (expectedValue == null) {

                final String formattedString = String.format("Account: %s: has unexpected storage data: %s = %s",
                        address,
                        key,
                        currentValue);

                results.add(formattedString);
                continue;
            }

            if (!expectedValue.equals(currentValue)) {

                final String formattedString = String.format("Account: %s: has unexpected value, for key: %s , expectedValue: %s real value: %s",
                        address,
                        key.toString(),
                        expectedValue.toString(), currentValue.toString());
                results.add(formattedString);
                continue;
            }

            checked.add(key);
        }

        for (final DataWord key : expectedKeys) {
            if (!checked.contains(key)) {
                final String formattedString = String.format("Account: %s: doesn't exist expected storage key: %s",
                        address, key.toString());
                results.add(formattedString);
            }
        }

        return results;
    }
}
