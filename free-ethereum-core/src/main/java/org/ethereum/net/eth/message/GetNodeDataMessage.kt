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

import org.ethereum.net.message.Message
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.ethereum.util.Utils
import org.spongycastle.util.encoders.Hex
import java.util.*

/**
 * Wrapper around an Ethereum GetNodeData message on the network
 * Could contain:
 * - state roots
 * - accounts state roots
 * - accounts code hashes

 * @see EthMessageCodes.GET_NODE_DATA
 */
class GetNodeDataMessage : EthMessage {

    /**
     * List of node hashes for which is state requested
     */
    private var nodeKeys: MutableList<ByteArray>? = null

    constructor(encoded: ByteArray) : super(encoded)

    constructor(nodeKeys: MutableList<ByteArray>) {
        this.nodeKeys = nodeKeys
        this.parsed = true
    }

    @Synchronized private fun parse() {
        if (parsed) return
        val paramsList = RLP.decode2(encoded)[0] as RLPList

        this.nodeKeys = ArrayList<ByteArray>()
        for (aParamsList in paramsList) {
            nodeKeys!!.add(aParamsList.rlpData)
        }

        this.parsed = true
    }

    private fun encode() {
        val encodedElements = ArrayList<ByteArray>()
        for (hash in nodeKeys!!)
            encodedElements.add(RLP.encodeElement(hash))
        val encodedElementArray = encodedElements.toTypedArray()

        this.encoded = RLP.encodeList(*encodedElementArray)
    }

    override fun getEncoded(): ByteArray {
        if (encoded == null) encode()
        return encoded
    }


    override fun getAnswerMessage(): Class<NodeDataMessage> {
        return NodeDataMessage::class.java
    }

    fun getNodeKeys(): List<ByteArray> {
        parse()
        return nodeKeys!!
    }

    override fun getCommand(): EthMessageCodes {
        return EthMessageCodes.GET_NODE_DATA
    }

    override fun toString(): String {
        parse()

        val payload = StringBuilder()

        payload.append("count( ").append(nodeKeys!!.size).append(" ) ")

        if (Message.logger.isDebugEnabled) {
            for (hash in nodeKeys!!) {
                payload.append(Hex.toHexString(hash).substring(0, 6)).append(" | ")
            }
            if (!nodeKeys!!.isEmpty()) {
                payload.delete(payload.length - 3, payload.length)
            }
        } else {
            payload.append(Utils.getHashListShort(nodeKeys))
        }

        return "[" + command.name + " " + payload + "]"
    }
}
