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

import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.util.ByteUtil.merge
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.charset.Charset

class RLPXTest {

    @Test // ping test
    fun test1() {

        val node = Node.instanceOf("85.65.19.231:30303")
        val key = ECKey.fromPrivate(BigInteger.TEN)

        val ping = PingMessage.create(node, node, key)
        logger.info("{}", ping)

        val wire = ping.packet
        val ping2 = Message.decode(wire) as PingMessage
        logger.info("{}", ping2)

        assertEquals(ping.toString(), ping2.toString())

        val key2 = ping2.key.toString()
        assertEquals(key.toString(), key2)
    }

    @Test // pong test
    fun test2() {

        val token = sha3("+++".toByteArray(Charset.forName("UTF-8")))
        val key = ECKey.fromPrivate(BigInteger.TEN)

        val pong = PongMessage.create(token, key)
        logger.info("{}", pong)

        val wire = pong.packet
        val pong2 = Message.decode(wire) as PongMessage
        logger.info("{}", pong)

        assertEquals(pong.toString(), pong2.toString())

        val key2 = pong2.key.toString()
        assertEquals(key.toString(), key2)
    }

    @Test // neighbors message
    fun test3() {

        val ip = "85.65.19.231"
        val port = 30303

        val part1 = sha3("007".toByteArray(Charset.forName("UTF-8")))
        val part2 = sha3("007".toByteArray(Charset.forName("UTF-8")))
        val id = merge(part1, part2)

        val node = Node(id, ip, port)

        val nodes = listOf(node)
        val key = ECKey.fromPrivate(BigInteger.TEN)

        val neighbors = NeighborsMessage.create(nodes, key)
        logger.info("{}", neighbors)

        val wire = neighbors.packet
        val neighbors2 = Message.decode(wire) as NeighborsMessage
        logger.info("{}", neighbors2)

        assertEquals(neighbors.toString(), neighbors2.toString())

        val key2 = neighbors2.key.toString()
        assertEquals(key.toString(), key2)
    }

    @Test // find node message
    fun test4() {

        val id = sha3("+++".toByteArray(Charset.forName("UTF-8")))
        val key = ECKey.fromPrivate(BigInteger.TEN)

        val findNode = FindNodeMessage.create(id, key)
        logger.info("{}", findNode)

        val wire = findNode.packet
        val findNode2 = Message.decode(wire) as FindNodeMessage
        logger.info("{}", findNode2)

        assertEquals(findNode.toString(), findNode2.toString())

        val key2 = findNode2.key.toString()
        assertEquals(key.toString(), key2)
    }


    @Test(expected = Exception::class) // failure on MDC
    fun test5() {

        val id = sha3("+++".toByteArray(Charset.forName("UTF-8")))
        val key = ECKey.fromPrivate(BigInteger.TEN)

        val findNode = FindNodeMessage.create(id, key)
        logger.info("{}", findNode)

        val wire = findNode.packet
        wire[64]++

        val findNode2 = Message.decode(wire) as FindNodeMessage
        logger.info("{}", findNode2)

        assertEquals(findNode.toString(), findNode2.toString())
    }


    @Test
    fun test6() {

        val id_1 = sha3("+++".toByteArray(Charset.forName("UTF-8")))
        val host_1 = "85.65.19.231"
        val port_1 = 30303

        val node_1 = Node(id_1, host_1, port_1)
        val node_2 = Node(node_1.rlp)

        val id_2 = node_2.id
        val host_2 = node_2.host
        val port_2 = node_2.port

        assertEquals(Hex.toHexString(id_1), Hex.toHexString(id_2))
        assertEquals(host_1, host_2)
        assertTrue(port_1 == port_2)
    }


    @Test // Neighbors parse data
    fun test7() {

        val wire = Hex.decode("d5106e888eeca1e0b4a93bf17c325f912b43ca4176a000966619aa6a96ac9d5a60e66c73ed5629c13d4d0c806a3127379541e8d90d7fcb52c33c5e36557ad92dfed9619fcd3b92e42683aed89bd3c6eef6b59bd0237c36d83ebb0075a59903f50104f90200f901f8f8528c38352e36352e31392e32333182f310b840aeb2dd107edd996adf1bbf835fb3f9a11aabb7ed3dfef84c7a3c8767482bff522906a11e8cddee969153bf5944e64e37943db509bb4cc714c217f20483802ec0f8528c38352e36352e31392e32333182e5b4b840b70cdf8f23024a65afbf12110ca06fa5c37bd9fe4f6234a0120cdaaf16e8bb96d090d0164c316aaa18158d346e9b0a29ad9bfa0404ab4ee9906adfbacb01c21bf8528c38352e36352e31392e32333182df38b840ed8e01b5f5468f32de23a7524af1b35605ffd7cdb79af4eacd522c94f8ed849bb81dfed4992c179caeef0952ecad2d868503164a434c300356b369a33c159289f8528c38352e36352e31392e32333182df38b840136996f11c2c80f231987fc4f0cbd061cb021c63afaf5dd879e7c851a57be8d023af14bc201be81588ecab7971693b3f689a4854df74ad2e2334e88ae76aa122f8528c38352e36352e31392e32333182f303b840742eac32e1e2343b89c03a20fc051854ea6a3ff28ca918d1994fe1e32d6d77ab63352131db3ed0e7d6cc057d859c114b102f49052daee3d1c5f5fdaab972e655f8528c38352e36352e31392e32333182f310b8407d9e1f9ceb66fc21787b830554d604f933be203be9366710fb33355975e874a72b87837cf28b1b9ae171826b64e3c5d178326cbf71f89b3dec614816a1a40ce38454f6b578")

        val msg1 = NeighborsMessage.decode(wire) as NeighborsMessage

        val key = ECKey.fromPrivate(BigInteger.TEN)
        val msg2 = NeighborsMessage.create(msg1.nodes, key)

        val msg3 = NeighborsMessage.decode(msg2.packet) as NeighborsMessage

        for (i in 0..msg1.nodes.size - 1) {

            val node_1 = msg1.nodes[i]
            val node_3 = msg3.nodes[i]

            assertEquals(node_1.toString(), node_3.toString())
        }

        println(msg1)

    }


    @Test // FindNodeMessage parse data
    fun test8() {

        val wire = Hex.decode("3770d98825a42cb69edf70ffdf8d6d2b28a8c5499a7e3350e4a42c94652339cac3f8e9c3b5a181c8dd13e491ad9229f6a8bd018d786e1fb9e5264f43bbd6ce93af9bc85b468dee651bcd518561f83cb166da7aef7e506057dc2fbb2ea582bcc00003f847b84083fba54f6bb80ce31f6d5d1ec0a9a2e4685bc185115b01da6dcb70cd13116a6bd08b86ffe60b7d7ea56c6498848e3741113f8e70b9f0d12dbfe895680d03fd658454f6e772")

        val msg1 = FindNodeMessage.decode(wire) as FindNodeMessage

        val key = ECKey.fromPrivate(BigInteger.TEN)
        val msg2 = FindNodeMessage.create(msg1.target, key)

        val msg3 = FindNodeMessage.decode(msg2.packet) as FindNodeMessage

        Assert.assertEquals(Hex.toHexString(msg1.target), Hex.toHexString(msg3.target))
    }

    @Ignore //TODO #POC9
    @Test // Ping parse data
    fun test9() {
        //        wire: 4c62e1b75f4003ef77032006a142bbf31772936a1e5098566b28a04a5c71c73f1f2c9f539a85458c50a554de12da9d7e69fb2507f7c0788885508d385bbe7a9538fa675712aa1eaad29902bb46eee4531d00a10fd81179e4151929f60fec4dc50001ce87302e302e302e30808454f8483c
        //        PingMessage: {mdc=4c62e1b75f4003ef77032006a142bbf31772936a1e5098566b28a04a5c71c73f, signature=1f2c9f539a85458c50a554de12da9d7e69fb2507f7c0788885508d385bbe7a9538fa675712aa1eaad29902bb46eee4531d00a10fd81179e4151929f60fec4dc500, type=01, data=ce87302e302e302e30808454f8483c}

        // FIXME: wire contains empty from data
        val wire = Hex.decode("4c62e1b75f4003ef77032006a142bbf31772936a1e5098566b28a04a5c71c73f1f2c9f539a85458c50a554de12da9d7e69fb2507f7c0788885508d385bbe7a9538fa675712aa1eaad29902bb46eee4531d00a10fd81179e4151929f60fec4dc50001ce87302e302e302e30808454f8483c")

        val msg1 = Message.decode(wire) as PingMessage

        val key = ECKey.fromPrivate(BigInteger.TEN)
        val node = Node.instanceOf(msg1.toHost + ":" + msg1.toPort)
        val msg2 = PingMessage.create(node, node, key)
        val msg3 = Message.decode(msg2.packet) as PingMessage

        assertEquals(msg1.toHost, msg3.toHost)
    }


    @Test // Pong parse data
    fun test10() {
        //        wire: 84db9bf6a1f7a3444f4d4946155da16c63a51abdd6822ac683d8243f260b99b265601b769acebfe3c76ddeb6e83e924f2bac2beca0c802ff0745d349bd58bc6662d62d38c2a3bb3e167a333d7d099496ebd35e096c5c1ee1587e9bd11f20e3d80002e6a079d49bdba3a7acfc9a2881d768d1aa246c2486ab166f0305a863bd47c5d21e0e8454f8483c
        //        PongMessage: {mdc=84db9bf6a1f7a3444f4d4946155da16c63a51abdd6822ac683d8243f260b99b2, signature=65601b769acebfe3c76ddeb6e83e924f2bac2beca0c802ff0745d349bd58bc6662d62d38c2a3bb3e167a333d7d099496ebd35e096c5c1ee1587e9bd11f20e3d800, type=02, data=e6a079d49bdba3a7acfc9a2881d768d1aa246c2486ab166f0305a863bd47c5d21e0e8454f8483c}

        val wire = Hex.decode("84db9bf6a1f7a3444f4d4946155da16c63a51abdd6822ac683d8243f260b99b265601b769acebfe3c76ddeb6e83e924f2bac2beca0c802ff0745d349bd58bc6662d62d38c2a3bb3e167a333d7d099496ebd35e096c5c1ee1587e9bd11f20e3d80002e6a079d49bdba3a7acfc9a2881d768d1aa246c2486ab166f0305a863bd47c5d21e0e8454f8483c")

        val msg1 = Message.decode(wire) as PongMessage

        val key = ECKey.fromPrivate(BigInteger.TEN)
        val msg2 = PongMessage.create(msg1.token, key, 1448375807)

        val msg3 = Message.decode(msg2.packet) as PongMessage
        assertEquals(Hex.toHexString(msg1.token), Hex.toHexString(msg3.token))
    }

    /**
     * Correct encoding of IP addresses according to official RLPx protocol documentation
     * https://github.com/ethereum/devp2p/blob/master/rlpx.md
     */
    @Test
    fun testCorrectIpPing() {
        //  {mdc=d7a3a7ce591180e2f6d6f8655ece88fe3d98fff2b9896578712f77aabb8394eb,
        //      signature=6a436c85ad30842cb64451f9a5705b96089b37ad7705cf28ee15e51be55a9b756fe178371d28961aa432ce625fb313fd8e6c8607a776107115bafdd591e89dab00,
        //      type=01, data=e804d7900000000000000000000000000000000082765f82765fc9843a8808ba8233d88084587328cd}
        val wire = Hex.decode("d7a3a7ce591180e2f6d6f8655ece88fe3d98fff2b9896578712f77aabb8394eb6a436c85ad30842cb64451f9a5705b96089b37ad7705cf28ee15e51be55a9b756fe178371d28961aa432ce625fb313fd8e6c8607a776107115bafdd591e89dab0001e804d7900000000000000000000000000000000082765f82765fc9843a8808ba8233d88084587328cd")

        val msg1 = Message.decode(wire) as PingMessage
        assertEquals(30303, msg1.fromPort.toLong())
        assertEquals("0.0.0.0", msg1.fromHost)
        assertEquals(13272, msg1.toPort.toLong())
        assertEquals("58.136.8.186", msg1.toHost)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
    }
}










