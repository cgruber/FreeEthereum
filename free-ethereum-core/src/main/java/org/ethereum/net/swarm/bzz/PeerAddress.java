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

package org.ethereum.net.swarm.bzz;

import org.ethereum.crypto.HashUtil;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.swarm.Key;
import org.ethereum.net.swarm.Util;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;



/**
 * Class similar for {@link Node} used in the swarm
 *
 * Created by Admin on 25.06.2015.
 */
public class PeerAddress {
    private byte[] ip;
    private int port;
    private byte[] id;

    private Key addrKeyCached = null;

    private PeerAddress() {
    }

    public PeerAddress(final Node discoverNode) {
        try {
            port = discoverNode.getPort();
            id = discoverNode.getId();
            ip = InetAddress.getByName(discoverNode.getHost()).getAddress();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public PeerAddress(final byte[] ip, final int port, final byte[] id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    static PeerAddress parse(final RLPList l) {
        final PeerAddress ret = new PeerAddress();
        ret.ip = l.get(0).getRLPData();
        ret.port = ByteUtil.byteArrayToInt(l.get(1).getRLPData());
        ret.id = l.get(2).getRLPData();
        return ret;
    }

    public Node toNode() {
        try {
            return new Node(id, InetAddress.getByAddress(ip).getHostAddress(), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the SHA3 hash if this node ID
     */
    public Key getAddrKey() {
        if (addrKeyCached == null) {
            addrKeyCached = new Key(HashUtil.INSTANCE.sha3(id));
        }
        return addrKeyCached;
    }

    public byte[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public byte[] getId() {
        return id;
    }

    byte[] encodeRlp() {
        return RLP.encodeList(RLP.encodeElement(ip),
                RLP.encodeInt(port),
                RLP.encodeElement(id));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PeerAddress that = (PeerAddress) o;

        if (port != that.port) return false;
        return Arrays.equals(ip, that.ip);

    }

    @Override
    public int hashCode() {
        int result = ip != null ? Arrays.hashCode(ip) : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "PeerAddress{" +
                "ip=" + Util.ipBytesToString(ip) +
                ", port=" + port +
                ", id=" + Hex.toHexString(id) +
                '}';
    }
}
