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

package org.ethereum.net.dht;

import org.ethereum.crypto.HashUtil;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

class Peer {
    private byte[] id;
    private String host = "127.0.0.1";
    private int port = 0;

    public Peer(final byte[] id, final String host, final int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Peer(final byte[] ip) {
        this.id= ip;
    }

    public Peer() {
        HashUtil.randomPeerId();
    }

    public byte nextBit(final String startPattern) {

        if (this.toBinaryString().startsWith(startPattern + "1"))
            return 1;
        else
            return 0;
    }

    public byte[] calcDistance(final Peer toPeer) {

        final BigInteger aPeer = new BigInteger(getId());
        final BigInteger bPeer = new BigInteger(toPeer.getId());

        final BigInteger distance = aPeer.xor(bPeer);
        return BigIntegers.asUnsignedByteArray(distance);
    }


    private byte[] getId() {
        return id;
    }

    public void setId(final byte[] ip) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("Peer {\n id=%s, \n host=%s, \n port=%d\n}", Hex.toHexString(id), host, port);
    }

    public String toBinaryString() {

        final BigInteger bi = new BigInteger(1, id);
        String out = String.format("%512s", bi.toString(2));
        out = out.replace(' ', '0');

        return out;
    }

}
