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

import org.ethereum.config.BlockchainConfig
import org.ethereum.config.BlockchainNetConfig
import org.ethereum.config.Constants
import java.util.*

open class BaseNetConfig : BlockchainNetConfig {
    private val blockNumbers = LongArray(64)
    private val configs = arrayOfNulls<BlockchainConfig>(64)
    private var count: Int = 0

    fun add(startBlockNumber: Long, config: BlockchainConfig) {
        if (count >= blockNumbers.size) throw RuntimeException()
        if (count > 0 && blockNumbers[count] >= startBlockNumber)
            throw RuntimeException("Block numbers should increase")
        if (count == 0 && startBlockNumber > 0) throw RuntimeException("First config should start from block 0")
        blockNumbers[count] = startBlockNumber
        configs[count] = config
        count++
    }

    override fun getConfigForBlock(blockNumber: Long): BlockchainConfig {
        return (0..count - 1)
                .firstOrNull { blockNumber < blockNumbers[it] }
                ?.let { configs[it - 1]!! }
                ?: configs[count - 1]!!
    }

    override // TODO make a guard wrapper which throws exception if the requested constant differs among configs
    val commonConstants: Constants
        get() = configs[0]?.constants!!

    override fun toString(): String {
        return "BaseNetConfig{" +
                "blockNumbers=" + Arrays.toString(Arrays.copyOfRange(blockNumbers, 0, count)) +
                ", configs=" + Arrays.toString(Arrays.copyOfRange<BlockchainConfig>(configs, 0, count)) +
                ", count=" + count +
                '}'
    }
}
