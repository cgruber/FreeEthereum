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

package org.ethereum.core;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Wraps {@link BlockHeader}</p>
 * Adds some additional data
 *
 * @author Mikhail Kalinin
 * @since 05.02.2016
 */
public class BlockHeaderWrapper {

    private BlockHeader header;
    private byte[] nodeId;

    public BlockHeaderWrapper(final BlockHeader header, final byte[] nodeId) {
        this.header = header;
        this.nodeId = nodeId;
    }

    public BlockHeaderWrapper(final byte[] bytes) {
        parse(bytes);
    }

    public byte[] getBytes() {
        final byte[] headerBytes = header.getEncoded();
        final byte[] nodeIdBytes = RLP.encodeElement(nodeId);
        return RLP.encodeList(headerBytes, nodeIdBytes);
    }

    private void parse(final byte[] bytes) {
        final List<RLPElement> params = RLP.decode2(bytes);
        final List<RLPElement> wrapper = (RLPList) params.get(0);

        final byte[] headerBytes = wrapper.get(0).getRLPData();

        this.header= new BlockHeader(headerBytes);
        this.nodeId = wrapper.get(1).getRLPData();
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public byte[] getHash() {
        return header.getHash();
    }

    public long getNumber() {
        return header.getNumber();
    }

    public BlockHeader getHeader() {
        return header;
    }

    public String getHexStrShort() {
        return Hex.toHexString(header.getHash()).substring(0, 6);
    }

    public boolean sentBy(final byte[] nodeId) {
        return Arrays.equals(this.nodeId, nodeId);
    }

    @Override
    public String toString() {
        return "BlockHeaderWrapper {" +
                "header=" + header +
                ", nodeId=" + Hex.toHexString(nodeId) +
                '}';
    }
}
