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

import org.ethereum.core.BlockHeader

import java.math.BigInteger
import java.util.ArrayList

import org.ethereum.util.ByteUtil.toHexString

object BlockHeaderValidator {


    fun valid(orig: BlockHeader, valid: BlockHeader): ArrayList<String> {

        val outputSummary = ArrayList<String>()

        if (toHexString(orig.parentHash) != toHexString(valid.parentHash)) {

            val output = String.format("wrong block.parentHash: \n expected: %s \n got: %s",
                    toHexString(valid.parentHash),
                    toHexString(orig.parentHash)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.unclesHash) != toHexString(valid.unclesHash)) {

            val output = String.format("wrong block.unclesHash: \n expected: %s \n got: %s",
                    toHexString(valid.unclesHash),
                    toHexString(orig.unclesHash)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.coinbase) != toHexString(valid.coinbase)) {

            val output = String.format("wrong block.coinbase: \n expected: %s \n got: %s",
                    toHexString(valid.coinbase),
                    toHexString(orig.coinbase)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.stateRoot) != toHexString(valid.stateRoot)) {

            val output = String.format("wrong block.stateRoot: \n expected: %s \n got: %s",
                    toHexString(valid.stateRoot),
                    toHexString(orig.stateRoot)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.txTrieRoot) != toHexString(valid.txTrieRoot)) {

            val output = String.format("wrong block.txTrieRoot: \n expected: %s \n got: %s",
                    toHexString(valid.txTrieRoot),
                    toHexString(orig.txTrieRoot)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.receiptsRoot) != toHexString(valid.receiptsRoot)) {

            val output = String.format("wrong block.receiptsRoot: \n expected: %s \n got: %s",
                    toHexString(valid.receiptsRoot),
                    toHexString(orig.receiptsRoot)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.logsBloom) != toHexString(valid.logsBloom)) {

            val output = String.format("wrong block.logsBloom: \n expected: %s \n got: %s",
                    toHexString(valid.logsBloom),
                    toHexString(orig.logsBloom)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.difficulty) != toHexString(valid.difficulty)) {

            val output = String.format("wrong block.difficulty: \n expected: %s \n got: %s",
                    toHexString(valid.difficulty),
                    toHexString(orig.difficulty)
            )

            outputSummary.add(output)
        }

        if (orig.timestamp != valid.timestamp) {

            val output = String.format("wrong block.timestamp: \n expected: %d \n got: %d",
                    valid.timestamp,
                    orig.timestamp
            )

            outputSummary.add(output)
        }

        if (orig.number != valid.number) {

            val output = String.format("wrong block.number: \n expected: %d \n got: %d",
                    valid.number,
                    orig.number
            )

            outputSummary.add(output)
        }

        if (BigInteger(1, orig.gasLimit) != BigInteger(1, valid.gasLimit)) {

            val output = String.format("wrong block.gasLimit: \n expected: %d \n got: %d",
                    BigInteger(1, valid.gasLimit),
                    BigInteger(1, orig.gasLimit)
            )

            outputSummary.add(output)
        }

        if (orig.gasUsed != valid.gasUsed) {

            val output = String.format("wrong block.gasUsed: \n expected: %d \n got: %d",
                    valid.gasUsed,
                    orig.gasUsed
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.mixHash) != toHexString(valid.mixHash)) {

            val output = String.format("wrong block.mixHash: \n expected: %s \n got: %s",
                    toHexString(valid.mixHash),
                    toHexString(orig.mixHash)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.extraData) != toHexString(valid.extraData)) {

            val output = String.format("wrong block.extraData: \n expected: %s \n got: %s",
                    toHexString(valid.extraData),
                    toHexString(orig.extraData)
            )

            outputSummary.add(output)
        }

        if (toHexString(orig.nonce) != toHexString(valid.nonce)) {

            val output = String.format("wrong block.nonce: \n expected: %s \n got: %s",
                    toHexString(valid.nonce),
                    toHexString(orig.nonce)
            )

            outputSummary.add(output)
        }


        return outputSummary
    }
}
