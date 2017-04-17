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

import org.ethereum.net.p2p.P2pMessage
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class RlpxConnection(secrets: EncryptionHandshake.Secrets, inp: InputStream, private val out: OutputStream) {

    private val codec: FrameCodec
    private val inp: DataInputStream
    var handshakeMessage: HandshakeMessage? = null
        private set

    init {
        val secrets1 = secrets
        this.inp = DataInputStream(inp)
        this.codec = FrameCodec(secrets)
    }

    @Throws(IOException::class)
    fun sendProtocolHandshake(message: HandshakeMessage) {
        logger.info("<=== " + message)
        val payload = message.encode()
        codec.writeFrame(FrameCodec.Frame(HandshakeMessage.HANDSHAKE_MESSAGE_TYPE, payload), out)
    }

    @Throws(IOException::class)
    fun handleNextMessage() {
        val frame = codec.readFrames(inp)[0]
        if (handshakeMessage == null) {
            if (frame.type != HandshakeMessage.HANDSHAKE_MESSAGE_TYPE.toLong())
                throw IOException("expected handshake or disconnect")
            // TODO handle disconnect
            val wire = ByteArray(frame.size)
            frame.payload.read(wire)
            println("packet " + Hex.toHexString(wire))
            handshakeMessage = HandshakeMessage.parse(wire)
            logger.info(" ===> " + handshakeMessage!!)
        } else {
            println("packet type " + frame.type)
            val wire = ByteArray(frame.size)
            frame.payload.read(wire)
            println("packet " + Hex.toHexString(wire))
        }
    }

    @Throws(IOException::class)
    fun writeMessage(message: P2pMessage) {
        val payload = message.encoded
        codec.writeFrame(FrameCodec.Frame(message.command.asByte().toInt(), payload), out)
    }

    companion object {
        private val logger = LoggerFactory.getLogger("discover")
    }
}
