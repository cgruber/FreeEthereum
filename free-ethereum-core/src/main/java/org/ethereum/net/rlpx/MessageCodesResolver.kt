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

import org.ethereum.net.client.Capability
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.eth.message.EthMessageCodes
import org.ethereum.net.p2p.P2pMessageCodes
import org.ethereum.net.shh.ShhMessageCodes
import org.ethereum.net.swarm.bzz.BzzMessageCodes
import java.util.*

class MessageCodesResolver {

    private val offsets = HashMap<String, Int>()

    constructor()

    constructor(caps: List<Capability>) {
        init(caps)
    }

    fun init(caps: List<Capability>) {
        Collections.sort(caps)
        var offset = P2pMessageCodes.USER.asByte() + 1

        for (capability in caps) {
            if (capability.name == Capability.ETH) {
                setEthOffset(offset)
                val v = EthVersion.fromCode(capability.version.toInt())
                offset += EthMessageCodes.values(v!!).size
            }

            if (capability.name == Capability.SHH) {
                setShhOffset(offset)
                offset += ShhMessageCodes.values().size
            }

            if (capability.name == Capability.BZZ) {
                setBzzOffset(offset)
                offset += BzzMessageCodes.values().size + 4
                // FIXME: for some reason Go left 4 codes between BZZ and ETH message codes
            }
        }
    }

    fun withP2pOffset(code: Byte): Byte {
        return withOffset(code, Capability.P2P)
    }

    fun withBzzOffset(code: Byte): Byte {
        return withOffset(code, Capability.BZZ)
    }

    fun withEthOffset(code: Byte): Byte {
        return withOffset(code, Capability.ETH)
    }

    fun withShhOffset(code: Byte): Byte {
        return withOffset(code, Capability.SHH)
    }

    private fun withOffset(code: Byte, cap: String): Byte {
        val offset = getOffset(cap)
        return (code + offset).toByte()
    }

    fun resolveP2p(code: Byte): Byte {
        return resolve(code, Capability.P2P)
    }

    fun resolveBzz(code: Byte): Byte {
        return resolve(code, Capability.BZZ)
    }

    fun resolveEth(code: Byte): Byte {
        return resolve(code, Capability.ETH)
    }

    fun resolveShh(code: Byte): Byte {
        return resolve(code, Capability.SHH)
    }

    private fun resolve(code: Byte, cap: String): Byte {
        val offset = getOffset(cap)
        return (code - offset).toByte()
    }

    private fun getOffset(cap: String): Byte {
        val offset = offsets[cap]
        return offset?.toByte() ?: 0
    }

    private fun setBzzOffset(offset: Int) {
        setOffset(Capability.BZZ, offset)
    }

    fun setEthOffset(offset: Int) {
        setOffset(Capability.ETH, offset)
    }

    fun setShhOffset(offset: Int) {
        setOffset(Capability.SHH, offset)
    }

    private fun setOffset(cap: String, offset: Int) {
        offsets.put(cap, offset)
    }
}
