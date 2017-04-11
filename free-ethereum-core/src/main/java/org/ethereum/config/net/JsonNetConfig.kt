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

package org.ethereum.config.net

import org.apache.commons.lang3.tuple.Pair
import org.ethereum.config.BlockchainConfig
import org.ethereum.config.blockchain.*
import org.ethereum.core.genesis.GenesisConfig
import java.util.*

/**
 * Convert JSON config from genesis to Java blockchain net config.
 * Created by Stan Reshetnyk on 23.12.2016.
 */
class JsonNetConfig
/**
 * We convert all string keys to lowercase before processing.

 * Homestead block is 0 if not specified.
 * If Homestead block is specified then Frontier will be used for 0 block.

 * @param config
 */
@Throws(RuntimeException::class)
constructor(config: GenesisConfig) : BaseNetConfig() {

    init {

        val candidates = ArrayList<Pair<Int, out BlockchainConfig>>()

        run {
            val initialBlockConfig = FrontierConfig()
            var lastCandidate: Pair<Int, out BlockchainConfig> = Pair.of<Int, BlockchainConfig>(0, initialBlockConfig)
            candidates.add(lastCandidate)

            // homestead block assumed to be 0 by default
            lastCandidate = Pair.of(if (config.homesteadBlock == null) 0 else config.homesteadBlock, HomesteadConfig())
            candidates.add(lastCandidate)

            if (config.daoForkBlock != null) {
                val daoConfig = if (config.daoForkSupport)
                    DaoHFConfig(lastCandidate.right, config.daoForkBlock!!.toLong())
                else
                    DaoNoHFConfig(lastCandidate.right, config.daoForkBlock!!.toLong())
                lastCandidate = Pair.of(config.daoForkBlock, daoConfig)
                candidates.add(lastCandidate)
            }

            if (config.eip150Block != null) {
                lastCandidate = Pair.of(config.eip150Block, Eip150HFConfig(lastCandidate.right))
                candidates.add(lastCandidate)
            }

            if (config.eip155Block != null || config.eip158Block != null) {
                val block: Int
                if (config.eip155Block != null) {
                    if (config.eip158Block != null && config.eip155Block != config.eip158Block) {
                        throw RuntimeException("Unable to build config with different blocks for EIP155 (" + config.eip155Block + ") and EIP158 (" + config.eip158Block + ")")
                    }
                    block = config.eip155Block!!
                } else {
                    block = config.eip158Block!!
                }

                if (config.chainId != null) {
                    val chainId = config.chainId
                    lastCandidate = Pair.of<Int, BlockchainConfig>(block, object : Eip160HFConfig(lastCandidate.right) {
                        override val chainId: Int?
                            get() = chainId
                    })
                } else {
                    lastCandidate = Pair.of(block, Eip160HFConfig(lastCandidate.right))
                }
                candidates.add(lastCandidate)
            }
        }

        run {
            // add candidate per each block (take last in row for same block)
            var last = candidates.removeAt(0)
            for (current in candidates) {
                if (current.left > last.left) {
                    add(last.left.toLong(), last.right)
                }
                last = current
            }
            add(last.left.toLong(), last.right)
        }
    }
}
