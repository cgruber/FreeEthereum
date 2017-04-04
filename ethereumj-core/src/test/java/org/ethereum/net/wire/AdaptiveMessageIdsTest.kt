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

package org.ethereum.net.wire

import org.ethereum.net.client.Capability
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.eth.message.EthMessageCodes
import org.ethereum.net.p2p.P2pMessageCodes
import org.ethereum.net.rlpx.MessageCodesResolver
import org.ethereum.net.shh.ShhHandler
import org.ethereum.net.shh.ShhMessageCodes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Roman Mandeleil
 * *
 * @since 15.10.2014
 */
class AdaptiveMessageIdsTest {

    private var messageCodesResolver: MessageCodesResolver? = null

    @Before
    fun setUp() {
        messageCodesResolver = MessageCodesResolver()
    }

    @Test
    fun test1() {

        assertEquals(7, P2pMessageCodes.values().size.toLong())

        assertEquals(0, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.HELLO.asByte()).toLong())
        assertEquals(1, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.DISCONNECT.asByte()).toLong())
        assertEquals(2, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.PING.asByte()).toLong())
        assertEquals(3, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.PONG.asByte()).toLong())
        assertEquals(4, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.GET_PEERS.asByte()).toLong())
        assertEquals(5, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.PEERS.asByte()).toLong())
        assertEquals(15, messageCodesResolver!!.withP2pOffset(P2pMessageCodes.USER.asByte()).toLong())
    }

    @Test
    fun test2() {

        assertEquals(8, EthMessageCodes.values(EthVersion.V62).size.toLong())

        assertEquals(0, EthMessageCodes.STATUS.asByte().toLong())
        assertEquals(1, EthMessageCodes.NEW_BLOCK_HASHES.asByte().toLong())
        assertEquals(2, EthMessageCodes.TRANSACTIONS.asByte().toLong())
        assertEquals(3, EthMessageCodes.GET_BLOCK_HEADERS.asByte().toLong())
        assertEquals(4, EthMessageCodes.BLOCK_HEADERS.asByte().toLong())
        assertEquals(5, EthMessageCodes.GET_BLOCK_BODIES.asByte().toLong())
        assertEquals(6, EthMessageCodes.BLOCK_BODIES.asByte().toLong())
        assertEquals(7, EthMessageCodes.NEW_BLOCK.asByte().toLong())

        messageCodesResolver!!.setEthOffset(0x10)

        assertEquals((0x10 + 0).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x10 + 1).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK_HASHES.asByte()).toLong())
        assertEquals((0x10 + 2).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.TRANSACTIONS.asByte()).toLong())
        assertEquals((0x10 + 3).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 4).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 5).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 6).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 7).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK.asByte()).toLong())
    }

    @Test
    fun test3() {

        assertEquals(3, ShhMessageCodes.values().size.toLong())

        assertEquals(0, ShhMessageCodes.STATUS.asByte().toLong())
        assertEquals(1, ShhMessageCodes.MESSAGE.asByte().toLong())
        assertEquals(2, ShhMessageCodes.FILTER.asByte().toLong())

        messageCodesResolver!!.setShhOffset(0x20)

        assertEquals((0x20 + 0).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x20 + 1).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.MESSAGE.asByte()).toLong())
        assertEquals((0x20 + 2).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.FILTER.asByte()).toLong())
    }

    @Test
    fun test4() {

        val capabilities = Arrays.asList(
                Capability(Capability.ETH, EthVersion.V62.code),
                Capability(Capability.SHH, ShhHandler.VERSION))

        messageCodesResolver!!.init(capabilities)

        assertEquals((0x10 + 0).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x10 + 1).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK_HASHES.asByte()).toLong())
        assertEquals((0x10 + 2).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.TRANSACTIONS.asByte()).toLong())
        assertEquals((0x10 + 3).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 4).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 5).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 6).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 7).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK.asByte()).toLong())

        assertEquals((0x18 + 0).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x18 + 1).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.MESSAGE.asByte()).toLong())
        assertEquals((0x18 + 2).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.FILTER.asByte()).toLong())
    }

    @Test // Capabilities should be read in alphabetical order
    fun test5() {

        val capabilities = Arrays.asList(
                Capability(Capability.SHH, ShhHandler.VERSION),
                Capability(Capability.ETH, EthVersion.V62.code))

        messageCodesResolver!!.init(capabilities)

        assertEquals((0x10 + 0).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x10 + 1).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK_HASHES.asByte()).toLong())
        assertEquals((0x10 + 2).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.TRANSACTIONS.asByte()).toLong())
        assertEquals((0x10 + 3).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 4).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_HEADERS.asByte()).toLong())
        assertEquals((0x10 + 5).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.GET_BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 6).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.BLOCK_BODIES.asByte()).toLong())
        assertEquals((0x10 + 7).toLong(), messageCodesResolver!!.withEthOffset(EthMessageCodes.NEW_BLOCK.asByte()).toLong())

        assertEquals((0x18 + 0).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.STATUS.asByte()).toLong())
        assertEquals((0x18 + 1).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.MESSAGE.asByte()).toLong())
        assertEquals((0x18 + 2).toLong(), messageCodesResolver!!.withShhOffset(ShhMessageCodes.FILTER.asByte()).toLong())
    }
}
