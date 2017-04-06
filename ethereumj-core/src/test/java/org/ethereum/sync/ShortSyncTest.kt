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

package org.ethereum.sync

import org.ethereum.config.NoAutoscan
import org.ethereum.config.SystemProperties
import org.ethereum.config.net.MainNetConfig
import org.ethereum.core.Block
import org.ethereum.core.BlockHeader
import org.ethereum.core.Blockchain
import org.ethereum.core.TransactionReceipt
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.handler.Eth62
import org.ethereum.net.eth.handler.EthHandler
import org.ethereum.net.eth.message.*
import org.ethereum.net.message.Message
import org.ethereum.net.p2p.DisconnectMessage
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.Channel
import org.ethereum.util.FileUtil.recursiveDelete
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.*
import org.junit.Assert.fail
import org.spongycastle.util.encoders.Hex.decode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Mikhail Kalinin
 * *
 * @since 14.12.2015
 */
@Ignore("Long network tests")
class ShortSyncTest {

    private var ethereumA: Ethereum? = null
    private var ethereumB: Ethereum? = null
    private var ethA: EthHandler? = null
    private var testDbA: String? = null
    private var testDbB: String? = null

    @Before
    @Throws(InterruptedException::class)
    fun setupTest() {
        testDbA = "test_db_" + BigInteger(32, Random())
        testDbB = "test_db_" + BigInteger(32, Random())

        SysPropConfigA.props.setDataBaseDir(testDbA)
        SysPropConfigB.props.setDataBaseDir(testDbB)
    }

    @After
    fun cleanupTest() {
        recursiveDelete(testDbA)
        recursiveDelete(testDbB)
        SysPropConfigA.eth62 = null
    }

    // positive gap, A on main, B on main
    // expected: B downloads missed blocks from A => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test1() {

        setupPeers()

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10, B == genesis

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // positive gap, A on fork, B on main
    // positive gap, A on fork, B on fork (same story)
    // expected: B downloads missed blocks from A => B on A's fork
    @Test
    @Throws(InterruptedException::class)
    fun test2() {

        setupPeers()

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b8', B == genesis

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b8_)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlock(b8_)

        semaphore.await(10, SECONDS)

        // check if B == b8'
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // positive gap, A on main, B on fork
    // expected: B finds common ancestor and downloads missed blocks from A => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test3() {

        setupPeers()

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in forkB1B5B8_!!) {
            blockchainB.tryToConnect(b)
        }

        // A == b10, B == b8'

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // negative gap, A on main, B on main
    // expected: B skips A's block as already imported => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test4() {

        setupPeers()

        val b5 = mainB1B10!![4]
        val b9 = mainB1B10!![8]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
            if (b.isEqual(b5)) break
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b9)) break
        }

        // A == b5, B == b9

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b5)

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // negative gap, A on fork, B on main
    // negative gap, A on fork, B on fork (same story)
    // expected: B downloads A's fork and imports it as NOT_BEST => B on its chain
    @Test
    @Throws(InterruptedException::class)
    fun test5() {

        setupPeers()

        val b9 = mainB1B10!![8]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b9)) break
        }

        // A == b8', B == b9

        val semaphore = CountDownLatch(1)
        val semaphoreB8_ = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
                if (block.isEqual(b8_)) {
                    semaphoreB8_.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b8_)

        semaphoreB8_.await(10, SECONDS)
        if (semaphoreB8_.count > 0) {
            fail("PeerB didn't import b8'")
        }

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // negative gap, A on main, B on fork
    // expected: B finds common ancestor and downloads A's blocks => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test6() {

        setupPeers()

        val b7 = mainB1B10!![6]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
            if (b.isEqual(b7)) break
        }

        for (b in forkB1B5B8_!!) {
            blockchainB.tryToConnect(b)
        }

        // A == b7, B == b8'

        val semaphore = CountDownLatch(1)
        val semaphoreB7 = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b7)) {
                    semaphoreB7.countDown()
                }
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b7)

        semaphoreB7.await(10, SECONDS)

        // check if B == b7
        if (semaphoreB7.count > 0) {
            fail("PeerB didn't recover a gap")
        }

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10
        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // positive gap, A on fork, B on main
    // A does a re-branch to main
    // expected: B downloads main blocks from A => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test7() {

        setupPeers()

        val b4 = mainB1B10!![3]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b4)) break
        }

        // A == b8', B == b4

        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is NewBlockMessage) {
                    // it's time to do a re-branch
                    for (b in mainB1B10!!) {
                        blockchainA.tryToConnect(b)
                    }
                }
            }
        })

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlock(b8_)
        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // negative gap, A on fork, B on main
    // A does a re-branch to main
    // expected: B downloads A's fork and imports it as NOT_BEST => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test8() {

        setupPeers()

        val b7_ = forkB1B5B8_!![6]
        val b8 = mainB1B10!![7]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
            if (b.isEqual(b7_)) break
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b8)) break
        }

        // A == b7', B == b8

        val semaphore = CountDownLatch(1)
        val semaphoreB7_ = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b7_)) {
                    // it's time to do a re-branch
                    for (b in mainB1B10!!) {
                        blockchainA.tryToConnect(b)
                    }

                    semaphoreB7_.countDown()
                }
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b7_)

        semaphoreB7_.await(10, SECONDS)
        if (semaphoreB7_.count > 0) {
            fail("PeerB didn't import b7'")
        }

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // positive gap, A on fork, B on main
    // A doesn't send common ancestor
    // expected: B drops A and all its blocks => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test9() {

        // handler which don't send an ancestor
        SysPropConfigA.eth62 = object : Eth62() {
            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                // process init header request correctly
                if (msg.maxHeaders == 1) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = ArrayList<BlockHeader>()
                for (i in 7..mainB1B10!!.size - 1) {
                    headers.add(mainB1B10!![i].header)
                }

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }
        }

        setupPeers()

        val b6 = mainB1B10!![5]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b6)) break
        }

        // A == b8', B == b6

        ethA!!.sendNewBlock(b8_)

        val semaphoreDisconnect = CountDownLatch(1)
        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is DisconnectMessage) {
                    semaphoreDisconnect.countDown()
                }
            }
        })

        semaphoreDisconnect.await(10, SECONDS)

        // check if peer was dropped
        if (semaphoreDisconnect.count > 0) {
            fail("PeerA is not dropped")
        }

        // back to usual handler
        SysPropConfigA.eth62 = null

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        val semaphoreConnect = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onPeerAddedToSyncPool(peer: Channel) {
                semaphoreConnect.countDown()
            }
        })
        ethereumB!!.connect(nodeA)

        // await connection
        semaphoreConnect.await(10, SECONDS)
        if (semaphoreConnect.count > 0) {
            fail("PeerB is not able to connect to PeerA")
        }

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // negative gap, A on fork, B on main
    // A doesn't send the gap block in ancestor response
    // expected: B drops A and all its blocks => B on main
    @Test
    @Throws(InterruptedException::class)
    fun test10() {

        // handler which don't send a gap block
        SysPropConfigA.eth62 = object : Eth62() {
            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (msg.maxHeaders == 1) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = ArrayList<BlockHeader>()
                for (i in 0..forkB1B5B8_!!.size - 1 - 1) {
                    headers.add(forkB1B5B8_!![i].header)
                }

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }
        }

        setupPeers()

        val b9 = mainB1B10!![8]

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b9)) break
        }

        // A == b8', B == b9

        ethA!!.sendNewBlockHashes(b8_)

        val semaphoreDisconnect = CountDownLatch(1)
        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is DisconnectMessage) {
                    semaphoreDisconnect.countDown()
                }
            }
        })

        semaphoreDisconnect.await(10, SECONDS)

        // check if peer was dropped
        if (semaphoreDisconnect.count > 0) {
            fail("PeerA is not dropped")
        }

        // back to usual handler
        SysPropConfigA.eth62 = null

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        val semaphoreConnect = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onPeerAddedToSyncPool(peer: Channel) {
                semaphoreConnect.countDown()
            }
        })
        ethereumB!!.connect(nodeA)

        // await connection
        semaphoreConnect.await(10, SECONDS)
        if (semaphoreConnect.count > 0) {
            fail("PeerB is not able to connect to PeerA")
        }

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10

        ethA!!.sendNewBlock(b10)

        semaphore.await(10, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // A sends block with low TD to B
    // expected: B skips this block
    @Test
    @Throws(InterruptedException::class)
    fun test11() {

        val b5 = mainB1B10!![4]
        val b6_ = forkB1B5B8_!![5]

        setupPeers()

        // A == B == genesis

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b5)) break
        }

        // A == b8', B == b5

        val semaphore1 = CountDownLatch(1)
        val semaphore2 = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b6_)) {
                    if (semaphore1.count > 0) {
                        semaphore1.countDown()
                    } else {
                        semaphore2.countDown()
                    }
                }
            }
        })

        ethA!!.sendNewBlock(b6_)
        semaphore1.await(10, SECONDS)

        if (semaphore1.count > 0) {
            fail("PeerB doesn't accept block with higher TD")
        }

        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
        }

        // B == b10

        ethA!!.sendNewBlock(b6_)

        semaphore2.await(5, SECONDS)

        // check if B skips b6'
        if (semaphore2.count.equals(0)) {
            fail("PeerB doesn't skip block with lower TD")
        }
    }

    // bodies validation: A doesn't send bodies corresponding to headers which were sent previously
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test12() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockBodies(msg: GetBlockBodiesMessage) {
                val bodies = Arrays.asList<ByteArray>(
                        mainB1B10!![0].encodedBody
                )

                val response = BlockBodiesMessage(bodies)
                sendMessage(response)
            }
        }

        setupPeers()

        val blockchainA = ethereumA!!.blockchain as Blockchain

        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10, B == genesis

        val semaphoreDisconnect = CountDownLatch(1)
        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is DisconnectMessage) {
                    semaphoreDisconnect.countDown()
                }
            }
        })

        ethA!!.sendNewBlock(b10)

        semaphoreDisconnect.await(10, SECONDS)

        // check if peer was dropped
        if (semaphoreDisconnect.count > 0) {
            fail("PeerA is not dropped")
        }
    }

    // bodies validation: headers order is incorrect in the response, reverse = true
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test13() {

        val b9 = mainB1B10!![8]

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (msg.maxHeaders == 1) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        forkB1B5B8_!![7].header,
                        forkB1B5B8_!![6].header,
                        forkB1B5B8_!![4].header,
                        forkB1B5B8_!![5].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }
        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b9)) break
        }

        // A == b8', B == b10

        val semaphoreDisconnect = CountDownLatch(1)
        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is DisconnectMessage) {
                    semaphoreDisconnect.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b8_)

        semaphoreDisconnect.await(10, SECONDS)

        // check if peer was dropped
        if (semaphoreDisconnect.count > 0) {
            fail("PeerA is not dropped")
        }
    }

    // bodies validation: ancestor's parent hash and header's hash does not match, reverse = true
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test14() {

        val b9 = mainB1B10!![8]

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (msg.maxHeaders == 1) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        forkB1B5B8_!![7].header,
                        forkB1B5B8_!![6].header,
                        BlockHeader(ByteArray(32), ByteArray(32), ByteArray(32), ByteArray(32), ByteArray(32),
                                6, byteArrayOf(0), 0, 0, ByteArray(0), ByteArray(0), ByteArray(0)),
                        forkB1B5B8_!![4].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        val blockchainA = ethereumA!!.blockchain as Blockchain
        val blockchainB = ethereumB!!.blockchain as Blockchain

        for (b in forkB1B5B8_!!) {
            blockchainA.tryToConnect(b)
        }
        for (b in mainB1B10!!) {
            blockchainB.tryToConnect(b)
            if (b.isEqual(b9)) break
        }

        // A == b8', B == b10

        val semaphoreDisconnect = CountDownLatch(1)
        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onRecvMessage(channel: Channel, message: Message) {
                if (message is DisconnectMessage) {
                    semaphoreDisconnect.countDown()
                }
            }
        })

        ethA!!.sendNewBlockHashes(b8_)

        semaphoreDisconnect.await(10, SECONDS)

        // check if peer was dropped
        if (semaphoreDisconnect.count > 0) {
            fail("PeerA is not dropped")
        }
    }

    @Throws(InterruptedException::class)
    private fun setupPeers() {

        ethereumA = EthereumFactory.createEthereum(SysPropConfigA.props, SysPropConfigA::class.java)
        ethereumB = EthereumFactory.createEthereum(SysPropConfigB.props, SysPropConfigB::class.java)

        ethereumA!!.addListener(object : EthereumListenerAdapter() {
            override fun onEthStatusUpdated(channel: Channel, statusMessage: StatusMessage) {
                ethA = channel.ethHandler as EthHandler
            }
        })

        val semaphore = CountDownLatch(1)

        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onPeerAddedToSyncPool(peer: Channel) {
                semaphore.countDown()
            }
        })

        ethereumB!!.connect(nodeA)

        semaphore.await(10, SECONDS)
        if (semaphore.count > 0) {
            fail("Failed to set up peers")
        }
    }

    @Configuration
    @NoAutoscan
    class SysPropConfigA {

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        @Bean
        @Scope("prototype")
        @Throws(IllegalAccessException::class, InstantiationException::class)
        fun eth62(): Eth62 {
            if (eth62 != null) return eth62!!
            return Eth62()
        }

        companion object {
            internal val props = SystemProperties()
            internal var eth62: Eth62? = null
        }
    }

    @Configuration
    @NoAutoscan
    class SysPropConfigB {

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        companion object {
            internal val props = SystemProperties()
        }
    }

    companion object {

        private val minDifficultyBackup: BigInteger? = null
        private var nodeA: Node? = null
        private var mainB1B10: List<Block>? = null
        private var forkB1B5B8_: List<Block>? = null
        private var b10: Block? = null
        private var b8_: Block? = null

        @BeforeClass
        @Throws(IOException::class, URISyntaxException::class)
        fun setup() {

            SystemProperties.getDefault()!!.blockchainConfig = StandaloneBlockchain.getEasyMiningConfig()

            nodeA = Node("enode://3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6@localhost:30334")

            SysPropConfigA.props.overrideParams(
                    "peer.listen.port", "30334",
                    "peer.privateKey", "3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c",
                    // nodeId: 3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6
                    "genesis", "genesis-light.json"
            )

            SysPropConfigB.props.overrideParams(
                    "peer.listen.port", "30335",
                    "peer.privateKey", "6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec",
                    "genesis", "genesis-light.json",
                    "sync.enabled", "true"
            )

            mainB1B10 = loadBlocks("sync/main-b1-b10.dmp")
            forkB1B5B8_ = loadBlocks("sync/fork-b1-b5-b8_.dmp")

            b10 = mainB1B10!![mainB1B10!!.size - 1]
            b8_ = forkB1B5B8_!![forkB1B5B8_!!.size - 1]
        }

        @Throws(URISyntaxException::class, IOException::class)
        private fun loadBlocks(path: String): List<Block> {

            val url = ClassLoader.getSystemResource(path)
            val file = File(url.toURI())
            val strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)

            val blocks = ArrayList<Block>(strData.size)
            for (rlp in strData) {
                blocks.add(Block(decode(rlp)))
            }

            return blocks
        }

        @AfterClass
        fun cleanup() {
            SystemProperties.getDefault()!!.blockchainConfig = MainNetConfig.INSTANCE
        }
    }
}
