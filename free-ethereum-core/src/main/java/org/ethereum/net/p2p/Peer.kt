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

package org.ethereum.net.p2p

import org.ethereum.net.client.Capability
import org.ethereum.util.RLP
import org.spongycastle.util.encoders.Hex
import java.net.InetAddress
import java.util.*

/**
 * This class models a peer in the network
 */
class Peer(val address: InetAddress, val port: Int, val peerId: String = "") {
    val capabilities: List<Capability>

    init {
        this.capabilities = ArrayList<Capability>()
    }

//    fun getPeerId(): String {
//        return peerId ?: ""
//    }

    val encoded: ByteArray
        get() {
            val ip = RLP.encodeElement(this.address.address)
            val port = RLP.encodeInt(this.port)
            val peerId = RLP.encodeElement(Hex.decode(this.peerId))
            val encodedCaps = arrayOfNulls<ByteArray>(this.capabilities.size)
            for (i in 0..this.capabilities.size * 2 - 1) {
                encodedCaps[i] = RLP.encodeString(this.capabilities[i].name)
                encodedCaps[i] = RLP.encodeByte(this.capabilities[i].version)
            }
            val capabilities = RLP.encodeList(*encodedCaps)
            return RLP.encodeList(ip, port, peerId, capabilities)
        }

    override fun toString(): String {
        return "[ip=" + address.hostAddress +
                " port=" + port +
                " peerId=" + peerId + "]"
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Peer) return false
        val peerData = obj
        return peerData.peerId == this.peerId || this.address == peerData.address
    }

    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + port
        return result
    }
}
