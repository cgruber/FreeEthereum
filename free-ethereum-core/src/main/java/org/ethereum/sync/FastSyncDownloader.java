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

import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.core.BlockWrapper;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.validator.BlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
@Lazy
public class FastSyncDownloader extends BlockDownloader {
    private final static Logger logger = LoggerFactory.getLogger("sync");

    private final
    SyncPool syncPool;

    private final
    IndexedBlockStore blockStore;

    private int counter;
    private int maxCount;
    private long t;

    @Autowired
    public FastSyncDownloader(final BlockHeaderValidator headerValidator, SyncPool syncPool, IndexedBlockStore blockStore) {
        super(headerValidator);
        this.syncPool = syncPool;
        this.blockStore = blockStore;
    }

    public void startImporting(final byte[] fromHash, final int count) {
        final SyncQueueReverseImpl syncQueueReverse = new SyncQueueReverseImpl(fromHash);
        init(syncQueueReverse, syncPool);
        this.maxCount = count <= 0 ? Integer.MAX_VALUE : count;
    }

    @Override
    protected void pushBlocks(final List<BlockWrapper> blockWrappers) {
        if (!blockWrappers.isEmpty()) {

            for (final BlockWrapper blockWrapper : blockWrappers) {
                blockStore.saveBlock(blockWrapper.getBlock(), BigInteger.ZERO, true);
                counter++;
                if (counter >= maxCount) {
                    logger.info("All requested " + counter + " blocks are downloaded. (last " + blockWrapper.getBlock().getShortDescr() + ")");
                    stop();
                    break;
                }
            }

            final long c = System.currentTimeMillis();
            if (c - t > 5000) {
                t = c;
                logger.info("FastSync: downloaded " + counter + " blocks so far. Last: " + blockWrappers.get(0).getBlock().getShortDescr());
                blockStore.flush();
            }
        }
    }

    @Override
    protected void pushHeaders(final List<BlockHeaderWrapper> headers) {
    }

    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    // TODO: receipts loading here

    public int getDownloadedBlocksCount() {
        return counter;
    }

    @Override
    protected void finishDownload() {
        blockStore.flush();
    }
}
