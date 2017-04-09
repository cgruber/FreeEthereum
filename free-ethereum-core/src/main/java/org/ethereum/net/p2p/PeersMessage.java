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

import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around an Ethereum Peers message on the network
 *
 * @see org.ethereum.net.p2p.P2pMessageCodes#PEERS
 */
public class PeersMessage extends P2pMessage {

    private boolean parsed = false;

    private Set<Peer> peers;

    public PeersMessage(final byte[] payload) {
        super(payload);
    }

    public PeersMessage(final Set<Peer> peers) {
        this.peers = peers;
        parsed = true;
    }

    private void parse() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        peers = new LinkedHashSet<>();
        for (int i = 1; i < paramsList.size(); ++i) {
            final RLPList peerParams = (RLPList) paramsList.get(i);
            final byte[] ipBytes = peerParams.get(0).getRLPData();
            final byte[] portBytes = peerParams.get(1).getRLPData();
            final byte[] peerIdRaw = peerParams.get(2).getRLPData();

            try {
                final int peerPort = ByteUtil.byteArrayToInt(portBytes);
                final InetAddress address = InetAddress.getByAddress(ipBytes);

                final String peerId = peerIdRaw == null ? "" : Hex.toHexString(peerIdRaw);
                final Peer peer = new Peer(address, peerPort, peerId);
                peers.add(peer);
            } catch (final UnknownHostException e) {
                throw new RuntimeException("Malformed ip", e);
            }
        }
        this.parsed = true;
    }

    private void encode() {
        final byte[][] encodedByteArrays = new byte[this.peers.size() + 1][];
        encodedByteArrays[0] = RLP.encodeByte(this.getCommand().asByte());
        final List<Peer> peerList = new ArrayList<>(this.peers);
        for (int i = 0; i < peerList.size(); i++) {
            encodedByteArrays[i + 1] = peerList.get(i).getEncoded();
        }
        this.encoded = RLP.encodeList(encodedByteArrays);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public Set<Peer> getPeers() {
        if (!parsed) this.parse();
        return peers;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.PEERS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) this.parse();

        final StringBuilder sb = new StringBuilder();
        for (final Peer peerData : peers) {
            sb.append("\n       ").append(peerData);
        }
        return "[" + this.getCommand().name() + sb.toString() + "]";
    }
}