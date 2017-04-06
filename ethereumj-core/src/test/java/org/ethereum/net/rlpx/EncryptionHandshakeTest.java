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

import org.ethereum.crypto.ECKey;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by android on 4/8/15.
 */
public class EncryptionHandshakeTest {
    private ECKey myKey;
    private ECKey remoteKey;
    private EncryptionHandshake initiator;

    @Before
    public void setUp() {
        remoteKey = new ECKey();
        myKey = new ECKey();
        initiator = new EncryptionHandshake(remoteKey.getPubKeyPoint());
    }

    @Test
    public void testCreateAuthInitiate() throws Exception {
        final AuthInitiateMessage message = initiator.createAuthInitiate(new byte[32], myKey);
        final int expectedLength = 65 + 32 + 64 + 32 + 1;
        final byte[] buffer = message.encode();
        assertEquals(expectedLength, buffer.length);
    }

    @Test
    public void testAgreement() throws Exception {
        final EncryptionHandshake responder = new EncryptionHandshake();
        final AuthInitiateMessage initiate = initiator.createAuthInitiate(null, myKey);
        final byte[] initiatePacket = initiator.encryptAuthMessage(initiate);
        final byte[] responsePacket = responder.handleAuthInitiate(initiatePacket, remoteKey);
        initiator.handleAuthResponse(myKey, initiatePacket, responsePacket);
        assertArrayEquals(initiator.getSecrets().aes, responder.getSecrets().aes);
        assertArrayEquals(initiator.getSecrets().mac, responder.getSecrets().mac);
        assertArrayEquals(initiator.getSecrets().token, responder.getSecrets().token);
    }
}
