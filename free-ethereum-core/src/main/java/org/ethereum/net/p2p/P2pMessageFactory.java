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

package org.ethereum.net.p2p;

import org.ethereum.net.message.Message;
import org.ethereum.net.message.MessageFactory;
import org.ethereum.net.message.StaticMessages;

/**
 * P2P message factory
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class P2pMessageFactory implements MessageFactory {

    @Override
    public Message create(final byte code, final byte[] encoded) {

        final P2pMessageCodes receivedCommand = P2pMessageCodes.fromByte(code);
        switch (receivedCommand) {
            case HELLO:
                return new HelloMessage(encoded);
            case DISCONNECT:
                return new DisconnectMessage(encoded);
            case PING:
                return StaticMessages.PING_MESSAGE;
            case PONG:
                return StaticMessages.PONG_MESSAGE;
            case GET_PEERS:
                return StaticMessages.GET_PEERS_MESSAGE;
            case PEERS:
                return new PeersMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}
