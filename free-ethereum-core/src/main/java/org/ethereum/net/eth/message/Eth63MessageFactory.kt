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
import org.ethereum.net.message.MessageFactory

import org.ethereum.net.eth.EthVersion.V63

/**
 * Fast synchronization (PV63) message factory
 */
class Eth63MessageFactory : MessageFactory {

    override fun create(code: Byte, encoded: ByteArray): Message {

        val receivedCommand = EthMessageCodes.fromByte(code, V63)
        when (receivedCommand) {
            EthMessageCodes.STATUS -> return StatusMessage(encoded)
            EthMessageCodes.NEW_BLOCK_HASHES -> return NewBlockHashesMessage(encoded)
            EthMessageCodes.TRANSACTIONS -> return TransactionsMessage(encoded)
            EthMessageCodes.GET_BLOCK_HEADERS -> return GetBlockHeadersMessage(encoded)
            EthMessageCodes.BLOCK_HEADERS -> return BlockHeadersMessage(encoded)
            EthMessageCodes.GET_BLOCK_BODIES -> return GetBlockBodiesMessage(encoded)
            EthMessageCodes.BLOCK_BODIES -> return BlockBodiesMessage(encoded)
            EthMessageCodes.NEW_BLOCK -> return NewBlockMessage(encoded)
            EthMessageCodes.GET_NODE_DATA -> return GetNodeDataMessage(encoded)
            EthMessageCodes.NODE_DATA -> return NodeDataMessage(encoded)
            EthMessageCodes.GET_RECEIPTS -> return GetReceiptsMessage(encoded)
            EthMessageCodes.RECEIPTS -> return ReceiptsMessage(encoded)
            else -> throw IllegalArgumentException("No such message")
        }
    }
}
