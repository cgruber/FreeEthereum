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

package org.ethereum.net.message

import org.ethereum.config.SystemProperties
import org.ethereum.net.client.ConfigCapabilities
import org.ethereum.net.p2p.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.regex.Pattern

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.

 * @author Roman Mandeleil
 * *
 * @since 13.04.14
 */
@Component
open class StaticMessages {
    @Autowired
    private val config: SystemProperties? = null
    @Autowired
    private val configCapabilities: ConfigCapabilities? = null

    fun createHelloMessage(peerId: String): HelloMessage {
        return createHelloMessage(peerId, config!!.listenPort())
    }

    private fun createHelloMessage(peerId: String, listenPort: Int): HelloMessage {

        val helloAnnouncement = buildHelloAnnouncement()
        val p2pVersion = config!!.defaultP2PVersion().toByte()
        val capabilities = configCapabilities!!.configCapabilities

        return HelloMessage(p2pVersion, helloAnnouncement,
                capabilities, listenPort, peerId)
    }

    private fun buildHelloAnnouncement(): String {
        val version = config!!.projectVersion()
        var numberVersion = version
        val pattern = Pattern.compile("^\\d+(\\.\\d+)*")
        val matcher = pattern.matcher(numberVersion)
        if (matcher.find()) {
            numberVersion = numberVersion.substring(matcher.start(), matcher.end())
        }
        var system = System.getProperty("os.name")
        if (system.contains(" "))
            system = system.substring(0, system.indexOf(" "))
        if (System.getProperty("java.vm.vendor").contains("Android"))
            system = "Android"
        val phrase = config.helloPhrase()

        return String.format("Ethereum(J)/v%s/%s/%s/Java/%s", numberVersion, system,
                if (config.projectVersionModifier().equals("release", ignoreCase = true)) "Release" else "Dev", phrase)
    }

    companion object {

        val PING_MESSAGE = PingMessage()
        val PONG_MESSAGE = PongMessage()
        val GET_PEERS_MESSAGE = GetPeersMessage()
        val DISCONNECT_MESSAGE = DisconnectMessage(ReasonCode.REQUESTED)
    }
}
