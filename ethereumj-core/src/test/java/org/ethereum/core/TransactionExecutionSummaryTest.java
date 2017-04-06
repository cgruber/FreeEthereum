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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.InternalTransaction;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.size;
import static org.ethereum.util.ByteUtil.toHexString;
import static org.junit.Assert.*;

public class TransactionExecutionSummaryTest {


    private static void assertStorageEquals(final Map<DataWord, DataWord> expected, final Map<DataWord, DataWord> actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (final DataWord key : expected.keySet()) {
            final DataWord actualValue = actual.get(key);
            assertNotNull(actualValue);
            assertArrayEquals(expected.get(key).getData(), actualValue.getData());
        }
    }

    private static void assertLogInfoEquals(final LogInfo expected, final LogInfo actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertArrayEquals(expected.getAddress(), actual.getAddress());
        assertEquals(size(expected.getTopics()), size(actual.getTopics()));
        for (int i = 0; i < size(expected.getTopics()); i++) {
            assertArrayEquals(expected.getTopics().get(i).getData(), actual.getTopics().get(i).getData());
        }
        assertArrayEquals(expected.getData(), actual.getData());
    }

    private static Map<DataWord, DataWord> randomStorageEntries(final int count) {
        final Map<DataWord, DataWord> result = new HashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(randomDataWord(), randomDataWord());
        }
        return result;
    }

    private static LogInfo randomLogInfo() {
        return new LogInfo(randomBytes(20), randomDataWords(5), randomBytes(8));
    }

    private static List<LogInfo> randomLogsInfo(final int count) {
        final List<LogInfo> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(randomLogInfo());
        }
        return result;
    }

    private static DataWord randomDataWord() {
        return new DataWord(randomBytes(32));
    }

    private static DataWord randomAddress() {
        return new DataWord(randomBytes(20));
    }

    private static List<DataWord> randomDataWords(final int count) {
        final List<DataWord> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(randomDataWord());
        }
        return result;
    }

    private static InternalTransaction randomInternalTransaction(final Transaction parent, final int deep, final int index) {
        return new InternalTransaction(parent.getHash(), deep, index, randomBytes(1), DataWord.ZERO, DataWord.ZERO,
                parent.getReceiveAddress(), randomBytes(20), randomBytes(2), randomBytes(64), "test note");
    }

    private static List<InternalTransaction> randomInternalTransactions(final Transaction parent, final int nestedLevelCount, final int countByLevel) {
        final List<InternalTransaction> result = new ArrayList<>();
        if (nestedLevelCount > 0) {
            for (int index = 0; index < countByLevel; index++) {
                result.add(randomInternalTransaction(parent, nestedLevelCount, index));
            }
            result.addAll(0, randomInternalTransactions(result.get(result.size() - 1), nestedLevelCount - 1, countByLevel));
        }

        return result;
    }

    private static Transaction randomTransaction() {
        final Transaction transaction = Transaction.createDefault(toHexString(randomBytes(20)), new BigInteger(randomBytes(2)), new BigInteger(randomBytes(1)), null);
        transaction.sign(randomBytes(32));
        return transaction;
    }

    private static byte[] randomBytes(final int len) {
        final byte[] bytes = new byte[len];
        new Random().nextBytes(bytes);
        return bytes;
    }

    @Test
    public void testRlpEncoding() {
        final Transaction tx = randomTransaction();
        final Set<DataWord> deleteAccounts = new HashSet<>(randomDataWords(10));
        final List<LogInfo> logs = randomLogsInfo(5);

        final Map<DataWord, DataWord> readOnly = randomStorageEntries(20);
        final Map<DataWord, DataWord> changed = randomStorageEntries(5);
        final Map<DataWord, DataWord> all = new HashMap<DataWord, DataWord>() {{
            putAll(readOnly);
            putAll(changed);
        }};

        final BigInteger gasLeftover = new BigInteger("123");
        final BigInteger gasRefund = new BigInteger("125");
        final BigInteger gasUsed = new BigInteger("556");

        final int nestedLevelCount = 5000;
        final int countByLevel = 1;
        final List<InternalTransaction> internalTransactions = randomInternalTransactions(tx, nestedLevelCount, countByLevel);

        final byte[] result = randomBytes(32);


        final byte[] encoded = new TransactionExecutionSummary.Builder(tx)
                .deletedAccounts(deleteAccounts)
                .logs(logs)
                .touchedStorage(all, changed)
                .gasLeftover(gasLeftover)
                .gasRefund(gasRefund)
                .gasUsed(gasUsed)
                .internalTransactions(internalTransactions)
                .result(result)
                .build()
                .getEncoded();


        final TransactionExecutionSummary summary = new TransactionExecutionSummary(encoded);
        assertArrayEquals(tx.getHash(), summary.getTransactionHash());

        assertEquals(size(deleteAccounts), size(summary.getDeletedAccounts()));
        for (final DataWord account : summary.getDeletedAccounts()) {
            assertTrue(deleteAccounts.contains(account));
        }

        assertEquals(size(logs), size(summary.getLogs()));
        for (int i = 0; i < logs.size(); i++) {
            assertLogInfoEquals(logs.get(i), summary.getLogs().get(i));
        }

        assertStorageEquals(all, summary.getTouchedStorage().getAll());
        assertStorageEquals(changed, summary.getTouchedStorage().getChanged());
        assertStorageEquals(readOnly, summary.getTouchedStorage().getReadOnly());

        assertEquals(gasRefund, summary.getGasRefund());
        assertEquals(gasLeftover, summary.getGasLeftover());
        assertEquals(gasUsed, summary.getGasUsed());

        assertEquals(nestedLevelCount * countByLevel, size(internalTransactions));

        assertArrayEquals(result, summary.getResult());
    }
}