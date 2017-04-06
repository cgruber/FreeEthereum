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
import org.ethereum.core.BlockWrapper;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.DataSourceArray;
import org.ethereum.db.DbFlushManager;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.net.server.Channel;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.validator.BlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Anton Nashatyrev on 27.10.2016.
 */
@Component
@Scope("prototype")
public class BlockBodiesDownloader extends BlockDownloader {
    private final static Logger logger = LoggerFactory.getLogger("sync");

    private final static byte[] EMPTY_BODY = new byte[]{-62, -64, -64};

    @Autowired
    private
    SyncPool syncPool;

    @Autowired
    private
    IndexedBlockStore blockStore;

    @Autowired @Qualifier("headerSource")
    private
    DataSourceArray<BlockHeader> headerStore;

    @Autowired
    private
    DbFlushManager dbFlushManager;

    private long t;

    private SyncQueueIfc syncQueue;
    private int curBlockIdx = 1;
    private BigInteger curTotalDiff;

    private Thread headersThread;
    private int downloadCnt = 0;

    @Autowired
    public BlockBodiesDownloader(final BlockHeaderValidator headerValidator) {
        super(headerValidator);
    }

    public void startImporting() {
        final Block genesis = blockStore.getChainBlockByNumber(0);
        syncQueue = new SyncQueueImpl(Collections.singletonList(genesis));
        curTotalDiff = genesis.getDifficultyBI();

        headersThread = new Thread("FastsyncHeadersFetchThread") {
            @Override
            public void run() {
                headerLoop();
            }
        };
        headersThread.start();

        setHeadersDownload(false);

        init(syncQueue, syncPool);
    }

    private void headerLoop() {
        while (curBlockIdx < headerStore.size() && !Thread.currentThread().isInterrupted()) {
            final List<BlockHeaderWrapper> wrappers = new ArrayList<>();
            final List<BlockHeader> emptyBodyHeaders = new ArrayList<>();
            for (int i = 0; i < 10000 - syncQueue.getHeadersCount() && curBlockIdx < headerStore.size(); i++) {
                final BlockHeader header = headerStore.get(curBlockIdx++);
                wrappers.add(new BlockHeaderWrapper(header, new byte[0]));

                // Skip bodies download for blocks with empty body
                boolean emptyBody = FastByteComparisons.equal(header.getTxTrieRoot(), HashUtil.EMPTY_TRIE_HASH);
                emptyBody &= FastByteComparisons.equal(header.getUnclesHash(), HashUtil.EMPTY_LIST_HASH);
                if (emptyBody) emptyBodyHeaders.add(header);
            }

            synchronized (this) {
                syncQueue.addHeaders(wrappers);
                if (!emptyBodyHeaders.isEmpty()) {
                    addEmptyBodyBlocks(emptyBodyHeaders);
                }
            }

            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                break;
            }
        }
        headersDownloadComplete = true;
    }

    private void addEmptyBodyBlocks(final List<BlockHeader> blockHeaders) {
        logger.debug("Adding {} empty body blocks to sync queue: {} ... {}", blockHeaders.size(),
                blockHeaders.get(0).getShortDescr(), blockHeaders.get(blockHeaders.size() - 1).getShortDescr());

        final List<Block> finishedBlocks = new ArrayList<>();
        for (final BlockHeader header : blockHeaders) {
            final Block block = new Block.Builder()
                    .withHeader(header)
                    .withBody(EMPTY_BODY)
                    .create();
            finishedBlocks.add(block);
        }

        final List<Block> startTrimmedBlocks = syncQueue.addBlocks(finishedBlocks);
        final List<BlockWrapper> trimmedBlockWrappers = new ArrayList<>();
        for (final Block b : startTrimmedBlocks) {
            trimmedBlockWrappers.add(new BlockWrapper(b, null));
        }

        pushBlocks(trimmedBlockWrappers);
    }

    @Override
    protected void pushBlocks(final List<BlockWrapper> blockWrappers) {
        if (!blockWrappers.isEmpty()) {

            for (final BlockWrapper blockWrapper : blockWrappers) {
                curTotalDiff = curTotalDiff.add(blockWrapper.getBlock().getDifficultyBI());
                blockStore.saveBlock(blockWrapper.getBlock(), curTotalDiff, true);
                downloadCnt++;
            }
            dbFlushManager.commit();

            final long c = System.currentTimeMillis();
            if (c - t > 5000) {
                t = c;
                logger.info("FastSync: downloaded blocks. Last: " + blockWrappers.get(blockWrappers.size() - 1).getBlock().getShortDescr());
            }
        }
    }

    /**
     * Download could block chain synchronization occupying all peers
     * Prevents this by leaving one peer without work
     * Fallbacks to any peer when low number of active peers available
     */
    @Override
    Channel getAnyPeer() {
        return syncPool.getActivePeersCount() > 2 ? syncPool.getNotLastIdle() : syncPool.getAnyIdle();
    }

    @Override
    protected void pushHeaders(final List<BlockHeaderWrapper> headers) {
    }

    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    public int getDownloadedCount() {
        return downloadCnt;
    }

    @Override
    public void stop() {
        headersThread.interrupt();
        super.stop();
    }

    @Override
    protected void finishDownload() {
        stop();
    }
}
