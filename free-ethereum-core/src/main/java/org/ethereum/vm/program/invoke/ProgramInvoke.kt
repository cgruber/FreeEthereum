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

package org.ethereum.vm.program.invoke

import org.ethereum.core.Repository
import org.ethereum.db.BlockStore
import org.ethereum.vm.DataWord

/**
 * @author Roman Mandeleil
 * *
 * @since 03.06.2014
 */
interface ProgramInvoke {

    val ownerAddress: DataWord

    val balance: DataWord

    val originAddress: DataWord

    val callerAddress: DataWord

    val minGasPrice: DataWord

    val gas: DataWord

    val gasLong: Long

    val callValue: DataWord

    val dataSize: DataWord

    fun getDataValue(indexData: DataWord): DataWord

    fun getDataCopy(offsetData: DataWord, lengthData: DataWord): ByteArray

    val prevHash: DataWord

    val coinbase: DataWord

    val timestamp: DataWord

    val number: DataWord

    val difficulty: DataWord

    val gaslimit: DataWord

    fun byTransaction(): Boolean

    fun byTestingSuite(): Boolean

    val callDeep: Int

    val repository: Repository

    val blockStore: BlockStore

}
