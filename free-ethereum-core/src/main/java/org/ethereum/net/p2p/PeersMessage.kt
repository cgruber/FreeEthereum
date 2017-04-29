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

import org.ethereum.util.ByteUtil
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.spongycastle.util.encoders.Hex
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Wrapper around an Ethereum Peers message on the network

 * @see org.ethereum.net.p2p.P2pMessageCodes.PEERS
 */
class PeersMessage : P2pMessage {

    private var peers: MutableSet<Peer>? = null

    constructor(payload: ByteArray) : super(payload)

    constructor(peers: MutableSet<Peer>) {
        this.peers = peers
        parsed = true
    }

    private fun parse() {
        val paramsList = RLP.decode2(encoded)[0] as RLPList

        peers = LinkedHashSet<Peer>()
        for (i in 1..paramsList.size - 1) {
            val peerParams = paramsList[i] as RLPList
            val ipBytes = peerParams[0].rlpData
            val portBytes = peerParams[1].rlpData
            val peerIdRaw = peerParams[2].rlpData

            try {
                val peerPort = ByteUtil.byteArrayToInt(portBytes)
                val address = InetAddress.getByAddress(ipBytes)

                val peerId = if (peerIdRaw == null) "" else Hex.toHexString(peerIdRaw)
                val peer = Peer(address, peerPort, peerId)
                peers!!.add(peer)
            } catch (e: UnknownHostException) {
                throw RuntimeException("Malformed ip", e)
            }

        }
        this.parsed = true
    }

    private fun encode() {
        val encodedByteArrays = arrayOfNulls<ByteArray>(this.peers!!.size + 1)
        encodedByteArrays[0] = RLP.encodeByte(this.command.asByte())
        val peerList = ArrayList(this.peers!!)
        for (i in peerList.indices) {
            encodedByteArrays[i + 1] = peerList[i].encoded
        }
        this.encoded = RLP.encodeList(*encodedByteArrays)
    }

    override fun getEncoded(): ByteArray {
        if (encoded == null) encode()
        return encoded
    }

    fun getPeers(): Set<Peer> {
        if (!parsed) this.parse()
        return peers!!
    }

    override fun getCommand(): P2pMessageCodes {
        return P2pMessageCodes.PEERS
    }

    override fun getAnswerMessage(): Class<*>? {
        return null
    }

    override fun toString(): String {
        if (!parsed) this.parse()

        val sb = StringBuilder()
        for (peerData in peers!!) {
            sb.append("\n       ").append(peerData)
        }
        return "[" + this.command.name + sb.toString() + "]"
    }
}