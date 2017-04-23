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

import org.ethereum.net.client.Capability;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.EthMessageCodes;
import org.ethereum.net.p2p.P2pMessageCodes;
import org.ethereum.net.shh.ShhMessageCodes;
import org.ethereum.net.swarm.bzz.BzzMessageCodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageCodesResolver {

    private final Map<String, Integer> offsets = new HashMap<>();

    public MessageCodesResolver() {
    }

    public MessageCodesResolver(final List<Capability> caps) {
        init(caps);
    }

    public void init(final List<Capability> caps) {
        Collections.sort(caps);
        int offset = P2pMessageCodes.USER.asByte() + 1;

        for (final Capability capability : caps) {
            if (capability.getName().equals(Capability.Companion.getETH())) {
                setEthOffset(offset);
                final EthVersion v = EthVersion.Companion.fromCode(capability.getVersion());
                offset += EthMessageCodes.Companion.values(v).length;
            }

            if (capability.getName().equals(Capability.Companion.getSHH())) {
                setShhOffset(offset);
                offset += ShhMessageCodes.values().length;
            }

            if (capability.getName().equals(Capability.Companion.getBZZ())) {
                setBzzOffset(offset);
                offset += BzzMessageCodes.values().length + 4;
                // FIXME: for some reason Go left 4 codes between BZZ and ETH message codes
            }
        }
    }

    public byte withP2pOffset(final byte code) {
        return withOffset(code, Capability.Companion.getP2P());
    }

    public byte withBzzOffset(final byte code) {
        return withOffset(code, Capability.Companion.getBZZ());
    }

    public byte withEthOffset(final byte code) {
        return withOffset(code, Capability.Companion.getETH());
    }

    public byte withShhOffset(final byte code) {
        return withOffset(code, Capability.Companion.getSHH());
    }

    private byte withOffset(final byte code, final String cap) {
        final byte offset = getOffset(cap);
        return (byte)(code + offset);
    }

    public byte resolveP2p(final byte code) {
        return resolve(code, Capability.Companion.getP2P());
    }

    public byte resolveBzz(final byte code) {
        return resolve(code, Capability.Companion.getBZZ());
    }

    public byte resolveEth(final byte code) {
        return resolve(code, Capability.Companion.getETH());
    }

    public byte resolveShh(final byte code) {
        return resolve(code, Capability.Companion.getSHH());
    }

    private byte resolve(final byte code, final String cap) {
        final byte offset = getOffset(cap);
        return (byte)(code - offset);
    }

    private byte getOffset(final String cap) {
        final Integer offset = offsets.get(cap);
        return offset == null ? 0 : offset.byteValue();
    }

    private void setBzzOffset(final int offset) {
        setOffset(Capability.Companion.getBZZ(), offset);
    }

    public void setEthOffset(final int offset) {
        setOffset(Capability.Companion.getETH(), offset);
    }

    public void setShhOffset(final int offset) {
        setOffset(Capability.Companion.getSHH(), offset);
    }

    private void setOffset(final String cap, final int offset) {
        offsets.put(cap, offset);
    }
}
