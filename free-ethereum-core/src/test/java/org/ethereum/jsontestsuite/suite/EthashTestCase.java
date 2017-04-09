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

package org.ethereum.jsontestsuite.suite;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ethereum.core.BlockHeader;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import static org.spongycastle.util.encoders.Hex.decode;

/**
 * @author Mikhail Kalinin
 * @since 03.09.2015
 */
public class EthashTestCase {

    @JsonIgnore
    private String name;

    private String nonce;

    @JsonProperty("mixhash")
    private String mixHash;
    private String header;
    private String seed;
    private String result;

    @JsonProperty("cache_size")
    private String cacheSize;

    @JsonProperty("full_size")
    private String fullSize;

    @JsonProperty("header_hash")
    private String headerHash;

    @JsonProperty("cache_hash")
    private String cacheHash;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(final String nonce) {
        this.nonce = nonce;
    }

    public String getMixHash() {
        return mixHash;
    }

    public void setMixHash(final String mixHash) {
        this.mixHash = mixHash;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(final String header) {
        this.header = header;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(final String seed) {
        this.seed = seed;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    public String getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(final String cacheSize) {
        this.cacheSize = cacheSize;
    }

    public String getFullSize() {
        return fullSize;
    }

    public void setFullSize(final String fullSize) {
        this.fullSize = fullSize;
    }

    public String getHeaderHash() {
        return headerHash;
    }

    public void setHeaderHash(final String headerHash) {
        this.headerHash = headerHash;
    }

    public String getCacheHash() {
        return cacheHash;
    }

    public void setCacheHash(final String cacheHash) {
        this.cacheHash = cacheHash;
    }

    public BlockHeader getBlockHeader() {
        final RLPList rlp = RLP.decode2(decode(header));
        return new BlockHeader((RLPList) rlp.get(0));
    }

    public byte[] getResultBytes() {
        return decode(result);
    }

    @Override
    public String toString() {
        return "EthashTestCase{" +
                "name='" + name + '\'' +
                ", header='" + header + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
