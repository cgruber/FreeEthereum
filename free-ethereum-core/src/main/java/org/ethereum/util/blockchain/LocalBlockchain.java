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

package org.ethereum.util.blockchain;

import org.ethereum.core.Block;

/**
 * This interface is implemented by the locally created blockchain
 * where block issuance can be controlled.
 *
 * All the pending transactions submitted via EasyBlockchain are
 * buffered and become part of the blockchain as soon as
 * a new block is generated
 *
 * Created by Anton Nashatyrev on 24.03.2016.
 */
interface LocalBlockchain extends EasyBlockchain {

    /**
     * Creates a new block which includes all the transactions
     * created via EasyBlockchain since the last created block
     * The pending transaction list is cleared.
     * The current best block on the chain becomes a parent of the
     * created block
     */
    Block createBlock();

    /**
     * The same as previous but the block parent is specified explicitly
     * This is handy for test/experiments with the chain fork branches
     */
    Block createForkBlock(Block parent);
}
