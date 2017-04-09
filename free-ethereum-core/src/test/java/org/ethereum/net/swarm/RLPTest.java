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

package org.ethereum.net.swarm;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

public class RLPTest {
    @Test
    public void simpleTest() {
        for (int i = 0; i < 255; i++) {
            final byte data = (byte) i;
            final byte[] bytes1 = RLP.encodeElement(new byte[]{data});
            final byte[] bytes2 = RLP.encodeByte(data);

            System.out.println(i + ": " + Arrays.toString(bytes1) + Arrays.toString(bytes2));
        }
    }

    @Test
    public void zeroTest() {
        {
            final byte[] e = RLP.encodeList(
                    RLP.encodeString("aaa"),
                    RLP.encodeInt((byte) 0)
            );

            System.out.println(Hex.toHexString(e));

            final RLPList l1 = (RLPList) RLP.decode2(e).get(0);

            System.out.println(new String (l1.get(0).getRLPData()));
            System.out.println(l1.get(1).getRLPData());

            final byte[] rlpData = l1.get(1).getRLPData();
            final byte ourByte = rlpData == null ? 0 : rlpData[0];

        }
        {
            final byte[] e = RLP.encodeList(
                    //                RLP.encodeString("aaa"),
                    RLP.encodeElement(new byte[] {1}),
                    RLP.encodeElement(new byte[] {0})
            );

            System.out.println(Hex.toHexString(e));

        }
    }

    @Test
    public void frameHaderTest() {
        final byte[] bytes = Hex.decode("c28080");
        final RLPList list = RLP.decode2(bytes);
        System.out.println(list.size());
        System.out.println(list.get(0));

        final byte[] bytes1 = RLP.encodeList(RLP.encodeInt(0));
        System.out.println(Arrays.toString(bytes1));
    }
}
