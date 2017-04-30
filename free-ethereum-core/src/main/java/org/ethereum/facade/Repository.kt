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

package org.ethereum.facade

import org.ethereum.vm.DataWord
import java.math.BigInteger

interface Repository {

    /**
     * @param addr - account to check
     * *
     * @return - true if account exist,
     * *           false otherwise
     */
    fun isExist(addr: ByteArray): Boolean


    /**
     * Retrieve balance of an account

     * @param addr of the account
     * *
     * @return balance of the account as a `BigInteger` value
     */
    fun getBalance(addr: ByteArray): BigInteger


    /**
     * Get current nonce of a given account

     * @param addr of the account
     * *
     * @return value of the nonce
     */
    fun getNonce(addr: ByteArray): BigInteger


    /**
     * Retrieve the code associated with an account

     * @param addr of the account
     * *
     * @return code in byte-array format
     */
    fun getCode(addr: ByteArray): ByteArray


    /**
     * Retrieve storage value from an account for a given key

     * @param addr of the account
     * *
     * @param key associated with this value
     * *
     * @return data in the form of a `DataWord`
     */
    fun getStorageValue(addr: ByteArray, key: DataWord): DataWord

    /**
     * Retrieve storage size for a given account

     * @param addr of the account
     * *
     * @return storage entries count
     */
    fun getStorageSize(addr: ByteArray): Int

    /**
     * Retrieve all storage keys for a given account

     * @param addr of the account
     * *
     * @return set of storage keys or empty set if account with specified address not exists
     */
    fun getStorageKeys(addr: ByteArray): Set<DataWord>

    /**
     * Retrieve storage entries from an account for given keys

     * @param addr of the account
     * *
     * @param keys
     * *
     * @return storage entries for specified keys, or full storage if keys parameter is `null`
     */
    fun getStorage(addr: ByteArray, keys: Collection<DataWord>?): Map<DataWord, DataWord>
}
