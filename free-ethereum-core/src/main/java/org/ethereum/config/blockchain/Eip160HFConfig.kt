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

package org.ethereum.config.blockchain

import org.ethereum.config.BlockchainConfig
import org.ethereum.config.Constants
import org.ethereum.config.ConstantsAdapter
import org.ethereum.config.blockchain.HomesteadConfig.SECP256K1N_HALF
import org.ethereum.core.Transaction
import org.ethereum.vm.GasCost

/**
 * Hard fork includes following EIPs:
 * EIP 155 - Simple replay attack protection
 * EIP 160 - EXP cost increase
 * EIP 161 - State trie clearing (invariant-preserving alternative)
 */
open class Eip160HFConfig(parent: BlockchainConfig) : Eip150HFConfig(parent) {
    final override val constants: Constants

    init {
        constants = object : ConstantsAdapter(parent.constants) {
            override val maxContractSize: Int
                get() = 0x6000
        }
    }

    override val gasCost: GasCost
        get() = NEW_GAS_COST

    override fun eip161(): Boolean {
        return true
    }

    override val chainId: Int?
        get() = 1

    override fun acceptTransactionSignature(tx: Transaction): Boolean {
        // Restoring old logic. Making this through inheritance stinks too much
        if (!tx.signature.validateComponents() || tx.signature.s > SECP256K1N_HALF)
            return false
        return tx.chainId == null || chainId == tx.chainId
    }

    internal class GasCostEip160HF : Eip150HFConfig.GasCostEip150HF() {
        override fun getEXP_BYTE_GAS(): Int {
            return 50
        }
    }

    companion object {

        private val NEW_GAS_COST = GasCostEip160HF()
    }
}
