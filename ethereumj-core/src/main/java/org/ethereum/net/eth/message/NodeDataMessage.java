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
import org.ethereum.util.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum NodeData message on the network
 *
 * @see EthMessageCodes#NODE_DATA
 */
public class NodeDataMessage extends EthMessage {

    private List<Value> dataList;

    public NodeDataMessage(final byte[] encoded) {
        super(encoded);
        parse();
    }

    public NodeDataMessage(final List<Value> dataList) {
        this.dataList = dataList;
        parsed = true;
    }

    private void parse() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        dataList = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            // Need it AS IS
            dataList.add(Value.Companion.fromRlpEncoded(aParamsList.getRLPData()));
        }
        parsed = true;
    }

    private void encode() {
        final List<byte[]> dataListRLP = new ArrayList<>();
        for (final Value value : dataList) {
            if (value == null) continue; // Bad sign
            dataListRLP.add(RLP.encodeElement(value.getData()));
        }
        final byte[][] encodedElementArray = dataListRLP.toArray(new byte[dataListRLP.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public List<Value> getDataList() {
        return dataList;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.NODE_DATA;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {

        final StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(dataList.size()).append(" )");

        if (logger.isTraceEnabled()) {
            payload.append(" ");
            for (final Value value : dataList) {
                payload.append(value).append(" | ");
            }
            if (!dataList.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
