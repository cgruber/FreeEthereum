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

package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECKey;
import org.ethereum.util.*;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class NeighborsMessage extends Message {

    private List<Node> nodes;
    private long expires;

    public static NeighborsMessage create(final List<Node> nodes, final ECKey privKey) {

        final long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[][] nodeRLPs = null;

        if (nodes != null) {
            nodeRLPs = new byte[nodes.size()][];
            int i = 0;
            for (final Node node : nodes) {
                nodeRLPs[i] = node.getRLP();
                ++i;
            }
        }

        final byte[] rlpListNodes = RLP.encodeList(nodeRLPs);
        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        final byte[] type = new byte[]{4};
        final byte[] data = RLP.encodeList(rlpListNodes, rlpExp);

        final NeighborsMessage neighborsMessage = new NeighborsMessage();
        neighborsMessage.encode(type, data, privKey);
        neighborsMessage.nodes = nodes;
        neighborsMessage.expires = expiration;

        return neighborsMessage;
    }

    @Override
    public void parse(final byte[] data) {
        final RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        final RLPList nodesRLP = (RLPList) list.get(0);
        final RLPItem expires = (RLPItem) list.get(1);

        nodes = new ArrayList<>();

        for (final RLPElement aNodesRLP : nodesRLP) {
            final RLPList nodeRLP = (RLPList) aNodesRLP;
            final Node node = new Node(nodeRLP.getRLPData());
            nodes.add(node);
        }
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public long getExpires() {
        return expires;
    }


    @Override
    public String toString() {

        final long currTime = System.currentTimeMillis() / 1000;

        final String out = String.format("[NeighborsMessage] \n nodes [%d]: %s \n expires in %d seconds \n %s\n",
                this.getNodes().size(), this.getNodes(), (expires - currTime), super.toString());

        return out;
    }


}
