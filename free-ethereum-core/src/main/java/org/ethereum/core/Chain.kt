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

package org.ethereum.core

import org.ethereum.db.ByteArrayWrapper
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*

/**
 * @author Roman Mandeleil
 * *
 * @since 09.11.2014
 */
internal class Chain {

    private val chain = ArrayList<Block>()
    private val index = HashMap<ByteArrayWrapper, Block>()
    var totalDifficulty: BigInteger = BigInteger.ZERO!!

    fun tryToConnect(block: Block): Boolean {

        if (chain.isEmpty()) {
            add(block)
            return true
        }

        val lastBlock = chain[chain.size - 1]
        if (lastBlock.isParentOf(block)) {
            add(block)
            return true
        }
        return false
    }

    private fun add(block: Block) {
        logger.info("adding block to alt chain block.hash: [{}] ", block.shortHash)
        totalDifficulty = totalDifficulty.add(block.cumulativeDifficulty)
        logger.info("total difficulty on alt chain is: [{}] ", totalDifficulty)
        chain.add(block)
        index.put(ByteArrayWrapper(block.hash), block)
    }

    operator fun get(i: Int): Block {
        return chain[i]
    }

    val last: Block
        get() = chain[chain.size - 1]

    fun isParentOnTheChain(block: Block): Boolean {
        return index[ByteArrayWrapper(block.parentHash)] != null
    }

    val size: Long
        get() = chain.size.toLong()

    companion object {

        private val logger = LoggerFactory.getLogger("blockchain")
    }


}
