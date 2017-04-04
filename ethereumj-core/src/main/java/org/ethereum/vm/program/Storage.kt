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

package org.ethereum.vm.program

import org.ethereum.core.AccountState
import org.ethereum.core.Block
import org.ethereum.core.Repository
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.db.ContractDetails
import org.ethereum.vm.DataWord
import org.ethereum.vm.program.invoke.ProgramInvoke
import org.ethereum.vm.program.listener.ProgramListener
import org.ethereum.vm.program.listener.ProgramListenerAware
import java.math.BigInteger
import java.util.*

class Storage(programInvoke: ProgramInvoke) : Repository, ProgramListenerAware {

    private val repository: Repository
    private val address: DataWord
    private var programListener: ProgramListener? = null

    init {
        this.address = programInvoke.ownerAddress
        this.repository = programInvoke.repository
    }

    override fun setProgramListener(listener: ProgramListener) {
        this.programListener = listener
    }

    override fun createAccount(addr: ByteArray): AccountState {
        return repository.createAccount(addr)
    }

    override fun isExist(addr: ByteArray): Boolean {
        return repository.isExist(addr)
    }

    override fun getAccountState(addr: ByteArray): AccountState {
        return repository.getAccountState(addr)
    }

    override fun delete(addr: ByteArray) {
        if (canListenTrace(addr)) programListener!!.onStorageClear()
        repository.delete(addr)
    }

    override fun increaseNonce(addr: ByteArray): BigInteger {
        return repository.increaseNonce(addr)
    }

    override fun setNonce(addr: ByteArray, nonce: BigInteger): BigInteger {
        return repository.setNonce(addr, nonce)
    }

    override fun getNonce(addr: ByteArray?): BigInteger {
        return repository.getNonce(addr)
    }

    override fun getContractDetails(addr: ByteArray): ContractDetails {
        return repository.getContractDetails(addr)
    }

    override fun hasContractDetails(addr: ByteArray): Boolean {
        return repository.hasContractDetails(addr)
    }

    override fun saveCode(addr: ByteArray, code: ByteArray) {
        repository.saveCode(addr, code)
    }

    override fun getCode(addr: ByteArray): ByteArray {
        return repository.getCode(addr)
    }

    override fun getCodeHash(addr: ByteArray): ByteArray {
        return repository.getCodeHash(addr)
    }

    override fun addStorageRow(addr: ByteArray, key: DataWord, value: DataWord) {
        if (canListenTrace(addr)) programListener!!.onStoragePut(key, value)
        repository.addStorageRow(addr, key, value)
    }

    private fun canListenTrace(address: ByteArray): Boolean {
        return programListener != null && this.address == DataWord(address)
    }

    override fun getStorageValue(addr: ByteArray, key: DataWord): DataWord {
        return repository.getStorageValue(addr, key)
    }

    override fun getBalance(addr: ByteArray?): BigInteger {
        return repository.getBalance(addr)
    }

    override fun addBalance(addr: ByteArray, value: BigInteger): BigInteger {
        return repository.addBalance(addr, value)
    }

    override val accountsKeys: Set<ByteArray>
        get() = repository.accountsKeys

    override fun dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ByteArray) {
        repository.dumpState(block, gasUsed, txNumber, txHash)
    }

    override fun startTracking(): Repository {
        return repository.startTracking()
    }

    override fun flush() {
        repository.flush()
    }

    override fun flushNoReconnect() {
        throw UnsupportedOperationException()
    }


    override fun commit() {
        repository.commit()
    }

    override fun rollback() {
        repository.rollback()
    }

    override fun syncToRoot(root: ByteArray) {
        repository.syncToRoot(root)
    }

    override val isClosed: Boolean
        get() = repository.isClosed

    override fun close() {
        repository.close()
    }

    override fun reset() {
        repository.reset()
    }

    override fun updateBatch(accountStates: HashMap<ByteArrayWrapper, AccountState>, contractDetails: HashMap<ByteArrayWrapper, ContractDetails>) {
        for (address in contractDetails.keys) {
            if (!canListenTrace(address.data)) return

            val details = contractDetails[address]
            if (details!!.isDeleted) {
                programListener!!.onStorageClear()
            } else if (details.isDirty) {
                for ((key, value) in details.storage) {
                    programListener!!.onStoragePut(key, value)
                }
            }
        }
        repository.updateBatch(accountStates, contractDetails)
    }

    override val root: ByteArray
        get() = repository.root

    override fun loadAccount(addr: ByteArray, cacheAccounts: HashMap<ByteArrayWrapper, AccountState>, cacheDetails: HashMap<ByteArrayWrapper, ContractDetails>) {
        repository.loadAccount(addr, cacheAccounts, cacheDetails)
    }

    override fun getSnapshotTo(root: ByteArray): Repository {
        throw UnsupportedOperationException()
    }

    override fun getStorageSize(addr: ByteArray): Int {
        return repository.getStorageSize(addr)
    }

    override fun getStorageKeys(addr: ByteArray): Set<DataWord> {
        return repository.getStorageKeys(addr)
    }

    override fun getStorage(addr: ByteArray, keys: Collection<DataWord>?): Map<DataWord, DataWord> {
        return repository.getStorage(addr, keys)
    }
}
