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

import org.ethereum.net.p2p.P2pMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by devrandom on 2015-04-12.
 */
class RlpxConnection {
    private static final Logger logger = LoggerFactory.getLogger("discover");

    private final FrameCodec codec;
    private final DataInputStream inp;
    private final OutputStream out;
    private HandshakeMessage handshakeMessage;

    public RlpxConnection(final EncryptionHandshake.Secrets secrets, final InputStream inp, final OutputStream out) {
        final EncryptionHandshake.Secrets secrets1 = secrets;
        this.inp = new DataInputStream(inp);
        this.out = out;
        this.codec = new FrameCodec(secrets);
    }

    public void sendProtocolHandshake(final HandshakeMessage message) throws IOException {
        logger.info("<=== " + message);
        final byte[] payload = message.encode();
        codec.writeFrame(new FrameCodec.Frame(HandshakeMessage.HANDSHAKE_MESSAGE_TYPE, payload), out);
    }

    public void handleNextMessage() throws IOException {
        final FrameCodec.Frame frame = codec.readFrames(inp).get(0);
        if (handshakeMessage == null) {
            if (frame.type != HandshakeMessage.HANDSHAKE_MESSAGE_TYPE)
                throw new IOException("expected handshake or disconnect");
            // TODO handle disconnect
            final byte[] wire = new byte[frame.size];
            frame.payload.read(wire);
            System.out.println("packet " + Hex.toHexString(wire));
            handshakeMessage = HandshakeMessage.parse(wire);
            logger.info(" ===> " + handshakeMessage);
        } else {
            System.out.println("packet type " + frame.type);
            final byte[] wire = new byte[frame.size];
            frame.payload.read(wire);
            System.out.println("packet " + Hex.toHexString(wire));
        }
    }

    public HandshakeMessage getHandshakeMessage() {
        return handshakeMessage;
    }

    public void writeMessage(final P2pMessage message) throws IOException {
        final byte[] payload = message.getEncoded();
        codec.writeFrame(new FrameCodec.Frame(message.getCommand().asByte(), payload), out);
    }
}
