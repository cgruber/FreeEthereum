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

package org.ethereum.jsontestsuite.suite.model;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties({"acomment", "comment", "chainname", "chainnetwork"})
public class BlockTck {

    private BlockHeaderTck blockHeader;
    private List<TransactionTck> transactions;
    private List<BlockHeaderTck> uncleHeaders;
    private String rlp;
    private String blocknumber;
    private boolean reverted;

    public BlockTck() {
    }

    public String getBlocknumber() {
        return blocknumber;
    }

    public void setBlocknumber(final String blocknumber) {
        this.blocknumber = blocknumber;
    }

    public BlockHeaderTck getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(final BlockHeaderTck blockHeader) {
        this.blockHeader = blockHeader;
    }

    public String getRlp() {
        return rlp;
    }

    public void setRlp(final String rlp) {
        this.rlp = rlp;
    }

    public List<TransactionTck> getTransactions() {
        return transactions;
    }

    public void setTransactions(final List<TransactionTck> transactions) {
        this.transactions = transactions;
    }

    public List<BlockHeaderTck> getUncleHeaders() {
        return uncleHeaders;
    }

    public void setUncleHeaders(final List<BlockHeaderTck> uncleHeaders) {
        this.uncleHeaders = uncleHeaders;
    }

    public boolean isReverted() {
        return reverted;
    }

    public void setReverted(final boolean reverted) {
        this.reverted = reverted;
    }

    @Override
    public String toString() {
        return "Block{" +
                "blockHeader=" + blockHeader +
                ", transactions=" + transactions +
                ", uncleHeaders=" + uncleHeaders +
                ", rlp='" + rlp + '\'' +
                '}';
    }
}
