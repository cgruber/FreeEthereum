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

package org.ethereum.validator

import org.ethereum.config.BlockchainNetConfig
import org.ethereum.config.SystemProperties
import org.ethereum.core.BlockHeader

/**
 * Checks if the block is from the right fork
 */
class BlockHashRule(config: SystemProperties) : BlockHeaderRule() {

    private val blockchainConfig: BlockchainNetConfig

    init {
        blockchainConfig = config.blockchainConfig
    }

    public override fun validate(header: BlockHeader): BlockHeaderRule.ValidationResult {
        val validators = blockchainConfig.getConfigForBlock(header.number).headerValidators()
        for (pair in validators) {
            if (header.number == pair.left) {
                val result = pair.right.validate(header)
                if (!result.success) {
                    return fault("Block " + header.number + " header constraint violated. " + result.error)
                }
            }
        }

        return BlockHeaderRule.Success
    }
}
