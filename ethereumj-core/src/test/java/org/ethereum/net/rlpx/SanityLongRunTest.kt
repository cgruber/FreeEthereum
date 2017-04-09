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

import com.typesafe.config.ConfigFactory
import org.ethereum.config.NoAutoscan
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.TransactionReceipt
import org.ethereum.crypto.ECKey
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.message.StatusMessage
import org.ethereum.net.message.Message
import org.ethereum.net.server.Channel
import org.ethereum.net.shh.MessageWatcher
import org.ethereum.net.shh.WhisperMessage
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Anton Nashatyrev on 13.10.2015.
 */
@Ignore
open class SanityLongRunTest {

    private val config1 =
            "peer.discovery.enabled = true \n" +
                    //            "peer.discovery.enabled = false \n" +
                    "database.dir = testDB-1 \n" +
                    "database.reset = true \n" +
                    "sync.enabled = true \n" +
                    //            "sync.enabled = false \n" +
                    "peer.capabilities = [eth, shh] \n" +
                    "peer.listen.port = 60300 \n" +
                    " # derived nodeId = deadbeea2250b3efb9e6268451e74bdbdc5632a1a03a0f5b626f59150ff772ac287e122531b5e8d55ff10cb541bbc8abf5def6bcbfa31cf5923ca3c3d783d312\n" +
                    "peer.privateKey = d3a4a240b107ab443d46187306d0b947ce3d6b6ed95aead8c4941afcebde43d2\n" +
                    "peer.p2p.version = 4 \n" +
                    "peer.p2p.framing.maxSize = 1024 \n"
    private val config2Key = ECKey()
    private val config2 =
            "peer.discovery.enabled = false \n" +
                    "database.dir = testDB-2 \n" +
                    "database.reset = true \n" +
                    "sync.enabled = true \n" +
                    "peer.capabilities = [eth, shh] \n" +
                    //            "peer.listen.port = 60300 \n" +
                    "peer.privateKey = " + Hex.toHexString(config2Key.privKeyBytes!!) + "\n" +
                    "peer { active = [" +
                    "   { url = \"enode://deadbeea2250b3efb9e6268451e74bdbdc5632a1a03a0f5b626f59150ff772ac287e122531b5e8d55ff10cb541bbc8abf5def6bcbfa31cf5923ca3c3d783d312" +
                    "@localhost:60300\" }" +
                    "] } \n" +
                    "peer.p2p.version = 5 \n" +
                    "peer.p2p.framing.maxSize = 1024 \n"

    @Test
    @Throws(FileNotFoundException::class, InterruptedException::class)
    fun testTest() {
        SysPropConfig1.props = SystemProperties(ConfigFactory.parseString(config1))
        SysPropConfig2.props = SystemProperties(ConfigFactory.parseString(config2))

        //        Ethereum ethereum1 = EthereumFactory.createEthereum(SysPropConfig1.props, SysPropConfig1.class);
        val ethereum1: Ethereum? = null

        //        Thread.sleep(1000000000);

        val ethereum2 = EthereumFactory.createEthereum(SysPropConfig2.props, SysPropConfig2::class.java)

        val semaphore = CountDownLatch(1)

        ethereum2.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is StatusMessage) {
                    println("=== Status received: " + message)
                    semaphore.countDown()
                }
            }

        })

        semaphore.await(60, TimeUnit.SECONDS)
        if (semaphore.count > 0) {
            throw RuntimeException("StatusMessage was not received in 60 sec: " + semaphore.count)
        }

        val semaphoreBlocks = CountDownLatch(1)
        val semaphoreFirstBlock = CountDownLatch(1)

        ethereum2.addListener(object : EthereumListenerAdapter() {
            internal var blocksCnt = 0

            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                blocksCnt++
                if (blocksCnt % 1000 == 0 || blocksCnt == 1) {
                    println("=== Blocks imported: " + blocksCnt)
                    if (blocksCnt == 1) {
                        semaphoreFirstBlock.countDown()
                    }
                }
                if (blocksCnt >= 10000) {
                    semaphoreBlocks.countDown()
                    println("=== Blocks task done.")
                }
            }
        })

        semaphoreFirstBlock.await(180, TimeUnit.SECONDS)
        if (semaphoreFirstBlock.count > 0) {
            throw RuntimeException("No blocks were received in 60 sec: " + semaphore.count)
        }

        // SHH messages exchange
        val identity1 = ethereum1!!.whisper.newIdentity()
        val identity2 = ethereum2.whisper.newIdentity()

        val counter1 = IntArray(1)
        val counter2 = IntArray(1)

        ethereum1.whisper.watch(object : MessageWatcher(identity1, null, null) {
            override fun newMessage(msg: WhisperMessage) {
                println("=== You have a new message to 1: " + msg)
                counter1[0]++
            }
        })
        ethereum2.whisper.watch(object : MessageWatcher(identity2, null, null) {
            override fun newMessage(msg: WhisperMessage) {
                println("=== You have a new message to 2: " + msg)
                counter2[0]++
            }
        })

        println("=== Sending messages ... ")
        var cnt = 0
        val end = System.currentTimeMillis() + 60 * 60 * 1000
        while (semaphoreBlocks.count > 0) {
            ethereum1.whisper.send(identity2, "Hello Eth2!".toByteArray(), null)
            ethereum2.whisper.send(identity1, "Hello Eth1!".toByteArray(), null)
            cnt++
            Thread.sleep((10 * 1000).toLong())
            if (counter1[0] != cnt || counter2[0] != cnt) {
                throw RuntimeException("Message was not delivered in 10 sec: " + cnt)
            }
            if (System.currentTimeMillis() > end) {
                throw RuntimeException("Wanted blocks amount was not received in a hour")
            }
        }

        ethereum1.close()
        ethereum2.close()

        println("Passed.")
    }

    @Configuration
    @NoAutoscan
    open class SysPropConfig1 {

        @Bean
        open fun systemProperties(): SystemProperties {
            return props!!
        }

        companion object {
            internal var props: SystemProperties? = null
        }
    }

    @Configuration
    @NoAutoscan
    open class SysPropConfig2 {

        @Bean
        open fun systemProperties(): SystemProperties {
            return props!!
        }

        companion object {
            internal var props: SystemProperties? = null
        }
    }
}
