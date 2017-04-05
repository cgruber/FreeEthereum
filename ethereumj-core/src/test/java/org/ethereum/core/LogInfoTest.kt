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

import org.ethereum.vm.LogInfo
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

/**
 * @author Roman Mandeleil
 * *
 * @since 05.12.2014
 */
class LogInfoTest {

    @Test // rlp decode
    fun test_1() {

        //   LogInfo{address=d5ccd26ba09ce1d85148b5081fa3ed77949417be, topics=[000000000000000000000000459d3a7595df9eba241365f4676803586d7d199c 436f696e73000000000000000000000000000000000000000000000000000000 ], data=}
        val rlp = Hex.decode("f85a94d5ccd26ba09ce1d85148b5081fa3ed77949417bef842a0000000000000000000000000459d3a7595df9eba241365f4676803586d7d199ca0436f696e7300000000000000000000000000000000000000000000000000000080")
        val logInfo = LogInfo(rlp)

        assertEquals("d5ccd26ba09ce1d85148b5081fa3ed77949417be",
                Hex.toHexString(logInfo.address))
        assertEquals("", Hex.toHexString(logInfo.data))

        assertEquals("000000000000000000000000459d3a7595df9eba241365f4676803586d7d199c",
                logInfo.topics[0].toString())
        assertEquals("436f696e73000000000000000000000000000000000000000000000000000000",
                logInfo.topics[1].toString())

        logger.info("{}", logInfo)
    }

    @Test // rlp decode
    fun test_2() {

        val log = LogInfo(Hex.decode("d5ccd26ba09ce1d85148b5081fa3ed77949417be"), null, null)
        assertEquals("d794d5ccd26ba09ce1d85148b5081fa3ed77949417bec080", Hex.toHexString(log.encoded))

        logger.info("{}", log)
    }

    @Ignore //TODO #POC9
    @Test // rlp decode
    fun test_3() {

        //   LogInfo{address=f2b1a404bcb6112a0ff2c4197cb02f3de40018b3, topics=[5a360139cff27713da0fe18a2100048a7ce1b7700c953a82bc3ff011437c8c2a 588d7ddcc06c14843ea68e690dfd4ec91ba09a8ada15c5b7fa6fead9c8befe4b ], data=}
        val rlp = Hex.decode("f85a94f2b1a404bcb6112a0ff2c4197cb02f3de40018b3f842a05a360139cff27713da0fe18a2100048a7ce1b7700c953a82bc3ff011437c8c2aa0588d7ddcc06c14843ea68e690dfd4ec91ba09a8ada15c5b7fa6fead9c8befe4b80")
        val logInfo = LogInfo(rlp)

        assertEquals("f2b1a404bcb6112a0ff2c4197cb02f3de40018b3",
                Hex.toHexString(logInfo.address))

        assertEquals("00800000000000000010000000000000000000000000002000000000000000000012000000100000000050000020000000000000000000000000000000000000",
                logInfo.bloom.toString())

        assertEquals("f85a94f2b1a404bcb6112a0ff2c4197cb02f3de40018b3f842a05a360139cff27713da0fe18a2100048a7ce1b7700c953a82bc3ff011437c8c2aa0588d7ddcc06c14843ea68e690dfd4ec91ba09a8ada15c5b7fa6fead9c8befe4b80",
                Hex.toHexString(logInfo.encoded))

        logger.info("{}", logInfo)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
    }
}
