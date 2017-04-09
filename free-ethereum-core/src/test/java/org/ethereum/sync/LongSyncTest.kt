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
import org.ethereum.core.*
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
class LongSyncTest {

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

    // general case, A has imported 10 blocks
    // expected: B downloads blocks from A => B synced
    @Test
    @Throws(InterruptedException::class)
    fun test1() {

        setupPeers()

        // A == b10, B == genesis

        val semaphore = CountDownLatch(1)
        ethereumB!!.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.isEqual(b10)) {
                    semaphore.countDown()
                }
            }
        })

        semaphore.await(40, SECONDS)

        // check if B == b10
        if (semaphore.count > 0) {
            fail("PeerB bestBlock is incorrect")
        }
    }

    // bodies validation: A doesn't send bodies for blocks lower than its best block
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test2() {

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

        // A == b10, B == genesis

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
    }

    // headers validation: headers count in A respond more than requested limit
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test3() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (Arrays.equals(msg.blockIdentifier.hash, b10!!.hash)) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        mainB1B10!![0].header,
                        mainB1B10!![1].header,
                        mainB1B10!![2].header,
                        mainB1B10!![3].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        // A == b10, B == genesis

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
    }

    // headers validation: A sends empty response
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test4() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (Arrays.equals(msg.blockIdentifier.hash, b10!!.hash)) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = emptyList<BlockHeader>()

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        // A == b10, B == genesis

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
    }

    // headers validation: first header in response doesn't meet expectations
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test5() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (Arrays.equals(msg.blockIdentifier.hash, b10!!.hash)) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        mainB1B10!![1].header,
                        mainB1B10!![2].header,
                        mainB1B10!![3].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        // A == b10, B == genesis

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
    }

    // headers validation: first header in response doesn't meet expectations - second story
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test6() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                val headers = listOf(mainB1B10!![1].header)

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        ethereumA = EthereumFactory.createEthereum(SysPropConfigA.props, SysPropConfigA::class.java)

        val blockchainA = ethereumA!!.blockchain as Blockchain
        for (b in mainB1B10!!) {
            blockchainA.tryToConnect(b)
        }

        // A == b10

        ethereumB = EthereumFactory.createEthereum(SysPropConfigB.props, SysPropConfigB::class.java)

        ethereumB!!.connect(nodeA!!)

        // A == b10, B == genesis

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
    }

    // headers validation: headers order is incorrect, reverse = false
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test7() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (Arrays.equals(msg.blockIdentifier.hash, b10!!.hash)) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        mainB1B10!![0].header,
                        mainB1B10!![2].header,
                        mainB1B10!![1].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        // A == b10, B == genesis

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
    }

    // headers validation: ancestor's parent hash and header's hash does not match, reverse = false
    // expected: B drops A
    @Test
    @Throws(InterruptedException::class)
    fun test8() {

        SysPropConfigA.eth62 = object : Eth62() {

            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {

                if (Arrays.equals(msg.blockIdentifier.hash, b10!!.hash)) {
                    super.processGetBlockHeaders(msg)
                    return
                }

                val headers = Arrays.asList(
                        mainB1B10!![0].header,
                        BlockHeader(ByteArray(32), ByteArray(32), ByteArray(32), ByteArray(32), ByteArray(32),
                                2, byteArrayOf(0), 0, 0, ByteArray(0), ByteArray(0), ByteArray(0)),
                        mainB1B10!![2].header
                )

                val response = BlockHeadersMessage(headers)
                sendMessage(response)
            }

        }

        setupPeers()

        // A == b10, B == genesis

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
    }

    @Throws(InterruptedException::class)
    private fun setupPeers(best: Block? = b10) {

        ethereumA = EthereumFactory.createEthereum(SysPropConfigA::class.java)

        val blockchainA = ethereumA!!.blockchain as Blockchain
        for (b in mainB1B10!!) {
            val result = blockchainA.tryToConnect(b)
            Assert.assertEquals(result, ImportResult.IMPORTED_BEST)
            if (b == best) break
        }

        // A == best

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

        ethereumB!!.connect(nodeA!!)

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

        private var nodeA: Node? = null
        private var mainB1B10: List<Block>? = null
        private var b10: Block? = null

        @BeforeClass
        @Throws(IOException::class, URISyntaxException::class)
        fun setup() {

            nodeA = Node("enode://3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6@localhost:30334")

            SysPropConfigA.props.overrideParams(
                    "peer.listen.port", "30334",
                    "peer.privateKey", "3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c",
                    // nodeId: 3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6
                    "genesis", "genesis-light-old.json"
            )
            SysPropConfigA.props.blockchainConfig = StandaloneBlockchain.getEasyMiningConfig()

            SysPropConfigB.props.overrideParams(
                    "peer.listen.port", "30335",
                    "peer.privateKey", "6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec",
                    "genesis", "genesis-light-old.json",
                    "sync.enabled", "true",
                    "sync.max.hashes.ask", "3",
                    "sync.max.blocks.ask", "2"
            )
            SysPropConfigB.props.blockchainConfig = StandaloneBlockchain.getEasyMiningConfig()

            /*
         1  => ed1b6f07d738ad92c5bdc3b98fe25afea9c863dd351711776d9ce1ffb9e3d276
         2  => 43808666b662d131c6cff336a0d13608767ead9c9d5f181e95caa3597f3faf14
         3  => 1b5c231211f500bc73148dc9d9bdb9de2265465ba441a0db1790ba4b3f5f3e9c
         4  => db517e04399dbf5a65caf6b2572b3966c8f98a1d29b1e50dc8db51e54c15d83d
         5  => c42d6dbaa756eda7f4244a3507670d764232bd7068d43e6d8ef680c6920132f6
         6  => 604c92e8d16dafb64134210d521fcc85aec27452e75aedf708ac72d8240585d3
         7  => 3f51b0471eb345b1c5f3c6628e69744358ff81d3f64a3744bbb2edf2adbb0ebc
         8  => 62cfd04e29d941954e68ac8ca18ef5cd78b19809eaed860ae72589ebad53a21d
         9  => d32fc8e151f158d52fe0be6cba6d0b5c20793a00c4ad0d32db8ccd9269199a29
         10 => 22d8c1d909eb142ea0d69d0a38711874f98d6eef1bc669836da36f6b557e9564
         */
            mainB1B10 = loadBlocks("sync/main-b1-b10.dmp")

            b10 = mainB1B10!![mainB1B10!!.size - 1]
        }

        @Throws(URISyntaxException::class, IOException::class)
        private fun loadBlocks(path: String): List<Block> {

            val url = ClassLoader.getSystemResource(path)
            val file = File(url.toURI())
            val strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)

            val blocks = ArrayList<Block>(strData.size)
            strData.mapTo(blocks) { Block(decode(it)) }

            return blocks
        }

        @AfterClass
        fun cleanup() {
            SystemProperties.resetToDefault()
        }
    }
}
