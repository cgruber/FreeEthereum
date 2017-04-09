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

package org.ethereum.datasource;

import org.ethereum.crypto.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special optimization when the majority of get requests to the slower underlying source
 * are targeted to missing entries. The BloomFilter handles most of these requests.
 *
 * Created by Anton Nashatyrev on 16.01.2017.
 */
public class BloomedSource extends AbstractChainedSource<byte[], byte[], byte[], byte[]> {
    private final static Logger logger = LoggerFactory.getLogger("db");

    private final byte[] filterKey = HashUtil.sha3("filterKey".getBytes());
    private final int maxBloomSize;
    private QuotientFilter filter;
    private int hits = 0;
    private int misses = 0;
    private int falseMisses = 0;
    private boolean dirty = false;

    public BloomedSource(final Source<byte[], byte[]> source, final int maxBloomSize) {
        super(source);
        this.maxBloomSize = maxBloomSize;
        final byte[] filterBytes = source.get(filterKey);
        if (filterBytes != null) {
            if (filterBytes.length > 0) {
                filter = QuotientFilter.deserialize(filterBytes);
            } else {
                // filter size exceeded limit and is disabled forever
                filter = null;
            }
        } else {
            if (maxBloomSize > 0) {
                filter = QuotientFilter.create(50_000_000, 100_000);
            } else {
                // we can't re-enable filter later
                getSource().put(filterKey, new byte[0]);
            }
        }
//
//        new Thread() {
//            @Override
//            public void run() {
//                while(true) {
//                    synchronized (BloomedSource.this) {
//                        logger.debug("BloomedSource: hits: " + hits + ", misses: " + misses + ", false: " + falseMisses);
//                        hits = misses = falseMisses = 0;
//                    }
//
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {}
//                }
//            }
//        }.start();
    }

    public void startBlooming(final QuotientFilter filter) {
        this.filter = filter;
    }

    public void stopBlooming() {
        filter = null;
    }

    @Override
    public void put(final byte[] key, final byte[] val) {
        if (filter != null) {
            filter.insert(key);
            dirty = true;
            if (filter.getAllocatedBytes() > maxBloomSize) {
                logger.info("Bloom filter became too large (" + filter.getAllocatedBytes() + " exceeds max threshold " + maxBloomSize + ") and is now disabled forever.");
                getSource().put(filterKey, new byte[0]);
                filter = null;
                dirty = false;
            }
        }
        getSource().put(key, val);
    }

    @Override
    public byte[] get(final byte[] key) {
        if (filter == null) return getSource().get(key);

        if (!filter.maybeContains(key)) {
            hits++;
            return null;
        } else {
            final byte[] ret = getSource().get(key);
            if (ret == null) falseMisses++;
            else misses++;
            return ret;
        }
    }

    @Override
    public void delete(final byte[] key) {
        if (filter != null) filter.remove(key);
        getSource().delete(key);
    }

    @Override
    protected boolean flushImpl() {
        if (filter != null && dirty) {
            getSource().put(filterKey, filter.serialize());
            dirty = false;
            return true;
        } else {
            return false;
        }
    }
}
