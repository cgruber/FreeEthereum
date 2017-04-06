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

import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.Constants;
import org.ethereum.config.ConstantsAdapter;
import org.ethereum.core.Transaction;
import org.ethereum.vm.GasCost;

import java.util.Objects;

import static org.ethereum.config.blockchain.HomesteadConfig.SECP256K1N_HALF;

/**
 * Hard fork includes following EIPs:
 * EIP 155 - Simple replay attack protection
 * EIP 160 - EXP cost increase
 * EIP 161 - State trie clearing (invariant-preserving alternative)
 */
public class Eip160HFConfig extends Eip150HFConfig {

    private static final GasCost NEW_GAS_COST = new GasCostEip160HF();
    private final Constants constants;

    public Eip160HFConfig(final BlockchainConfig parent) {
        super(parent);
        constants = new ConstantsAdapter(parent.getConstants()) {
            @Override
            public int getMaxContractSize() {
                return 0x6000;
            }
        };
    }

    @Override
    public GasCost getGasCost() {
        return NEW_GAS_COST;
    }

    @Override
    public boolean eip161() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return 1;
    }

    @Override
    public Constants getConstants() {
        return constants;
    }

    @Override
    public boolean acceptTransactionSignature(final Transaction tx) {
        // Restoring old logic. Making this through inheritance stinks too much
        if (!tx.getSignature().validateComponents() ||
                tx.getSignature().s.compareTo(SECP256K1N_HALF) > 0) return false;
        return  tx.getChainId() == null || Objects.equals(getChainId(), tx.getChainId());
    }

    static class GasCostEip160HF extends GasCostEip150HF {
        public int getEXP_BYTE_GAS() {
            return 50;
        }
    }
}
