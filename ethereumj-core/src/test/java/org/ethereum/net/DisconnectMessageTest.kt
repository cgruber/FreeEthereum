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

package org.ethereum.net

import org.ethereum.net.message.ReasonCode
import org.ethereum.net.p2p.DisconnectMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

class DisconnectMessageTest {

    /* DISCONNECT_MESSAGE */

    @Test /* DisconnectMessage 1 - Requested */
    fun test_1() {

        val payload = Hex.decode("C100")
        val disconnectMessage = DisconnectMessage(payload)

        logger.trace("{}" + disconnectMessage)
        assertEquals(disconnectMessage.reason, ReasonCode.REQUESTED)
    }

    @Test /* DisconnectMessage 2 - TCP Error */
    fun test_2() {

        val payload = Hex.decode("C101")
        val disconnectMessage = DisconnectMessage(payload)

        logger.trace("{}" + disconnectMessage)
        assertEquals(disconnectMessage.reason, ReasonCode.TCP_ERROR)
    }

    @Test /* DisconnectMessage 2 - from constructor */
    fun test_3() {

        val disconnectMessage = DisconnectMessage(ReasonCode.NULL_IDENTITY)

        logger.trace("{}" + disconnectMessage)

        val expected = "c107"
        assertEquals(expected, Hex.toHexString(disconnectMessage.encoded))

        assertEquals(ReasonCode.NULL_IDENTITY, disconnectMessage.reason)
    }

    @Test //handling boundary-high
    fun test_4() {

        val payload = Hex.decode("C180")

        val disconnectMessage = DisconnectMessage(payload)
        logger.trace("{}" + disconnectMessage)

        assertEquals(disconnectMessage.reason, ReasonCode.UNKNOWN) //high numbers are zeroed
    }

    @Test //handling boundary-low minus 1 (error)
    fun test_6() {

        val disconnectMessageRaw = "C19999"
        val payload = Hex.decode(disconnectMessageRaw)

        try {
            val disconnectMessage = DisconnectMessage(payload)
            disconnectMessage.toString() //throws exception
            assertTrue("Valid raw encoding for disconnectMessage", false)
        } catch (e: RuntimeException) {
            assertTrue("Invalid raw encoding for disconnectMessage", true)
        }

    }

    @Test //handling boundary-high plus 1 (error)
    fun test_7() {

        val disconnectMessageRaw = "C28081"
        val payload = Hex.decode(disconnectMessageRaw)

        try {
            val disconnectMessage = DisconnectMessage(payload)
            disconnectMessage.toString() //throws exception
            assertTrue("Valid raw encoding for disconnectMessage", false)
        } catch (e: RuntimeException) {
            assertTrue("Invalid raw encoding for disconnectMessage", true)
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
    }
}

