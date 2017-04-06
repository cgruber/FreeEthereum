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

import org.ethereum.net.swarm.Key;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * The message is the immediate response on the {#RETRIEVE_REQUEST} with the nearest known nodes
 * of the requested hash.
 * Contains a list of nearest Nodes (addresses) to the requested hash.
 */
public class BzzPeersMessage extends BzzMessage {

    private List<PeerAddress> peers;
    private long timeout;
    // optional
    private Key key;

    public BzzPeersMessage(final byte[] encoded) {
        super(encoded);
    }

    public BzzPeersMessage(final List<PeerAddress> peers, final long timeout, final Key key, final long id) {
        this.peers = peers;
        this.timeout = timeout;
        this.key = key;
        this.id = id;
    }

    public BzzPeersMessage(final List<PeerAddress> peers) {
        this.peers = peers;
    }

    @Override
    protected void decode() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        peers = new ArrayList<>();
        final RLPList addrs = (RLPList) paramsList.get(0);
        for (final RLPElement a : addrs) {
            peers.add(PeerAddress.parse((RLPList) a));
        }
        timeout = ByteUtil.byteArrayToLong(paramsList.get(1).getRLPData());
        if (paramsList.size() > 2) {
            key = new Key(paramsList.get(2).getRLPData());
        }
        if (paramsList.size() > 3) {
            id = ByteUtil.byteArrayToLong(paramsList.get(3).getRLPData());
        }

        parsed = true;
    }

    private void encode() {
        final byte[][] bPeers = new byte[this.peers.size()][];
        for (int i = 0; i < this.peers.size(); i++) {
            final PeerAddress peer = this.peers.get(i);
            bPeers[i] = peer.encodeRlp();
        }
        final byte[] bPeersList = RLP.encodeList(bPeers);
        final byte[] bTimeout = RLP.encodeInt((int) timeout);

        if (key == null) {
            this.encoded = RLP.encodeList(bPeersList, bTimeout);
        } else {
            this.encoded = RLP.encodeList(bPeersList,
                    bTimeout,
                    RLP.encodeElement(key.getBytes()),
                    RLP.encodeInt((int) id));
        }
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public List<PeerAddress> getPeers() {
        return peers;
    }

    public Key getKey() {
        return key;
    }

    public long getId() {
        return id;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.PEERS;
    }

    @Override
    public String toString() {
        return "BzzPeersMessage{" +
                "peers=" + peers +
                ", key=" + key +
                ", id=" + id +
                '}';
    }
}
