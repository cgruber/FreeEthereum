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

import com.google.common.base.Throwables
import com.google.common.collect.Lists
import org.ethereum.crypto.ECIESCoder
import org.ethereum.crypto.ECKey
import org.ethereum.net.client.Capability
import org.ethereum.net.message.ReasonCode
import org.ethereum.net.p2p.DisconnectMessage
import org.ethereum.net.p2p.PingMessage
import org.ethereum.net.rlpx.EncryptionHandshake.Secrets
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.util.encoders.Hex
import java.io.EOFException
import java.io.IOException
import java.net.Socket
import java.net.URI
import java.net.URISyntaxException
import java.util.*

internal class Handshaker private constructor() {
    private val myKey: ECKey = ECKey()
    private val nodeId: ByteArray
    var secrets: Secrets? = null
        private set

    init {
        nodeId = myKey.nodeId
        println("Node ID " + Hex.toHexString(nodeId))
    }

    /**
     * Sample output:
     * <pre>
     * Node ID b7fb52ddb1f269fef971781b9568ad65d30ac3b6055ebd6a0a762e6b67a7c92bd7c1fdf3c7c722d65ae70bfe6a9a58443297485aa29e3acd9bdf2ee0df4f5c45
     * packet f86b0399476574682f76302e392e372f6c696e75782f676f312e342e32ccc5836574683cc5837368680280b840f1c041a7737e8e06536d9defb92cb3db6ecfeb1b1208edfca6953c0c683a31ff0a478a832bebb6629e4f5c13136478842cc87a007729f3f1376f4462eb424ded
     * [eth:60, shh:2]
     * packet type 16
     * packet f8453c7b80a0fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdcaa0fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdca
     * packet type 24
     * packet c102
     * packet type 3
     * packet c0
     * packet type 1
     * packet c180
    </pre> *
     */
    @Throws(IOException::class)
    private fun doHandshake(host: String, port: Int, remoteIdHex: String) {
        val remoteId = Hex.decode(remoteIdHex)
        val initiator = EncryptionHandshake(ECKey.fromNodeId(remoteId).pubKeyPoint)
        val sock = Socket(host, port)
        val inp = sock.getInputStream()
        val out = sock.getOutputStream()
        val initiateMessage = initiator.createAuthInitiate(null, myKey)
        val initiatePacket = initiator.encryptAuthMessage(initiateMessage)

        out.write(initiatePacket)
        val responsePacket = ByteArray(AuthResponseMessage.length + ECIESCoder.getOverhead())
        val n = inp.read(responsePacket)
        if (n < responsePacket.size)
            throw IOException("could not read, got " + n)

        initiator.handleAuthResponse(myKey, initiatePacket, responsePacket)
        val buf = ByteArray(initiator.secrets.getEgressMac().digestSize)
        KeccakDigest(initiator.secrets.getEgressMac()).doFinal(buf, 0)
        KeccakDigest(initiator.secrets.getIngressMac()).doFinal(buf, 0)

        val conn = RlpxConnection(initiator.secrets, inp, out)
        val handshakeMessage = HandshakeMessage(
                3,
                "computronium1",
                Lists.newArrayList(
                        Capability("eth", 60.toByte()),
                        Capability("shh", 2.toByte())
                ),
                3333,
                nodeId
        )

        conn.sendProtocolHandshake(handshakeMessage)
        conn.handleNextMessage()
        if (!Arrays.equals(remoteId, conn.handshakeMessage!!.nodeId))
            throw IOException("returns node ID doesn't match the node ID we dialed to")
        println(conn.handshakeMessage!!.caps)
        conn.writeMessage(PingMessage())
        conn.writeMessage(DisconnectMessage(ReasonCode.PEER_QUITING))
        conn.handleNextMessage()

        while (true) {
            try {
                conn.handleNextMessage()
            } catch (e: EOFException) {
                break
            }

        }


        this.secrets = initiator.secrets
    }


    private fun delay(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (e: InterruptedException) {
            Throwables.propagate(e)
        }

    }

    companion object {

        @Throws(IOException::class, URISyntaxException::class)
        @JvmStatic fun main(args: Array<String>) {
            val uri = URI(args[0])
            if (uri.scheme != "enode")
                throw RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT")

            Handshaker().doHandshake(uri.host, uri.port, uri.userInfo)
        }
    }
}
