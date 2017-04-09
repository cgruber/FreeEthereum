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

import org.ethereum.crypto.ECIESCoder
import org.ethereum.crypto.ECKey
import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.util.*

/**
 * Created by Anton Nashatyrev on 25.09.2015.
 */
class EnvelopeTest {

    @Test
    fun testBroadcast1() {
        val msg1 = WhisperMessage()
                .setTopics(*Topic.createTopics("Topic1", "Topic2"))
                .setPayload("Hello")
        val msg2 = WhisperMessage()
                .setTopics(*Topic.createTopics("Topic1", "Topic3"))
                .setPayload("Hello again")
        val envelope = ShhEnvelopeMessage(msg1, msg2)

        val bytes = envelope.encoded

        val inbound = ShhEnvelopeMessage(bytes)
        Assert.assertEquals(2, inbound.messages.size.toLong())
        val inMsg1 = inbound.messages[0]
        var b = inMsg1.decrypt(Collections.emptyList(),
                Arrays.asList(*Topic.createTopics("Topic2", "Topic3")))
        Assert.assertTrue(b)
        Assert.assertEquals("Hello", String(inMsg1.payload))

        val inMsg2 = inbound.messages[1]
        b = inMsg2.decrypt(Collections.emptyList(),
                Arrays.asList(*Topic.createTopics("Topic1", "Topic3")))
        Assert.assertTrue(b)
        Assert.assertEquals("Hello again", String(inMsg2.payload))
    }

    @Test
    fun testPow1() {
        val from = ECKey()
        val to = ECKey()
        println("From: " + Hex.toHexString(from.privKeyBytes!!))
        println("To: " + Hex.toHexString(to.privKeyBytes!!))
        val msg1 = WhisperMessage()
                .setTopics(*Topic.createTopics("Topic1", "Topic2"))
                .setPayload("Hello")
                .setFrom(from)
                .setTo(WhisperImpl.toIdentity(to))
                .setWorkToProve(1000)
        val msg2 = WhisperMessage()
                .setTopics(*Topic.createTopics("Topic1", "Topic3"))
                .setPayload("Hello again")
                .setWorkToProve(500)
        val envelope = ShhEnvelopeMessage(msg1, msg2)

        val bytes = envelope.encoded

        //        System.out.println(RLPTest.dump(RLP.decode2(bytes), 0));

        val inbound = ShhEnvelopeMessage(bytes)
        Assert.assertEquals(2, inbound.messages.size.toLong())
        val inMsg1 = inbound.messages[0]
        var b = inMsg1.decrypt(setOf(to),
                Arrays.asList(*Topic.createTopics("Topic2", "Topic3")))
        Assert.assertTrue(b)
        Assert.assertEquals("Hello", String(inMsg1.payload))
        Assert.assertEquals(msg1.to, inMsg1.to)
        Assert.assertEquals(msg1.from, inMsg1.from)
        //        System.out.println(msg1.nonce + ": " + inMsg1.nonce + ", " + inMsg1.getPow());
        Assert.assertTrue(inMsg1.pow > 10)

        val inMsg2 = inbound.messages[1]
        b = inMsg2.decrypt(Collections.emptyList(),
                Arrays.asList(*Topic.createTopics("Topic2", "Topic3")))
        Assert.assertTrue(b)
        Assert.assertEquals("Hello again", String(inMsg2.payload))
        //        System.out.println(msg2.nonce + ": " + inMsg2.getPow());
        Assert.assertTrue(inMsg2.pow > 8)
    }

    @Test
    @Throws(Exception::class)
    fun testCpp1() {
        val cipherText1 = Hex.decode("0469e324b8ab4a8e2bf0440548498226c9864d1210248ebf76c3396dd1748f0b04d347728b683993e4061998390c2cc8d6d09611da6df9769ebec888295f9be99e86ddad866f994a494361a5658d2b48d1140d73f71a382a4dc7ee2b0b5487091b0c25a3f0e6")
        val priv = ECKey.fromPrivate(Hex.decode("d0b043b4c5d657670778242d82d68a29d25d7d711127d17b8e299f156dad361a"))
        val pub = ECKey.fromPublicOnly(Hex.decode("04bd27a63c91fe3233c5777e6d3d7b39204d398c8f92655947eb5a373d46e1688f022a1632d264725cbc7dc43ee1cfebde42fa0a86d08b55d2acfbb5e9b3b48dc5"))
        val plain1 = ECIESCoder.decryptSimple(priv.privKey, cipherText1)
        val cipherText2 = ECIESCoder.encryptSimple(pub.pubKeyPoint, plain1)

        //        System.out.println("Cipher1: " + Hex.toHexString(cipherText1));
        //        System.out.println("Cipher2: " + Hex.toHexString(cipherText2));

        val plain2 = ECIESCoder.decryptSimple(priv.privKey, cipherText2)

        Assert.assertArrayEquals(plain1, plain2)
    }
}
