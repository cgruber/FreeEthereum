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
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.Constants;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.db.BlockStore;
import org.ethereum.mine.MinerIfc;
import org.ethereum.util.Utils;
import org.ethereum.validator.BlockHeaderValidator;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.GasCost;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.program.Program;

import java.math.BigInteger;
import java.util.List;

public class Eip150HFConfig implements BlockchainConfig, BlockchainNetConfig {
    private static final GasCost NEW_GAS_COST = new GasCostEip150HF();
    private final BlockchainConfig parent;

    public Eip150HFConfig(final BlockchainConfig parent) {
        this.parent = parent;
    }

    @Override
    public DataWord getCallGas(final OpCode op, final DataWord requestedGas, final DataWord availableGas) throws Program.OutOfGasException {
        final DataWord maxAllowed = Utils.allButOne64th(availableGas);
        return requestedGas.compareTo(maxAllowed) > 0 ? maxAllowed : requestedGas;
    }

    @Override
    public DataWord getCreateGas(final DataWord availableGas) {
        return Utils.allButOne64th(availableGas);
    }

    @Override
    public Constants getConstants() {
        return parent.getConstants();
    }

    @Override
    public MinerIfc getMineAlgorithm(final SystemProperties config) {
        return parent.getMineAlgorithm(config);
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
        return parent.acceptTransactionSignature(tx) && tx.getChainId() == null;
    }

    @Override
    public String validateTransactionChanges(final BlockStore blockStore, final Block curBlock, final Transaction tx, final Repository repository) {
        return parent.validateTransactionChanges(blockStore, curBlock, tx, repository);
    }

    @Override
    public void hardForkTransfers(final Block block, final Repository repo) {
        parent.hardForkTransfers(block, repo);
    }

    @Override
    public byte[] getExtraData(final byte[] minerExtraData, final long blockNumber) {
        return parent.getExtraData(minerExtraData, blockNumber);
    }

    @Override
    public List<Pair<Long, BlockHeaderValidator>> headerValidators() {
        return parent.headerValidators();
    }

    @Override
    public boolean eip161() {
        return parent.eip161();
    }

    @Override
    public GasCost getGasCost() {
        return NEW_GAS_COST;
    }

    @Override
    public BlockchainConfig getConfigForBlock(final long blockNumber) {
        return this;
    }

    @Override
    public Constants getCommonConstants() {
        return getConstants();
    }

    @Override
    public Integer getChainId() {
        return null;
    }

    static class GasCostEip150HF extends GasCost {
        public int getBALANCE() {
            return 400;
        }

        public int getEXT_CODE_SIZE() {
            return 700;
        }

        public int getEXT_CODE_COPY() {
            return 700;
        }

        public int getSLOAD() {
            return 200;
        }

        public int getCALL() {
            return 700;
        }

        public int getSUICIDE() {
            return 5000;
        }

        public int getNEW_ACCT_SUICIDE() {
            return 25000;
        }
    }
}
