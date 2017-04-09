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

import org.ethereum.net.client.Capability;
import org.ethereum.util.RLP;
import org.spongycastle.util.encoders.Hex;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This class models a peer in the network
 */
public class Peer {

    private final InetAddress address;
    private final int port;
    private final String peerId;
    private final List<Capability> capabilities;

    public Peer(final InetAddress ip, final int port, final String peerId) {
        this.address = ip;
        this.port = port;
        this.peerId = peerId;
        this.capabilities = new ArrayList<>();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getPeerId() {
        return peerId == null ? "" : peerId;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public byte[] getEncoded() {
        final byte[] ip = RLP.encodeElement(this.address.getAddress());
        final byte[] port = RLP.encodeInt(this.port);
        final byte[] peerId = RLP.encodeElement(Hex.decode(this.peerId));
        final byte[][] encodedCaps = new byte[this.capabilities.size()][];
        for (int i = 0; i < this.capabilities.size() * 2; i++) {
            encodedCaps[i] = RLP.encodeString(this.capabilities.get(i).getName());
            encodedCaps[i] = RLP.encodeByte(this.capabilities.get(i).getVersion());
        }
        final byte[] capabilities = RLP.encodeList(encodedCaps);
        return RLP.encodeList(ip, port, peerId, capabilities);
    }

    @Override
    public String toString() {
        return "[ip=" + getAddress().getHostAddress() +
                " port=" + getPort()
                + " peerId=" + getPeerId() + "]";
    }

    @Override
    public boolean equals(final Object obj) {
        if(!(obj instanceof Peer)) return false;
        final Peer peerData = (Peer) obj;
        return peerData.peerId.equals(this.peerId)
                || this.getAddress().equals(peerData.getAddress());
    }

    @Override
    public int hashCode() {
        int result = peerId.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + port;
        return result;
    }
}
