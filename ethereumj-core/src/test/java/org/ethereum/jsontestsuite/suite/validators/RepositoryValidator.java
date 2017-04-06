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
import org.ethereum.core.Repository;
import org.ethereum.db.ContractDetails;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.ethereum.util.ByteUtil.difference;

public class RepositoryValidator {

    public static List<String> valid(final Repository currentRepository, final Repository postRepository) {

        final List<String> results = new ArrayList<>();

        final Set<byte[]> expectedKeys = postRepository.getAccountsKeys();

        for (final byte[] key : expectedKeys) {
            // force to load known accounts to cache to enumerate them
            currentRepository.getAccountState(key);
        }

        final Set<byte[]> currentKeys = currentRepository.getAccountsKeys();

        if (expectedKeys.size() != currentKeys.size()) {

            final String out =
                    String.format("The size of the repository is invalid \n expected: %d, \n current: %d",
                            expectedKeys.size(), currentKeys.size());
            results.add(out);
        }

        for (final byte[] address : currentKeys) {

            final AccountState state = currentRepository.getAccountState(address);
            final ContractDetails details = currentRepository.getContractDetails(address);

            final AccountState postState = postRepository.getAccountState(address);
            final ContractDetails postDetails = postRepository.getContractDetails(address);

            final List<String> accountResult =
                AccountValidator.valid(Hex.toHexString(address), postState, postDetails, state, details);

            results.addAll(accountResult);
        }

        final Set<byte[]> expectedButAbsent = difference(expectedKeys, currentKeys);
        for (final byte[] address : expectedButAbsent) {
            final String formattedString = String.format("Account: %s: expected but doesn't exist",
                    Hex.toHexString(address));
            results.add(formattedString);
        }

        // Compare roots
        final String postRoot = Hex.toHexString(postRepository.getRoot());
        final String currRoot = Hex.toHexString(currentRepository.getRoot());

        if (!postRoot.equals(currRoot)){

            final String formattedString = String.format("Root hash don't much: expected: %s current: %s",
                    postRoot, currRoot);
            results.add(formattedString);
        }

        return results;
    }

}
