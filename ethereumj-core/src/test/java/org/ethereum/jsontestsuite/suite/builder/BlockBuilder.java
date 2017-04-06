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

package org.ethereum.jsontestsuite.suite.builder;

import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Transaction;
import org.ethereum.jsontestsuite.suite.Env;
import org.ethereum.jsontestsuite.suite.model.BlockHeaderTck;
import org.ethereum.jsontestsuite.suite.model.TransactionTck;
import org.ethereum.util.ByteUtil;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.util.ByteUtil.byteArrayToLong;

public class BlockBuilder {


    public static Block build(final BlockHeaderTck header,
                              final List<TransactionTck> transactionsTck,
                              final List<BlockHeaderTck> unclesTck) {

        if (header == null) return null;

        final List<BlockHeader> uncles = new ArrayList<>();
        if (unclesTck != null) for (final BlockHeaderTck uncle : unclesTck)
            uncles.add(BlockHeaderBuilder.build(uncle));

        final List<Transaction> transactions = new ArrayList<>();
        if (transactionsTck != null) for (final TransactionTck tx : transactionsTck)
            transactions.add(TransactionBuilder.build(tx));

        final BlockHeader blockHeader = BlockHeaderBuilder.build(header);
        final Block block = new Block(
                blockHeader,
                transactions, uncles);

        return block;
    }


    public static Block build(final Env env) {

        final Block block = new Block(
                ByteUtil.EMPTY_BYTE_ARRAY,
                ByteUtil.EMPTY_BYTE_ARRAY,
                env.getCurrentCoinbase(),
                ByteUtil.EMPTY_BYTE_ARRAY,
                env.getCurrentDifficulty(),

                byteArrayToLong(env.getCurrentNumber()),
                env.getCurrentGasLimit(),
                0L,
                byteArrayToLong(env.getCurrentTimestamp()),
                new byte[32],
                ByteUtil.ZERO_BYTE_ARRAY,
                ByteUtil.ZERO_BYTE_ARRAY,
                null, null);

        return block;
    }
}
