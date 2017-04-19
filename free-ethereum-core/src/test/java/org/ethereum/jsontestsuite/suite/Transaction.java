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

import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.Arrays;

import static org.ethereum.util.ByteUtil.toHexString;

/**
 * @author Roman Mandeleil
 * @since 28.06.2014
 */
class Transaction {

    final byte[] gasLimit;
    final long gasPrice;
    final long nonce;
    final long value;
    private final byte[] data;
    private final byte[] secretKey;
    private final byte[] to;

/* e.g.
    "transaction" : {
            "data" : "",
            "gasLimit" : "10000",
            "gasPrice" : "1",
            "nonce" : "0",
            "secretKey" : "45a915e4d060149eb4365960e6a7a45f334393093061116b197e3240065ff2d8",
            "to" : "095e7baea6a6c7c4c2dfeb977efac326af552d87",
            "value" : "100000"
}
*/

    public Transaction(final JSONObject callCreateJSON) {

        final String dataStr = callCreateJSON.get("data").toString();
        final String gasLimitStr = Utils.parseUnidentifiedBase(callCreateJSON.get("gasLimit").toString());
        final String gasPriceStr = Utils.parseUnidentifiedBase(callCreateJSON.get("gasPrice").toString());
        final String nonceStr = callCreateJSON.get("nonce").toString();
        final String secretKeyStr = callCreateJSON.get("secretKey").toString();
        final String toStr = callCreateJSON.get("to").toString();
        final String valueStr = callCreateJSON.get("value").toString();

        this.data = Utils.parseData(dataStr);
        this.gasLimit = !gasLimitStr.isEmpty() ? new BigInteger(gasLimitStr).toByteArray() : new byte[]{0};
        this.gasPrice = Utils.parseLong(gasPriceStr);
        this.nonce = Utils.parseLong(nonceStr);
        this.secretKey = Utils.parseData(secretKeyStr);
        this.to = Utils.parseData(toStr);
        this.value = Utils.parseLong(valueStr);
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getGasLimit() {
        return gasLimit;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public long getNonce() {
        return nonce;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public byte[] getTo() {
        return to;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "data=" + toHexString(data) +
                ", gasLimit=" + Arrays.toString(gasLimit) +
                ", gasPrice=" + gasPrice +
                ", nonce=" + nonce +
                ", secretKey=" + toHexString(secretKey) +
                ", to=" + toHexString(to) +
                ", value=" + value +
                '}';
    }
}
