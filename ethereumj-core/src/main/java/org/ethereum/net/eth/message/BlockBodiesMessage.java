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

import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum BlockBodies message on the network
 *
 * @see EthMessageCodes#BLOCK_BODIES
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockBodiesMessage extends EthMessage {

    private List<byte[]> blockBodies;

    public BlockBodiesMessage(final byte[] encoded) {
        super(encoded);
    }

    public BlockBodiesMessage(final List<byte[]> blockBodies) {
        this.blockBodies = blockBodies;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockBodies = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            final RLPList rlpData = ((RLPList) aParamsList);
            blockBodies.add(rlpData.getRLPData());
        }
        parsed = true;
    }

    private void encode() {

        final byte[][] encodedElementArray = blockBodies
                .toArray(new byte[blockBodies.size()][]);

        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public List<byte[]> getBlockBodies() {
        parse();
        return blockBodies;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.BLOCK_BODIES;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        parse();

        final StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blockBodies.size()).append(" )");

        if (logger.isTraceEnabled()) {
            payload.append(" ");
            for (final byte[] body : blockBodies) {
                payload.append(Hex.toHexString(body)).append(" | ");
            }
            if (!blockBodies.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
