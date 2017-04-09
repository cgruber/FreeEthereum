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

package org.ethereum.samples;

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.datasource.Source;
import org.ethereum.db.IndexedBlockStore;

import java.util.List;

class CheckFork {
    public static void main(final String[] args) throws Exception {
        SystemProperties.getDefault().overrideParams("database.dir", "");
        final Source<byte[], byte[]> index = CommonConfig.getDefault().cachedDbSource("index");
        final Source<byte[], byte[]> blockDS = CommonConfig.getDefault().cachedDbSource("block");

        final IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
        indexedBlockStore.init(index, blockDS);

        for (int i = 1_919_990; i < 1_921_000; i++) {
            final Block chainBlock = indexedBlockStore.getChainBlockByNumber(i);
            final List<Block> blocks = indexedBlockStore.getBlocksByNumber(i);
            final StringBuilder s = new StringBuilder(chainBlock.getShortDescr() + " (");
            for (final Block block : blocks) {
                if (!block.isEqual(chainBlock)) {
                    s.append(block.getShortDescr()).append(" ");
                }
            }
            System.out.println(s);
        }
    }
}
