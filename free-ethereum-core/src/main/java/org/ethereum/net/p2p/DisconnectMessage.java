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

import org.ethereum.net.message.ReasonCode;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import static org.ethereum.net.message.ReasonCode.UNKNOWN;

/**
 * Wrapper around an Ethereum Disconnect message on the network
 *
 * @see org.ethereum.net.p2p.P2pMessageCodes#DISCONNECT
 */
public class DisconnectMessage extends P2pMessage {

    private ReasonCode reason;

    public DisconnectMessage(final byte[] encoded) {
        super(encoded);
    }

    public DisconnectMessage(final ReasonCode reason) {
        this.reason = reason;
        parsed = true;
    }

    private void parse() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        if (paramsList.size() > 0) {
            final byte[] reasonBytes = paramsList.get(0).getRLPData();
            if (reasonBytes == null)
                this.reason = UNKNOWN;
            else
                this.reason = ReasonCode.Companion.fromInt(reasonBytes[0]);
        } else {
            this.reason = UNKNOWN;
        }

        parsed = true;
    }

    private void encode() {
        final byte[] encodedReason = RLP.encodeByte(this.reason.asByte());
        this.encoded = RLP.encodeList(encodedReason);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.DISCONNECT;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public ReasonCode getReason() {
        if (!parsed) parse();
        return reason;
    }

    public String toString() {
        if (!parsed) parse();
        return "[" + this.getCommand().name() + " reason=" + reason + "]";
    }
}