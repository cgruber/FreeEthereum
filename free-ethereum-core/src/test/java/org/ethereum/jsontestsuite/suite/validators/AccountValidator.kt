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

package org.ethereum.jsontestsuite.suite.validators

import org.ethereum.core.AccountState
import org.ethereum.db.ContractDetails
import org.ethereum.vm.DataWord
import org.spongycastle.util.encoders.Hex
import java.util.*

internal object AccountValidator {


    fun valid(address: String, expectedState: AccountState?, expectedDetails: ContractDetails?,
              currentState: AccountState?, currentDetails: ContractDetails?): List<String> {

        val results = ArrayList<String>()

        if (currentState == null || currentDetails == null) {
            val formattedString = String.format("Account: %s: expected but doesn't exist",
                    address)
            results.add(formattedString)
            return results
        }

        if (expectedState == null || expectedDetails == null) {
            val formattedString = String.format("Account: %s: unexpected account in the repository",
                    address)
            results.add(formattedString)
            return results
        }


        val expectedBalance = expectedState.balance
        if (currentState.balance.compareTo(expectedBalance) != 0) {
            val formattedString = String.format("Account: %s: has unexpected balance, expected balance: %s found balance: %s",
                    address, expectedBalance.toString(), currentState.balance.toString())
            results.add(formattedString)
        }

        val expectedNonce = expectedState.nonce
        if (currentState.nonce.compareTo(expectedNonce) != 0) {
            val formattedString = String.format("Account: %s: has unexpected nonce, expected nonce: %s found nonce: %s",
                    address, expectedNonce.toString(), currentState.nonce.toString())
            results.add(formattedString)
        }

        val code = currentDetails.code
        if (!Arrays.equals(expectedDetails.code, code)) {
            val formattedString = String.format("Account: %s: has unexpected code, expected code: %s found code: %s",
                    address, Hex.toHexString(expectedDetails.code), Hex.toHexString(currentDetails.code))
            results.add(formattedString)
        }


        // compare storage
        val expectedKeys = expectedDetails.storage.keys

        for (key in expectedKeys) {
            // force to load known keys to cache to enumerate them
            currentDetails[key]
        }

        val currentKeys = currentDetails.storage.keys
        val checked = HashSet<DataWord>()

        for (key in currentKeys) {

            val currentValue = currentDetails.storage[key]
            val expectedValue = expectedDetails.storage[key]
            if (expectedValue == null) {

                val formattedString = String.format("Account: %s: has unexpected storage data: %s = %s",
                        address,
                        key,
                        currentValue)

                results.add(formattedString)
                continue
            }

            if (expectedValue != currentValue) {

                val formattedString = String.format("Account: %s: has unexpected value, for key: %s , expectedValue: %s real value: %s",
                        address,
                        key.toString(),
                        expectedValue.toString(), currentValue.toString())
                results.add(formattedString)
                continue
            }

            checked.add(key)
        }

        expectedKeys
                .asSequence()
                .filterNot { checked.contains(it) }
                .mapTo(results) {
                    String.format("Account: %s: doesn't exist expected storage key: %s",
                            address, it.toString())
                }

        return results
    }
}
