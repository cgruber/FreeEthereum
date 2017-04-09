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

package org.ethereum.jsontestsuite.suite.model

class BlockHeaderTck {

    var bloom: String? = null
    var coinbase: String? = null
    var difficulty: String? = null
    var extraData: String? = null
    var gasLimit: String? = null
    var gasUsed: String? = null
    var hash: String? = null
    var mixHash: String? = null
    var nonce: String? = null
    var number: String? = null
    var parentHash: String? = null
    var receiptTrie: String? = null
    var seedHash: String? = null
    var stateRoot: String? = null
    var timestamp: String? = null
    var transactionsTrie: String? = null
    var uncleHash: String? = null

    override fun toString(): String {
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
                '}'
    }
}
