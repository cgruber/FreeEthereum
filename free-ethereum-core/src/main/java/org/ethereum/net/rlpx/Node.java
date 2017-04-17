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
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.ethereum.util.ByteUtil.*;

public class Node implements Serializable {
    private static final long serialVersionUID = -4267600517925770636L;

    private byte[] id;
    private String host;
    private int port;
    // discovery endpoint doesn't have real nodeId for example
    private boolean isFakeNodeId = false;

    public Node(final String enodeURL) {
        try {
            final URI uri = new URI(enodeURL);
            if (!uri.getScheme().equals("enode")) {
                throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
        } catch (final URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
        }
    }

    public Node(final byte[] id, final String host, final int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Node(final byte[] rlp) {

        RLPList nodeRLP = RLP.decode2(rlp);
        nodeRLP = (RLPList) nodeRLP.get(0);

        final byte[] hostB = nodeRLP.get(0).getRLPData();
        final byte[] portB = nodeRLP.get(1).getRLPData();
        final byte[] idB;

        if (nodeRLP.size() > 3) {
            idB = nodeRLP.get(3).getRLPData();
        } else {
            idB = nodeRLP.get(2).getRLPData();
        }

        final int port = byteArrayToInt(portB);

        this.host = bytesToIp(hostB);
        this.port = port;
        this.id = idB;
    }

    /**
     * - create Node instance from enode if passed,
     * - otherwise fallback to random nodeId, if supplied with only "address:port"
     * NOTE: validation is absent as method is not heavily used
     */
    public static Node instanceOf(final String addressOrEnode) {
        try {
            final URI uri = new URI(addressOrEnode);
            if (uri.getScheme().equals("enode")) {
                return new Node(addressOrEnode);
            }
        } catch (final URISyntaxException e) {
            // continue
        }

        final ECKey generatedNodeKey = ECKey.fromPrivate(HashUtil.INSTANCE.sha3(addressOrEnode.getBytes()));
        final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
        final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
        node.isFakeNodeId = true;
        return node;
    }

    /**
     * @return true if this node is endpoint for discovery loaded from config
     */
    public boolean isDiscoveryNode() {
        return isFakeNodeId;
    }

    public void setDiscoveryNode(final boolean isDiscoveryNode) {
        isFakeNodeId = isDiscoveryNode;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(final byte[] id) {
        this.id = id;
    }

    public String getHexId() {
        return Hex.toHexString(id);
    }

    public String getHexIdShort() {
        return Utils.getNodeIdShort(getHexId());
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Full RLP
     * [host, udpPort, tcpPort, nodeId]
     * @return RLP-encoded node data
     */
    public byte[] getRLP() {
        final byte[] rlphost = RLP.encodeElement(hostToBytes(host));
        final byte[] rlpTCPPort = RLP.encodeInt(port);
        final byte[] rlpUDPPort = RLP.encodeInt(port);
        final byte[] rlpId = RLP.encodeElement(id);

        return RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort, rlpId);
    }

    /**
     * RLP without nodeId
     * [host, udpPort, tcpPort]
     * @return RLP-encoded node data
     */
    public byte[] getBriefRLP() {
        final byte[] rlphost = RLP.encodeElement(hostToBytes(host));
        final byte[] rlpTCPPort = RLP.encodeInt(port);
        final byte[] rlpUDPPort = RLP.encodeInt(port);

        return RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort);
    }

    @Override
    public String toString() {
        return "Node{" +
                " host='" + host + '\'' +
                ", port=" + port +
                ", id=" + Hex.toHexString(id) +
                '}';
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o instanceof Node) {
            return Arrays.equals(((Node) o).getId(), this.getId());
        }

        return false;
    }
}
