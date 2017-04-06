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

public class EnvTck {

    private String currentCoinbase;
    private String currentDifficulty;
    private String currentGasLimit;
    private String currentNumber;
    private String currentTimestamp;
    private String previousHash;

    public EnvTck() {
    }

    public String getCurrentCoinbase() {
        return currentCoinbase;
    }

    public void setCurrentCoinbase(final String currentCoinbase) {
        this.currentCoinbase = currentCoinbase;
    }

    public String getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(final String currentDifficulty) {
        this.currentDifficulty = currentDifficulty;
    }

    public String getCurrentGasLimit() {
        return currentGasLimit;
    }

    public void setCurrentGasLimit(final String currentGasLimit) {
        this.currentGasLimit = currentGasLimit;
    }

    public String getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(final String currentNumber) {
        this.currentNumber = currentNumber;
    }

    public String getCurrentTimestamp() {
        return currentTimestamp;
    }

    public void setCurrentTimestamp(final String currentTimestamp) {
        this.currentTimestamp = currentTimestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(final String previousHash) {
        this.previousHash = previousHash;
    }
}
