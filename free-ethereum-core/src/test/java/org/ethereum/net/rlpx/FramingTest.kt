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

import org.ethereum.config.NoAutoscan
import org.ethereum.config.SystemProperties
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.server.Channel
import org.junit.Ignore
import org.junit.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Ignore
open class FramingTest {

    @Test
    @Throws(FileNotFoundException::class, InterruptedException::class)
    fun testTest() {
        SysPropConfig1.props = SystemProperties()
        SysPropConfig1.props!!.overrideParams(
                "peer.listen.port", "30334",
                "peer.privateKey", "ba43d10d069f0c41a8914849c1abeeac2a681b21ae9b60a6a2362c06e6eb1bc8",
                "database.dir", "testDB-1")
        SysPropConfig2.props = SystemProperties()
        SysPropConfig2.props!!.overrideParams(
                "peer.listen.port", "30335",
                "peer.privateKey", "d3a4a240b107ab443d46187306d0b947ce3d6b6ed95aead8c4941afcebde43d2",
                "database.dir", "testDB-2")

        val ethereum1 = EthereumFactory.createEthereum(SysPropConfig1.props!!, SysPropConfig1::class.java)
        val ethereum2 = EthereumFactory.createEthereum(SysPropConfig2.props!!, SysPropConfig2::class.java)

        val semaphore = CountDownLatch(2)

        ethereum1.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is StatusMessage) {
                    println("1: -> " + message)
                    semaphore.countDown()
                }
            }
        })
        ethereum2.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is StatusMessage) {
                    println("2: -> " + message)
                    semaphore.countDown()
                }
            }

        })

        ethereum2.connect(Node("enode://a560c55a0a5b5d137c638eb6973812f431974e4398c6644fa0c19181954fab530bb2a1e2c4eec7cc855f6bab9193ea41d6cf0bf2b8b41ed6b8b9e09c072a1e5a" + "@localhost:30334"))

        semaphore.await(60, TimeUnit.SECONDS)

        ethereum1.close()
        ethereum2.close()

        if (semaphore.count > 0) {
            throw RuntimeException("One or both StatusMessage was not received: " + semaphore.count)
        }

        println("Passed.")
    }

    @Configuration
    @NoAutoscan
    open class SysPropConfig1 {

        @Bean
        open fun systemProperties(): SystemProperties {
            return props!!
        }

        @Bean
        @Scope("prototype")
        open fun messageCodec(): MessageCodec {
            val codec = MessageCodec()
            codec.setMaxFramePayloadSize(16)
            println("SysPropConfig1.messageCodec")
            return codec
        }

        companion object {
            internal var props: SystemProperties? = null
        }
    }

    @Configuration
    @NoAutoscan
    open class SysPropConfig2 {

        @Bean
        @Scope("prototype")
        open fun messageCodec(): MessageCodec {
            val codec = MessageCodec()
            codec.setMaxFramePayloadSize(16)
            println("SysPropConfig2.messageCodec")
            return codec
        }

        @Bean
        open fun systemProperties(): SystemProperties {
            return props!!
        }

        companion object {
            internal var props: SystemProperties? = null
        }
    }
}
