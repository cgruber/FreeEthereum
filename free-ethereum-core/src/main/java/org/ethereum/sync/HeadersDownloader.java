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

import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockHeaderWrapper;
import org.ethereum.core.BlockWrapper;
import org.ethereum.datasource.DataSourceArray;
import org.ethereum.db.DbFlushManager;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.net.server.Channel;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.validator.BlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Lazy
public class HeadersDownloader extends BlockDownloader {
    private final static Logger logger = LoggerFactory.getLogger("sync");
    private final
    IndexedBlockStore blockStore;
    private final
    SyncPool syncPool;
    private final
    ChannelManager channelManager;
    private final
    DataSourceArray<BlockHeader> headerStore;

    private final
    DbFlushManager dbFlushManager;

    private byte[] genesisHash;

    private int headersLoaded = 0;

    @Autowired
    public HeadersDownloader(final BlockHeaderValidator headerValidator, IndexedBlockStore blockStore, SyncPool syncPool, ChannelManager channelManager, @Qualifier("headerSource") DataSourceArray<BlockHeader> headerStore, DbFlushManager dbFlushManager) {
        super(headerValidator);
        setHeaderQueueLimit(200000);
        setBlockBodiesDownload(false);
        logger.info("HeaderDownloader created.");
        this.blockStore = blockStore;
        this.syncPool = syncPool;
        this.channelManager = channelManager;
        this.headerStore = headerStore;
        this.dbFlushManager = dbFlushManager;
    }

    public void init(final byte[] startFromBlockHash) {
        logger.info("HeaderDownloader init: startHash = " + Hex.toHexString(startFromBlockHash));
        final SyncQueueReverseImpl syncQueue = new SyncQueueReverseImpl(startFromBlockHash, true);
        super.init(syncQueue, syncPool);
        syncPool.init(channelManager);
    }

    @Override
    protected synchronized void pushBlocks(final List<BlockWrapper> blockWrappers) {
    }

    @Override
    protected void pushHeaders(final List<BlockHeaderWrapper> headers) {
        if (headers.get(headers.size() - 1).getNumber() == 0) {
            genesisHash = headers.get(headers.size() - 1).getHash();
        }
        if (headers.get(headers.size() - 1).getNumber() == 1) {
            genesisHash = headers.get(headers.size() - 1).getHeader().getParentHash();
        }
        logger.info(headers.size() + " headers loaded: " + headers.get(0).getNumber() + " - " + headers.get(headers.size() - 1).getNumber());
        for (final BlockHeaderWrapper header : headers) {
            headerStore.set((int) header.getNumber(), header.getHeader());
            headersLoaded++;
        }
        dbFlushManager.commit();
    }

    /**
     * Headers download could block chain synchronization occupying all peers
     * Prevents this by leaving one peer without work
     * Fallbacks to any peer when low number of active peers available
     */
    @Override
    Channel getAnyPeer() {
        return syncPool.getActivePeersCount() > 2 ? syncPool.getNotLastIdle() : syncPool.getAnyIdle();
    }

    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    public int getHeadersLoaded() {
        return headersLoaded;
    }

    @Override
    protected void finishDownload() {
        stop();
    }

    public byte[] getGenesisHash() {
        return genesisHash;
    }
}
