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
import org.ethereum.util.Value
import java.util.*

/**
 * Wrapper around an Ethereum NodeData message on the network

 * @see EthMessageCodes.NODE_DATA
 */
class NodeDataMessage : EthMessage {

    private var dataList: MutableList<Value>? = null

    constructor(encoded: ByteArray) : super(encoded) {
        parse()
    }

    constructor(dataList: MutableList<Value>) {
        this.dataList = dataList
        parsed = true
    }

    private fun parse() {
        val paramsList = RLP.decode2(encoded)[0] as RLPList

        dataList = ArrayList<Value>()
        for (aParamsList in paramsList) {
            // Need it AS IS
            dataList!!.add(Value.fromRlpEncoded(aParamsList.rlpData)!!)
        }
        parsed = true
    }

    private fun encode() {
        val dataListRLP = ArrayList<ByteArray>()
        for (value in dataList!!) {
            if (value == null) continue // Bad sign
            dataListRLP.add(RLP.encodeElement(value.data))
        }
        val encodedElementArray = dataListRLP.toTypedArray()
        this.encoded = RLP.encodeList(*encodedElementArray)
    }


    override fun getEncoded(): ByteArray {
        if (encoded == null) encode()
        return encoded
    }

    fun getDataList(): List<Value> {
        return dataList!!
    }

    override fun getCommand(): EthMessageCodes {
        return EthMessageCodes.NODE_DATA
    }

    override fun getAnswerMessage(): Class<*>? {
        return null
    }

    override fun toString(): String {

        val payload = StringBuilder()

        payload.append("count( ").append(dataList!!.size).append(" )")

        if (Message.logger.isTraceEnabled) {
            payload.append(" ")
            for (value in dataList!!) {
                payload.append(value).append(" | ")
            }
            if (!dataList!!.isEmpty()) {
                payload.delete(payload.length - 3, payload.length)
            }
        }

        return "[" + command.name + " " + payload + "]"
    }
}
