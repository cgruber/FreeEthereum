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

package org.ethereum.util

import org.ethereum.config.SystemProperties
import org.ethereum.crypto.ECKey
import org.ethereum.util.blockchain.EtherUtil.Unit.ETHER
import org.ethereum.util.blockchain.EtherUtil.convert
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

/**
 * Created by Anton Nashatyrev on 06.07.2016.
 */
class StandaloneBlockchainTest {

    @Test
    fun constructorTest() {
        val sb = StandaloneBlockchain().withAutoblock(true)
        val a = sb.submitNewContract(
                "contract A {" +
                        "  uint public a;" +
                        "  uint public b;" +
                        "  function A(uint a_, uint b_) {a = a_; b = b_; }" +
                        "}",
                "A", 555, 777
        )
        Assert.assertEquals(BigInteger.valueOf(555), a.callConstFunction("a")[0])
        Assert.assertEquals(BigInteger.valueOf(777), a.callConstFunction("b")[0])

        val b = sb.submitNewContract(
                "contract A {" +
                        "  string public a;" +
                        "  uint public b;" +
                        "  function A(string a_, uint b_) {a = a_; b = b_; }" +
                        "}",
                "A", "This string is longer than 32 bytes...", 777
        )
        Assert.assertEquals("This string is longer than 32 bytes...", b.callConstFunction("a")[0])
        Assert.assertEquals(BigInteger.valueOf(777), b.callConstFunction("b")[0])
    }

    @Test
    fun fixedSizeArrayTest() {
        val sb = StandaloneBlockchain().withAutoblock(true)
        run {
            val a = sb.submitNewContract(
                    "contract A {" +
                            "  uint public a;" +
                            "  uint public b;" +
                            "  address public c;" +
                            "  address public d;" +
                            "  function f(uint[2] arr, address[2] arr2) {a = arr[0]; b = arr[1]; c = arr2[0]; d = arr2[1];}" +
                            "}")
            val addr1 = ECKey()
            val addr2 = ECKey()
            a.callFunction("f", arrayOf(111, 222), arrayOf(addr1.address, addr2.address))
            Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0])
            Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0])
            Assert.assertArrayEquals(addr1.address, a.callConstFunction("c")[0] as ByteArray)
            Assert.assertArrayEquals(addr2.address, a.callConstFunction("d")[0] as ByteArray)
        }

        run {
            val addr1 = ECKey()
            val addr2 = ECKey()
            val a = sb.submitNewContract(
                    "contract A {" +
                            "  uint public a;" +
                            "  uint public b;" +
                            "  address public c;" +
                            "  address public d;" +
                            "  function A(uint[2] arr, address a1, address a2) {a = arr[0]; b = arr[1]; c = a1; d = a2;}" +
                            "}", "A",
                    arrayOf(111, 222), addr1.address, addr2.address)
            Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0])
            Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0])
            Assert.assertArrayEquals(addr1.address, a.callConstFunction("c")[0] as ByteArray)
            Assert.assertArrayEquals(addr2.address, a.callConstFunction("d")[0] as ByteArray)

            val a1 = "0x1111111111111111111111111111111111111111"
            val a2 = "0x2222222222222222222222222222222222222222"
        }
    }

    @Test
    fun encodeTest1() {
        val sb = StandaloneBlockchain().withAutoblock(true)
        val a = sb.submitNewContract(
                "contract A {" +
                        "  uint public a;" +
                        "  function f(uint a_) {a = a_;}" +
                        "}")
        a.callFunction("f", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        val r = a.callConstFunction("a")[0] as BigInteger
        println(r.toString(16))
        Assert.assertEquals(BigInteger(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")), r)
    }

    @Test
    fun invalidTxTest() {
        // check that invalid tx doesn't break implementation
        val sb = StandaloneBlockchain()
        val alice = sb.sender
        val bob = ECKey()
        sb.sendEther(bob.address, BigInteger.valueOf(1000))
        sb.sender = bob
        sb.sendEther(alice.address, BigInteger.ONE)
        sb.sender = alice
        sb.sendEther(bob.address, BigInteger.valueOf(2000))

        sb.createBlock()
    }

    @Test
    fun initBalanceTest() {
        // check StandaloneBlockchain.withAccountBalance method
        val sb = StandaloneBlockchain()
        val alice = sb.sender
        val bob = ECKey()
        sb.withAccountBalance(bob.address, convert(123, ETHER))

        val aliceInitBal = sb.blockchain.repository.getBalance(alice.address)
        val bobInitBal = sb.blockchain.repository.getBalance(bob.address)
        assert(convert(123, ETHER) == bobInitBal)

        sb.sender = bob
        sb.sendEther(alice.address, BigInteger.ONE)

        sb.createBlock()

        assert(convert(123, ETHER) > sb.blockchain.repository.getBalance(bob.address))
        assert(aliceInitBal.add(BigInteger.ONE) == sb.blockchain.repository.getBalance(alice.address))
    }

    companion object {

        @AfterClass
        fun cleanup() {
            SystemProperties.resetToDefault()
        }
    }

}
