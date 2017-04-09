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

import com.google.common.collect.Lists;
import org.ethereum.net.client.Capability;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import static org.ethereum.util.ByteUtil.longToBytes;

/**
 * Created by devrandom on 2015-04-12.
 */
public class HandshakeMessage {
    public static final int HANDSHAKE_MESSAGE_TYPE = 0x00;
    public static final int NODE_ID_BITS = 512;
    long version;
    String name;
    List<Capability> caps;
    long listenPort;
    byte[] nodeId;

    public HandshakeMessage(final long version, final String name, final List<Capability> caps, final long listenPort, final byte[] nodeId) {
        this.version = version;
        this.name = name;
        this.caps = caps;
        this.listenPort = listenPort;
        this.nodeId = nodeId;
    }

    private HandshakeMessage() {
    }

    static HandshakeMessage parse(final byte[] wire) {
        final RLPList list = (RLPList) RLP.decode2(wire).get(0);
        final HandshakeMessage message = new HandshakeMessage();
        final Iterator<RLPElement> iter = list.iterator();
        message.version = ByteUtil.byteArrayToInt(iter.next().getRLPData()); // FIXME long
        message.name = new String(iter.next().getRLPData(), Charset.forName("UTF-8"));
        // caps
        message.caps = Lists.newArrayList();
        for (final RLPElement capEl : (RLPList) iter.next()) {
            final RLPList capElList = (RLPList) capEl;
            final String name = new String(capElList.get(0).getRLPData(), Charset.forName("UTF-8"));
            final long version = ByteUtil.byteArrayToInt(capElList.get(1).getRLPData());

            message.caps.add(new Capability(name, (byte)version)); // FIXME long
        }
        message.listenPort = ByteUtil.byteArrayToInt(iter.next().getRLPData());
        message.nodeId = iter.next().getRLPData();
        return message;
    }

    public byte[] encode() {
        final List<byte[]> capsItemBytes = Lists.newArrayList();
        for (final Capability cap : caps) {
            capsItemBytes.add(RLP.encodeList(
                    RLP.encodeElement(cap.getName().getBytes()),
                    RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(cap.getVersion())))
            ));
        }
        return RLP.encodeList(
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(version))),
                RLP.encodeElement(name.getBytes()),
                RLP.encodeList(capsItemBytes.toArray(new byte[0][])),
                RLP.encodeElement(ByteUtil.stripLeadingZeroes(longToBytes(listenPort))),
                RLP.encodeElement(nodeId)
        );
    }
}
