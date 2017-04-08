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

package org.ethereum.core

import org.apache.commons.collections4.CollectionUtils.size
import org.ethereum.util.ByteUtil.toHexString
import org.ethereum.vm.DataWord
import org.ethereum.vm.LogInfo
import org.ethereum.vm.program.InternalTransaction
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.util.*

class TransactionExecutionSummaryTest {


    private fun assertStorageEquals(expected: Map<DataWord, DataWord>, actual: Map<DataWord, DataWord>) {
        assertNotNull(expected)
        assertNotNull(actual)
        assertEquals(expected.size.toLong(), actual.size.toLong())
        for (key in expected.keys) {
            val actualValue = actual[key]
            assertNotNull(actualValue)
            assertArrayEquals(expected[key]?.data, actualValue?.data)
        }
    }

    private fun assertLogInfoEquals(expected: LogInfo, actual: LogInfo) {
        assertNotNull(expected)
        assertNotNull(actual)
        assertArrayEquals(expected.address, actual.address)
        assertEquals(size(expected.topics).toLong(), size(actual.topics).toLong())
        for (i in 0..size(expected.topics) - 1) {
            assertArrayEquals(expected.topics[i].data, actual.topics[i].data)
        }
        assertArrayEquals(expected.data, actual.data)
    }

    private fun randomStorageEntries(count: Int): Map<DataWord, DataWord> {
        val result = HashMap<DataWord, DataWord>()
        for (i in 0..count - 1) {
            result.put(randomDataWord(), randomDataWord())
        }
        return result
    }

    private fun randomLogInfo(): LogInfo {
        return LogInfo(randomBytes(20), randomDataWords(5), randomBytes(8))
    }

    private fun randomLogsInfo(count: Int): List<LogInfo> {
        val result = ArrayList<LogInfo>(count)
        for (i in 0..count - 1) {
            result.add(randomLogInfo())
        }
        return result
    }

    private fun randomDataWord(): DataWord {
        return DataWord(randomBytes(32))
    }

    private fun randomAddress(): DataWord {
        return DataWord(randomBytes(20))
    }

    private fun randomDataWords(count: Int): List<DataWord> {
        val result = ArrayList<DataWord>(count)
        for (i in 0..count - 1) {
            result.add(randomDataWord())
        }
        return result
    }

    private fun randomInternalTransaction(parent: Transaction, deep: Int, index: Int): InternalTransaction {
        return InternalTransaction(parent.hash, deep, index, randomBytes(1), DataWord.ZERO, DataWord.ZERO,
                parent.receiveAddress, randomBytes(20), randomBytes(2), randomBytes(64), "test note")
    }

    private fun randomInternalTransactions(parent: Transaction, nestedLevelCount: Int, countByLevel: Int): List<InternalTransaction> {
        val result = ArrayList<InternalTransaction>()
        if (nestedLevelCount > 0) {
            for (index in 0..countByLevel - 1) {
                result.add(randomInternalTransaction(parent, nestedLevelCount, index))
            }
            result.addAll(0, randomInternalTransactions(result[result.size - 1], nestedLevelCount - 1, countByLevel))
        }

        return result
    }

    private fun randomTransaction(): Transaction {
        val transaction = Transaction.createDefault(toHexString(randomBytes(20)), BigInteger(randomBytes(2)), BigInteger(randomBytes(1)), null)
        transaction.sign(randomBytes(32))
        return transaction
    }

    private fun randomBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        Random().nextBytes(bytes)
        return bytes
    }

    @Test
    fun testRlpEncoding() {
        val tx = randomTransaction()
        val deleteAccounts = HashSet(randomDataWords(10))
        val logs = randomLogsInfo(5)

        val readOnly = randomStorageEntries(20)
        val changed = randomStorageEntries(5)
        val all = object : HashMap<DataWord, DataWord>() {
            init {
                putAll(readOnly)
                putAll(changed)
            }
        }

        val gasLeftover = BigInteger("123")
        val gasRefund = BigInteger("125")
        val gasUsed = BigInteger("556")

        val nestedLevelCount = 5000
        val countByLevel = 1
        val internalTransactions = randomInternalTransactions(tx, nestedLevelCount, countByLevel)

        val result = randomBytes(32)


        val encoded = TransactionExecutionSummary.Builder(tx)
                .deletedAccounts(deleteAccounts)
                .logs(logs)
                .touchedStorage(all, changed)
                .gasLeftover(gasLeftover)
                .gasRefund(gasRefund)
                .gasUsed(gasUsed)
                .internalTransactions(internalTransactions)
                .result(result)
                .build()
                .encoded


        val summary = TransactionExecutionSummary(encoded)
        assertArrayEquals(tx.hash, summary.transactionHash)

        assertEquals(size(deleteAccounts).toLong(), size(summary.deletedAccounts).toLong())
        for (account in summary.deletedAccounts) {
            assertTrue(deleteAccounts.contains(account))
        }

        assertEquals(size(logs).toLong(), size(summary.logs).toLong())
        for (i in logs.indices) {
            assertLogInfoEquals(logs[i], summary.logs[i])
        }

        assertStorageEquals(all, summary.touchedStorage.all)
        assertStorageEquals(changed, summary.touchedStorage.changed)
        assertStorageEquals(readOnly, summary.touchedStorage.readOnly)

        assertEquals(gasRefund, summary.gasRefund)
        assertEquals(gasLeftover, summary.gasLeftover)
        assertEquals(gasUsed, summary.gasUsed)

        assertEquals((nestedLevelCount * countByLevel).toLong(), size(internalTransactions).toLong())

        assertArrayEquals(result, summary.result)
    }
}