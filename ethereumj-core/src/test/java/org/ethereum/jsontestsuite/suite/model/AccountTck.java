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

package org.ethereum.jsontestsuite.suite.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman Mandeleil
 * @since 28.06.2014
 */
public class AccountTck {

    private String balance;
    private String code;
    private String nonce;

    private Map<String, String> storage = new HashMap<>();

    private String privateKey;

    public AccountTck() {
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(final String balance) {
        this.balance = balance;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    public Map<String, String> getStorage() {
        return storage;
    }

    public void setStorage(final Map<String, String> storage) {
        this.storage = storage;
    }

    @Override
    public String toString() {
        return "AccountState2{" +
                "balance='" + balance + '\'' +
                ", code='" + code + '\'' +
                ", nonce='" + nonce + '\'' +
                ", storage=" + storage +
                '}';
    }
}
