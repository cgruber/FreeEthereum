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

import org.ethereum.core.Transaction
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil
import org.ethereum.net.eth.message.EthMessageCodes
import org.ethereum.net.eth.message.TransactionsMessage
import org.ethereum.util.ByteUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

class TransactionsMessageTest {

    /* TRANSACTIONS */

    @Ignore
    @Test /* Transactions message 1 */
    fun test_1() {

        val txsPacketRaw = "f86e12f86b04648609184e72a00094cd2a3d9f938e13cd947ec05abc7fe734df8dd826" +
                "881bc16d674ec80000801ba05c89ebf2b77eeab88251e553f6f9d53badc1d800" +
                "bbac02d830801c2aa94a4c9fa00b7907532b1f29c79942b75fff98822293bf5f" +
                "daa3653a8d9f424c6a3265f06c"

        val payload = Hex.decode(txsPacketRaw)

        val transactionsMessage = TransactionsMessage(payload)
        println(transactionsMessage)

        assertEquals(EthMessageCodes.TRANSACTIONS, transactionsMessage.command)
        assertEquals(1, transactionsMessage.transactions.size.toLong())

        val tx = transactionsMessage.transactions.iterator().next()

        assertEquals("5d2aee0490a9228024158433d650335116b4af5a30b8abb10e9b7f9f7e090fd8", Hex.toHexString(tx.hash))
        assertEquals("04", Hex.toHexString(tx.nonce))
        assertEquals("1bc16d674ec80000", Hex.toHexString(tx.value))
        assertEquals("cd2a3d9f938e13cd947ec05abc7fe734df8dd826", Hex.toHexString(tx.receiveAddress))
        assertEquals("64", Hex.toHexString(tx.gasPrice))
        assertEquals("09184e72a000", Hex.toHexString(tx.gasLimit))
        assertEquals("", ByteUtil.toHexString(tx.data))

        assertEquals("1b", Hex.toHexString(byteArrayOf(tx.signature.v)))
        assertEquals("5c89ebf2b77eeab88251e553f6f9d53badc1d800bbac02d830801c2aa94a4c9f", Hex.toHexString(tx.signature.r.toByteArray()))
        assertEquals("0b7907532b1f29c79942b75fff98822293bf5fdaa3653a8d9f424c6a3265f06c", Hex.toHexString(tx.signature.s.toByteArray()))
    }

    @Ignore
    @Test /* Transactions message 2 */
    fun test_2() {

        val txsPacketRaw = "f9025012f89d8080940000000000000000000000000000000000000000860918" +
                "4e72a000822710b3606956330c0d630000003359366000530a0d630000003359" +
                "602060005301356000533557604060005301600054630000000c588433606957" +
                "1ca07f6eb94576346488c6253197bde6a7e59ddc36f2773672c849402aa9c402" +
                "c3c4a06d254e662bf7450dd8d835160cbb053463fed0b53f2cdd7f3ea8731919" +
                "c8e8ccf901050180940000000000000000000000000000000000000000860918" +
                "4e72a000822710b85336630000002e59606956330c0d63000000155933ff3356" +
                "0d63000000275960003356576000335700630000005358600035560d63000000" +
                "3a590033560d63000000485960003356573360003557600035335700b84a7f4e" +
                "616d655265670000000000000000000000000000000000000000000000000030" +
                "57307f4e616d6552656700000000000000000000000000000000000000000000" +
                "00000057336069571ba04af15a0ec494aeac5b243c8a2690833faa74c0f73db1" +
                "f439d521c49c381513e9a05802e64939be5a1f9d4d614038fbd5479538c48795" +
                "614ef9c551477ecbdb49d2f8a6028094ccdeac59d35627b7de09332e819d5159" +
                "e7bb72508609184e72a000822710b84000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000002d0aceee" +
                "7e5ab874e22ccf8d1a649f59106d74e81ba0d05887574456c6de8f7a0d172342" +
                "c2cbdd4cf7afe15d9dbb8b75b748ba6791c9a01e87172a861f6c37b5a9e3a5d0" +
                "d7393152a7fbe41530e5bb8ac8f35433e5931b"

        val payload = Hex.decode(txsPacketRaw)

        val transactionsMessage = TransactionsMessage(payload)
        println(transactionsMessage)

        assertEquals(EthMessageCodes.TRANSACTIONS, transactionsMessage.command)

        assertEquals(3, transactionsMessage.transactions.size.toLong())

        val txIter = transactionsMessage.transactions.iterator()
        val tx1 = txIter.next()
        txIter.next() // skip one
        val tx3 = txIter.next()

        assertEquals("1b9d9456293cbcbc2f28a0fdc67028128ea571b033fb0e21d0ee00bcd6167e5d",
                Hex.toHexString(tx3.hash))

        assertEquals("00",
                Hex.toHexString(tx3.nonce))

        assertEquals("2710",
                Hex.toHexString(tx3.value))

        assertEquals("09184e72a000",
                Hex.toHexString(tx3.receiveAddress))

        assertNull(tx3.gasPrice)

        assertEquals("0000000000000000000000000000000000000000",
                Hex.toHexString(tx3.gasLimit))

        assertEquals("606956330c0d630000003359366000530a0d630000003359602060005301356000533557604060005301600054630000000c58",
                Hex.toHexString(tx3.data))

        assertEquals("33",
                Hex.toHexString(byteArrayOf(tx3.signature.v)))

        assertEquals("1c",
                Hex.toHexString(tx3.signature.r.toByteArray()))

        assertEquals("7f6eb94576346488c6253197bde6a7e59ddc36f2773672c849402aa9c402c3c4",
                Hex.toHexString(tx3.signature.s.toByteArray()))

        // Transaction #2

        assertEquals("dde9543921850f41ca88e5401322cd7651c78a1e4deebd5ee385af8ac343f0ad",
                Hex.toHexString(tx1.hash))

        assertEquals("02",
                Hex.toHexString(tx1.nonce))

        assertEquals("2710",
                Hex.toHexString(tx1.value))

        assertEquals("09184e72a000",
                Hex.toHexString(tx1.receiveAddress))

        assertNull(tx1.gasPrice)

        assertEquals("ccdeac59d35627b7de09332e819d5159e7bb7250",
                Hex.toHexString(tx1.gasLimit))

        assertEquals("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000002d0aceee7e5ab874e22ccf8d1a649f59106d74e8",
                Hex.toHexString(tx1.data))

        assertEquals("1b",
                Hex.toHexString(byteArrayOf(tx1.signature.v)))

        assertEquals("00d05887574456c6de8f7a0d172342c2cbdd4cf7afe15d9dbb8b75b748ba6791c9",
                Hex.toHexString(tx1.signature.r.toByteArray()))

        assertEquals("1e87172a861f6c37b5a9e3a5d0d7393152a7fbe41530e5bb8ac8f35433e5931b",
                Hex.toHexString(tx1.signature.s.toByteArray()))
    }

    @Test /* Transactions msg encode */
    @Throws(Exception::class)
    fun test_3() {

        val expected = "f872f870808b00d3c21bcecceda10000009479b08ad8787060333663d19704909ee7b1903e588609184e72a000824255801ca00f410a70e42b2c9854a8421d32c87c370a2b9fff0a27f9f031bb4443681d73b5a018a7dc4c4f9dee9f3dc35cb96ca15859aa27e219a8e4a8547be6bd3206979858"

        val value = BigInteger("1000000000000000000000000")

        val privKey = HashUtil.sha3("cat".toByteArray())
        val ecKey = ECKey.fromPrivate(privKey)

        val gasPrice = Hex.decode("09184e72a000")
        val gas = Hex.decode("4255")

        val tx = Transaction(null, value.toByteArray(),
                ecKey.address, gasPrice, gas, null)

        tx.sign(privKey)
        tx.encoded

        val transactionsMessage = TransactionsMessage(listOf(tx))

        assertEquals(expected, Hex.toHexString(transactionsMessage.encoded))
    }

    @Test
    fun test_4() {
        val msg = "f872f87083011a6d850ba43b740083015f9094ec210ec3715d5918b37cfa4d344a45d177ed849f881b461c1416b9d000801ba023a3035235ca0a6f80f08a1d4bd760445d5b0f8a25c32678fe18a451a88d6377a0765dde224118bdb40a67f315583d542d93d17d8637302b1da26e1013518d3ae8"
        val tmsg = TransactionsMessage(Hex.decode(msg))
        assertEquals(1, tmsg.transactions.size.toLong())
    }
}

