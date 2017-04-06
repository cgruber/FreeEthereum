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

import org.ethereum.util.*;
import org.ethereum.vm.LogInfo;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * The transaction receipt is a tuple of three items
 * comprising the transaction, together with the post-transaction state,
 * and the cumulative gas used in the block containing the transaction receipt
 * as of immediately after the transaction has happened,
 */
public class TransactionReceipt {

    private Transaction transaction;

    private byte[] postTxState = EMPTY_BYTE_ARRAY;
    private byte[] cumulativeGas = EMPTY_BYTE_ARRAY;
    private Bloom bloomFilter = new Bloom();
    private List<LogInfo> logInfoList = new ArrayList<>();

    private byte[] gasUsed = EMPTY_BYTE_ARRAY;
    private byte[] executionResult = EMPTY_BYTE_ARRAY;
    private String error = "";

    /* Tx Receipt in encoded form */
    private byte[] rlpEncoded;

    public TransactionReceipt() {
    }

    public TransactionReceipt(final byte[] rlp) {

        final RLPList params = RLP.decode2(rlp);
        final RLPList receipt = (RLPList) params.get(0);

        final RLPItem postTxStateRLP = (RLPItem) receipt.get(0);
        final RLPItem cumulativeGasRLP = (RLPItem) receipt.get(1);
        final RLPItem bloomRLP = (RLPItem) receipt.get(2);
        final RLPList logs = (RLPList) receipt.get(3);
        final RLPItem gasUsedRLP = (RLPItem) receipt.get(4);
        final RLPItem result = (RLPItem) receipt.get(5);

        postTxState = nullToEmpty(postTxStateRLP.getRLPData());
        cumulativeGas = cumulativeGasRLP.getRLPData();
        bloomFilter = new Bloom(bloomRLP.getRLPData());
        gasUsed = gasUsedRLP.getRLPData();
        executionResult = (executionResult = result.getRLPData()) == null ? EMPTY_BYTE_ARRAY : executionResult;

        if (receipt.size() > 6) {
            final byte[] errBytes = receipt.get(6).getRLPData();
            error = errBytes != null ? new String(errBytes, StandardCharsets.UTF_8) : "";
        }

        for (final RLPElement log : logs) {
            final LogInfo logInfo = new LogInfo(log.getRLPData());
            logInfoList.add(logInfo);
        }

        rlpEncoded = rlp;
    }


    public TransactionReceipt(final byte[] postTxState, final byte[] cumulativeGas,
                              final Bloom bloomFilter, final List<LogInfo> logInfoList) {
        this.postTxState = postTxState;
        this.cumulativeGas = cumulativeGas;
        this.bloomFilter = bloomFilter;
        this.logInfoList = logInfoList;
    }

    public TransactionReceipt(final RLPList rlpList) {
        if (rlpList == null || rlpList.size() != 4)
            throw new RuntimeException("Should provide RLPList with postTxState, cumulativeGas, bloomFilter, logInfoList");

        this.postTxState = rlpList.get(0).getRLPData();
        this.cumulativeGas = rlpList.get(1).getRLPData();
        this.bloomFilter = new Bloom(rlpList.get(2).getRLPData());

        final List<LogInfo> logInfos = new ArrayList<>();
        for (final RLPElement logInfoEl : (RLPList) rlpList.get(3)) {
            final LogInfo logInfo = new LogInfo(logInfoEl.getRLPData());
            logInfos.add(logInfo);
        }
        this.logInfoList = logInfos;
    }

    public byte[] getPostTxState() {
        return postTxState;
    }

    public void setPostTxState(final byte[] postTxState) {
        this.postTxState = postTxState;
        rlpEncoded = null;
    }

    public byte[] getCumulativeGas() {
        return cumulativeGas;
    }

    public void setCumulativeGas(final byte[] cumulativeGas) {
        this.cumulativeGas = cumulativeGas;
        rlpEncoded = null;
    }

    public byte[] getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(final long gasUsed) {
        this.gasUsed = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(gasUsed));
        rlpEncoded = null;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(final byte[] executionResult) {
        this.executionResult = executionResult;
        rlpEncoded = null;
    }

    public long getCumulativeGasLong() {
        return new BigInteger(1, cumulativeGas).longValue();
    }

    public Bloom getBloomFilter() {
        return bloomFilter;
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public void setLogInfoList(final List<LogInfo> logInfoList) {
        if (logInfoList == null) return;
        this.logInfoList = logInfoList;

        for (final LogInfo loginfo : logInfoList) {
            bloomFilter.or(loginfo.getBloom());
        }
        rlpEncoded = null;
    }

    public boolean isValid() {
        return ByteUtil.byteArrayToLong(gasUsed) > 0;
    }

    public boolean isSuccessful() {
        return error.isEmpty();
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error == null ? "" : error;
    }

    /**
     *  Used for Receipt trie hash calculation. Should contain only the following items encoded:
     *  [postTxState, cumulativeGas, bloomFilter, logInfoList]
     */
    public byte[] getReceiptTrieEncoded() {
        return getEncoded(true);
    }

    /**
     * Used for serialization, contains all the receipt data encoded
     */
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            rlpEncoded = getEncoded(false);
        }

        return rlpEncoded;
    }

    public byte[] getEncoded(final boolean receiptTrie) {

        final byte[] postTxStateRLP = RLP.encodeElement(this.postTxState);
        final byte[] cumulativeGasRLP = RLP.encodeElement(this.cumulativeGas);
        final byte[] bloomRLP = RLP.encodeElement(this.bloomFilter.data);

        final byte[] logInfoListRLP;
        if (logInfoList != null) {
            final byte[][] logInfoListE = new byte[logInfoList.size()][];

            int i = 0;
            for (final LogInfo logInfo : logInfoList) {
                logInfoListE[i] = logInfo.getEncoded();
                ++i;
            }
            logInfoListRLP = RLP.encodeList(logInfoListE);
        } else {
            logInfoListRLP = RLP.encodeList();
        }

        return receiptTrie ?
                RLP.encodeList(postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP):
                RLP.encodeList(postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP,
                        RLP.encodeElement(gasUsed), RLP.encodeElement(executionResult),
                        RLP.encodeElement(error.getBytes(StandardCharsets.UTF_8)));

    }

    public void setCumulativeGas(final long cumulativeGas) {
        this.cumulativeGas = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(cumulativeGas));
        rlpEncoded = null;
    }

    public void setGasUsed(final byte[] gasUsed) {
        this.gasUsed = gasUsed;
        rlpEncoded = null;
    }

    public Transaction getTransaction() {
        if (transaction == null) throw new NullPointerException("Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");
        return transaction;
    }

    public void setTransaction(final Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {

        // todo: fix that

        return "TransactionReceipt[" +
                "\n  , postTxState=" + Hex.toHexString(postTxState) +
                "\n  , cumulativeGas=" + Hex.toHexString(cumulativeGas) +
                "\n  , gasUsed=" + Hex.toHexString(gasUsed) +
                "\n  , error=" + error +
                "\n  , executionResult=" + Hex.toHexString(executionResult) +
                "\n  , bloom=" + bloomFilter.toString() +
                "\n  , logs=" + logInfoList +
                ']';
    }

}
