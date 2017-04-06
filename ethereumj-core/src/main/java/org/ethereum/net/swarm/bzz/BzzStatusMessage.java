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

import org.ethereum.net.client.Capability;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.net.swarm.Util.*;

/**
 * BZZ handshake message
 */
public class BzzStatusMessage extends BzzMessage {

    private long version;
    private String id;
    private PeerAddress addr;
    private long networkId;
    private List<Capability> capabilities;

    public BzzStatusMessage(final byte[] encoded) {
        super(encoded);
    }

    public BzzStatusMessage(final int version, final String id, final PeerAddress addr, final long networkId, final List<Capability> capabilities) {
        this.version = version;
        this.id = id;
        this.addr = addr;
        this.networkId = networkId;
        this.capabilities = capabilities;
        parsed = true;
    }

    @Override
    protected void decode() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        version = rlpDecodeLong(paramsList.get(0));
        id = rlpDecodeString(paramsList.get(1));
        addr = PeerAddress.parse((RLPList) paramsList.get(2));
        networkId = rlpDecodeInt(paramsList.get(3));

        capabilities = new ArrayList<>();
        final RLPList caps = (RLPList) paramsList.get(4);
        for (final RLPElement c : caps) {
            final RLPList e = (RLPList) c;
            capabilities.add(new Capability(rlpDecodeString(e.get(0)), rlpDecodeByte(e.get(1))));
        }

        parsed = true;
    }

    private void encode() {
        final byte[][] capabilities = new byte[this.capabilities.size()][];
        for (int i = 0; i < this.capabilities.size(); i++) {
            final Capability capability = this.capabilities.get(i);
            capabilities[i] = rlpEncodeList(capability.getName(),capability.getVersion());
        }
        this.encoded = rlpEncodeList(version, id, addr.encodeRlp(), networkId, rlpEncodeList(capabilities));
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    /**
     * BZZ protocol version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Gets the remote peer address
     */
    public PeerAddress getAddr() {
        return addr;
    }

    public long getNetworkId() {
        return networkId;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.STATUS;
    }


    @Override
    public String toString() {
        return "BzzStatusMessage{" +
                "version=" + version +
                ", id='" + id + '\'' +
                ", addr=" + addr +
                ", networkId=" + networkId +
                ", capabilities=" + capabilities +
                '}';
    }
}
