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

package org.ethereum.mine;

import com.google.common.util.concurrent.ListenableFuture;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;

/**
 * The adapter of Ethash for MinerIfc
 *
 * Created by Anton Nashatyrev on 26.02.2016.
 */
public class EthashMiner implements MinerIfc {

    private final SystemProperties config;

    private final int cpuThreads;
    private boolean fullMining = true;

    public EthashMiner(final SystemProperties config) {
        this.config = config;
        cpuThreads = config.getMineCpuThreads();
        fullMining = config.isMineFullDataset();
    }

    @Override
    public ListenableFuture<MiningResult> mine(final Block block) {
        return fullMining ?
                Ethash.Companion.getForBlock(config, block.getNumber()).mine(block, cpuThreads) :
                Ethash.Companion.getForBlock(config, block.getNumber()).mineLight(block, cpuThreads);
    }

    @Override
    public boolean validate(final BlockHeader blockHeader) {
        return Ethash.Companion.getForBlock(config, blockHeader.getNumber()).validate(blockHeader);
    }
}
