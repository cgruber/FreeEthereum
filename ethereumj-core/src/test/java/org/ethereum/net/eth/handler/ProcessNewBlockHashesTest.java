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

package org.ethereum.net.eth.handler;

import org.ethereum.core.BlockIdentifier;
import org.ethereum.net.eth.message.NewBlockHashesMessage;
import org.ethereum.net.server.Channel;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Testing {@link Eth62#processNewBlockHashes(NewBlockHashesMessage)}
 */
public class ProcessNewBlockHashesTest {
    private static final Logger logger = LoggerFactory.getLogger("test");
    private final Eth62Tester ethHandler;

    public ProcessNewBlockHashesTest() {
        ethHandler = new Eth62Tester();
    }

    @Test
    public void testSingleHashHandling() {
        final List<BlockIdentifier> blockIdentifiers = new ArrayList<>();
        final byte[] blockHash = new byte[]{2, 3, 4};
        final long blockNumber = 123;
        blockIdentifiers.add(new BlockIdentifier(blockHash, blockNumber));
        final NewBlockHashesMessage msg = new NewBlockHashesMessage(blockIdentifiers);

        ethHandler.setGetNewBlockHeadersParams(blockHash, 1, 0, false);
        ethHandler.processNewBlockHashes(msg);
        assert ethHandler.wasCalled;
    }

    @Test
    public void testSeveralHashesHandling() {
        final List<BlockIdentifier> blockIdentifiers = new ArrayList<>();
        final byte[] blockHash1 = new byte[]{2, 3, 4};
        final long blockNumber1 = 123;
        final byte[] blockHash2 = new byte[]{5, 3, 4};
        final long blockNumber2 = 124;
        final byte[] blockHash3 = new byte[]{2, 6, 4};
        final long blockNumber3 = 125;
        blockIdentifiers.add(new BlockIdentifier(blockHash1, blockNumber1));
        blockIdentifiers.add(new BlockIdentifier(blockHash2, blockNumber2));
        blockIdentifiers.add(new BlockIdentifier(blockHash3, blockNumber3));
        final NewBlockHashesMessage msg = new NewBlockHashesMessage(blockIdentifiers);

        ethHandler.setGetNewBlockHeadersParams(blockHash1, 3, 0, false);
        ethHandler.processNewBlockHashes(msg);
        assert ethHandler.wasCalled;
    }

    @Test
    public void testSeveralHashesMixedOrderHandling() {
        final List<BlockIdentifier> blockIdentifiers = new ArrayList<>();
        final byte[] blockHash1 = new byte[]{5, 3, 4};
        final long blockNumber1 = 124;
        final byte[] blockHash2 = new byte[]{2, 3, 4};
        final long blockNumber2 = 123;
        final byte[] blockHash3 = new byte[]{2, 6, 4};
        final long blockNumber3 = 125;
        blockIdentifiers.add(new BlockIdentifier(blockHash1, blockNumber1));
        blockIdentifiers.add(new BlockIdentifier(blockHash2, blockNumber2));
        blockIdentifiers.add(new BlockIdentifier(blockHash3, blockNumber3));
        final NewBlockHashesMessage msg = new NewBlockHashesMessage(blockIdentifiers);

        ethHandler.setGetNewBlockHeadersParams(blockHash2, 3, 0, false);
        ethHandler.processNewBlockHashes(msg);
        assert ethHandler.wasCalled;
    }

    private class Eth62Tester extends Eth62 {

        private byte[] blockHash;
        private int maxBlockAsk;
        private int skip;
        private boolean reverse;

        private boolean wasCalled = false;

        Eth62Tester() {
            this.syncDone = true;
            this.channel = new Channel();
        }

        void setGetNewBlockHeadersParams(final byte[] blockHash, final int maxBlocksAsk, final int skip, final boolean reverse) {
            this.blockHash = blockHash;
            this.maxBlockAsk = maxBlocksAsk;
            this.skip = skip;
            this.reverse = reverse;
            this.wasCalled = false;
        }

        @Override
        protected synchronized void sendGetNewBlockHeaders(final byte[] blockHash, final int maxBlocksAsk, final int skip, final boolean reverse) {
            this.wasCalled = true;
            logger.error("Request for sending new headers: hash {}, max {}, skip {}, reverse {}",
                    Hex.toHexString(blockHash), maxBlocksAsk, skip, reverse);
            assert Arrays.equals(blockHash, this.blockHash) &&
                    maxBlocksAsk == this.maxBlockAsk && skip == this.skip && reverse == this.reverse;
        }
    }
}
