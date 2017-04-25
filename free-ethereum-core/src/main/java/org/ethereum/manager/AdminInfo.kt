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

package org.ethereum.manager

import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.PostConstruct

/**
 * @author Roman Mandeleil
 * *
 * @since 11.12.2014
 */
@Component
class AdminInfo {
    private val blockExecTime = LinkedList<Long>()
    var startupTimeStamp: Long = 0
        private set
    var isConsensus = true
        private set

    @PostConstruct
    fun init() {
        startupTimeStamp = System.currentTimeMillis()
    }

    fun lostConsensus() {
        isConsensus = false
    }

    fun addBlockExecTime(time: Long) {
        while (blockExecTime.size > ExecTimeListLimit) {
            blockExecTime.removeAt(0)
        }
        blockExecTime.add(time)
    }

    val execAvg: Long?
        get() {

            if (blockExecTime.isEmpty()) return 0L

            val sum: Long = blockExecTime.sum()

            return sum / blockExecTime.size
        }

    fun getBlockExecTime(): List<Long> {
        return blockExecTime
    }

    companion object {
        private val ExecTimeListLimit = 10000
    }
}
