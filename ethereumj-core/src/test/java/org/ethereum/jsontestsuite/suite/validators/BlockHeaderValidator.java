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

import org.ethereum.core.BlockHeader;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.ethereum.util.ByteUtil.toHexString;

public class BlockHeaderValidator {


    public static ArrayList<String> valid(final BlockHeader orig, final BlockHeader valid) {

        final ArrayList<String> outputSummary = new ArrayList<>();

        if (!toHexString(orig.getParentHash())
                .equals(toHexString(valid.getParentHash()))) {

            final String output =
                    String.format("wrong block.parentHash: \n expected: %s \n got: %s",
                            toHexString(valid.getParentHash()),
                            toHexString(orig.getParentHash())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getUnclesHash())
                .equals(toHexString(valid.getUnclesHash()))) {

            final String output =
                    String.format("wrong block.unclesHash: \n expected: %s \n got: %s",
                            toHexString(valid.getUnclesHash()),
                            toHexString(orig.getUnclesHash())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getCoinbase())
                .equals(toHexString(valid.getCoinbase()))) {

            final String output =
                    String.format("wrong block.coinbase: \n expected: %s \n got: %s",
                            toHexString(valid.getCoinbase()),
                            toHexString(orig.getCoinbase())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getStateRoot())
                .equals(toHexString(valid.getStateRoot()))) {

            final String output =
                    String.format("wrong block.stateRoot: \n expected: %s \n got: %s",
                            toHexString(valid.getStateRoot()),
                            toHexString(orig.getStateRoot())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getTxTrieRoot())
                .equals(toHexString(valid.getTxTrieRoot()))) {

            final String output =
                    String.format("wrong block.txTrieRoot: \n expected: %s \n got: %s",
                            toHexString(valid.getTxTrieRoot()),
                            toHexString(orig.getTxTrieRoot())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getReceiptsRoot())
                .equals(toHexString(valid.getReceiptsRoot()))) {

            final String output =
                    String.format("wrong block.receiptsRoot: \n expected: %s \n got: %s",
                            toHexString(valid.getReceiptsRoot()),
                            toHexString(orig.getReceiptsRoot())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getLogsBloom())
                .equals(toHexString(valid.getLogsBloom()))) {

            final String output =
                    String.format("wrong block.logsBloom: \n expected: %s \n got: %s",
                            toHexString(valid.getLogsBloom()),
                            toHexString(orig.getLogsBloom())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getDifficulty())
                .equals(toHexString(valid.getDifficulty()))) {

            final String output =
                    String.format("wrong block.difficulty: \n expected: %s \n got: %s",
                            toHexString(valid.getDifficulty()),
                            toHexString(orig.getDifficulty())
                    );

            outputSummary.add(output);
        }

        if (orig.getTimestamp() != valid.getTimestamp()) {

            final String output =
                    String.format("wrong block.timestamp: \n expected: %d \n got: %d",
                            valid.getTimestamp(),
                            orig.getTimestamp()
                    );

            outputSummary.add(output);
        }

        if (orig.getNumber() != valid.getNumber()) {

            final String output =
                    String.format("wrong block.number: \n expected: %d \n got: %d",
                            valid.getNumber(),
                            orig.getNumber()
                    );

            outputSummary.add(output);
        }

        if (!new BigInteger(1, orig.getGasLimit()).equals(new BigInteger(1, valid.getGasLimit()))) {

            final String output =
                    String.format("wrong block.gasLimit: \n expected: %d \n got: %d",
                            new BigInteger(1, valid.getGasLimit()),
                            new BigInteger(1, orig.getGasLimit())
                    );

            outputSummary.add(output);
        }

        if (orig.getGasUsed() != valid.getGasUsed()) {

            final String output =
                    String.format("wrong block.gasUsed: \n expected: %d \n got: %d",
                            valid.getGasUsed(),
                            orig.getGasUsed()
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getMixHash())
                .equals(toHexString(valid.getMixHash()))) {

            final String output =
                    String.format("wrong block.mixHash: \n expected: %s \n got: %s",
                            toHexString(valid.getMixHash()),
                            toHexString(orig.getMixHash())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getExtraData())
                .equals(toHexString(valid.getExtraData()))) {

            final String output =
                    String.format("wrong block.extraData: \n expected: %s \n got: %s",
                            toHexString(valid.getExtraData()),
                            toHexString(orig.getExtraData())
                    );

            outputSummary.add(output);
        }

        if (!toHexString(orig.getNonce())
                .equals(toHexString(valid.getNonce()))) {

            final String output =
                    String.format("wrong block.nonce: \n expected: %s \n got: %s",
                            toHexString(valid.getNonce()),
                            toHexString(orig.getNonce())
                    );

            outputSummary.add(output);
        }


        return outputSummary;
    }
}
