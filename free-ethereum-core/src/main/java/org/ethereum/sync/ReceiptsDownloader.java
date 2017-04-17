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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.DataSourceArray;
import org.ethereum.db.DbFlushManager;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.TransactionStore;
import org.ethereum.net.eth.handler.Eth63;
import org.ethereum.net.server.Channel;
import org.ethereum.util.FastByteComparisons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Component
@Scope("prototype")
class ReceiptsDownloader {
    private final static Logger logger = LoggerFactory.getLogger("sync");
    private final long toBlock;
    private final Set<Long> completedBlocks = new HashSet<>();
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    long t;
    @Autowired
    private
    SyncPool syncPool;
    @Autowired
    private
    IndexedBlockStore blockStore;
    @Autowired
    private
    DbFlushManager dbFlushManager;
    @Autowired
    private
    TransactionStore txStore;
    @Autowired @Qualifier("headerSource")
    private
    DataSourceArray<BlockHeader> headerStore;
    private long fromBlock;
    private int cnt;
    private Thread retrieveThread;

    public ReceiptsDownloader(final long fromBlock, final long toBlock) {
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
    }

    public void startImporting() {
        retrieveThread = new Thread("FastsyncReceiptsFetchThread") {
            @Override
            public void run() {
                retrieveLoop();
            }
        };
        retrieveThread.start();
    }

    private List<List<byte[]>> getToDownload(final int maxAskSize, final int maxAsks) {
        final List<byte[]> toDownload = getToDownload(maxAskSize * maxAsks);
        final List<List<byte[]>> ret = new ArrayList<>();
        for (int i = 0; i < toDownload.size(); i += maxAskSize) {
            ret.add(toDownload.subList(i, Math.min(toDownload.size(), i + maxAskSize)));
        }
        return ret;
    }

    private synchronized List<byte[]> getToDownload(int maxSize) {
        final List<byte[]> ret = new ArrayList<>();
        for (long i = fromBlock; i < toBlock && maxSize > 0; i++) {
            if (!completedBlocks.contains(i)) {
                final BlockHeader header = headerStore.get((int) i);

                // Skipping download for blocks with no transactions
                if (FastByteComparisons.equal(header.getReceiptsRoot(), HashUtil.INSTANCE.getEMPTY_TRIE_HASH())) {
                    finalizeBlock(header.getNumber());
                    continue;
                }

                ret.add(header.getHash());
                maxSize--;
            }
        }
        return ret;
    }

    private void processDownloaded(final byte[] blockHash, final List<TransactionReceipt> receipts) {
        final Block block = blockStore.getBlockByHash(blockHash);
        if (block.getNumber() >= fromBlock && validate(block, receipts) && !completedBlocks.contains(block.getNumber())) {
            for (int i = 0; i < receipts.size(); i++) {
                final TransactionReceipt receipt = receipts.get(i);
                final TransactionInfo txInfo = new TransactionInfo(receipt, block.getHash(), i);
                txInfo.setTransaction(block.getTransactionsList().get(i));
                txStore.put(txInfo);
            }

            finalizeBlock(block.getNumber());
        }
    }

    private void finalizeBlock(final Long blockNumber) {
        synchronized (this) {
            completedBlocks.add(blockNumber);

            while (fromBlock < toBlock && completedBlocks.remove(fromBlock)) fromBlock++;

            if (fromBlock >= toBlock) finishDownload();

            cnt++;
            if (cnt % 1000 == 0) logger.info("FastSync: downloaded receipts for " + cnt + " blocks.");
        }
        dbFlushManager.commit();
    }

    private boolean validate(final Block block, final List<TransactionReceipt> receipts) {
        final byte[] receiptsRoot = BlockchainImpl.calcReceiptsTrie(receipts);
        return FastByteComparisons.equal(receiptsRoot, block.getReceiptsRoot());
    }

    private void retrieveLoop() {
        List<List<byte[]>> toDownload = Collections.emptyList();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (toDownload.isEmpty()) {
                    toDownload = getToDownload(100, 20);
                }

                final Channel idle = getAnyPeer();
                if (idle != null) {
                    final List<byte[]> list = toDownload.remove(0);
                    final ListenableFuture<List<List<TransactionReceipt>>> future =
                            ((Eth63) idle.getEthHandler()).requestReceipts(list);
                    if (future != null) {
                        Futures.addCallback(future, new FutureCallback<List<List<TransactionReceipt>>>() {
                            @Override
                            public void onSuccess(final List<List<TransactionReceipt>> result) {
                                for (int i = 0; i < result.size(); i++) {
                                    processDownloaded(list.get(i), result.get(i));
                                }
                            }
                            @Override
                            public void onFailure(final Throwable t) {
                            }
                        });
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            } catch (final Exception e) {
                logger.warn("Unexpected during receipts downloading", e);
            }
        }
    }

    /**
     * Download could block chain synchronization occupying all peers
     * Prevents this by leaving one peer without work
     * Fallbacks to any peer when low number of active peers available
     */
    private Channel getAnyPeer() {
        return syncPool.getActivePeersCount() > 2 ? syncPool.getNotLastIdle() : syncPool.getAnyIdle();
    }

    public int getDownloadedBlocksCount() {
        return cnt;
    }

    private void stop() {
        retrieveThread.interrupt();
        stopLatch.countDown();
    }

    public void waitForStop() {
        try {
            stopLatch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void finishDownload() {
        stop();
    }
}
