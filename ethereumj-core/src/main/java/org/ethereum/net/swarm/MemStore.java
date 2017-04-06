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

package org.ethereum.net.swarm;

import org.apache.commons.collections4.map.LRUMap;

import java.util.Collections;
import java.util.Map;

/**
 * Limited capacity memory storage. Last recently used chunks are purged when
 * max memory threshold is reached.
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
public class MemStore implements ChunkStore {
    public final Statter statCurChunks = Statter.create("net.swarm.memstore.curChunkCnt");
    private final Statter statCurSize = Statter.create("net.swarm.memstore.curSize");
    private long maxSizeBytes = 10 * 1000000;
    private long curSizeBytes = 0;
    // TODO: SoftReference for Chunks?
    public final Map<Key, Chunk> store = Collections.synchronizedMap(new LRUMap<Key, Chunk>(10000) {
        @Override
        protected boolean removeLRU(final LinkEntry<Key, Chunk> entry) {
            curSizeBytes -= entry.getValue().getData().length;
            final boolean ret = super.removeLRU(entry);
            statCurSize.add(curSizeBytes);
            statCurChunks.add(size());
            return ret;
        }

        @Override
        public Chunk put(final Key key, final Chunk value) {
            curSizeBytes += value.getData().length;
            final Chunk ret = super.put(key, value);
            statCurSize.add(curSizeBytes);
            statCurChunks.add(size());
            return ret;
        }

        @Override
        public boolean isFull() {
            return curSizeBytes >= maxSizeBytes;
        }
    });

    public MemStore() {
    }

    public MemStore(final long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public void put(final Chunk chunk) {
        store.put(chunk.getKey(), chunk);
    }

    @Override
    public Chunk get(final Key key) {
        return store.get(key);
    }

    public void clear() {
        store.clear();
    }
}
