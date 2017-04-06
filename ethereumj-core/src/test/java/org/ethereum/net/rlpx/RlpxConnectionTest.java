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

package org.ethereum.net.rlpx;

import com.google.common.collect.Lists;
import org.ethereum.crypto.ECKey;
import org.ethereum.net.client.Capability;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.SecureRandom;

import static org.junit.Assert.*;

/**
 * Created by devrandom on 2015-04-11.
 */
public class RlpxConnectionTest {
    private FrameCodec iCodec;
    private FrameCodec rCodec;
    private EncryptionHandshake initiator;
    private EncryptionHandshake responder;
    private HandshakeMessage iMessage;
    private PipedInputStream to;
    private PipedOutputStream toOut;
    private PipedInputStream from;
    private PipedOutputStream fromOut;

    @Before
    public void setUp() throws Exception {
        final ECKey remoteKey = new ECKey();
        final ECKey myKey = new ECKey();
        initiator = new EncryptionHandshake(remoteKey.getPubKeyPoint());
        responder = new EncryptionHandshake();
        final AuthInitiateMessage initiate = initiator.createAuthInitiate(null, myKey);
        final byte[] initiatePacket = initiator.encryptAuthMessage(initiate);
        final byte[] responsePacket = responder.handleAuthInitiate(initiatePacket, remoteKey);
        initiator.handleAuthResponse(myKey, initiatePacket, responsePacket);
        to = new PipedInputStream(1024*1024);
        toOut = new PipedOutputStream(to);
        from = new PipedInputStream(1024*1024);
        fromOut = new PipedOutputStream(from);
        iCodec = new FrameCodec(initiator.getSecrets());
        rCodec = new FrameCodec(responder.getSecrets());
        final byte[] nodeId = {1, 2, 3, 4};
        iMessage = new HandshakeMessage(
                123,
                "abcd",
                Lists.newArrayList(
                        new Capability("zz", (byte) 1),
                        new Capability("yy", (byte) 3)
                ),
                3333,
                nodeId
        );
    }

    @Test
    public void testFrame() throws Exception {
        final byte[] payload = new byte[123];
        new SecureRandom().nextBytes(payload);
        final FrameCodec.Frame frame = new FrameCodec.Frame(12345, 123, new ByteArrayInputStream(payload));
        iCodec.writeFrame(frame, toOut);
        final FrameCodec.Frame frame1 = rCodec.readFrames(new DataInputStream(to)).get(0);
        final byte[] payload1 = new byte[frame1.size];
        assertEquals(frame.size, frame1.size);
        frame1.payload.read(payload1);
        assertArrayEquals(payload, payload1);
        assertEquals(frame.type, frame1.type);
    }

    @Test
    public void testMessageEncoding() throws IOException {
        final byte[] wire = iMessage.encode();
        final HandshakeMessage message1 = HandshakeMessage.parse(wire);
        assertEquals(123, message1.version);
        assertEquals("abcd", message1.name);
        assertEquals(3333, message1.listenPort);
        assertArrayEquals(message1.nodeId, message1.nodeId);
        assertEquals(iMessage.caps, message1.caps);
    }

    @Test
    public void testHandshake() throws IOException {
        final RlpxConnection iConn = new RlpxConnection(initiator.getSecrets(), from, toOut);
        final RlpxConnection rConn = new RlpxConnection(responder.getSecrets(), to, fromOut);
        iConn.sendProtocolHandshake(iMessage);
        rConn.handleNextMessage();
        final HandshakeMessage receivedMessage = rConn.getHandshakeMessage();
        assertNotNull(receivedMessage);
        assertArrayEquals(iMessage.nodeId, receivedMessage.nodeId);
    }
}
