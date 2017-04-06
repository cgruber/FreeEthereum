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

package org.ethereum.net.shh;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import static org.ethereum.net.shh.ShhMessageCodes.STATUS;

/**
 * @author by Konstantin Shabalin
 */
public class ShhStatusMessage extends ShhMessage {

    private byte protocolVersion;

    public ShhStatusMessage(final byte[] encoded) {
        super(encoded);
    }

    public ShhStatusMessage(final byte protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.parsed = true;
    }

    private void encode() {
        final byte[] protocolVersion = RLP.encodeByte(this.protocolVersion);
        this.encoded = RLP.encodeList(protocolVersion);
    }

    private void parse() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);
        this.protocolVersion = paramsList.get(0).getRLPData()[0];
        parsed = true;
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public ShhMessageCodes getCommand() {
        return STATUS;
    }

    @Override
    public String toString() {
        if (!parsed) parse();
        return "[" + this.getCommand().name() +
            " protocolVersion=" + this.protocolVersion + "]";
    }

}
