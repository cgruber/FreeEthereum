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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.ethereum.util.TimeUtils.secondsToMillis;

/**
 * <p> Wraps {@link Block} </p>
 * Adds some additional data required by core during blocks processing
 *
 * @author Mikhail Kalinin
 * @since 24.07.2015
 */
public class BlockWrapper {

    private static final long SOLID_BLOCK_DURATION_THRESHOLD = secondsToMillis(60);

    private Block block;
    private long importFailedAt = 0;
    private long receivedAt = 0;
    private boolean newBlock;
    private byte[] nodeId;

    public BlockWrapper(final Block block, final byte[] nodeId) {
        this(block, false, nodeId);
    }

    public BlockWrapper(final Block block, final boolean newBlock, final byte[] nodeId) {
        this.block = block;
        this.newBlock = newBlock;
        this.nodeId = nodeId;
    }

    public BlockWrapper(final byte[] bytes) {
        parse(bytes);
    }

    public Block getBlock() {
        return block;
    }

    public boolean isNewBlock() {
        return newBlock;
    }

    public boolean isSolidBlock() {
        return !newBlock || timeSinceReceiving() > SOLID_BLOCK_DURATION_THRESHOLD;
    }

    public long getImportFailedAt() {
        return importFailedAt;
    }

    public void setImportFailedAt(final long importFailedAt) {
        this.importFailedAt = importFailedAt;
    }

    public byte[] getHash() {
        return block.getHash();
    }

    public long getNumber() {
        return block.getNumber();
    }

    public byte[] getEncoded() {
        return block.getEncoded();
    }

    public String getShortHash() {
        return block.getShortHash();
    }

    public byte[] getParentHash() {
        return block.getParentHash();
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(final long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public boolean sentBy(final byte[] nodeId) {
        return Arrays.equals(this.nodeId, nodeId);
    }

    public boolean isEqual(final BlockWrapper wrapper) {
        return wrapper != null && block.isEqual(wrapper.getBlock());
    }

    public void importFailed() {
        if (importFailedAt == 0) {
            importFailedAt = System.currentTimeMillis();
        }
    }

    public void resetImportFail() {
        importFailedAt = 0;
    }

    public long timeSinceFail() {
        if(importFailedAt == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - importFailedAt;
        }
    }

    private long timeSinceReceiving() {
        return System.currentTimeMillis() - receivedAt;
    }

    public byte[] getBytes() {
        final byte[] blockBytes = block.getEncoded();
        final byte[] importFailedBytes = RLP.encodeBigInteger(BigInteger.valueOf(importFailedAt));
        final byte[] receivedAtBytes = RLP.encodeBigInteger(BigInteger.valueOf(receivedAt));
        final byte[] newBlockBytes = RLP.encodeByte((byte) (newBlock ? 1 : 0));
        final byte[] nodeIdBytes = RLP.encodeElement(nodeId);
        return RLP.encodeList(blockBytes, importFailedBytes,
                receivedAtBytes, newBlockBytes, nodeIdBytes);
    }

    private void parse(final byte[] bytes) {
        final List<RLPElement> params = RLP.decode2(bytes);
        final List<RLPElement> wrapper = (RLPList) params.get(0);

        final byte[] blockBytes = wrapper.get(0).getRLPData();
        final byte[] importFailedBytes = wrapper.get(1).getRLPData();
        final byte[] receivedAtBytes = wrapper.get(2).getRLPData();
        final byte[] newBlockBytes = wrapper.get(3).getRLPData();

        this.block = new Block(blockBytes);
        this.importFailedAt = importFailedBytes == null ? 0 : new BigInteger(1, importFailedBytes).longValue();
        this.receivedAt = receivedAtBytes == null ? 0 : new BigInteger(1, receivedAtBytes).longValue();
        final byte newBlock = newBlockBytes == null ? 0 : new BigInteger(1, newBlockBytes).byteValue();
        this.newBlock = newBlock == 1;
        this.nodeId = wrapper.get(4).getRLPData();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BlockWrapper wrapper = (BlockWrapper) o;

        return block.isEqual(wrapper.block);
    }
}
