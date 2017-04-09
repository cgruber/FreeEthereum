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

package org.ethereum.config.blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Transaction;
import org.ethereum.validator.BlockHeaderRule;
import org.ethereum.validator.BlockHeaderValidator;
import org.ethereum.validator.ExtraDataPresenceRule;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public abstract class AbstractDaoConfig extends FrontierConfig {

    // "dao-hard-fork" encoded message
    public static final byte[] DAO_EXTRA_DATA = Hex.decode("64616f2d686172642d666f726b");
    /**
     * Hardcoded values from live network
     */
    static final long ETH_FORK_BLOCK_NUMBER = 1_920_000;
    // MAYBE find a way to remove block number value from blockchain config
    long forkBlockNumber;

    // set in child classes
    boolean supportFork;

    private BlockchainConfig parent;

    void initDaoConfig(final BlockchainConfig parent, final long forkBlockNumber) {
        this.parent = parent;
        this.constants = parent.getConstants();
        this.forkBlockNumber = forkBlockNumber;
        final BlockHeaderRule rule = new ExtraDataPresenceRule(DAO_EXTRA_DATA, supportFork);
        headerValidators().add(Pair.of(forkBlockNumber, new BlockHeaderValidator(rule)));
    }

    /**
     * Miners should include marker for initial 10 blocks. Either "dao-hard-fork" or ""
     */
    @Override
    public byte[] getExtraData(final byte[] minerExtraData, final long blockNumber) {
        final long EXTRA_DATA_AFFECTS_BLOCKS_NUMBER = 10;
        if (blockNumber >= forkBlockNumber && blockNumber < forkBlockNumber + EXTRA_DATA_AFFECTS_BLOCKS_NUMBER) {
            if (supportFork) {
                return DAO_EXTRA_DATA;
            } else {
                return new byte[0];
            }

        }
        return minerExtraData;
    }

    @Override
    public BigInteger calcDifficulty(final BlockHeader curBlock, final BlockHeader parent) {
        return this.parent.calcDifficulty(curBlock, parent);
    }

    @Override
    public long getTransactionCost(final Transaction tx) {
        return parent.getTransactionCost(tx);
    }

    @Override
    public boolean acceptTransactionSignature(final Transaction tx) {
        return parent.acceptTransactionSignature(tx);
    }

}
