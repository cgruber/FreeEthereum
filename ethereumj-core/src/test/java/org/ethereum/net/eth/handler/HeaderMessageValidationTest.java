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

import org.ethereum.core.BlockHeader;
import org.ethereum.net.eth.message.BlockHeadersMessage;
import org.ethereum.net.eth.message.GetBlockHeadersMessage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Testing {@link org.ethereum.net.eth.handler.Eth62#isValid(BlockHeadersMessage, GetBlockHeadersMessageWrapper)}
 */
public class HeaderMessageValidationTest {

    private final byte[] EMPTY_ARRAY = new byte[0];
    private final Eth62Tester ethHandler;

    public HeaderMessageValidationTest() {
        ethHandler = new Eth62Tester();
    }

    @Test
    public void testSingleBlockResponse() {
        final long blockNumber = 0L;
        final BlockHeader blockHeader = new BlockHeader(new byte[]{11, 12}, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        final List<BlockHeader> blockHeaders = new ArrayList<>();
        blockHeaders.add(blockHeader);
        final BlockHeadersMessage msg = new BlockHeadersMessage(blockHeaders);

        final byte[] hash = blockHeader.getHash();
        // Block number doesn't matter when hash is provided in request
        final GetBlockHeadersMessage requestHash = new GetBlockHeadersMessage(123L, hash, 1, 0, false);
        final GetBlockHeadersMessageWrapper wrapperHash = new GetBlockHeadersMessageWrapper(requestHash);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperHash);

        // Getting same with block number request
        final GetBlockHeadersMessage requestNumber = new GetBlockHeadersMessage(blockNumber, null, 1, 0, false);
        final GetBlockHeadersMessageWrapper wrapperNumber = new GetBlockHeadersMessageWrapper(requestNumber);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperNumber);

        // Getting same with reverse request
        final GetBlockHeadersMessage requestReverse = new GetBlockHeadersMessage(blockNumber, null, 1, 0, true);
        final GetBlockHeadersMessageWrapper wrapperReverse = new GetBlockHeadersMessageWrapper(requestReverse);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperReverse);

        // Getting same with skip request
        final GetBlockHeadersMessage requestSkip = new GetBlockHeadersMessage(blockNumber, null, 1, 10, false);
        final GetBlockHeadersMessageWrapper wrapperSkip = new GetBlockHeadersMessageWrapper(requestSkip);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperSkip);
    }

    @Test
    public void testFewBlocksNoSkip() {
        final List<BlockHeader> blockHeaders = new ArrayList<>();

        final long blockNumber1 = 0L;
        final BlockHeader blockHeader1 = new BlockHeader(new byte[]{11, 12}, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber1, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        final byte[] hash1 = blockHeader1.getHash();
        blockHeaders.add(blockHeader1);

        final long blockNumber2 = 1L;
        final BlockHeader blockHeader2 = new BlockHeader(hash1, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber2, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        final byte[] hash2 = blockHeader2.getHash();
        blockHeaders.add(blockHeader2);

        final BlockHeadersMessage msg = new BlockHeadersMessage(blockHeaders);

        // Block number doesn't matter when hash is provided in request
        final GetBlockHeadersMessage requestHash = new GetBlockHeadersMessage(123L, hash1, 2, 0, false);
        final GetBlockHeadersMessageWrapper wrapperHash = new GetBlockHeadersMessageWrapper(requestHash);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperHash);

        // Getting same with block number request
        final GetBlockHeadersMessage requestNumber = new GetBlockHeadersMessage(blockNumber1, null, 2, 0, false);
        final GetBlockHeadersMessageWrapper wrapperNumber = new GetBlockHeadersMessageWrapper(requestNumber);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperNumber);

        // Reverse list
        Collections.reverse(blockHeaders);
        final GetBlockHeadersMessage requestReverse = new GetBlockHeadersMessage(blockNumber2, null, 2, 0, true);
        final GetBlockHeadersMessageWrapper wrapperReverse = new GetBlockHeadersMessageWrapper(requestReverse);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperReverse);
    }

    @Test
    public void testFewBlocksWithSkip() {
        final List<BlockHeader> blockHeaders = new ArrayList<>();

        final long blockNumber1 = 0L;
        final BlockHeader blockHeader1 = new BlockHeader(new byte[]{11, 12}, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber1, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        blockHeaders.add(blockHeader1);

        final long blockNumber2 = 16L;
        final BlockHeader blockHeader2 = new BlockHeader(new byte[]{12, 13}, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber2, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        blockHeaders.add(blockHeader2);

        final long blockNumber3 = 32L;
        final BlockHeader blockHeader3 = new BlockHeader(new byte[]{14, 15}, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY,
                EMPTY_ARRAY, blockNumber3, EMPTY_ARRAY, 1L, 2L, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY);
        blockHeaders.add(blockHeader3);

        final BlockHeadersMessage msg = new BlockHeadersMessage(blockHeaders);

        final GetBlockHeadersMessage requestNumber = new GetBlockHeadersMessage(blockNumber1, null, 3, 15, false);
        final GetBlockHeadersMessageWrapper wrapperNumber = new GetBlockHeadersMessageWrapper(requestNumber);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperNumber);

        // Requesting more than we have
        final GetBlockHeadersMessage requestMore = new GetBlockHeadersMessage(blockNumber1, null, 4, 15, false);
        final GetBlockHeadersMessageWrapper wrapperMore = new GetBlockHeadersMessageWrapper(requestMore);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperMore);

        // Reverse list
        Collections.reverse(blockHeaders);
        final GetBlockHeadersMessage requestReverse = new GetBlockHeadersMessage(blockNumber3, null, 3, 15, true);
        final GetBlockHeadersMessageWrapper wrapperReverse = new GetBlockHeadersMessageWrapper(requestReverse);
        assert ethHandler.blockHeaderMessageValid(msg, wrapperReverse);
    }

    private class Eth62Tester extends Eth62 {

        boolean blockHeaderMessageValid(final BlockHeadersMessage msg, final GetBlockHeadersMessageWrapper request) {
            return super.isValid(msg, request);
        }
    }
}
