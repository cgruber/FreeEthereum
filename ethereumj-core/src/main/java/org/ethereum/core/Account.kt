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

import org.ethereum.crypto.ECKey
import org.ethereum.util.Utils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.*

/**
 * Representation of an actual account or contract
 */
@Component
@Scope("prototype")
class Account {

    private val pendingTransactions = Collections.synchronizedSet(HashSet<Transaction>())
    @Autowired
    private val repository: Repository? = null
    var ecKey: ECKey? = null
        private set
    var address: ByteArray? = null

    fun init() {
        this.ecKey = ECKey(Utils.getRandom())
        address = this.ecKey!!.address
    }

    fun init(ecKey: ECKey) {
        this.ecKey = ecKey
        address = this.ecKey!!.address
    }

    val nonce: BigInteger
        get() = repository!!.getNonce(address)

    // todo: calculate the fee for pending
    val balance: BigInteger
        get() {

            var balance = repository!!.getBalance(this.address)

            synchronized(getPendingTransactions()) {
                if (!getPendingTransactions().isEmpty()) {

                    for (tx in getPendingTransactions()) {
                        if (Arrays.equals(address, tx.sender)) {
                            balance = balance.subtract(BigInteger(1, tx.value))
                        }

                        if (Arrays.equals(address, tx.receiveAddress)) {
                            balance = balance.add(BigInteger(1, tx.value))
                        }
                    }
                }
            }


            return balance
        }

    private fun getPendingTransactions(): Set<Transaction> {
        return this.pendingTransactions
    }

    fun addPendingTransaction(transaction: Transaction) {
        synchronized(pendingTransactions) {
            pendingTransactions.add(transaction)
        }
    }

    fun clearAllPendingTransactions() {
        synchronized(pendingTransactions) {
            pendingTransactions.clear()
        }
    }
}
