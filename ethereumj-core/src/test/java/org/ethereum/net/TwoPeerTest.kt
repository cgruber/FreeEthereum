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

import com.typesafe.config.ConfigFactory
import org.ethereum.config.NoAutoscan
import org.ethereum.config.SystemProperties
import org.ethereum.core.*
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.mine.Ethash
import org.ethereum.net.eth.handler.Eth62
import org.ethereum.net.eth.message.*
import org.ethereum.net.server.Channel
import org.ethereum.util.RLP
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.io.FileNotFoundException
import java.lang.Math.max
import java.lang.Math.min
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Anton Nashatyrev on 13.10.2015.
 */
@Ignore
class TwoPeerTest {

    private fun createNextBlock(parent: Block, stateRoot: String, extraData: String?): Block {
        val b = Block(parent.hash, sha3(RLP.encodeList()), parent.coinbase, parent.logBloom,
                parent.difficulty, parent.number + 1, parent.gasLimit, parent.gasUsed,
                System.currentTimeMillis() / 1000, ByteArray(0), ByteArray(0), ByteArray(0),
                parent.receiptsRoot, parent.txTrieRoot,
                Hex.decode(stateRoot),
                //                    Hex.decode("7c22bebbe3e6cf5af810bef35ad7a7b8172e0a247eaeb44f63fffbce87285a7a"),
                emptyList<Transaction>(), emptyList<BlockHeader>())
        //        b.getHeader().setDifficulty(b.getHeader().calcDifficulty(bestBlock.getHeader()).toByteArray());
        if (extraData != null) {
            b.header.extraData = extraData.toByteArray()
        }
        return b
    }

    private fun addNextBlock(blockchain1: BlockchainImpl, parent: Block, extraData: String?): Block {
        val b = createNextBlock(parent, "00", extraData)
        println("Adding block.")
        //        blockchain1.add(b, new Miner() {
        //            @Override
        //            public long mine(BlockHeader header) {
        //                return Ethash.getForBlock(header.getNumber()).mineLight(header);
        //            }
        //
        //            @Override
        //            public boolean validate(BlockHeader header) {
        //                return true;
        //            }
        //        });
        return b
    }

    @Test
    @Throws(FileNotFoundException::class, InterruptedException::class)
    fun testTest() {
        SysPropConfig1.props.overrideParams(
                "peer.listen.port", "30334",
                "peer.privateKey", "3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c",
                // nodeId: 3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6
                "genesis", "genesis-light.json",
                "database.dir", "testDB-1")

        SysPropConfig2.props.overrideParams(ConfigFactory.parseString(
                "peer.listen.port = 30335 \n" +
                        "peer.privateKey = 6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec \n" +
                        "peer.active = [{ url = \"enode://3973cb86d7bef9c96e5d589601d788370f9e24670dcba0480c0b3b1b0647d13d0f0fffed115dd2d4b5ca1929287839dcd4e77bdc724302b44ae48622a8766ee6@localhost:30334\" }] \n" +
                        "sync.enabled = true \n" +
                        "genesis = genesis-light.json \n" +
                        "database.dir = testDB-2 \n"))

        val alternativeFork = ArrayList<Block>()
        SysPropConfig1.testHandler = object : Eth62() {
            override fun processGetBlockHeaders(msg: GetBlockHeadersMessage) {
                if (msg.blockHash != null) {
                    println("=== (1)")
                    for (i in alternativeFork.indices) {
                        if (Arrays.equals(msg.blockHash, alternativeFork[i].hash)) {
                            println("=== (2)")
                            var endIdx = max(0, i - msg.skipBlocks)
                            var startIdx = max(0, i - msg.maxHeaders)
                            if (!msg.isReverse) {
                                startIdx = min(alternativeFork.size - 1, i + msg.skipBlocks)
                                endIdx = min(alternativeFork.size - 1, i + msg.maxHeaders)
                            }

                            val headers = ArrayList<BlockHeader>()
                            for (j in startIdx..endIdx) {
                                headers.add(alternativeFork[j].header)
                            }

                            if (msg.isReverse) {
                                Collections.reverse(headers)
                            }

                            sendMessage(BlockHeadersMessage(headers))

                            return
                        }
                    }

                }
                super.processGetBlockHeaders(msg)
            }

            override fun processGetBlockBodies(msg: GetBlockBodiesMessage) {
                val bodies = ArrayList<ByteArray>(msg.blockHashes.size)

                for (hash in msg.blockHashes) {
                    var block: Block? = null
                    for (b in alternativeFork) {
                        if (Arrays.equals(b.hash, hash)) {
                            block = b
                            break
                        }
                    }
                    if (block == null) {
                        block = blockchain.getBlockByHash(hash)
                    }
                    bodies.add(block!!.encodedBody)
                }

                sendMessage(BlockBodiesMessage(bodies))
            }
        }

        val ethereum1 = EthereumFactory.createEthereum(SysPropConfig1.props, SysPropConfig1::class.java)
        val blockchain = ethereum1.blockchain as BlockchainImpl
        val bGen = blockchain.bestBlock
        val b1 = addNextBlock(blockchain, bGen, "chain A")
        val b2 = addNextBlock(blockchain, b1, null)
        val b3 = addNextBlock(blockchain, b2, null)
        val b4 = addNextBlock(blockchain, b3, null)

        val listOfHeadersStartFrom = blockchain.getListOfHeadersStartFrom(BlockIdentifier(null, 3), 0, 100, true)

        //        Block b1b = addNextBlock(blockchain, bGen, "chain B");
        val b1b = createNextBlock(bGen, "7c22bebbe3e6cf5af810bef35ad7a7b8172e0a247eaeb44f63fffbce87285a7a", "chain B")
        Ethash.getForBlock(SystemProperties.getDefault(), b1b.number).mineLight(b1b)
        val b2b = createNextBlock(b1b, Hex.toHexString(b2.stateRoot), "chain B")
        Ethash.getForBlock(SystemProperties.getDefault(), b2b.number).mineLight(b2b)

        alternativeFork.add(bGen)
        alternativeFork.add(b1b)
        alternativeFork.add(b2b)

        //        byte[] root = ((RepositoryImpl) ethereum.getRepository()).getRoot();
        //        ((RepositoryImpl) ethereum.getRepository()).syncToRoot(root);
        //        byte[] root1 = ((RepositoryImpl) ethereum.getRepository()).getRoot();
        //        Block b2b = addNextBlock(blockchain, b1, "chain B");

        println("Blocks added")

        val ethereum2 = EthereumFactory.createEthereum(SysPropConfig2.props, SysPropConfig2::class.java)

        val semaphore = CountDownLatch(1)

        val channel1 = arrayOfNulls<Channel>(1)
        ethereum1.addListener(object : EthereumListenerAdapter() {
            override fun onEthStatusUpdated(channel: Channel, statusMessage: StatusMessage) {
                channel1[0] = channel
                println("==== Got the Channel: " + channel)
            }
        })
        ethereum2.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                if (block.number.equals(4)) {
                    semaphore.countDown()
                }
            }

        })

        println("======= Waiting for block #4")
        semaphore.await(60, TimeUnit.SECONDS)
        if (semaphore.count > 0) {
            throw RuntimeException("4 blocks were not imported.")
        }

        println("======= Sending forked block without parent...")
        //        ((EthHandler) channel1[0].getEthHandler()).sendNewBlock(b2b);

        //        Block b = b4;
        //        for (int i = 0; i < 10; i++) {
        //            Thread.sleep(3000);
        //            System.out.println("=====  Adding next block...");
        //            b = addNextBlock(blockchain, b, null);
        //        }

        Thread.sleep(10000000)


        ethereum1.close()
        ethereum2.close()

        println("Passed.")

    }

    @Configuration
    @NoAutoscan
    class SysPropConfig1 {

        @Bean
        @Scope("prototype")
        fun eth62(): Eth62 {
            return testHandler!!
            //            return new Eth62();
        }

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        companion object {
            internal val props = SystemProperties()
            internal var testHandler: Eth62? = null
        }
    }

    @Configuration
    @NoAutoscan
    class SysPropConfig2 {

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        companion object {
            internal val props = SystemProperties()
        }

    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            val k = ECKey.fromPrivate(Hex.decode("6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec"))
            println(Hex.toHexString(k.privKeyBytes!!))
            println(Hex.toHexString(k.address))
            println(Hex.toHexString(k.nodeId))
        }
    }
}
