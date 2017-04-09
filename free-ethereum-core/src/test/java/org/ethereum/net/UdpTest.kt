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

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import org.apache.commons.lang3.RandomStringUtils
import org.ethereum.crypto.ECKey
import org.ethereum.net.rlpx.FindNodeMessage
import org.ethereum.net.rlpx.discover.DiscoveryEvent
import org.ethereum.net.rlpx.discover.PacketDecoder
import org.ethereum.util.Functional
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Not for regular run, but just for testing UDP client/server communication

 * For running server with gradle:
 * - adjust constants
 * - remove @Ignore from server() method
 * - > ./gradlew -Dtest.single=UdpTest test

 * Created by Anton Nashatyrev on 28.12.2016.
 */
class UdpTest {

    private val nodeManager = SimpleNodeManager()

    @Throws(InterruptedException::class)
    private fun create(bindAddr: String, port: Int): Channel {
        val group = NioEventLoopGroup(1)

        val b = Bootstrap()
        b.group(group)
                .channel(NioDatagramChannel::class.java)
                .handler(object : ChannelInitializer<NioDatagramChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: NioDatagramChannel) {
                        ch.pipeline().addLast(PacketDecoder())
                        val messageHandler = SimpleMessageHandler(ch, nodeManager)
                        nodeManager.messageSender = messageHandler
                        ch.pipeline().addLast(messageHandler)
                    }
                })

        return b.bind(bindAddr, port).sync().channel()
    }

    @Throws(InterruptedException::class)
    private fun startServer() {
        create(serverAddr, serverPort).closeFuture().sync()
    }

    @Throws(InterruptedException::class)
    private fun startClient() {
        val defaultMessage = RandomStringUtils.randomAlphanumeric(MAX_LENGTH)
        for (i in defaultMessage.length - 1 downTo 0) {
            var sendAttempts = 0
            var ok = false
            while (sendAttempts < 3) {
                val channel = create(clientAddr, clientPort)
                val sendMessage = defaultMessage.substring(i, defaultMessage.length)
                val msg = FindNodeMessage.create(sendMessage.toByteArray(), privKey)
                System.out.printf("Sending message with string payload of size %s, packet size %s, attempt %s%n", sendMessage.length, msg.packet.size, sendAttempts + 1)
                nodeManager.messageSender?.sendPacket(msg.packet, InetSocketAddress(serverAddr, serverPort))
                ok = channel.closeFuture().await(1, TimeUnit.SECONDS)
                if (ok) break
                sendAttempts++
                channel.close().sync()
            }
            if (!ok) {
                println("ERROR: Timeout waiting for response after all attempts")
                assert(false)
            } else {
                println("OK")
            }
        }
    }

    @Ignore
    @Test
    @Throws(Exception::class)
    fun server() {
        startServer()
    }

    @Ignore
    @Test
    @Throws(Exception::class)
    fun client() {
        startClient()
    }

    private inner class SimpleMessageHandler(internal val channel: Channel, internal val nodeManager: SimpleNodeManager) : SimpleChannelInboundHandler<DiscoveryEvent>(), Functional.Consumer<DiscoveryEvent> {

        @Throws(Exception::class)
        override fun channelActive(ctx: ChannelHandlerContext) {
            val localAddress = ctx.channel().localAddress() as InetSocketAddress
            System.out.printf("Channel initialized on %s:%s%n", localAddress.hostString, localAddress.port)
        }

        @Throws(Exception::class)
        override fun channelRead0(ctx: ChannelHandlerContext, msg: DiscoveryEvent) {
            val localAddress = ctx.channel().localAddress() as InetSocketAddress
            System.out.printf("Message received on %s:%s%n", localAddress.hostString, localAddress.port)
            nodeManager.handleInbound(msg)
        }

        override fun accept(discoveryEvent: DiscoveryEvent) {
            val address = discoveryEvent.address
            sendPacket(discoveryEvent.message.packet, address)
        }

        fun sendPacket(payload: ByteArray, address: InetSocketAddress) {
            val packet = DatagramPacket(Unpooled.copiedBuffer(payload), address)
            println("Sending message from " + clientAddr + ":" + clientPort +
                    " to " + address.hostString + ":" + address.port)
            channel.writeAndFlush(packet)
        }

        @Throws(Exception::class)
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
        }
    }

    private inner class SimpleNodeManager {

        var messageSender: SimpleMessageHandler? = null

        fun handleInbound(discoveryEvent: DiscoveryEvent) {
            val m = discoveryEvent.message as? FindNodeMessage ?: return
            val msg = String(m.target)
            System.out.printf("Inbound message \"%s\" from %s:%s%n", msg,
                    discoveryEvent.address.hostString, discoveryEvent.address.port)
            if (msg.endsWith("+1")) {
                messageSender?.channel?.close()
            } else {
                val newMsg = FindNodeMessage.create((msg + "+1").toByteArray(), privKey)
                messageSender?.sendPacket(newMsg.packet, discoveryEvent.address)
            }
        }
    }

    companion object {

        private val clientAddr = bindIp()
        private val clientPort = 8888
        private val serverAddr = bindIp()
        private val serverPort = 30321

        private val privKeyStr = "abb51256c1324a1350598653f46aa3ad693ac3cf5d05f36eba3f495a1f51590f"
        private val privKey = ECKey.fromPrivate(Hex.decode(privKeyStr))
        private val MAX_LENGTH = 4096

        private fun bindIp(): String {
            var bindIp: String
            try {
                val s = Socket("www.google.com", 80)
                bindIp = s.localAddress.hostAddress
                System.out.printf("UDP local bound to: %s%n", bindIp)
            } catch (e: IOException) {
                System.out.printf("Can't get bind IP. Fall back to 0.0.0.0: " + e)
                bindIp = "0.0.0.0"
            }

            return bindIp
        }
    }
}
