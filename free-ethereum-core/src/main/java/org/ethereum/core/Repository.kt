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

import org.ethereum.db.ByteArrayWrapper
import org.ethereum.db.ContractDetails
import org.ethereum.vm.DataWord
import java.math.BigInteger
import java.util.*

/**
 * @author Roman Mandeleil
 * *
 * @since 08.09.2014
 */
interface Repository : org.ethereum.facade.Repository {

    /**
     * Create a new account in the database

     * @param addr of the contract
     * *
     * @return newly created account state
     */
    fun createAccount(addr: ByteArray): AccountState


    /**
     * @param addr - account to check
     * *
     * @return - true if account exist,
     * *           false otherwise
     */
    override fun isExist(addr: ByteArray): Boolean

    /**
     * Retrieve an account

     * @param addr of the account
     * *
     * @return account state as stored in the database
     */
    fun getAccountState(addr: ByteArray): AccountState

    /**
     * Deletes the account

     * @param addr of the account
     */
    fun delete(addr: ByteArray)

    /**
     * Increase the account nonce of the given account by one

     * @param addr of the account
     * *
     * @return new value of the nonce
     */
    fun increaseNonce(addr: ByteArray): BigInteger

    /**
     * Sets the account nonce of the given account

     * @param addr of the account
     * *
     * @param nonce new nonce
     * *
     * @return new value of the nonce
     */
    fun setNonce(addr: ByteArray, nonce: BigInteger): BigInteger

    /**
     * Get current nonce of a given account

     * @param addr of the account
     * *
     * @return value of the nonce
     */
    override fun getNonce(addr: ByteArray?): BigInteger

    /**
     * Retrieve contract details for a given account from the database

     * @param addr of the account
     * *
     * @return new contract details
     */
    fun getContractDetails(addr: ByteArray): ContractDetails

    fun hasContractDetails(addr: ByteArray): Boolean

    /**
     * Store code associated with an account

     * @param addr for the account
     * *
     * @param code that will be associated with this account
     */
    fun saveCode(addr: ByteArray, code: ByteArray)

    /**
     * Retrieve the code associated with an account

     * @param addr of the account
     * *
     * @return code in byte-array format
     */
    override fun getCode(addr: ByteArray): ByteArray

    /**
     * Retrieve the code hash associated with an account

     * @param addr of the account
     * *
     * @return code hash
     */
    fun getCodeHash(addr: ByteArray): ByteArray

    /**
     * Put a value in storage of an account at a given key

     * @param addr of the account
     * *
     * @param key of the data to store
     * *
     * @param value is the data to store
     */
    fun addStorageRow(addr: ByteArray, key: DataWord, value: DataWord)


    /**
     * Retrieve storage value from an account for a given key

     * @param addr of the account
     * *
     * @param key associated with this value
     * *
     * @return data in the form of a `DataWord`
     */
    override fun getStorageValue(addr: ByteArray, key: DataWord): DataWord


    /**
     * Retrieve balance of an account

     * @param addr of the account
     * *
     * @return balance of the account as a `BigInteger` value
     */
    override fun getBalance(addr: ByteArray?): BigInteger

    /**
     * Add value to the balance of an account

     * @param addr of the account
     * *
     * @param value to be added
     * *
     * @return new balance of the account
     */
    fun addBalance(addr: ByteArray, value: BigInteger): BigInteger

    /**
     * @return Returns set of all the account addresses
     */
    val accountsKeys: Set<ByteArray>


    /**
     * Dump the full state of the current repository into a file with JSON format
     * It contains all the contracts/account, their attributes and

     * @param block of the current state
     * *
     * @param gasUsed the amount of gas used in the block until that point
     * *
     * @param txNumber is the number of the transaction for which the dump has to be made
     * *
     * @param txHash is the hash of the given transaction.
     * * If null, the block state post coinbase reward is dumped.
     */
    fun dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ByteArray)

    /**
     * Save a snapshot and start tracking future changes

     * @return the tracker repository
     */
    fun startTracking(): Repository

    fun flush()
    fun flushNoReconnect()


    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    fun commit()

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    fun rollback()

    /**
     * Return to one of the previous snapshots
     * by moving the root.

     * @param root - new root
     */
    fun syncToRoot(root: ByteArray)

    /**
     * Check to see if the current repository has an open connection to the database

     * @return <tt>true</tt> if connection to database is open
     */
    val isClosed: Boolean

    /**
     * Close the database
     */
    fun close()

    /**
     * Reset
     */
    fun reset()

    fun updateBatch(accountStates: HashMap<ByteArrayWrapper, AccountState>,
                    contractDetailes: HashMap<ByteArrayWrapper, ContractDetails>)


    val root: ByteArray

    fun loadAccount(addr: ByteArray, cacheAccounts: HashMap<ByteArrayWrapper, AccountState>,
                    cacheDetails: HashMap<ByteArrayWrapper, ContractDetails>)

    fun getSnapshotTo(root: ByteArray): Repository
}
