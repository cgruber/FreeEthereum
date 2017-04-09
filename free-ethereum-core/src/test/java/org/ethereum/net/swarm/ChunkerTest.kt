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

package org.ethereum.net.swarm

import org.junit.Test
import java.util.*

class ChunkerTest {

    @Test
    fun simpleTest() {
        val arr = ByteArray(200)
        Random(0).nextBytes(arr)
        val r = Util.ArrayReader(arr)
        val tc = TreeChunker(4, TreeChunker.DEFAULT_HASHER)
        val l = ArrayList<Chunk>()
        val root = tc.split(r, l)

        val chunkStore = SimpleChunkStore()
        for (chunk in l) {
            chunkStore.put(chunk)
        }

        val reader = tc.join(chunkStore, root)
        val size = reader.size
        val off = 30
        val arrOut = ByteArray(size.toInt())
        val readLen = reader.read(arrOut, off)

        println("Read len: " + readLen)
        var i = 0
        while (i < arr.size && off + i < arrOut.size) {
            if (arr[i] != arrOut[off + i]) throw RuntimeException("Not equal at " + i)
            i++
        }
        println("Done.")
    }

    class SimpleChunkStore : ChunkStore {
        internal val map: MutableMap<Key, ByteArray> = HashMap()

        override fun put(chunk: Chunk) {
            map.put(chunk.getKey(), chunk.data)
        }

        override fun get(key: Key): Chunk? {
            val bytes = map[key]
            return if (bytes == null) null else Chunk(key, bytes)
        }
    }
}
