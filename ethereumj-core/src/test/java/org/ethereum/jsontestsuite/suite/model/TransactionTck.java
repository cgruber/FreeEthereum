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

/**
 * @author Roman Mandeleil
 * @since 28.06.2014
 */
public class TransactionTck {

    private String data;
    private String gasLimit;
    private String gasPrice;
    private String nonce;
    private String r;
    private String s;
    private String to;
    private String v;
    private String value;
    private String secretKey;


    public TransactionTck() {
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public String getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(final String gasLimit) {
        this.gasLimit = gasLimit;
    }

    public String getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(final String gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    public String getR() {
        return r;
    }

    public void setR(final String r) {
        this.r = r;
    }

    public String getS() {
        return s;
    }

    public void setS(final String s) {
        this.s = s;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public String getV() {
        return v;
    }

    public void setV(final String v) {
        this.v = v;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "TransactionTck{" +
                "data='" + data + '\'' +
                ", gasLimit='" + gasLimit + '\'' +
                ", gasPrice='" + gasPrice + '\'' +
                ", nonce='" + nonce + '\'' +
                ", r='" + r + '\'' +
                ", s='" + s + '\'' +
                ", to='" + to + '\'' +
                ", v='" + v + '\'' +
                ", value='" + value + '\'' +
                ", secretKey='" + secretKey + '\'' +
                '}';
    }
}
