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

import com.google.common.base.Joiner;
import org.ethereum.net.client.Capability;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Wrapper around an Ethereum HelloMessage on the network
 *
 * @see org.ethereum.net.p2p.P2pMessageCodes#HELLO
 */
public class HelloMessage extends P2pMessage {

    /**
     * The implemented version of the P2P protocol.
     */
    private byte p2pVersion;
    /**
     * The underlying client. A user-readable string.
     */
    private String clientId;
    /**
     * A peer-network capability code, readable ASCII and 3 letters.
     * Currently only "eth", "shh" and "bzz" are known.
     */
    private List<Capability> capabilities = Collections.emptyList();
    /**
     * The port on which the peer is listening for an incoming connection
     */
    private int listenPort;
    /**
     * The identity and public key of the peer
     */
    private String peerId;

    public HelloMessage(final byte[] encoded) {
        super(encoded);
    }

    public HelloMessage(final byte p2pVersion, final String clientId,
                        final List<Capability> capabilities, final int listenPort, final String peerId) {
        this.p2pVersion = p2pVersion;
        this.clientId = clientId;
        this.capabilities = capabilities;
        this.listenPort = listenPort;
        this.peerId = peerId;
        this.parsed = true;
    }

    private void parse() {
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        final byte[] p2pVersionBytes = paramsList.get(0).getRLPData();
        this.p2pVersion = p2pVersionBytes != null ? p2pVersionBytes[0] : 0;

        final byte[] clientIdBytes = paramsList.get(1).getRLPData();
        this.clientId = new String(clientIdBytes != null ? clientIdBytes : EMPTY_BYTE_ARRAY);

        final RLPList capabilityList = (RLPList) paramsList.get(2);
        this.capabilities = new ArrayList<>();
        for (final Object aCapabilityList : capabilityList) {

            final RLPElement capId = ((RLPList) aCapabilityList).get(0);
            final RLPElement capVersion = ((RLPList) aCapabilityList).get(1);

            final String name = new String(capId.getRLPData());
            final byte version = capVersion.getRLPData() == null ? 0 : capVersion.getRLPData()[0];

            final Capability cap = new Capability(name, version);
            this.capabilities.add(cap);
        }

        final byte[] peerPortBytes = paramsList.get(3).getRLPData();
        this.listenPort = ByteUtil.byteArrayToInt(peerPortBytes);

        final byte[] peerIdBytes = paramsList.get(4).getRLPData();
        this.peerId = Hex.toHexString(peerIdBytes);
        this.parsed = true;
    }

    private void encode() {
        final byte[] p2pVersion = RLP.encodeByte(this.p2pVersion);
        final byte[] clientId = RLP.encodeString(this.clientId);
        final byte[][] capabilities = new byte[this.capabilities.size()][];
        for (int i = 0; i < this.capabilities.size(); i++) {
            final Capability capability = this.capabilities.get(i);
            capabilities[i] = RLP.encodeList(
                    RLP.encodeElement(capability.getName().getBytes()),
                    RLP.encodeInt(capability.getVersion()));
        }
        final byte[] capabilityList = RLP.encodeList(capabilities);
        final byte[] peerPort = RLP.encodeInt(this.listenPort);
        final byte[] peerId = RLP.encodeElement(Hex.decode(this.peerId));

        this.encoded = RLP.encodeList(p2pVersion, clientId,
                capabilityList, peerPort, peerId);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public byte getP2PVersion() {
        if (!parsed) parse();
        return p2pVersion;
    }

    public String getClientId() {
        if (!parsed) parse();
        return clientId;
    }

    public List<Capability> getCapabilities() {
        if (!parsed) parse();
        return capabilities;
    }

    public int getListenPort() {
        if (!parsed) parse();
        return listenPort;
    }

    public String getPeerId() {
        if (!parsed) parse();
        return peerId;
    }

    public void setPeerId(final String peerId) {
        this.peerId = peerId;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.HELLO;
    }

    public void setP2pVersion(final byte p2pVersion) {
        this.p2pVersion = p2pVersion;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) parse();
        return "[" + this.getCommand().name() + " p2pVersion="
                + this.p2pVersion + " clientId=" + this.clientId
                + " capabilities=[" + Joiner.on(" ").join(this.capabilities)
                + "]" + " peerPort=" + this.listenPort + " peerId="
                + this.peerId + "]";
    }
}