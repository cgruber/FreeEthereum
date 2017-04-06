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

package org.ethereum.util;

import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.ethereum.util.blockchain.EtherUtil.Unit.ETHER;
import static org.ethereum.util.blockchain.EtherUtil.convert;

/**
 * Created by Anton Nashatyrev on 06.07.2016.
 */
public class StandaloneBlockchainTest {

    @AfterClass
    public static void cleanup() {
        SystemProperties.resetToDefault();
    }

    @Test
    public void constructorTest() {
        final StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
        final SolidityContract a = sb.submitNewContract(
                "contract A {" +
                        "  uint public a;" +
                        "  uint public b;" +
                        "  function A(uint a_, uint b_) {a = a_; b = b_; }" +
                        "}",
                "A", 555, 777
        );
        Assert.assertEquals(BigInteger.valueOf(555), a.callConstFunction("a")[0]);
        Assert.assertEquals(BigInteger.valueOf(777), a.callConstFunction("b")[0]);

        final SolidityContract b = sb.submitNewContract(
                "contract A {" +
                        "  string public a;" +
                        "  uint public b;" +
                        "  function A(string a_, uint b_) {a = a_; b = b_; }" +
                        "}",
                "A", "This string is longer than 32 bytes...", 777
        );
        Assert.assertEquals("This string is longer than 32 bytes...", b.callConstFunction("a")[0]);
        Assert.assertEquals(BigInteger.valueOf(777), b.callConstFunction("b")[0]);
    }

    @Test
    public void fixedSizeArrayTest() {
        final StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
        {
            final SolidityContract a = sb.submitNewContract(
                    "contract A {" +
                            "  uint public a;" +
                            "  uint public b;" +
                            "  address public c;" +
                            "  address public d;" +
                            "  function f(uint[2] arr, address[2] arr2) {a = arr[0]; b = arr[1]; c = arr2[0]; d = arr2[1];}" +
                            "}");
            final ECKey addr1 = new ECKey();
            final ECKey addr2 = new ECKey();
            a.callFunction("f", new Integer[]{111, 222}, new byte[][] {addr1.getAddress(), addr2.getAddress()});
            Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0]);
            Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0]);
            Assert.assertArrayEquals(addr1.getAddress(), (byte[])a.callConstFunction("c")[0]);
            Assert.assertArrayEquals(addr2.getAddress(), (byte[])a.callConstFunction("d")[0]);
        }

        {
            final ECKey addr1 = new ECKey();
            final ECKey addr2 = new ECKey();
            final SolidityContract a = sb.submitNewContract(
                    "contract A {" +
                            "  uint public a;" +
                            "  uint public b;" +
                            "  address public c;" +
                            "  address public d;" +
                            "  function A(uint[2] arr, address a1, address a2) {a = arr[0]; b = arr[1]; c = a1; d = a2;}" +
                            "}", "A",
                    new Integer[]{111, 222}, addr1.getAddress(), addr2.getAddress());
            Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0]);
            Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0]);
            Assert.assertArrayEquals(addr1.getAddress(), (byte[]) a.callConstFunction("c")[0]);
            Assert.assertArrayEquals(addr2.getAddress(), (byte[]) a.callConstFunction("d")[0]);

            final String a1 = "0x1111111111111111111111111111111111111111";
            final String a2 = "0x2222222222222222222222222222222222222222";
        }
    }

    @Test
    public void encodeTest1() {
        final StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
        final SolidityContract a = sb.submitNewContract(
                "contract A {" +
                        "  uint public a;" +
                        "  function f(uint a_) {a = a_;}" +
                        "}");
        a.callFunction("f", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        final BigInteger r = (BigInteger) a.callConstFunction("a")[0];
        System.out.println(r.toString(16));
        Assert.assertEquals(new BigInteger(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")), r);
    }

    @Test
    public void invalidTxTest() {
        // check that invalid tx doesn't break implementation
        final StandaloneBlockchain sb = new StandaloneBlockchain();
        final ECKey alice = sb.getSender();
        final ECKey bob = new ECKey();
        sb.sendEther(bob.getAddress(), BigInteger.valueOf(1000));
        sb.setSender(bob);
        sb.sendEther(alice.getAddress(), BigInteger.ONE);
        sb.setSender(alice);
        sb.sendEther(bob.getAddress(), BigInteger.valueOf(2000));

        sb.createBlock();
    }

    @Test
    public void initBalanceTest() {
        // check StandaloneBlockchain.withAccountBalance method
        final StandaloneBlockchain sb = new StandaloneBlockchain();
        final ECKey alice = sb.getSender();
        final ECKey bob = new ECKey();
        sb.withAccountBalance(bob.getAddress(), convert(123, ETHER));

        final BigInteger aliceInitBal = sb.getBlockchain().getRepository().getBalance(alice.getAddress());
        final BigInteger bobInitBal = sb.getBlockchain().getRepository().getBalance(bob.getAddress());
        assert convert(123, ETHER).equals(bobInitBal);

        sb.setSender(bob);
        sb.sendEther(alice.getAddress(), BigInteger.ONE);

        sb.createBlock();

        assert convert(123, ETHER).compareTo(sb.getBlockchain().getRepository().getBalance(bob.getAddress())) > 0;
        assert aliceInitBal.add(BigInteger.ONE).equals(sb.getBlockchain().getRepository().getBalance(alice.getAddress()));
    }

}
