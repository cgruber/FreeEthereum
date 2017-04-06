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

import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPItem;
import org.ethereum.util.RLPList;

import static org.ethereum.util.ByteUtil.*;

public class PingMessage extends Message {

    int version;
    private String toHost;
    private int toPort;
    private String fromHost;
    private int fromPort;
    private long expires;

    public static PingMessage create(final Node fromNode, final Node toNode, final ECKey privKey) {
        return create(fromNode, toNode, privKey, 4);
    }

    private static PingMessage create(final Node fromNode, final Node toNode, final ECKey privKey, final int version) {

        final long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        final byte[] tmpExp = longToBytes(expiration);
        final byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        final byte[] type = new byte[]{1};
        final byte[] rlpVer = RLP.encodeInt(version);
        final byte[] rlpFromList = fromNode.getBriefRLP();
        final byte[] rlpToList = toNode.getBriefRLP();
        final byte[] data = RLP.encodeList(rlpVer, rlpFromList, rlpToList, rlpExp);

        final PingMessage ping = new PingMessage();
        ping.encode(type, data, privKey);

        ping.expires = expiration;
        ping.toHost = toNode.getHost();
        ping.toPort = toNode.getPort();
        ping.fromHost = fromNode.getHost();
        ping.fromPort = fromNode.getPort();

        return ping;
    }

    @Override
    public void parse(final byte[] data) {

        final RLPList dataList = (RLPList) RLP.decode2OneItem(data, 0);

        final RLPList fromList = (RLPList) dataList.get(1);
        final byte[] ipF = fromList.get(0).getRLPData();
        this.fromHost = bytesToIp(ipF);
        this.fromPort = ByteUtil.byteArrayToInt(fromList.get(1).getRLPData());

        final RLPList toList = (RLPList) dataList.get(2);
        final byte[] ipT = toList.get(0).getRLPData();
        this.toHost = bytesToIp(ipT);
        this.toPort = ByteUtil.byteArrayToInt(toList.get(1).getRLPData());

        final RLPItem expires = (RLPItem) dataList.get(3);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());

        this.version = ByteUtil.byteArrayToInt(dataList.get(0).getRLPData());
    }


    public String getToHost() {
        return toHost;
    }

    public int getToPort() {
        return toPort;
    }

    public String getFromHost() {
        return fromHost;
    }

    public int getFromPort() {
        return fromPort;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        final long currTime = System.currentTimeMillis() / 1000;

        final String out = String.format("[PingMessage] \n %s:%d ==> %s:%d \n expires in %d seconds \n %s\n",
                fromHost, fromPort, toHost, toPort, (expires - currTime), super.toString());

        return out;
    }
}
