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

import org.ethereum.net.client.Capability
import org.ethereum.net.p2p.Peer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.net.InetAddress
import java.util.*

class PeerTest {

    /* PEER */

    @Test
    fun testPeer() {

        //Init
        val address = InetAddress.getLoopbackAddress()
        val capabilities = ArrayList<Capability>()
        val port = 1010
        val peerId = "1010"
        val peerCopy = Peer(address, port, peerId)

        //Peer
        val peer = Peer(address, port, peerId)

        //getAddress
        assertEquals("127.0.0.1", peer.address.hostAddress)

        //getPort
        assertEquals(port.toLong(), peer.port.toLong())

        //getPeerId
        assertEquals(peerId, peer.peerId)

        //getCapabilities
        assertEquals(capabilities, peer.capabilities)

        //getEncoded
        assertEquals("CC847F0000018203F2821010C0", Hex.toHexString(peer.encoded).toUpperCase())

        //toString
        assertEquals("[ip=" + address.hostAddress + " port=" + Integer.toString(port) + " peerId=" + peerId + "]", peer.toString())

        //equals
        assertEquals(true, peer == peerCopy)
        assertEquals(false, peer == null)

        //hashCode
        assertEquals(-1218913009, peer.hashCode().toLong())
    }
}

