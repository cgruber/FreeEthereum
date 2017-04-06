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

public class BlockHeaderTck {

    private String bloom;
    private String coinbase;
    private String difficulty;
    private String extraData;
    private String gasLimit;
    private String gasUsed;
    private String hash;
    private String mixHash;
    private String nonce;
    private String number;
    private String parentHash;
    private String receiptTrie;
    private String seedHash;
    private String stateRoot;
    private String timestamp;
    private String transactionsTrie;
    private String uncleHash;

    public BlockHeaderTck() {
    }

    public String getBloom() {
        return bloom;
    }

    public void setBloom(final String bloom) {
        this.bloom = bloom;
    }

    public String getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(final String coinbase) {
        this.coinbase = coinbase;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(final String difficulty) {
        this.difficulty = difficulty;
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

    public String getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(final String gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(final String hash) {
        this.hash = hash;
    }

    public String getMixHash() {
        return mixHash;
    }

    public void setMixHash(final String mixHash) {
        this.mixHash = mixHash;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(final String parentHash) {
        this.parentHash = parentHash;
    }

    public String getReceiptTrie() {
        return receiptTrie;
    }

    public void setReceiptTrie(final String receiptTrie) {
        this.receiptTrie = receiptTrie;
    }

    public String getSeedHash() {
        return seedHash;
    }

    public void setSeedHash(final String seedHash) {
        this.seedHash = seedHash;
    }

    public String getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(final String stateRoot) {
        this.stateRoot = stateRoot;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionsTrie() {
        return transactionsTrie;
    }

    public void setTransactionsTrie(final String transactionsTrie) {
        this.transactionsTrie = transactionsTrie;
    }

    public String getUncleHash() {
        return uncleHash;
    }

    public void setUncleHash(final String uncleHash) {
        this.uncleHash = uncleHash;
    }

    @Override
    public String toString() {
        return "BlockHeader{" +
                "bloom='" + bloom + '\'' +
                ", coinbase='" + coinbase + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", extraData='" + extraData + '\'' +
                ", gasLimit='" + gasLimit + '\'' +
                ", gasUsed='" + gasUsed + '\'' +
                ", hash='" + hash + '\'' +
                ", mixHash='" + mixHash + '\'' +
                ", nonce='" + nonce + '\'' +
                ", number='" + number + '\'' +
                ", parentHash='" + parentHash + '\'' +
                ", receiptTrie='" + receiptTrie + '\'' +
                ", seedHash='" + seedHash + '\'' +
                ", stateRoot='" + stateRoot + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", transactionsTrie='" + transactionsTrie + '\'' +
                ", uncleHash='" + uncleHash + '\'' +
                '}';
    }
}
