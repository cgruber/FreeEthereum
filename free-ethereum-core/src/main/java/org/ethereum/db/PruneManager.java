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

package org.ethereum.db;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.datasource.JournalSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PruneManager {

    private final int pruneBlocksCnt;
    private final IndexedBlockStore blockStore;
    private JournalSource journal;

    @Autowired
    private PruneManager(final SystemProperties config, IndexedBlockStore blockStore) {
        pruneBlocksCnt = config.databasePruneDepth();
        this.blockStore = blockStore;
    }

    public PruneManager(final IndexedBlockStore blockStore, final JournalSource journal, final int pruneBlocksCnt) {
        this.blockStore = blockStore;
        this.journal = journal;
        this.pruneBlocksCnt = pruneBlocksCnt;
    }

    @Autowired
    public void setStateSource(final StateSource stateSource) {
        journal = stateSource.getJournalSource();
    }

    public void blockCommitted(final BlockHeader block) {
        if (pruneBlocksCnt < 0) return; // pruning disabled

        journal.commitUpdates(block.getHash());
        final long pruneBlockNum = block.getNumber() - pruneBlocksCnt;
        if (pruneBlockNum < 0) return;

        final List<Block> pruneBlocks = blockStore.getBlocksByNumber(pruneBlockNum);
        final Block chainBlock = blockStore.getChainBlockByNumber(pruneBlockNum);
        for (final Block pruneBlock : pruneBlocks) {
            if (journal.hasUpdate(pruneBlock.getHash())) {
                if (chainBlock.isEqual(pruneBlock)) {
                    journal.persistUpdate(pruneBlock.getHash());
                } else {
                    journal.revertUpdate(pruneBlock.getHash());
                }
            }
        }
    }
}
