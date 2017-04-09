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

import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.core.Blockchain;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.util.ByteArrayMap;
import org.ethereum.util.Functional;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

import static java.lang.Math.min;

/**
 * Created by Anton Nashatyrev on 27.05.2016.
 */
public class SyncQueueImpl implements SyncQueueIfc {
    private static final int MAX_CHAIN_LEN = 192;
    private final Map<Long, Map<ByteArrayWrapper, HeaderElement>> headers = new HashMap<>();
    private final Random rnd = new Random(); // ;)
    private long minNum = Integer.MAX_VALUE;
    private long maxNum = 0;
    private long darkZoneNum = 0;
    private Long endBlockNumber = null;

    public SyncQueueImpl(final List<Block> initBlocks) {
        init(initBlocks);
    }

    public SyncQueueImpl(final Blockchain bc) {
        final Block bestBlock = bc.getBestBlock();
        long start = bestBlock.getNumber() - MAX_CHAIN_LEN;
        start = start < 0 ? 0 : start;
        final List<Block> initBlocks = new ArrayList<>();
        for (long i = start; i <= bestBlock.getNumber(); i++) {
            initBlocks.add(bc.getBlockByNumber(i));
        }
        init(initBlocks);
    }

    /**
     * Init with blockchain and download until endBlockNumber (included)
     * @param bc                Blockchain
     * @param endBlockNumber    last block to download
     */
    public SyncQueueImpl(final Blockchain bc, final Long endBlockNumber) {
        this(bc);
        this.endBlockNumber = endBlockNumber;
    }

    private void init(final List<Block> initBlocks) {
        if (initBlocks.size() < MAX_CHAIN_LEN && initBlocks.get(0).getNumber() != 0) {
            throw new RuntimeException("Queue should be initialized with a chain of at least " + MAX_CHAIN_LEN + " size or with the first genesis block");
        }
        for (final Block block : initBlocks) {
            addHeaderPriv(new BlockHeaderWrapper(block.getHeader(), null));
            addBlock(block).exported = true;
        }
        darkZoneNum = initBlocks.get(0).getNumber();
    }

    private void putGenHeaders(final long num, final Map<ByteArrayWrapper, HeaderElement> genHeaders) {
        minNum = min(minNum, num);
        maxNum = Math.max(maxNum, num);
        headers.put(num, genHeaders);
    }

    List<HeaderElement> getLongestChain() {
        final Map<ByteArrayWrapper, HeaderElement> lastValidatedGen = headers.get(darkZoneNum);
        assert lastValidatedGen.size() == 1;
        final HeaderElement lastHeader = lastValidatedGen.values().iterator().next();

        Map<byte[], HeaderElement> chainedParents = new ByteArrayMap<>();
        chainedParents.put(lastHeader.header.getHash(), lastHeader);

        for(long curNum = darkZoneNum + 1; ; curNum++) {
            // keep track of blocks chained to lastHeader until no children
            final Map<byte[], HeaderElement> chainedBlocks = new ByteArrayMap<>();
            final Map<ByteArrayWrapper, HeaderElement> curLevel = headers.get(curNum);
            if (curLevel == null) break;
            for (final HeaderElement element : curLevel.values()) {
                if (chainedParents.containsKey(element.header.getHeader().getParentHash())) {
                    chainedBlocks.put(element.header.getHash(), element);
                }
            }
            if (chainedBlocks.isEmpty()) break;
            chainedParents = chainedBlocks;
        }

        // reconstruct the chain back from the last block in the longest path
        final List<HeaderElement> ret = new ArrayList<>();
        for (HeaderElement el = chainedParents.values().iterator().next(); el != lastHeader.getParent(); el = el.getParent()) {
            ret.add(0, el);
        }
        return ret;
    }

    private boolean hasGaps() {
        final List<HeaderElement> longestChain = getLongestChain();
        return longestChain.get(longestChain.size() - 1).header.getNumber() < maxNum;
    }

    private void trimChain() {
        final List<HeaderElement> longestChain = getLongestChain();
        if (longestChain.size() > MAX_CHAIN_LEN) {
            final long newTrimNum = getLongestChain().get(longestChain.size() - MAX_CHAIN_LEN).header.getNumber();
            for (int i = 0; darkZoneNum < newTrimNum; darkZoneNum++, i++) {
                final ByteArrayWrapper wHash = new ByteArrayWrapper(longestChain.get(i).header.getHash());
                putGenHeaders(darkZoneNum, Collections.singletonMap(wHash, longestChain.get(i)));
            }
            darkZoneNum--;
        }
    }

    private void trimExported() {
        for (; minNum < darkZoneNum; minNum++) {
            final Map<ByteArrayWrapper, HeaderElement> genHeaders = headers.get(minNum);
            assert genHeaders.size() == 1;
            final HeaderElement headerElement = genHeaders.values().iterator().next();
            if (headerElement.exported) {
                headers.remove(minNum);
            } else {
                break;
            }
        }
    }

    private boolean addHeader(final BlockHeaderWrapper header) {
        final long num = header.getNumber();
        if (num <= darkZoneNum || num > maxNum + MAX_CHAIN_LEN * 128) {
            // dropping too distant headers
            return false;
        }
        return addHeaderPriv(header);
    }

    private boolean addHeaderPriv(final BlockHeaderWrapper header) {
        final long num = header.getNumber();
        Map<ByteArrayWrapper, HeaderElement> genHeaders = headers.get(num);
        if (genHeaders == null) {
            genHeaders = new HashMap<>();
            putGenHeaders(num, genHeaders);
        }
        final ByteArrayWrapper wHash = new ByteArrayWrapper(header.getHash());
        HeaderElement headerElement = genHeaders.get(wHash);
        if (headerElement != null) return false;

        headerElement = new HeaderElement(header);
        genHeaders.put(wHash, headerElement);

        return true;
    }

    @Override
    public synchronized List<HeadersRequest> requestHeaders(final int maxSize, final int maxRequests, final int maxTotalHeaders) {
        return requestHeadersImpl(maxSize, maxRequests, maxTotalHeaders);
    }

    private List<HeadersRequest> requestHeadersImpl(final int count, final int maxRequests, final int maxTotHeaderCount) {
        final List<HeadersRequest> ret = new ArrayList<>();

        long startNumber;
        if (hasGaps()) {
            final List<HeaderElement> longestChain = getLongestChain();
            startNumber = longestChain.get(longestChain.size() - 1).header.getNumber();
            final boolean reverse = rnd.nextBoolean();
            ret.add(new HeadersRequestImpl(startNumber, MAX_CHAIN_LEN, reverse));
            startNumber += reverse ? 1 : MAX_CHAIN_LEN;
//            if (maxNum - startNumber > 2000) return ret;
        } else {
            startNumber = maxNum + 1;
        }

        while (ret.size() <= maxRequests && getHeadersCount() <= maxTotHeaderCount) {
            final HeadersRequestImpl nextReq = getNextReq(startNumber, count);
            if (nextReq.getEnd() > minNum + maxTotHeaderCount) break;
            ret.add(nextReq);
            startNumber = nextReq.getEnd();
        }

        return ret;
    }

    private HeadersRequestImpl getNextReq(long startFrom, int maxCount) {
        while(headers.containsKey(startFrom)) startFrom++;
        if (endBlockNumber != null && maxCount > endBlockNumber - startFrom + 1) {
            maxCount = (int) (endBlockNumber - startFrom + 1);
        }
        return new HeadersRequestImpl(startFrom, maxCount, false);
    }

    @Override
    public synchronized List<BlockHeaderWrapper> addHeaders(final Collection<BlockHeaderWrapper> headers) {
        for (final BlockHeaderWrapper header : headers) {
            addHeader(header);
        }
        trimChain();
        return null;
    }

    @Override
    public synchronized int getHeadersCount() {
        return (int) (maxNum - minNum);
    }

    @Override
    public synchronized BlocksRequest requestBlocks(final int maxSize) {
        final BlocksRequest ret = new BlocksRequestImpl();

        outer:
        for (long i = minNum; i <= maxNum; i++) {
            final Map<ByteArrayWrapper, HeaderElement> gen = headers.get(i);
            if (gen != null) {
                for (final HeaderElement element : gen.values()) {
                    if (element.block == null) {
                        ret.getBlockHeaders().add(element.header);
                        if (ret.getBlockHeaders().size() >= maxSize) break outer;
                    }
                }
            }
        }
        return ret;
    }

    private HeaderElement findHeaderElement(final BlockHeader bh) {
        final Map<ByteArrayWrapper, HeaderElement> genHeaders = headers.get(bh.getNumber());
        if (genHeaders == null) return null;
        return genHeaders.get(new ByteArrayWrapper(bh.getHash()));
    }

    private HeaderElement addBlock(final Block block) {
        final HeaderElement headerElement = findHeaderElement(block.getHeader());
        if (headerElement != null) {
            headerElement.block = block;
        }
        return headerElement;
    }

    @Override
    public synchronized List<Block> addBlocks(final Collection<Block> blocks) {
        for (final Block block : blocks) {
            addBlock(block);
        }
        return exportBlocks();
    }

    private List<Block> exportBlocks() {
        final List<Block> ret = new ArrayList<>();
        for (long i = minNum; i <= maxNum; i++) {
            final Map<ByteArrayWrapper, HeaderElement> gen = headers.get(i);
            if (gen == null) break;

            boolean hasAny = false;
            for (final HeaderElement element : gen.values()) {
                final HeaderElement parent = element.getParent();
                if (element.block != null && (i == minNum || parent != null && parent.exported)) {
                    if (!element.exported) {
                        exportNewBlock(element.block);
                        ret.add(element.block);
                        element.exported = true;
                    }
                    hasAny = true;
                }
            }
            if (!hasAny) break;
        }
        trimExported();
        return ret;
    }

    void exportNewBlock(final Block block) {

    }

    public synchronized List<Block> pollBlocks() {
        return null;
    }

    interface Visitor<T> {
        T visit(HeaderElement el, List<T> childrenRes);
    }

    static class HeadersRequestImpl implements HeadersRequest {
        private final int count;
        private final boolean reverse;
        private long start;
        private byte[] hash;
        private int step = 0;

        public HeadersRequestImpl(final long start, final int count, final boolean reverse) {
            this.start = start;
            this.count = count;
            this.reverse = reverse;
        }

        public HeadersRequestImpl(final byte[] hash, final int count, final boolean reverse) {
            this.hash = hash;
            this.count = count;
            this.reverse = reverse;
        }

        public HeadersRequestImpl(final byte[] hash, final int count, final boolean reverse, final int step) {
            this.hash = hash;
            this.count = count;
            this.reverse = reverse;
            this.step = step;
        }

        @Override
        public List<HeadersRequest> split(final int maxCount) {
            if (this.hash != null) return Collections.singletonList(this);
            final List<HeadersRequest> ret = new ArrayList<>();
            int remaining = count;
            while (remaining > 0) {
                final int reqSize = min(maxCount, remaining);
                ret.add(new HeadersRequestImpl(start, reqSize, reverse));
                remaining -= reqSize;
                start = reverse ? start - reqSize : start + reqSize;
            }
            return ret;
        }

        @Override
        public String toString() {
            return "HeadersRequest{" +
                    (hash == null ? "start=" + getStart() : "hash=" + Hex.toHexString(hash).substring(0, 8)) +
                    ", count=" + getCount() +
                    ", reverse=" + isReverse() +
                    ", step=" + getStep() +
                    '}';
        }

        @Override
        public long getStart() {
            return start;
        }

        public long getEnd() {
            return getStart() + getCount();
        }

        @Override
        public byte[] getHash() {
            return hash;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public boolean isReverse() {
            return reverse;
        }

        @Override
        public int getStep() {
            return step;
        }
    }

    static class BlocksRequestImpl implements BlocksRequest {
        private List<BlockHeaderWrapper> blockHeaders = new ArrayList<>();

        public BlocksRequestImpl() {
        }

        public BlocksRequestImpl(final List<BlockHeaderWrapper> blockHeaders) {
            this.blockHeaders = blockHeaders;
        }

        @Override
        public List<BlocksRequest> split(int count) {
            final List<BlocksRequest> ret = new ArrayList<>();
            int start = 0;
            while (start < getBlockHeaders().size()) {
                count = min(getBlockHeaders().size() - start, count);
                ret.add(new BlocksRequestImpl(getBlockHeaders().subList(start, start + count)));
                start += count;
            }
            return ret;
        }

        @Override
        public List<BlockHeaderWrapper> getBlockHeaders() {
            return blockHeaders;
        }
    }

    class HeaderElement {
        final BlockHeaderWrapper header;
        Block block;
        boolean exported;

        public HeaderElement(final BlockHeaderWrapper header) {
            this.header = header;
        }

        public HeaderElement getParent() {
            final Map<ByteArrayWrapper, HeaderElement> genHeaders = headers.get(header.getNumber() - 1);
            if (genHeaders == null) return null;
            return genHeaders.get(new ByteArrayWrapper(header.getHeader().getParentHash()));
        }

        public List<HeaderElement> getChildren() {
            final List<HeaderElement> ret = new ArrayList<>();
            final Map<ByteArrayWrapper, HeaderElement> childGenHeaders = headers.get(header.getNumber() + 1);
            if (childGenHeaders != null) {
                for (final HeaderElement child : childGenHeaders.values()) {
                    if (Arrays.equals(child.header.getHeader().getParentHash(), header.getHash())) {
                        ret.add(child);
                    }
                }
            }
            return ret;
        }
    }

    class ChildVisitor<T> {
        boolean downUp = true;
        private Visitor<T> handler;

        public ChildVisitor(final Functional.Function<HeaderElement, List<T>> handler) {
//            this.handler = handler;
        }

        public T traverse(final HeaderElement el) {
            final List<T> childrenRet = new ArrayList<>();
            for (final HeaderElement child : el.getChildren()) {
                final T res = traverse(child);
                childrenRet.add(res);
            }
            return handler.visit(el, childrenRet);
        }
    }
}
