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
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;

/**
 * Mine algorithm interface
 *
 * Created by Anton Nashatyrev on 25.02.2016.
 */
public interface MinerIfc {

    /**
     * Starts mining the block. On successful mining the Block is update with necessary nonce and hash.
     * @return MiningResult Future object. The mining can be canceled via this Future. The Future is complete
     * when the block successfully mined.
     */
    ListenableFuture<MiningResult> mine(Block block);

    /**
     * Validates the Proof of Work for the block
     */
    boolean validate(BlockHeader blockHeader);

    final class MiningResult {

        public final long nonce;

        public final byte[] digest;

        /**
         * Mined block
         */
        public final Block block;

        public MiningResult(final long nonce, final byte[] digest, final Block block) {
            this.nonce = nonce;
            this.digest = digest;
            this.block = block;
        }
    }
}
