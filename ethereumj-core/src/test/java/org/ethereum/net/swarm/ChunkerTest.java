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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Admin on 19.06.2015.
 */
public class ChunkerTest {

    @Test
    public void simpleTest() {
        final byte[] arr = new byte[200];
        new Random(0).nextBytes(arr);
        final Util.ArrayReader r = new Util.ArrayReader(arr);
        final TreeChunker tc = new TreeChunker(4, TreeChunker.DEFAULT_HASHER);
        final ArrayList<Chunk> l = new ArrayList<>();
        final Key root = tc.split(r, l);

        final SimpleChunkStore chunkStore = new SimpleChunkStore();
        for (final Chunk chunk : l) {
            chunkStore.put(chunk);
        }

        final SectionReader reader = tc.join(chunkStore, root);
        final long size = reader.getSize();
        final int off = 30;
        final byte[] arrOut = new byte[(int) size];
        final int readLen = reader.read(arrOut, off);

        System.out.println("Read len: " + readLen);
        for (int i = 0; i < arr.length && off + i < arrOut.length; i++) {
            if (arr[i] != arrOut[off+ i]) throw new RuntimeException("Not equal at " + i);
        }
        System.out.println("Done.");
    }

    public static class SimpleChunkStore implements ChunkStore {
        final Map<Key, byte[]> map = new HashMap<>();

        @Override
        public void put(final Chunk chunk) {
            map.put(chunk.getKey(), chunk.getData());
        }

        @Override
        public Chunk get(final Key key) {
            final byte[] bytes = map.get(key);
            return bytes == null ? null : new Chunk(key, bytes);
        }
    }
}
