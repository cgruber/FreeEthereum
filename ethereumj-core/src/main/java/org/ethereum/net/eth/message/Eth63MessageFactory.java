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

package org.ethereum.net.eth.message;

import org.ethereum.net.message.Message;
import org.ethereum.net.message.MessageFactory;

import static org.ethereum.net.eth.EthVersion.V63;

/**
 * Fast synchronization (PV63) message factory
 */
public class Eth63MessageFactory implements MessageFactory {

    @Override
    public Message create(final byte code, final byte[] encoded) {

        final EthMessageCodes receivedCommand = EthMessageCodes.fromByte(code, V63);
        switch (receivedCommand) {
            case STATUS:
                return new StatusMessage(encoded);
            case NEW_BLOCK_HASHES:
                return new NewBlockHashesMessage(encoded);
            case TRANSACTIONS:
                return new TransactionsMessage(encoded);
            case GET_BLOCK_HEADERS:
                return new GetBlockHeadersMessage(encoded);
            case BLOCK_HEADERS:
                return new BlockHeadersMessage(encoded);
            case GET_BLOCK_BODIES:
                return new GetBlockBodiesMessage(encoded);
            case BLOCK_BODIES:
                return new BlockBodiesMessage(encoded);
            case NEW_BLOCK:
                return new NewBlockMessage(encoded);
            case GET_NODE_DATA:
                return new GetNodeDataMessage(encoded);
            case NODE_DATA:
                return new NodeDataMessage(encoded);
            case GET_RECEIPTS:
                return new GetReceiptsMessage(encoded);
            case RECEIPTS:
                return new ReceiptsMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}
