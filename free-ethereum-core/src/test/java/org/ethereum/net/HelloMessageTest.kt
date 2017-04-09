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
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.p2p.P2pHandler
import org.ethereum.net.p2p.P2pMessageCodes
import org.ethereum.net.shh.ShhHandler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.util.*

class HelloMessageTest {

    //Parsing from raw bytes
    @Test
    fun test1() {
        val helloMessageRaw = "f87902a5457468657265756d282b2b292f76302e372e392f52656c656173652f4c696e75782f672b2bccc58365746827c583736868018203e0b8401fbf1e41f08078918c9f7b6734594ee56d7f538614f602c71194db0a1af5a77f9b86eb14669fe7a8a46a2dd1b7d070b94e463f4ecd5b337c8b4d31bbf8dd5646"

        val payload = Hex.decode(helloMessageRaw)
        val helloMessage = HelloMessage(payload)
        logger.info(helloMessage.toString())

        assertEquals(P2pMessageCodes.HELLO, helloMessage.command)
        assertEquals(2, helloMessage.p2PVersion.toLong())
        assertEquals("Ethereum(++)/v0.7.9/Release/Linux/g++", helloMessage.clientId)
        assertEquals(2, helloMessage.capabilities.size.toLong())
        assertEquals(992, helloMessage.listenPort.toLong())
        assertEquals(
                "1fbf1e41f08078918c9f7b6734594ee56d7f538614f602c71194db0a1af5a77f9b86eb14669fe7a8a46a2dd1b7d070b94e463f4ecd5b337c8b4d31bbf8dd5646",
                helloMessage.peerId)
    }

    //Instantiate from constructor
    @Test
    fun test2() {

        //Init
        val version: Byte = 2
        val clientStr = "Ethereum(++)/v0.7.9/Release/Linux/g++"
        val capabilities = Arrays.asList(
                Capability(Capability.ETH, EthVersion.UPPER),
                Capability(Capability.SHH, ShhHandler.VERSION),
                Capability(Capability.P2P, P2pHandler.VERSION))
        val listenPort = 992
        val peerId = "1fbf1e41f08078918c9f7b6734594ee56d7f538614f602c71194db0a1af5a"

        val helloMessage = HelloMessage(version, clientStr, capabilities, listenPort, peerId)
        logger.info(helloMessage.toString())

        assertEquals(P2pMessageCodes.HELLO, helloMessage.command)
        assertEquals(version.toLong(), helloMessage.p2PVersion.toLong())
        assertEquals(clientStr, helloMessage.clientId)
        assertEquals(3, helloMessage.capabilities.size.toLong())
        assertEquals(listenPort.toLong(), helloMessage.listenPort.toLong())
        assertEquals(peerId, helloMessage.peerId)
    }

    //Fail test
    @Test
    fun test3() {
        //Init
        val version: Byte = -1 //invalid version
        val clientStr = "" //null id
        val capabilities = Arrays.asList<Capability>(
                Capability(null, 0.toByte()),
                Capability(null, 0.toByte()), null, //null here causes NullPointerException when using toString
                Capability(null, 0.toByte())) //encoding null capabilities
        val listenPort = 99999 //invalid port
        val peerId = "" //null id

        val helloMessage = HelloMessage(version, clientStr, capabilities, listenPort, peerId)

        assertEquals(P2pMessageCodes.HELLO, helloMessage.command)
        assertEquals(version.toLong(), helloMessage.p2PVersion.toLong())
        assertEquals(clientStr, helloMessage.clientId)
        assertEquals(4, helloMessage.capabilities.size.toLong())
        assertEquals(listenPort.toLong(), helloMessage.listenPort.toLong())
        assertEquals(peerId, helloMessage.peerId)
    }

    companion object {

        /* HELLO_MESSAGE */
        private val logger = LoggerFactory.getLogger("test")
    }
}
