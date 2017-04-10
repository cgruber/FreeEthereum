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

package org.ethereum.sync

/**
 * Manages sync measurements

 * @author Mikhail Kalinin
 * *
 * @since 20.08.2015
 */
class SyncStatistics {
    private var updatedAt: Long = 0
    var blocksCount: Long = 0
        private set
    var headersCount: Long = 0
        private set
    var headerBunchesCount: Int = 0
        private set

    init {
        reset()
    }

    fun reset() {
        updatedAt = System.currentTimeMillis()
        blocksCount = 0
        headersCount = 0
        headerBunchesCount = 0
    }

    fun addBlocks(cnt: Long) {
        blocksCount += cnt
        fixCommon(cnt)
    }

    fun addHeaders(cnt: Long) {
        headerBunchesCount++
        headersCount += cnt
        fixCommon(cnt)
    }

    private fun fixCommon(cnt: Long) {
        updatedAt = System.currentTimeMillis()
    }

    fun secondsSinceLastUpdate(): Long {
        return (System.currentTimeMillis() - updatedAt) / 1000
    }
}
