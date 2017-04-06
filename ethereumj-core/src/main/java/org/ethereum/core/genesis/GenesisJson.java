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

package org.ethereum.core.genesis;

import java.util.Map;

public class GenesisJson {

    String mixhash;
    String coinbase;
    String timestamp;
    String parentHash;
    String extraData;
    String gasLimit;
    String nonce;
    String difficulty;

    private Map<String, AllocatedAccount> alloc;

    private GenesisConfig config;

    public GenesisJson() {
    }


    public String getMixhash() {
        return mixhash;
    }

    public void setMixhash(final String mixhash) {
        this.mixhash = mixhash;
    }

    public String getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(final String coinbase) {
        this.coinbase = coinbase;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(final String parentHash) {
        this.parentHash = parentHash;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(final String extraData) {
        this.extraData = extraData;
    }

    public String getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(final String gasLimit) {
        this.gasLimit = gasLimit;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(final String difficulty) {
        this.difficulty = difficulty;
    }

    public Map<String, AllocatedAccount> getAlloc() {
        return alloc;
    }

    public void setAlloc(final Map<String, AllocatedAccount> alloc) {
        this.alloc = alloc;
    }

    public GenesisConfig getConfig() {
        return config;
    }

    public void setConfig(final GenesisConfig config) {
        this.config = config;
    }

    public static class AllocatedAccount {

        public Map<String, String> storage;
        public String nonce;
        public String code;
        public String balance;

    }
}
