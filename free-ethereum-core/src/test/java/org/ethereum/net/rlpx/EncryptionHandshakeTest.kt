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

package org.ethereum.net.rlpx

import org.ethereum.crypto.ECKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EncryptionHandshakeTest {
    private var myKey: ECKey? = null
    private var remoteKey: ECKey? = null
    private var initiator: EncryptionHandshake? = null

    @Before
    fun setUp() {
        remoteKey = ECKey()
        myKey = ECKey()
        initiator = EncryptionHandshake(remoteKey!!.pubKeyPoint)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAuthInitiate() {
        val message = initiator!!.createAuthInitiate(ByteArray(32), myKey)
        val expectedLength = 65 + 32 + 64 + 32 + 1
        val buffer = message.encode()
        assertEquals(expectedLength.toLong(), buffer.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAgreement() {
        val responder = EncryptionHandshake()
        val initiate = initiator!!.createAuthInitiate(null, myKey)
        val initiatePacket = initiator!!.encryptAuthMessage(initiate)
        val responsePacket = responder.handleAuthInitiate(initiatePacket, remoteKey)
        initiator!!.handleAuthResponse(myKey, initiatePacket, responsePacket)
        assertArrayEquals(initiator!!.secrets.aes, responder.secrets.aes)
        assertArrayEquals(initiator!!.secrets.mac, responder.secrets.mac)
        assertArrayEquals(initiator!!.secrets.token, responder.secrets.token)
    }
}
