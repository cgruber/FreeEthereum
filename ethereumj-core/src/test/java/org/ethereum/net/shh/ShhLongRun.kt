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

package org.ethereum.net.shh

import org.apache.commons.lang3.tuple.Pair
import org.ethereum.config.NoAutoscan
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.manager.WorldManager
import org.ethereum.net.p2p.HelloMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*
import javax.annotation.PostConstruct

/**
 * This is not a JUnit test but rather a long running standalone test for messages exchange with another peer.
 * To start it another peer with JSON PRC API should be started.
 * E.g. the following is the cmd for starting up C++ eth:

 * > eth --no-bootstrap --no-discovery --listen 10003 --shh --json-rpc

 * If the eth is running on a remote host:port the appropriate constants in the test need to be updated

 * Created by Anton Nashatyrev on 05.10.2015.
 */
@Ignore
open class ShhLongRun : Thread() {

    @Test
    @Throws(Exception::class)
    fun test() {
        //        remoteJsonRpc = new URL("http://whisper-1.ether.camp:8545");
        //        Node node = new Node("enode://52994910050f13cbd7848f02709f2f5ebccc363f13dafc4ec201e405e2f143ebc9c440935b3217073f6ec47f613220e0bc6b7b34274b7d2de125b82a2acd34ee" +
        //                "@whisper-1.ether.camp:30303");

        remoteJsonRpc = URL("http://localhost:8545")
        val node = Node("enode://6ed738b650ac2b771838506172447dc683b7e9dae7b91d699a48a0f94651b1a0d2e2ef01c6fffa22f762aaa553286047f0b0bb39f2e3a24b2a18fe1b9637dcbe" + "@localhost:10003")

        val ethereum = EthereumFactory.createEthereum(Config::class.java)
        ethereum.connect(
                node.host,
                node.port,
                Hex.toHexString(node.id))
        Thread.sleep(1000000000)
    }

    @Configuration
    @NoAutoscan
    private open class Config {

        @Bean
        open fun testBean(): TestComponent {
            return TestComponent()
        }
    }

    @Component
    @NoAutoscan
    open class TestComponent : Thread() {

        @Autowired
        internal var worldManager: WorldManager? = null

        @Autowired
        internal var ethereum: Ethereum? = null


        @Autowired
        internal var whisper: Whisper? = null

        internal var remoteWhisper: Whisper? = null

        @PostConstruct
        internal fun init() {
            println("========= init")
            worldManager!!.addListener(object : EthereumListenerAdapter() {
                override fun onHandShakePeer(channel: Channel, helloMessage: HelloMessage) {
                    println("========= onHandShakePeer")
                    if (!isAlive) {
                        start()
                    }
                }
            })
        }

        override fun run() {

            try {
                remoteWhisper = JsonRpcWhisper(remoteJsonRpc)
                val whisper = this.whisper
                //            Whisper whisper = new JsonRpcWhisper(remoteJsonRpc1);

                println("========= Waiting for SHH init")
                Thread.sleep((1 * 1000).toLong())

                println("========= Running")


                val localKey1 = whisper!!.newIdentity()
                val localKey2 = whisper.newIdentity()
                val remoteKey1 = remoteWhisper?.newIdentity()
                val remoteKey2 = remoteWhisper?.newIdentity()

                val localTopic = "LocalTopic"
                val remoteTopic = "RemoteTopic"

                val localMatcherBroad = MessageMatcher(null, null, Topic.createTopics(remoteTopic))
                val remoteMatcherBroad = MessageMatcher(null, null, Topic.createTopics(localTopic))
                val localMatcherTo = MessageMatcher(localKey1, null, null)
                val localMatcherToFrom = MessageMatcher(localKey2, remoteKey2, null)
                val remoteMatcherTo = MessageMatcher(remoteKey1, null, Topic.createTopics("aaa"))
                val remoteMatcherToFrom = MessageMatcher(remoteKey2, localKey2, Topic.createTopics("aaa"))

                whisper.watch(localMatcherBroad)
                whisper.watch(localMatcherTo)
                whisper.watch(localMatcherToFrom)
                remoteWhisper?.watch(remoteMatcherBroad)
                remoteWhisper?.watch(remoteMatcherTo)
                remoteWhisper?.watch(remoteMatcherToFrom)

                var cnt = 0
                while (true) {
                    run {
                        val msg = WhisperMessage()
                                .setPayload("local-" + cnt)
                                .setTopics(*Topic.createTopics(localTopic))
                        remoteMatcherBroad.waitForMessage(msg)
                        whisper.send(msg.payload, msg.topics)
                    }
                    run {
                        val msg = WhisperMessage()
                                .setPayload("remote-" + cnt)
                                .setTopics(*Topic.createTopics(remoteTopic))
                        localMatcherBroad.waitForMessage(msg)
                        remoteWhisper?.send(msg.payload, msg.topics)
                    }
                    run {
                        val msg = WhisperMessage()
                                .setPayload("local-to-" + cnt)
                                .setTo(remoteKey1)
                                .setTopics(*Topic.createTopics("aaa"))
                        remoteMatcherTo.waitForMessage(msg)
                        whisper.send(msg.to, msg.payload, msg.topics)
                    }
                    run {
                        val msg = WhisperMessage()
                                .setPayload("remote-to-" + cnt)
                                .setTo(localKey1)
                        localMatcherTo.waitForMessage(msg)
                        remoteWhisper?.send(msg.to, msg.payload, Topic.createTopics())
                    }
                    run {
                        val msg = WhisperMessage()
                                .setPayload("local-to-from-" + cnt)
                                .setTo(remoteKey2)
                                .setFrom(localKey2)
                                .setTopics(*Topic.createTopics("aaa"))
                        remoteMatcherToFrom.waitForMessage(msg)
                        whisper.send(msg.from, msg.to, msg.payload, msg.topics)
                    }
                    run {
                        val msg = WhisperMessage()
                                .setPayload("remote-to-from-" + cnt)
                                .setTo(localKey2)
                                .setFrom(remoteKey2)
                        localMatcherToFrom.waitForMessage(msg)
                        remoteWhisper?.send(msg.from, msg.to, msg.payload, msg.topics)
                    }

                    Thread.sleep(1000)
                    cnt++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        internal class MessageMatcher(to: String?, from: String?, topics: Array<Topic>?) : MessageWatcher(to, from, topics) {
            val awaitedMsgs: MutableList<Pair<Date, WhisperMessage>> = ArrayList()

            @Synchronized override fun newMessage(msg: WhisperMessage) {
                println("=== Msg received: " + msg)
                awaitedMsgs
                        .filter { Arrays.equals(msg.payload, it.right.payload) }
                        .forEach {
                            if (!match(msg, it.right)) {
                                throw RuntimeException("Messages not matched: \n" + it + "\n" + msg)
                            } else {
                                awaitedMsgs.remove(it)
                                return
                            }
                        }
                checkForMissingMessages()
            }

            private fun equal(o1: Any?, o2: Any?): Boolean {
                if (o1 == null) return o2 == null
                return o1 == o2
            }

            fun match(m1: WhisperMessage, m2: WhisperMessage): Boolean {
                if (!Arrays.equals(m1.payload, m2.payload)) return false
                if (!equal(m1.from, m2.from)) return false
                if (!equal(m1.to, m2.to)) return false
                if (m1.topics != null) {
                    if (m1.topics.size != m2.topics.size) return false
                    (0..m1.topics.size - 1)
                            .filter { m1.topics[it] != m2.topics[it] }
                            .forEach { return false }
                } else if (m2.topics != null) return false
                return true
            }

            @Synchronized fun waitForMessage(msg: WhisperMessage) {
                checkForMissingMessages()
                awaitedMsgs.add(Pair.of(Date(), msg))
            }

            private fun checkForMissingMessages() {
                awaitedMsgs
                        .filter { System.currentTimeMillis() > it.left.time + 10 * 1000 }
                        .forEach { throw RuntimeException("Message was not delivered: " + it) }
            }
        }
    }

    companion object {
        private var remoteJsonRpc: URL? = null

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            ShhLongRun().test()
        }
    }
}
