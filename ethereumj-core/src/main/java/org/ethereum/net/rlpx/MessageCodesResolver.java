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

    public MessageCodesResolver(List<Capability> caps) {
        init(caps);
    }

    public void init(List<Capability> caps) {
        Collections.sort(caps);
        int offset = P2pMessageCodes.USER.asByte() + 1;

        for (Capability capability : caps) {
            if (capability.getName().equals(Capability.ETH)) {
                setEthOffset(offset);
                EthVersion v = EthVersion.Companion.fromCode(capability.getVersion());
                offset += EthMessageCodes.values(v).length;
            }

            if (capability.getName().equals(Capability.SHH)) {
                setShhOffset(offset);
                offset += ShhMessageCodes.values().length;
            }

            if (capability.getName().equals(Capability.BZZ)) {
                setBzzOffset(offset);
                offset += BzzMessageCodes.values().length + 4;
                // FIXME: for some reason Go left 4 codes between BZZ and ETH message codes
            }
        }
    }

    public byte withP2pOffset(byte code) {
        return withOffset(code, Capability.P2P);
    }

    public byte withBzzOffset(byte code) {
        return withOffset(code, Capability.BZZ);
    }

    public byte withEthOffset(byte code) {
        return withOffset(code, Capability.ETH);
    }

    public byte withShhOffset(byte code) {
        return withOffset(code, Capability.SHH);
    }

    private byte withOffset(byte code, String cap) {
        byte offset = getOffset(cap);
        return (byte)(code + offset);
    }

    public byte resolveP2p(byte code) {
        return resolve(code, Capability.P2P);
    }

    public byte resolveBzz(byte code) {
        return resolve(code, Capability.BZZ);
    }

    public byte resolveEth(byte code) {
        return resolve(code, Capability.ETH);
    }

    public byte resolveShh(byte code) {
        return resolve(code, Capability.SHH);
    }

    private byte resolve(byte code, String cap) {
        byte offset = getOffset(cap);
        return (byte)(code - offset);
    }

    private byte getOffset(String cap) {
        Integer offset = offsets.get(cap);
        return offset == null ? 0 : offset.byteValue();
    }

    private void setBzzOffset(int offset) {
        setOffset(Capability.BZZ, offset);
    }

    public void setEthOffset(int offset) {
        setOffset(Capability.ETH, offset);
    }

    public void setShhOffset(int offset) {
        setOffset(Capability.SHH, offset);
    }

    private void setOffset(String cap, int offset) {
        offsets.put(cap, offset);
    }
}
