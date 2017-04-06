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

import org.ethereum.util.ByteUtil;
import org.ethereum.vm.DataWord;
import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Roman Mandeleil
 * @since 28.06.2014
 */
class AccountState {

    private final byte[] address;
    private final byte[] balance;
    private final byte[] code;
    private final byte[] nonce;

    private final Map<DataWord, DataWord> storage = new HashMap<>();


    public AccountState(final byte[] address, final JSONObject accountState) {

        this.address = address;
        final String balance = accountState.get("balance").toString();
        final String code = (String) accountState.get("code");
        final String nonce = accountState.get("nonce").toString();

        final JSONObject store = (JSONObject) accountState.get("storage");

        this.balance = TestCase.toBigInt(balance).toByteArray();

        if (code != null && code.length() > 2)
            this.code = Hex.decode(code.substring(2));
        else
            this.code = ByteUtil.EMPTY_BYTE_ARRAY;

        this.nonce = TestCase.toBigInt(nonce).toByteArray();

        final int size = store.keySet().size();
        final Object[] keys = store.keySet().toArray();
        for (int i = 0; i < size; ++i) {

            final String keyS = keys[i].toString();
            final String valS = store.get(keys[i]).toString();

            final byte[] key = Utils.parseData(keyS);
            final byte[] value = Utils.parseData(valS);
            storage.put(new DataWord(key), new DataWord(value));
        }
    }

    public byte[] getAddress() {
        return address;
    }

    public byte[] getBalance() {
        return balance;
    }

    public BigInteger getBigIntegerBalance() {
        return new BigInteger(balance);
    }


    public byte[] getCode() {
        return code;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public long getNonceLong() {
        return new BigInteger(nonce).longValue();
    }


    public Map<DataWord, DataWord> getStorage() {
        return storage;
    }

    public List<String> compareToReal(final org.ethereum.core.AccountState state, final ContractDetailsImpl details) {

        final List<String> results = new ArrayList<>();

        final BigInteger expectedBalance = new BigInteger(1, this.getBalance());
        if (!state.getBalance().equals(expectedBalance)) {
            final String formattedString = String.format("Account: %s: has unexpected balance, expected balance: %s found balance: %s",
                    Hex.toHexString(this.address), expectedBalance.toString(), state.getBalance().toString());
            results.add(formattedString);
        }

        final BigInteger expectedNonce = new BigInteger(1, this.getNonce());
        if (!state.getNonce().equals(expectedNonce)) {
            state.getNonce();
            this.getNonce();
            final String formattedString = String.format("Account: %s: has unexpected nonce, expected nonce: %s found nonce: %s",
                    Hex.toHexString(this.address), expectedNonce.toString(), state.getNonce().toString());
            results.add(formattedString);
        }

        if (!Arrays.equals(details.getCode(), this.getCode())) {
            final String formattedString = String.format("Account: %s: has unexpected nonce, expected nonce: %s found nonce: %s",
                    Hex.toHexString(this.address), Hex.toHexString(this.getCode()), Hex.toHexString(details.getCode()));
            results.add(formattedString);
        }


        // compare storage
        final Set<DataWord> keys = details.getStorage().keySet();
        final Set<DataWord> expectedKeys = this.getStorage().keySet();
        final Set<DataWord> checked = new HashSet<>();

        for (final DataWord key : keys) {

            final DataWord value = details.getStorage().get(key);
            final DataWord expectedValue = this.getStorage().get(key);
            if (expectedValue == null) {

                final String formattedString = String.format("Account: %s: has unexpected storage data: %s = %s",
                        Hex.toHexString(this.address),
                        key.toString(),
                        value.toString());

                results.add(formattedString);

                continue;
            }

            if (!expectedValue.equals(value)) {

                final String formattedString = String.format("Account: %s: has unexpected value, for key: %s , expectedValue: %s real value: %s",
                        Hex.toHexString(this.address), key.toString(),
                        expectedValue.toString(), value.toString());
                results.add(formattedString);
                continue;
            }

            checked.add(key);
        }

        for (final DataWord key : expectedKeys) {
            if (!checked.contains(key)) {
                final String formattedString = String.format("Account: %s: doesn't exist expected storage key: %s",
                        Hex.toHexString(this.address), key.toString());
                results.add(formattedString);
            }
        }

        return results;
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "address=" + Hex.toHexString(address) +
                ", balance=" + Hex.toHexString(balance) +
                ", code=" + Hex.toHexString(code) +
                ", nonce=" + Hex.toHexString(nonce) +
                ", storage=" + storage +
                '}';
    }
}
