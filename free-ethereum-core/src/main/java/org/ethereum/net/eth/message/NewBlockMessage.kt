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

package org.ethereum.net.eth.message

import org.ethereum.core.Block
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.spongycastle.util.encoders.Hex

import java.math.BigInteger

/**
 * Wrapper around an Ethereum Blocks message on the network

 * @see EthMessageCodes.NEW_BLOCK
 */
class NewBlockMessage : EthMessage {

    var block: Block? = null
        get() {
            parse()
            return block!!
        }

    private var difficulty: ByteArray? = null

    constructor(encoded: ByteArray) : super(encoded)

    constructor(block: Block, difficulty: ByteArray) {
        this.block = block
        this.difficulty = difficulty
        this.parsed = true
        encode()
    }

    private fun encode() {
        val block = this.block!!.encoded
        val diff = RLP.encodeElement(this.difficulty)

        this.encoded = RLP.encodeList(block, diff)
    }

    @Synchronized private fun parse() {
        if (parsed) return
        val paramsList = RLP.decode2(encoded)[0] as RLPList

        val blockRLP = paramsList[0] as RLPList
        block = Block(blockRLP.rlpData)
        difficulty = paramsList[1].rlpData

        parsed = true
    }

    fun getDifficulty(): ByteArray {
        parse()
        return difficulty!!
    }

    val difficultyAsBigInt: BigInteger
        get() = BigInteger(1, difficulty!!)

    override fun getEncoded(): ByteArray {
        return encoded
    }

    override fun getCommand(): EthMessageCodes {
        return EthMessageCodes.NEW_BLOCK
    }

    override fun getAnswerMessage(): Class<*>? {
        return null
    }

    override fun toString(): String {
        parse()

        val hash = this.block?.shortHash
        val number = this.block?.number
        return "NEW_BLOCK [ number: " + number + " hash:" + hash + " difficulty: " + Hex.toHexString(difficulty!!) + " ]"
    }
}