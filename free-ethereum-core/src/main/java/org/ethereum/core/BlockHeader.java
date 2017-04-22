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

package org.ethereum.core;

import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.util.Utils;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;

import static org.ethereum.util.ByteUtil.toHexString;

/**
 * Block header is a value object containing
 * the basic information of a block
 */
public class BlockHeader {

    public static final int NONCE_LENGTH = 8;
    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;
    public static final int MAX_HEADER_SIZE = 592;

    /* The SHA3 256-bit hash of the parent block, in its entirety */
    private final byte[] parentHash;
    /* The SHA3 256-bit hash of the uncles list portion of this block */
    private byte[] unclesHash;
    /* The 160-bit address to which all fees collected from the
     * successful mining of this block be transferred; formally */
    private byte[] coinbase;
    /* The SHA3 256-bit hash of the root node of the state trie,
     * after all transactions are executed and finalisations applied */
    private byte[] stateRoot;
    /* The SHA3 256-bit hash of the root node of the trie structure
     * populated with each transaction in the transaction
     * list portion, the trie is populate by [key, val] --> [rlp(index), rlp(tx_recipe)]
     * of the block */
    private byte[] txTrieRoot;
    /* The SHA3 256-bit hash of the root node of the trie structure
     * populated with each transaction recipe in the transaction recipes
     * list portion, the trie is populate by [key, val] --> [rlp(index), rlp(tx_recipe)]
     * of the block */
    private byte[] receiptTrieRoot;

    /*todo: comment it when you know what the fuck it is*/
    private byte[] logsBloom;
    /* A scalar value corresponding to the difficulty level of this block.
     * This can be calculated from the previous block’s difficulty level
     * and the timestamp */
    private byte[] difficulty;
    /* A scalar value equal to the reasonable output of Unix's time()
     * at this block's inception */
    private long timestamp;
    /* A scalar value equal to the number of ancestor blocks.
     * The genesis block has a number of zero */
    private long number;
    /* A scalar value equal to the current limit of gas expenditure per block */
    private byte[] gasLimit;
    /* A scalar value equal to the total gas used in transactions in this block */
    private long gasUsed;


    private byte[] mixHash;

    /* An arbitrary byte array containing data relevant to this block.
     * With the exception of the genesis block, this must be 32 bytes or fewer */
    private byte[] extraData;
    /* A 256-bit hash which proves that a sufficient amount
     * of computation has been carried out on this block */
    private byte[] nonce;

    private byte[] hashCache;

    public BlockHeader(final byte[] encoded) {
        this((RLPList) RLP.decode2(encoded).get(0));
    }

    public BlockHeader(final RLPList rlpHeader) {

        this.parentHash = rlpHeader.get(0).getRLPData();
        this.unclesHash = rlpHeader.get(1).getRLPData();
        this.coinbase = rlpHeader.get(2).getRLPData();
        this.stateRoot = rlpHeader.get(3).getRLPData();

        this.txTrieRoot = rlpHeader.get(4).getRLPData();
        if (this.txTrieRoot == null)
            this.txTrieRoot = HashUtil.INSTANCE.getEMPTY_TRIE_HASH();

        this.receiptTrieRoot = rlpHeader.get(5).getRLPData();
        if (this.receiptTrieRoot == null)
            this.receiptTrieRoot = HashUtil.INSTANCE.getEMPTY_TRIE_HASH();

        this.logsBloom = rlpHeader.get(6).getRLPData();
        this.difficulty = rlpHeader.get(7).getRLPData();

        final byte[] nrBytes = rlpHeader.get(8).getRLPData();
        final byte[] glBytes = rlpHeader.get(9).getRLPData();
        final byte[] guBytes = rlpHeader.get(10).getRLPData();
        final byte[] tsBytes = rlpHeader.get(11).getRLPData();

        this.number = nrBytes == null ? 0 : (new BigInteger(1, nrBytes)).longValue();

        this.gasLimit = glBytes;
        this.gasUsed = guBytes == null ? 0 : (new BigInteger(1, guBytes)).longValue();
        this.timestamp = tsBytes == null ? 0 : (new BigInteger(1, tsBytes)).longValue();

        this.extraData = rlpHeader.get(12).getRLPData();
        this.mixHash = rlpHeader.get(13).getRLPData();
        this.nonce = rlpHeader.get(14).getRLPData();
    }

    public BlockHeader(final byte[] parentHash, final byte[] unclesHash, final byte[] coinbase,
                       final byte[] logsBloom, final byte[] difficulty, final long number,
                       final byte[] gasLimit, final long gasUsed, final long timestamp,
                       final byte[] extraData, final byte[] mixHash, final byte[] nonce) {
        this.parentHash = parentHash;
        this.unclesHash = unclesHash;
        this.coinbase = coinbase;
        this.logsBloom = logsBloom;
        this.difficulty = difficulty;
        this.number = number;
        this.gasLimit = gasLimit;
        this.gasUsed = gasUsed;
        this.timestamp = timestamp;
        this.extraData = extraData;
        this.mixHash = mixHash;
        this.nonce = nonce;
        this.stateRoot = HashUtil.INSTANCE.getEMPTY_TRIE_HASH();
    }

    public boolean isGenesis() {
        return this.getNumber() == Genesis.Companion.getNUMBER();
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public byte[] getUnclesHash() {
        return unclesHash;
    }

    public void setUnclesHash(final byte[] unclesHash) {
        this.unclesHash = unclesHash;
        hashCache = null;
    }

    public byte[] getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(final byte[] coinbase) {
        this.coinbase = coinbase;
        hashCache = null;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(final byte[] stateRoot) {
        this.stateRoot = stateRoot;
        hashCache = null;
    }

    public byte[] getTxTrieRoot() {
        return txTrieRoot;
    }

    public byte[] getReceiptsRoot() {
        return receiptTrieRoot;
    }

    public void setReceiptsRoot(final byte[] receiptTrieRoot) {
        this.receiptTrieRoot = receiptTrieRoot;
        hashCache = null;
    }

    public void setTransactionsRoot(final byte[] stateRoot) {
        this.txTrieRoot = stateRoot;
        hashCache = null;
    }


    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public void setLogsBloom(final byte[] logsBloom) {
        this.logsBloom = logsBloom;
        hashCache = null;
    }

    public byte[] getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(final byte[] difficulty) {
        this.difficulty = difficulty;
        hashCache = null;
    }

    public BigInteger getDifficultyBI() {
        return new BigInteger(1, difficulty);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
        hashCache = null;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(final long number) {
        this.number = number;
        hashCache = null;
    }

    public byte[] getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(final byte[] gasLimit) {
        this.gasLimit = gasLimit;
        hashCache = null;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(final long gasUsed) {
        this.gasUsed = gasUsed;
        hashCache = null;
    }

    public byte[] getMixHash() {
        return mixHash;
    }

    public void setMixHash(final byte[] mixHash) {
        this.mixHash = mixHash;
        hashCache = null;
    }

    public byte[] getExtraData() {
        return extraData;
    }

    public void setExtraData(final byte[] extraData) {
        this.extraData = extraData;
        hashCache = null;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(final byte[] nonce) {
        this.nonce = nonce;
        hashCache = null;
    }

    public byte[] getHash() {
        if (hashCache == null) {
            hashCache = HashUtil.INSTANCE.sha3(getEncoded());
        }
        return hashCache;
    }

    public byte[] getEncoded() {
        return this.getEncoded(true); // with nonce
    }

    public byte[] getEncodedWithoutNonce() {
        return this.getEncoded(false);
    }

    private byte[] getEncoded(final boolean withNonce) {
        final byte[] parentHash = RLP.encodeElement(this.parentHash);

        final byte[] unclesHash = RLP.encodeElement(this.unclesHash);
        final byte[] coinbase = RLP.encodeElement(this.coinbase);

        final byte[] stateRoot = RLP.encodeElement(this.stateRoot);

        if (txTrieRoot == null) this.txTrieRoot = HashUtil.INSTANCE.getEMPTY_TRIE_HASH();
        final byte[] txTrieRoot = RLP.encodeElement(this.txTrieRoot);

        if (receiptTrieRoot == null) this.receiptTrieRoot = HashUtil.INSTANCE.getEMPTY_TRIE_HASH();
        final byte[] receiptTrieRoot = RLP.encodeElement(this.receiptTrieRoot);

        final byte[] logsBloom = RLP.encodeElement(this.logsBloom);
        final byte[] difficulty = RLP.encodeBigInteger(new BigInteger(1, this.difficulty));
        final byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        final byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        final byte[] gasUsed = RLP.encodeBigInteger(BigInteger.valueOf(this.gasUsed));
        final byte[] timestamp = RLP.encodeBigInteger(BigInteger.valueOf(this.timestamp));

        final byte[] extraData = RLP.encodeElement(this.extraData);
        if (withNonce) {
            final byte[] mixHash = RLP.encodeElement(this.mixHash);
            final byte[] nonce = RLP.encodeElement(this.nonce);
            return RLP.encodeList(parentHash, unclesHash, coinbase,
                    stateRoot, txTrieRoot, receiptTrieRoot, logsBloom, difficulty, number,
                    gasLimit, gasUsed, timestamp, extraData, mixHash, nonce);
        } else {
            return RLP.encodeList(parentHash, unclesHash, coinbase,
                    stateRoot, txTrieRoot, receiptTrieRoot, logsBloom, difficulty, number,
                    gasLimit, gasUsed, timestamp, extraData);
        }
    }

    public byte[] getUnclesEncoded(final List<BlockHeader> uncleList) {

        final byte[][] unclesEncoded = new byte[uncleList.size()][];
        int i = 0;
        for (final BlockHeader uncle : uncleList) {
            unclesEncoded[i] = uncle.getEncoded();
            ++i;
        }
        return RLP.encodeList(unclesEncoded);
    }

    public byte[] getPowBoundary() {
        return BigIntegers.asUnsignedByteArray(32, BigInteger.ONE.shiftLeft(256).divide(getDifficultyBI()));
    }

    public byte[] calcPowValue() {

        // nonce bytes are expected in Little Endian order, reverting
        final byte[] nonceReverted = Arrays.reverse(nonce);
        final byte[] hashWithoutNonce = HashUtil.INSTANCE.sha3(getEncodedWithoutNonce());

        final byte[] seed = Arrays.concatenate(hashWithoutNonce, nonceReverted);
        final byte[] seedHash = HashUtil.INSTANCE.sha512(seed);

        final byte[] concat = Arrays.concatenate(seedHash, mixHash);
        return HashUtil.INSTANCE.sha3(concat);
    }

    public BigInteger calcDifficulty(final BlockchainNetConfig config, final BlockHeader parent) {
        return config.getConfigForBlock(getNumber()).
                calcDifficulty(this, parent);
    }

    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        return "  hash=" + toHexString(getHash()) + suffix +
                "  parentHash=" + toHexString(parentHash) + suffix +
                "  unclesHash=" + toHexString(unclesHash) + suffix +
                "  coinbase=" + toHexString(coinbase) + suffix +
                "  stateRoot=" + toHexString(stateRoot) + suffix +
                "  txTrieHash=" + toHexString(txTrieRoot) + suffix +
                "  receiptsTrieHash=" + toHexString(receiptTrieRoot) + suffix +
                "  difficulty=" + toHexString(difficulty) + suffix +
                "  number=" + number + suffix +
                "  gasLimit=" + toHexString(gasLimit) + suffix +
                "  gasUsed=" + gasUsed + suffix +
                "  timestamp=" + timestamp + " (" + Utils.longToDateTime(timestamp) + ")" + suffix +
                "  extraData=" + toHexString(extraData) + suffix +
                "  mixHash=" + toHexString(mixHash) + suffix +
                "  nonce=" + toHexString(nonce) + suffix;
    }

    public String toFlatString() {
        return toStringWithSuffix("");
    }

    public String getShortDescr() {
        return "#" + getNumber() + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + Hex.toHexString(getParentHash()).substring(0,6) + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BlockHeader that = (BlockHeader) o;
        return FastByteComparisons.equal(getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }
}
