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

package org.ethereum.sync;

import org.ethereum.TestUtils;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.util.FastByteComparisons;
import org.junit.Test;

import java.util.*;

public class SyncQueueImplTest {
    private static final int DEFAULT_REQUEST_LEN = 192;
    private final byte[] peer0 = new byte[32];

    @Test
    public void test1() {
        final List<Block> randomChain = TestUtils.getRandomChain(new byte[32], 0, 1024);

        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain.subList(0, 32));

        final SyncQueueIfc.HeadersRequest headersRequest = syncQueue.requestHeaders(DEFAULT_REQUEST_LEN, 1, Integer.MAX_VALUE).iterator().next();
        System.out.println(headersRequest);

        syncQueue.addHeaders(createHeadersFromBlocks(TestUtils.getRandomChain(randomChain.get(16).getHash(), 17, 64), peer0));

        syncQueue.addHeaders(createHeadersFromBlocks(randomChain.subList(32, 1024), peer0));
    }

    @Test
    public void test2() {
        final List<Block> randomChain = TestUtils.getRandomChain(new byte[32], 0, 1024);

        final Peer[] peers = new Peer[10];
        peers[0] = new Peer(randomChain);
        for (int i = 1; i < peers.length; i++) {
            peers[i] = new Peer(TestUtils.getRandomChain(TestUtils.randomBytes(32), 1, 1024));
        }

    }

    @Test
    public void testHeadersSplit() {
        // 1, 2, 3, 4, 5
        final SyncQueueImpl.HeadersRequestImpl headersRequest = new SyncQueueImpl.HeadersRequestImpl(1, 5, false);
        final List<SyncQueueIfc.HeadersRequest> requests = headersRequest.split(2);
        assert requests.size() == 3;

        // 1, 2
        assert requests.get(0).getStart() == 1;
        assert requests.get(0).getCount() == 2;

        // 3, 4
        assert requests.get(1).getStart() == 3;
        assert requests.get(1).getCount() == 2;

        // 5
        assert requests.get(2).getStart() == 5;
        assert requests.get(2).getCount() == 1;
    }

    @Test
    public void testReverseHeaders1() {
        final List<Block> randomChain = TestUtils.getRandomChain(new byte[32], 0, 699);
        final List<Block> randomChain1 = TestUtils.getRandomChain(new byte[32], 0, 699);
        final Peer[] peers = new Peer[]{new Peer(randomChain), new Peer(randomChain, false), new Peer(randomChain1)};
        final SyncQueueReverseImpl syncQueue = new SyncQueueReverseImpl(randomChain.get(randomChain.size() - 1).getHash(), true);
        final List<BlockHeaderWrapper> result = new ArrayList<>();
        int peerIdx = 1;
        final Random rnd = new Random();
        int cnt = 0;
        while (cnt < 1000) {
            System.out.println("Cnt: " + cnt++);
            final Collection<SyncQueueIfc.HeadersRequest> headersRequests = syncQueue.requestHeaders(20, 5, Integer.MAX_VALUE);
            if (headersRequests == null) break;
            for (final SyncQueueIfc.HeadersRequest request : headersRequests) {
                System.out.println("Req: " + request);
                final List<BlockHeader> headers = rnd.nextBoolean() ? peers[peerIdx].getHeaders(request)
                        : peers[peerIdx].getRandomHeaders(10);
                //                List<BlockHeader> headers = peers[0].getHeaders(request);

                peerIdx = (peerIdx + 1) % peers.length;
                final List<BlockHeaderWrapper> ret = syncQueue.addHeaders(createHeadersFromHeaders(headers, peer0));
                result.addAll(ret);
                System.out.println("Result length: " + result.size());
            }
        }

        final List<BlockHeaderWrapper> extraHeaders =
                syncQueue.addHeaders(createHeadersFromHeaders(peers[0].getRandomHeaders(10), peer0));
        assert extraHeaders.isEmpty();

        assert cnt != 1000;
        assert result.size() == randomChain.size() - 1;
        for (int i = 0; i < result.size() - 1; i++) {
            assert Arrays.equals(result.get(i + 1).getHash(), result.get(i).getHeader().getParentHash());
        }
        assert Arrays.equals(randomChain.get(0).getHash(), result.get(result.size() - 1).getHeader().getParentHash());
    }

    @Test
    public void testReverseHeaders2() {
        final List<Block> randomChain = TestUtils.getRandomChain(new byte[32], 0, 194);
        final Peer[] peers = new Peer[]{new Peer(randomChain), new Peer(randomChain)};
        final SyncQueueReverseImpl syncQueue = new SyncQueueReverseImpl(randomChain.get(randomChain.size() - 1).getHash(), true);
        final List<BlockHeaderWrapper> result = new ArrayList<>();
        int peerIdx = 1;
        int cnt = 0;
        while (cnt < 100) {
            System.out.println("Cnt: " + cnt++);
            final Collection<SyncQueueIfc.HeadersRequest> headersRequests = syncQueue.requestHeaders(192, 10, Integer.MAX_VALUE);
            if (headersRequests == null) break;
            for (final SyncQueueIfc.HeadersRequest request : headersRequests) {
                System.out.println("Req: " + request);
                final List<BlockHeader> headers = peers[peerIdx].getHeaders(request);

                // Removing genesis header, which we will not get from real peers
                headers.removeIf(blockHeader -> FastByteComparisons.equal(blockHeader.getHash(), randomChain.get(0).getHash()));

                peerIdx = (peerIdx + 1) % 2;
                final List<BlockHeaderWrapper> ret = syncQueue.addHeaders(createHeadersFromHeaders(headers, peer0));
                result.addAll(ret);
                System.out.println("Result length: " + result.size());
            }
        }

        assert cnt != 100;
        assert result.size() == randomChain.size() - 1; // - genesis
        for (int  i = 0; i < result.size() - 1; i++) {
            assert Arrays.equals(result.get(i + 1).getHash(), result.get(i).getHeader().getParentHash());
        }
        assert Arrays.equals(randomChain.get(0).getHash(), result.get(result.size() - 1).getHeader().getParentHash());
    }

    @Test
    public void testLongLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 10500, 3);
        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        assert syncQueue.getLongestChain().size() == 10500;
    }

    @Test
    public void testWideLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 100, 100);
        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        assert syncQueue.getLongestChain().size() == 100;
    }

    @Test
    public void testGapedLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 100, 5);
        randomChain.removeIf(block -> block.getHeader().getNumber() == 15);
        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        assert syncQueue.getLongestChain().size() == 15; // 0 .. 14
    }

    @Test
    public void testFirstBlockGapedLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 100, 5);
        randomChain.removeIf(block -> block.getHeader().getNumber() == 1);
        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        assert syncQueue.getLongestChain().size() == 1; // 0
    }

    @Test(expected = AssertionError.class)
    public void testZeroBlockGapedLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 100, 5);
        randomChain.removeIf(block -> block.getHeader().getNumber() == 0);
        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        syncQueue.getLongestChain().size();
    }

    @Test
    public void testNoParentGapeLongestChain() {
        final List<Block> randomChain = TestUtils.getRandomAltChain(new byte[32], 0, 100, 5);

        // Moving #15 blocks to the end to be sure it didn't trick SyncQueue
        final Iterator<Block> it = randomChain.iterator();
        final List<Block> blockSaver = new ArrayList<>();
        while (it.hasNext()) {
            final Block block = it.next();
            if (block.getHeader().getNumber() == 15) {
                blockSaver.add(block);
                it.remove();
            }
        }
        randomChain.addAll(blockSaver);

        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain);
        // We still have linked chain
        assert syncQueue.getLongestChain().size() == 100;


        final List<Block> randomChain2 = TestUtils.getRandomAltChain(new byte[32], 0, 100, 5);

        final Iterator<Block> it2 = randomChain2.iterator();
        final List<Block> blockSaver2 = new ArrayList<>();
        while (it2.hasNext()) {
            final Block block = it2.next();
            if (block.getHeader().getNumber() == 15) {
                blockSaver2.add(block);
            }
        }

        // Removing #15 blocks
        for (int i = 0; i < 5; ++i) {
            randomChain.remove(randomChain.size() - 1);
        }
        // Adding wrong #15 blocks
        assert blockSaver2.size() == 5;
        randomChain.addAll(blockSaver2);

        assert new SyncQueueImpl(randomChain).getLongestChain().size() == 15; // 0 .. 14
    }

    public void test2Impl(final List<Block> mainChain, final List<Block> initChain, final Peer[] peers) {
        final List<Block> randomChain = TestUtils.getRandomChain(new byte[32], 0, 1024);
        final Block[] maxExportedBlock = new Block[] {randomChain.get(31)};
        final Map<ByteArrayWrapper, Block> exportedBlocks = new HashMap<>();
        for (final Block block : randomChain.subList(0, 32)) {
            exportedBlocks.put(new ByteArrayWrapper(block.getHash()), block);
        }

        final SyncQueueImpl syncQueue = new SyncQueueImpl(randomChain.subList(0, 32)) {
            @Override
            protected void exportNewBlock(final Block block) {
                exportedBlocks.put(new ByteArrayWrapper(block.getHash()), block);
                if (!exportedBlocks.containsKey(new ByteArrayWrapper(block.getParentHash()))) {
                    throw new RuntimeException("No parent for " + block);
                }
                if (block.getNumber() > maxExportedBlock[0].getNumber()) {
                    maxExportedBlock[0] = block;
                }
            }
        };


        final Random rnd = new Random();

        int i = 0;
        for (; i < 1000; i++) {
            final SyncQueueIfc.HeadersRequest headersRequest = syncQueue.requestHeaders(DEFAULT_REQUEST_LEN, 1, Integer.MAX_VALUE).iterator().next();
            final List<BlockHeader> headers = peers[rnd.nextInt(peers.length)].getHeaders(headersRequest.getStart(), headersRequest.getCount(), headersRequest.isReverse());
            syncQueue.addHeaders(createHeadersFromHeaders(headers, peer0));
            final SyncQueueIfc.BlocksRequest blocksRequest = syncQueue.requestBlocks(rnd.nextInt(128 + 1));
            final List<Block> blocks = peers[rnd.nextInt(peers.length)].getBlocks(blocksRequest.getBlockHeaders());
            syncQueue.addBlocks(blocks);
            if (maxExportedBlock[0].getNumber() == randomChain.get(randomChain.size() - 1).getNumber()) {
                break;
            }
        }

        if (i == 1000) throw new RuntimeException("Exported only till block: " + maxExportedBlock[0]);
    }

    private List<BlockHeaderWrapper> createHeadersFromHeaders(final List<BlockHeader> headers, final byte[] peer) {
        final List<BlockHeaderWrapper> ret = new ArrayList<>();
        for (final BlockHeader header : headers) {
            ret.add(new BlockHeaderWrapper(header, peer));
        }
        return ret;
    }

    private List<BlockHeaderWrapper> createHeadersFromBlocks(final List<Block> blocks, final byte[] peer) {
        final List<BlockHeaderWrapper> ret = new ArrayList<>();
        for (final Block block : blocks) {
            ret.add(new BlockHeaderWrapper(block.getHeader(), peer));
        }
        return ret;
    }

    private static class Peer {
        final Map<ByteArrayWrapper, Block> blocks = new HashMap<>();
        final List<Block> chain;
        final boolean returnGenesis;

        public Peer(final List<Block> chain) {
            this(chain, true);
        }

        public Peer(final List<Block> chain, final boolean returnGenesis) {
            this.returnGenesis = returnGenesis;
            this.chain = chain;
            for (final Block block : chain) {
                blocks.put(new ByteArrayWrapper(block.getHash()), block);
            }
        }

        public List<BlockHeader> getHeaders(final long startBlockNum, final int count, final boolean reverse) {
            return getHeaders(startBlockNum, count, reverse, 0);
        }

        public List<BlockHeader> getHeaders(final SyncQueueIfc.HeadersRequest req) {
            if (req.getHash() == null) {
                return getHeaders(req.getStart(), req.getCount(), req.isReverse(), req.getStep());
            } else {
                final Block block = blocks.get(new ByteArrayWrapper(req.getHash()));
                if (block == null) return Collections.emptyList();
                return getHeaders(block.getNumber(), req.getCount(), req.isReverse(), req.getStep());
            }
        }

        public List<BlockHeader> getRandomHeaders(final int count) {
            final List<BlockHeader> ret = new ArrayList<>();
            final Random rnd = new Random();
            for (int i = 0; i < count; i++) {
                ret.add(chain.get(rnd.nextInt(chain.size())).getHeader());
            }
            return ret;
        }


        public List<BlockHeader> getHeaders(final long startBlockNum, int count, final boolean reverse, int step) {
            step = step == 0 ? 1 : step;

            final List<BlockHeader> ret = new ArrayList<>();
            int i = (int) startBlockNum;
            for(; count-- > 0 && i >= (returnGenesis ? 0 : 1)  && i <= chain.get(chain.size() - 1).getNumber();
                i += reverse ? -step : step) {

                ret.add(chain.get(i).getHeader());

            }

//            step = step == 0 ? 1 : step;
//
//            if (reverse) {
//                startBlockNum = startBlockNum - (count - 1 ) * step;
//            }
//
//            startBlockNum = Math.max(startBlockNum, chain.get(0).getNumber());
//            startBlockNum = Math.min(startBlockNum, chain.get(chain.size() - 1).getNumber());
//            long endBlockNum = startBlockNum + (count - 1) * step;
//            endBlockNum = Math.max(endBlockNum, chain.get(0).getNumber());
//            endBlockNum = Math.min(endBlockNum, chain.get(chain.size() - 1).getNumber());
//            List<BlockHeader> ret = new ArrayList<>();
//            int startIdx = (int) (startBlockNum - chain.get(0).getNumber());
//            for (int i = startIdx; i < startIdx + (endBlockNum - startBlockNum + 1); i+=step) {
//                ret.add(chain.get(i).getHeader());
//            }
            return ret;
        }

        public List<Block> getBlocks(final Collection<BlockHeaderWrapper> hashes) {
            final List<Block> ret = new ArrayList<>();
            for (final BlockHeaderWrapper hash : hashes) {
                final Block block = blocks.get(new ByteArrayWrapper(hash.getHash()));
                if (block != null) ret.add(block);
            }
            return ret;
        }
    }
}
