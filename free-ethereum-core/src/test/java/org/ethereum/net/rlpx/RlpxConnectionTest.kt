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

import com.google.common.collect.Lists
import org.ethereum.crypto.ECKey
import org.ethereum.net.client.Capability
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.*
import java.security.SecureRandom

class RlpxConnectionTest {
    private var iCodec: FrameCodec? = null
    private var rCodec: FrameCodec? = null
    private var initiator: EncryptionHandshake? = null
    private var responder: EncryptionHandshake? = null
    private var iMessage: HandshakeMessage? = null
    private var to: PipedInputStream? = null
    private var toOut: PipedOutputStream? = null
    private var from: PipedInputStream? = null
    private var fromOut: PipedOutputStream? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val remoteKey = ECKey()
        val myKey = ECKey()
        initiator = EncryptionHandshake(remoteKey.pubKeyPoint)
        responder = EncryptionHandshake()
        val initiate = initiator!!.createAuthInitiate(null, myKey)
        val initiatePacket = initiator!!.encryptAuthMessage(initiate)
        val responsePacket = responder!!.handleAuthInitiate(initiatePacket, remoteKey)
        initiator!!.handleAuthResponse(myKey, initiatePacket, responsePacket)
        to = PipedInputStream(1024 * 1024)
        toOut = PipedOutputStream(to)
        from = PipedInputStream(1024 * 1024)
        fromOut = PipedOutputStream(from)
        iCodec = FrameCodec(initiator!!.secrets)
        rCodec = FrameCodec(responder!!.secrets)
        val nodeId = byteArrayOf(1, 2, 3, 4)
        iMessage = HandshakeMessage(
                123,
                "abcd",
                Lists.newArrayList(
                        Capability("zz", 1.toByte()),
                        Capability("yy", 3.toByte())
                ),
                3333,
                nodeId
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFrame() {
        val payload = ByteArray(123)
        SecureRandom().nextBytes(payload)
        val frame = FrameCodec.Frame(12345, 123, ByteArrayInputStream(payload))
        iCodec!!.writeFrame(frame, toOut)
        val frame1 = rCodec!!.readFrames(DataInputStream(to!!))[0]
        val payload1 = ByteArray(frame1.size)
        assertEquals(frame.size.toLong(), frame1.size.toLong())
        frame1.payload.read(payload1)
        assertArrayEquals(payload, payload1)
        assertEquals(frame.type, frame1.type)
    }

    @Test
    @Throws(IOException::class)
    fun testMessageEncoding() {
        val wire = iMessage!!.encode()
        val message1 = HandshakeMessage.parse(wire)
        assertEquals(123, message1.version)
        assertEquals("abcd", message1.name)
        assertEquals(3333, message1.listenPort)
        assertArrayEquals(message1.nodeId, message1.nodeId)
        assertEquals(iMessage!!.caps, message1.caps)
    }

    @Test
    @Throws(IOException::class)
    fun testHandshake() {
        val iConn = RlpxConnection(initiator!!.secrets, from!!, toOut!!)
        val rConn = RlpxConnection(responder!!.secrets, to!!, fromOut!!)
        iConn.sendProtocolHandshake(iMessage!!)
        rConn.handleNextMessage()
        val receivedMessage = rConn.handshakeMessage
        assertNotNull(receivedMessage)
        assertArrayEquals(iMessage!!.nodeId, receivedMessage!!.nodeId)
    }
}
