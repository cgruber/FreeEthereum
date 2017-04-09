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
import org.ethereum.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum GetNodeData message on the network
 * Could contain:
 * - state roots
 * - accounts state roots
 * - accounts code hashes
 *
 * @see EthMessageCodes#GET_NODE_DATA
 */
public class GetNodeDataMessage extends EthMessage {

    /**
     * List of node hashes for which is state requested
     */
    private List<byte[]> nodeKeys;

    public GetNodeDataMessage(final byte[] encoded) {
        super(encoded);
    }

    public GetNodeDataMessage(final List<byte[]> nodeKeys) {
        this.nodeKeys = nodeKeys;
        this.parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        this.nodeKeys = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            nodeKeys.add(aParamsList.getRLPData());
        }

        this.parsed = true;
    }

    private void encode() {
        final List<byte[]> encodedElements = new ArrayList<>();
        for (final byte[] hash : nodeKeys)
            encodedElements.add(RLP.encodeElement(hash));
        final byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);

        this.encoded = RLP.encodeList(encodedElementArray);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }


    @Override
    public Class<NodeDataMessage> getAnswerMessage() {
        return NodeDataMessage.class;
    }

    public List<byte[]> getNodeKeys() {
        parse();
        return nodeKeys;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.GET_NODE_DATA;
    }

    public String toString() {
        parse();

        final StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(nodeKeys.size()).append(" ) ");

        if (logger.isDebugEnabled()) {
            for (final byte[] hash : nodeKeys) {
                payload.append(Hex.toHexString(hash).substring(0, 6)).append(" | ");
            }
            if (!nodeKeys.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            payload.append(Utils.getHashListShort(nodeKeys));
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
