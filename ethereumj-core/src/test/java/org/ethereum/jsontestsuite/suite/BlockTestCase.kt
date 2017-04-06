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

package org.ethereum.jsontestsuite.suite

import org.ethereum.jsontestsuite.suite.model.AccountTck
import org.ethereum.jsontestsuite.suite.model.BlockHeaderTck
import org.ethereum.jsontestsuite.suite.model.BlockTck

class BlockTestCase {

    var acomment: String? = null
    var blocks: List<BlockTck>? = null
    var genesisBlockHeader: BlockHeaderTck? = null
    val genesisRLP: String? = null
    var pre: Map<String, AccountTck>? = null
    var postState: Map<String, AccountTck>? = null
    var lastblockhash: String? = null
    var noBlockChainHistory: Int = 0

    override fun toString(): String {
        return "BlockTestCase{" +
                "blocks=" + blocks +
                ", genesisBlockHeader=" + genesisBlockHeader +
                ", pre=" + pre +
                '}'
    }
}
