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

import org.ethereum.util.BIUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.InternalTransaction;
import org.springframework.util.Assert;

import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

public class TransactionExecutionSummary {

    private Transaction tx;
    private BigInteger value = BigInteger.ZERO;
    private BigInteger gasPrice = BigInteger.ZERO;
    private BigInteger gasLimit = BigInteger.ZERO;
    private BigInteger gasUsed = BigInteger.ZERO;
    private BigInteger gasLeftover = BigInteger.ZERO;
    private BigInteger gasRefund = BigInteger.ZERO;

    private List<DataWord> deletedAccounts = emptyList();
    private List<InternalTransaction> internalTransactions = emptyList();
    private Map<DataWord, DataWord> storageDiff = emptyMap();
    private TransactionTouchedStorage touchedStorage = new TransactionTouchedStorage();


    private byte[] result;
    private List<LogInfo> logs;

    private boolean failed;

    private byte[] rlpEncoded;
    private boolean parsed;


    private TransactionExecutionSummary(final Transaction transaction) {
        this.tx = transaction;
        this.gasLimit = BIUtil.INSTANCE.toBI(transaction.getGasLimit());
        this.gasPrice = BIUtil.INSTANCE.toBI(transaction.getGasPrice());
        this.value = BIUtil.INSTANCE.toBI(transaction.getValue());
    }

    public TransactionExecutionSummary(final byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
        this.parsed = false;
    }

    private static BigInteger decodeBigInteger(final byte[] encoded) {
        return isEmpty(encoded) ? BigInteger.ZERO : new BigInteger(1, encoded);
    }

    private static byte[] encodeTouchedStorage(final TransactionTouchedStorage touchedStorage) {
        final Collection<TransactionTouchedStorage.Entry> entries = touchedStorage.getEntries();
        final byte[][] result = new byte[entries.size()][];

        int i = 0;
        for (final TransactionTouchedStorage.Entry entry : entries) {
            final byte[] key = RLP.encodeElement(entry.getKey().getData());
            final byte[] value = RLP.encodeElement(entry.getValue().getData());
            final byte[] changed = RLP.encodeInt(entry.isChanged() ? 1 : 0);

            result[i++] = RLP.encodeList(key, value, changed);
        }

        return RLP.encodeList(result);
    }

    private static TransactionTouchedStorage decodeTouchedStorage(final RLPElement encoded) {
        final TransactionTouchedStorage result = new TransactionTouchedStorage();

        for (final RLPElement entry : (RLPList) encoded) {
            final RLPList asList = (RLPList) entry;

            final DataWord key = new DataWord(asList.get(0).getRLPData());
            final DataWord value = new DataWord(asList.get(1).getRLPData());
            final byte[] changedBytes = asList.get(2).getRLPData();
            final boolean changed = isNotEmpty(changedBytes) && RLP.decodeInt(changedBytes, 0) == 1;

            result.add(new TransactionTouchedStorage.Entry(key, value, changed));
        }

        return result;
    }

    private static List<LogInfo> decodeLogs(final RLPList logs) {
        final ArrayList<LogInfo> result = new ArrayList<>();
        for (final RLPElement log : logs) {
            result.add(new LogInfo(log.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeLogs(final List<LogInfo> logs) {
        final byte[][] result = new byte[logs.size()][];
        for (int i = 0; i < logs.size(); i++) {
            final LogInfo log = logs.get(i);
            result[i] = log.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static byte[] encodeStorageDiff(final Map<DataWord, DataWord> storageDiff) {
        final byte[][] result = new byte[storageDiff.size()][];
        int i = 0;
        for (final Map.Entry<DataWord, DataWord> entry : storageDiff.entrySet()) {
            final byte[] key = RLP.encodeElement(entry.getKey().getData());
            final byte[] value = RLP.encodeElement(entry.getValue().getData());
            result[i++] = RLP.encodeList(key, value);
        }
        return RLP.encodeList(result);
    }

    private static Map<DataWord, DataWord> decodeStorageDiff(final RLPList storageDiff) {
        final Map<DataWord, DataWord> result = new HashMap<>();
        for (final RLPElement entry : storageDiff) {
            final DataWord key = new DataWord(((RLPList) entry).get(0).getRLPData());
            final DataWord value = new DataWord(((RLPList) entry).get(1).getRLPData());
            result.put(key, value);
        }
        return result;
    }

    private static byte[] encodeInternalTransactions(final List<InternalTransaction> internalTransactions) {
        final byte[][] result = new byte[internalTransactions.size()][];
        for (int i = 0; i < internalTransactions.size(); i++) {
            final InternalTransaction transaction = internalTransactions.get(i);
            result[i] = transaction.getEncoded();
        }

        return RLP.encodeList(result);
    }

    private static List<InternalTransaction> decodeInternalTransactions(final RLPList internalTransactions) {
        final List<InternalTransaction> result = new ArrayList<>();
        for (final RLPElement internalTransaction : internalTransactions) {
            result.add(new InternalTransaction(internalTransaction.getRLPData()));
        }
        return result;
    }

    private static byte[] encodeDeletedAccounts(final List<DataWord> deletedAccounts) {
        final byte[][] result = new byte[deletedAccounts.size()][];
        for (int i = 0; i < deletedAccounts.size(); i++) {
            final DataWord deletedAccount = deletedAccounts.get(i);
            result[i] = RLP.encodeElement(deletedAccount.getData());

        }
        return RLP.encodeList(result);
    }

    private static List<DataWord> decodeDeletedAccounts(final RLPList deletedAccounts) {
        final List<DataWord> result = new ArrayList<>();
        for (final RLPElement deletedAccount : deletedAccounts) {
            result.add(new DataWord(deletedAccount.getRLPData()));
        }
        return result;
    }

    public static Builder builderFor(final Transaction transaction) {
        return new Builder(transaction);
    }

    private void rlpParse() {
        if (parsed) return;

        final RLPList decodedTxList = RLP.decode2(rlpEncoded);
        final RLPList summary = (RLPList) decodedTxList.get(0);

        this.tx = new Transaction(summary.get(0).getRLPData());
        this.value = decodeBigInteger(summary.get(1).getRLPData());
        this.gasPrice = decodeBigInteger(summary.get(2).getRLPData());
        this.gasLimit = decodeBigInteger(summary.get(3).getRLPData());
        this.gasUsed = decodeBigInteger(summary.get(4).getRLPData());
        this.gasLeftover = decodeBigInteger(summary.get(5).getRLPData());
        this.gasRefund = decodeBigInteger(summary.get(6).getRLPData());
        this.deletedAccounts = decodeDeletedAccounts((RLPList) summary.get(7));
        this.internalTransactions = decodeInternalTransactions((RLPList) summary.get(8));
        this.touchedStorage = decodeTouchedStorage(summary.get(9));
        this.result = summary.get(10).getRLPData();
        this.logs = decodeLogs((RLPList) summary.get(11));
        final byte[] failed = summary.get(12).getRLPData();
        this.failed = isNotEmpty(failed) && RLP.decodeInt(failed, 0) == 1;
    }

    public byte[] getEncoded() {
        if (rlpEncoded != null) return rlpEncoded;


        this.rlpEncoded = RLP.encodeList(
                this.tx.getEncoded(),
                RLP.encodeBigInteger(this.value),
                RLP.encodeBigInteger(this.gasPrice),
                RLP.encodeBigInteger(this.gasLimit),
                RLP.encodeBigInteger(this.gasUsed),
                RLP.encodeBigInteger(this.gasLeftover),
                RLP.encodeBigInteger(this.gasRefund),
                encodeDeletedAccounts(this.deletedAccounts),
                encodeInternalTransactions(this.internalTransactions),
                encodeTouchedStorage(this.touchedStorage),
                RLP.encodeElement(this.result),
                encodeLogs(this.logs),
                RLP.encodeInt(this.failed ? 1 : 0)
        );

        return rlpEncoded;
    }

    public Transaction getTransaction() {
        if (!parsed) rlpParse();
        return tx;
    }

    public byte[] getTransactionHash() {
        return getTransaction().getHash();
    }

    private BigInteger calcCost(final BigInteger gas) {
        return gasPrice.multiply(gas);
    }

    public BigInteger getFee() {
        if (!parsed) rlpParse();
        return calcCost(gasLimit.subtract(gasLeftover.add(gasRefund)));
    }

    public BigInteger getRefund() {
        if (!parsed) rlpParse();
        return calcCost(gasRefund);
    }

    public BigInteger getLeftover() {
        if (!parsed) rlpParse();
        return calcCost(gasLeftover);
    }

    public BigInteger getGasPrice() {
        if (!parsed) rlpParse();
        return gasPrice;
    }

    public BigInteger getGasLimit() {
        if (!parsed) rlpParse();
        return gasLimit;
    }

    public BigInteger getGasUsed() {
        if (!parsed) rlpParse();
        return gasUsed;
    }

    public BigInteger getGasLeftover() {
        if (!parsed) rlpParse();
        return gasLeftover;
    }

    public BigInteger getValue() {
        if (!parsed) rlpParse();
        return value;
    }

    public List<DataWord> getDeletedAccounts() {
        if (!parsed) rlpParse();
        return deletedAccounts;
    }

    public List<InternalTransaction> getInternalTransactions() {
        if (!parsed) rlpParse();
        return internalTransactions;
    }

    @Deprecated
    /* Use getTouchedStorage().getAll() instead */
    public Map<DataWord, DataWord> getStorageDiff() {
        if (!parsed) rlpParse();
        return storageDiff;
    }

    public BigInteger getGasRefund() {
        if (!parsed) rlpParse();
        return gasRefund;
    }

    public boolean isFailed() {
        if (!parsed) rlpParse();
        return failed;
    }

    public byte[] getResult() {
        if (!parsed) rlpParse();
        return result;
    }

    public List<LogInfo> getLogs() {
        if (!parsed) rlpParse();
        return logs;
    }

    public TransactionTouchedStorage getTouchedStorage() {
        return touchedStorage;
    }

    public static class Builder {

        private final TransactionExecutionSummary summary;

        Builder(final Transaction transaction) {
            Assert.notNull(transaction, "Cannot build TransactionExecutionSummary for null transaction.");
            summary = new TransactionExecutionSummary(transaction);
        }

        public Builder gasUsed(final BigInteger gasUsed) {
            summary.gasUsed = gasUsed;
            return this;
        }

        public Builder gasLeftover(final BigInteger gasLeftover) {
            summary.gasLeftover = gasLeftover;
            return this;
        }

        public Builder gasRefund(final BigInteger gasRefund) {
            summary.gasRefund = gasRefund;
            return this;
        }

        public Builder internalTransactions(final List<InternalTransaction> internalTransactions) {
            summary.internalTransactions = unmodifiableList(internalTransactions);
            return this;
        }

        public Builder deletedAccounts(final Set<DataWord> deletedAccounts) {
            summary.deletedAccounts = new ArrayList<>();
            summary.deletedAccounts.addAll(deletedAccounts);
            return this;
        }

        public Builder storageDiff(final Map<DataWord, DataWord> storageDiff) {
            summary.storageDiff = unmodifiableMap(storageDiff);
            return this;
        }

        public Builder touchedStorage(final Map<DataWord, DataWord> touched, final Map<DataWord, DataWord> changed) {
            summary.touchedStorage.addReading(touched);
            summary.touchedStorage.addWriting(changed);
            return this;
        }

        public Builder markAsFailed() {
            summary.failed = true;
            return this;
        }

        public Builder logs(final List<LogInfo> logs) {
            summary.logs = logs;
            return this;
        }

        public Builder result(final byte[] result) {
            summary.result = result;
            return this;
        }

        public TransactionExecutionSummary build() {
            summary.parsed = true;
            if (summary.failed) {
                for (final InternalTransaction transaction : summary.internalTransactions) {
                    transaction.reject();
                }
            }
            return summary;
        }
    }
}
