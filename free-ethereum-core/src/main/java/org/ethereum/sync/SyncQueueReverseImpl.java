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
import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.util.ByteArrayMap;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.MinMaxMap;

import java.util.*;

/**
 * Created by Anton Nashatyrev on 27.10.2016.
 */
public class SyncQueueReverseImpl implements SyncQueueIfc {

    private final byte[] curHeaderHash;
//    List<BlockHeaderWrapper> headers = new ArrayList<>();
private final MinMaxMap<BlockHeaderWrapper> headers = new MinMaxMap<>();
    private final ByteArrayMap<Block> blocks = new ByteArrayMap<>();
    private long minValidated = -1;
    private boolean headersOnly;

    public SyncQueueReverseImpl(final byte[] startHash) {
        this.curHeaderHash = startHash;
    }

    public SyncQueueReverseImpl(final byte[] startHash, final boolean headersOnly) {
        this.curHeaderHash = startHash;
        this.headersOnly = headersOnly;
    }

    @Override
    public synchronized List<HeadersRequest> requestHeaders(final int maxSize, int maxRequests, final int maxTotalHeaders) {
        final List<HeadersRequest> ret = new ArrayList<>();
        if (minValidated < 0) {
            ret.add(new SyncQueueImpl.HeadersRequestImpl(curHeaderHash, maxSize, true, maxSize - 1));
        } else if (minValidated == 0) {
            // genesis reached
            return null;
        } else {
            if (minValidated - headers.getMin() < maxSize * maxSize && minValidated > maxSize) {
                ret.add(new SyncQueueImpl.HeadersRequestImpl(
                        headers.get(headers.getMin()).getHash(), maxSize, true, maxSize - 1));
                maxRequests--;
            }

            final Set<Map.Entry<Long, BlockHeaderWrapper>> entries =
                    headers.descendingMap().subMap(minValidated, true, headers.getMin(), true).entrySet();
            final Iterator<Map.Entry<Long, BlockHeaderWrapper>> it = entries.iterator();
            BlockHeaderWrapper prevEntry = it.next().getValue();
            while(maxRequests > 0 && it.hasNext()) {
                final BlockHeaderWrapper entry = it.next().getValue();
                if (prevEntry.getNumber() - entry.getNumber() > 1) {
                    ret.add(new SyncQueueImpl.HeadersRequestImpl(prevEntry.getHash(), maxSize, true));
                    maxRequests--;
                }
                prevEntry = entry;
            }
            if (maxRequests > 0) {
                ret.add(new SyncQueueImpl.HeadersRequestImpl(prevEntry.getHash(), maxSize, true));
            }
        }

        return ret;
    }

    @Override
    public synchronized List<BlockHeaderWrapper> addHeaders(final Collection<BlockHeaderWrapper> newHeaders) {
        if (minValidated < 0) {
            // need to fetch initial header
            for (final BlockHeaderWrapper header : newHeaders) {
                if (FastByteComparisons.equal(curHeaderHash, header.getHash())) {
                    minValidated = header.getNumber();
                    headers.put(header.getNumber(), header);
                }
            }
        }

        // start header not found or we are already done
        if (minValidated <= 0) return Collections.emptyList();

        for (final BlockHeaderWrapper header : newHeaders) {
            if (header.getNumber() < minValidated) {
                headers.put(header.getNumber(), header);
            }
        }


        if (minValidated == -1) minValidated = headers.getMax();
        for (; minValidated >= headers.getMin() ; minValidated--) {
            final BlockHeaderWrapper header = headers.get(minValidated);
            final BlockHeaderWrapper parent = headers.get(minValidated - 1);
            if (parent == null) {
                // Some peers doesn't return 0 block header
                if (minValidated == 1) minValidated = 0;
                break;
            }
            if (!FastByteComparisons.equal(header.getHeader().getParentHash(), parent.getHash())) {
                // chain is broken here (unlikely) - refetch the rest
                headers.clearAllBefore(header.getNumber());
                break;
            }
        }
        if (headersOnly) {
            final List<BlockHeaderWrapper> ret = new ArrayList<>();
            for (long i = headers.getMax(); i > minValidated; i--) {
                ret.add(headers.remove(i));
            }
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public synchronized BlocksRequest requestBlocks(int maxSize) {
        final List<BlockHeaderWrapper> reqHeaders = new ArrayList<>();
        for (final BlockHeaderWrapper header : headers.descendingMap().values()) {
            if (maxSize == 0) break;
            if (blocks.get(header.getHash()) == null) {
                reqHeaders.add(header);
                maxSize--;
            }
        }
        return new SyncQueueImpl.BlocksRequestImpl(reqHeaders);
    }

    @Override
    public synchronized List<Block> addBlocks(final Collection<Block> newBlocks) {
        for (final Block block : newBlocks) {
            blocks.put(block.getHash(), block);
        }
        final List<Block> ret = new ArrayList<>();
        for (long i = headers.getMax(); i > minValidated; i--) {
            final Block block = blocks.get(headers.get(i).getHash());
            if (block == null) break;
            ret.add(block);
            blocks.remove(headers.get(i).getHash());
            headers.remove(i);
        }
        return ret;
    }

    @Override
    public synchronized int getHeadersCount() {
        return headers.size();
    }
}
