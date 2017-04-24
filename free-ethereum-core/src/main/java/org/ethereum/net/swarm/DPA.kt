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

/**
 * Distributed Preimage Archive
 * Acts as a high-level API to the Swarm

 * From Go implementation:
 * DPA provides the client API entrypoints Store and Retrieve to store and retrieve
 * It can store anything that has a byte slice representation, so files or serialised objects etc.
 * Storage: DPA calls the Chunker to segment the input datastream of any size to a merkle hashed tree of chunks.
 * The key of the root block is returned to the client.
 * Retrieval: given the key of the root block, the DPA retrieves the block chunks and reconstructs the original
 * data and passes it back as a lazy reader. A lazy reader is a reader with on-demand delayed processing,
 * i.e. the chunks needed to reconstruct a large file are only fetched and processed if that particular part
 * of the document is actually read.
 * As the chunker produces chunks, DPA dispatches them to the chunk store for storage or retrieval.
 * The ChunkStore interface is implemented by :
 * - memStore: a memory cache
 * - dbStore: local disk/db store
 * - localStore: a combination (sequence of) memStore and dbStore
 * - netStore: dht storage

 * Created by Anton Nashatyrev on 18.06.2015.
 */
open class DPA(private val chunkStore: ChunkStore?) {
    // this is now the default and the only possible Chunker implementation
    private val chunker = TreeChunker()

    /**
     * Main entry point for document storage directly. Used by the
     * FS-aware API and httpaccess

     * @return key
     */
    open fun store(reader: SectionReader): Key {
        return chunker.split(reader, Util.ChunkConsumer(chunkStore))
    }

    /**
     * Main entry point for document retrieval directly. Used by the
     * FS-aware API and httpaccess
     * Chunk retrieval blocks on netStore requests with a timeout so reader will
     * report error if retrieval of chunks within requested range time out.

     * @return key
     */
    open fun retrieve(key: Key): SectionReader {
        return chunker.join(chunkStore!!, key)
    }
}
