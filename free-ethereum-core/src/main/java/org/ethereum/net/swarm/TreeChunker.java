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

import org.ethereum.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * From Go implementation:
 *
 * The distributed storage implemented in this package requires fix sized chunks of content
 * Chunker is the interface to a component that is responsible for disassembling and assembling larger data.
 * TreeChunker implements a Chunker based on a tree structure defined as follows:
 * 1 each node in the tree including the root and other branching nodes are stored as a chunk.
 * 2 branching nodes encode data contents that includes the size of the dataslice covered by its
 *   entire subtree under the node as well as the hash keys of all its children
 *   data_{i} := size(subtree_{i}) || key_{j} || key_{j+1} .... || key_{j+n-1}
 * 3 Leaf nodes encode an actual subslice of the input data.
 * 4 if data size is not more than maximum chunksize, the data is stored in a single chunk
 *   key = sha256(int64(size) + data)
 * 2 if data size is more than chunksize*Branches^l, but no more than
 *   chunksize*Branches^l length (except the last one).
 *   key = sha256(int64(size) + key(slice0) + key(slice1) + ...)
 *   Tree chunker is a concrete implementation of data chunking.
 *   This chunker works in a simple way, it builds a tree out of the document so that each node either
 *   represents a chunk of real data or a chunk of data representing an branching non-leaf node of the tree.
 *   In particular each such non-leaf chunk will represent is a concatenation of the hash of its respective children.
 *   This scheme simultaneously guarantees data integrity as well as self addressing. Abstract nodes are
 *   transparent since their represented size component is strictly greater than their maximum data size,
 *   since they encode a subtree.
 *   If all is well it is possible to implement this by simply composing readers so that no extra allocation or
 *   buffering is necessary for the data splitting and joining. This means that in principle there
 *   can be direct IO between : memory, file system, network socket (bzz peers storage request is
 *   read from the socket ). In practice there may be need for several stages of internal buffering.
 *   Unfortunately the hashing itself does use extra copies and allocation though since it does need it.
 */
public class TreeChunker implements Chunker {

    public static final MessageDigest DEFAULT_HASHER;
    private static final int DEFAULT_BRANCHES = 128;

    static {
        try {
            DEFAULT_HASHER = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    private int branches;
    private MessageDigest hasher;
    private int hashSize;
    private long chunkSize;
    public TreeChunker() {
        this(DEFAULT_BRANCHES, DEFAULT_HASHER);
    }

    public TreeChunker(final int branches, final MessageDigest hasher) {
        this.branches = branches;
        this.hasher = hasher;

        hashSize = hasher.getDigestLength();
        chunkSize = hashSize * branches;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    @NotNull
    @Override
    public Key split(@NotNull SectionReader sectionReader, @NotNull Collection<? extends Chunk> consumer) {
        final TreeSize ts = new TreeSize(sectionReader.getSize());
        return splitImpl(ts.depth, ts.treeSize / branches, sectionReader, (Collection<Chunk>) consumer);
    }

    private Key splitImpl(int depth, long treeSize, final SectionReader data, final Collection<Chunk> consumer) {
        final long size = data.getSize();
        final TreeChunk newChunk;

        while (depth > 0 && size < treeSize) {
            treeSize /= branches;
            depth--;
        }

        if (depth == 0) {
            newChunk = new TreeChunk((int) size); // safe to cast since leaf chunk size < 2Gb
            data.read(newChunk.getData(), newChunk.getDataOffset());
        } else {
            // intermediate chunk containing child nodes hashes
            final int branchCnt = (int) ((size + treeSize - 1) / treeSize);

            final HashesChunk hChunk = new HashesChunk(size);

            long pos = 0;
            long secSize;

            // TODO the loop can be parallelized
            for (int i = 0; i < branchCnt; i++) {
                // the last item can have shorter data
                if (size-pos < treeSize) {
                    secSize = size - pos;
                } else {
                    secSize = treeSize;
                }
                // take the section of the data corresponding encoded in the subTree
                final SectionReader subTreeData = new SlicedReader(data, pos, secSize);
                // the hash of that data
                final Key subTreeKey = splitImpl(depth - 1, treeSize / branches, subTreeData, consumer);

                hChunk.setKey(i, subTreeKey);

                pos += treeSize;
            }
            // now we got the hashes in the chunk, then hash the chunk
            newChunk = hChunk;
        }

        consumer.add(newChunk);
        // report hash of this chunk one level up (keys corresponds to the proper subslice of the parent chunk)x
        return newChunk.getKey();

    }

    @Override
    public SectionReader join(final ChunkStore chunkStore, final Key key) {
        return new LazyChunkReader(chunkStore, key);
    }

    @Override
    public long keySize() {
        return hashSize;
    }

//    @NotNull
//    @Override
//    public Key split(@NotNull SectionReader sectionReader, @NotNull Collection<? extends Chunk> consumer) {
//        return null;
//    }

    /**
     * A 'subReader'
     */
    public static class SlicedReader implements SectionReader {
        final SectionReader delegate;
        final long offset;
        final long len;

        public SlicedReader(final SectionReader delegate, final long offset, final long len) {
            this.delegate = delegate;
            this.offset = offset;
            this.len = len;
        }

        @Override
        public long seek(final long offset, final int whence) {
            return delegate.seek(this.offset + offset, whence);
        }

        @Override
        public int read(final byte[] dest, final int destOff) {
            return delegate.readAt(dest, destOff, offset);
        }

        @Override
        public int readAt(final byte[] dest, final int destOff, final long readerOffset) {
            return delegate.readAt(dest, destOff, offset + readerOffset);
        }

        @Override
        public long getSize() {
            return len;
        }
    }

    public class TreeChunk extends Chunk {
        private static final int DATA_OFFSET = 8;

        public TreeChunk(final int dataSize) {
            super(null, new byte[DATA_OFFSET + dataSize]);
            setSubtreeSize(dataSize);
        }

        public TreeChunk(final Chunk chunk) {
            super(chunk.getKey(), chunk.getData());
        }

        public long getSubtreeSize() {
            return ByteBuffer.wrap(getData()).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
        }

        public void setSubtreeSize(final long size) {
            ByteBuffer.wrap(getData()).order(ByteOrder.LITTLE_ENDIAN).putLong(0, size);
        }

        public int getDataOffset() {
            return DATA_OFFSET;
        }

        public Key getKey() {
            if (key == null) {
                key = new Key(hasher.digest(getData()));
            }
            return key;
        }

        @Override
        public String toString() {
            final String dataString = ByteUtil.toHexString(
                    Arrays.copyOfRange(getData(), getDataOffset(), getDataOffset() + 16)) + "...";
            return "TreeChunk[" + getSubtreeSize() + ", " + getKey() + ", " + dataString + "]";
        }
    }

    public class HashesChunk extends TreeChunk {

        public HashesChunk(final long subtreeSize) {
            super(branches * hashSize);
            setSubtreeSize(subtreeSize);
        }

        public HashesChunk(final Chunk chunk) {
            super(chunk);
        }

        public int getKeyCount() {
            return branches;
        }

        public Key getKey(final int idx) {
            final int off = getDataOffset() + idx * hashSize;
            return new Key(Arrays.copyOfRange(getData(), off, off + hashSize));
        }

        public void setKey(final int idx, final Key key) {
            final int off = getDataOffset() + idx * hashSize;
            System.arraycopy(key.getBytes(), 0, getData(), off, hashSize);
        }

        @Override
        public String toString() {
            final StringBuilder hashes = new StringBuilder("{");
            for (int i = 0; i < getKeyCount(); i++) {
                hashes.append(i == 0 ? "" : ", ").append(getKey(i));
            }
            hashes.append("}");
            return "HashesChunk[" + getSubtreeSize() + ", " + getKey() + ", " + hashes + "]";
        }
    }

    private class TreeSize {
        int depth;
        long treeSize;

        public TreeSize(final long dataSize) {
            treeSize = chunkSize;
            for (; treeSize < dataSize; treeSize *= branches) {
                depth++;
            }
        }
    }

    private class LazyChunkReader implements SectionReader {
        final long size;
        final Chunk root;
        final Key key;
        final ChunkStore chunkStore;

        public LazyChunkReader(final ChunkStore chunkStore, final Key key) {
            this.chunkStore = chunkStore;
            this.key = key;
            root = chunkStore.get(key);
            this.size = new TreeChunk(root).getSubtreeSize();
        }

        @Override
        public int readAt(final byte[] dest, final int destOff, final long readerOffset) {
            final int size = dest.length - destOff;
            final TreeSize ts = new TreeSize(this.size);
            return readImpl(dest, destOff, root, ts.treeSize, 0, readerOffset,
                    readerOffset + min(size, this.size - readerOffset));
        }

        private int readImpl(final byte[] dest, final int destOff, final Chunk chunk, final long chunkWidth, final long chunkStart,
                             final long readStart, final long readEnd) {
            final long chunkReadStart = max(readStart - chunkStart, 0);
            final long chunkReadEnd = min(chunkWidth, readEnd - chunkStart);

            int ret = 0;
            if (chunkWidth > chunkSize) {
                final long subChunkWidth = chunkWidth / branches;
                if (chunkReadStart >= chunkWidth || chunkReadEnd <= 0) {
                    throw new RuntimeException("Not expected.");
                }

                final int startSubChunk = (int) (chunkReadStart / subChunkWidth);
                final int lastSubChunk = (int) ((chunkReadEnd - 1) / subChunkWidth);

                // TODO the loop can be parallelized
                for (int i = startSubChunk; i <= lastSubChunk; i++) {
                    final HashesChunk hChunk = new HashesChunk(chunk);
                    final Chunk subChunk = chunkStore.get(hChunk.getKey(i));
                    ret += readImpl(dest, (int) (destOff + (i - startSubChunk) * subChunkWidth),
                            subChunk, subChunkWidth, chunkStart + i * subChunkWidth, readStart, readEnd);
                }
            } else {
                final TreeChunk dataChunk = new TreeChunk(chunk);
                ret = (int) (chunkReadEnd - chunkReadStart);
                System.arraycopy(dataChunk.getData(), (int) (dataChunk.getDataOffset() + chunkReadStart),
                        dest, destOff, ret);
            }
            return ret;
        }

        @Override
        public long seek(final long offset, final int whence) {
            throw new RuntimeException("Not implemented");
        }


        @Override
        public long getSize() {
            return size;
        }

        @Override
        public int read(final byte[] dest, final int destOff) {
            return readAt(dest, destOff, 0);
        }
    }
}
