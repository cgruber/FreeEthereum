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

import org.ethereum.net.client.Capability;
import org.ethereum.net.swarm.bzz.BzzStatusMessage;
import org.ethereum.net.swarm.bzz.PeerAddress;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class MessageTest {

    @Test
    public void statusMessageTest() {
        final BzzStatusMessage m1 = new BzzStatusMessage(777, "IdString",
                new PeerAddress(new byte[] {127,0,0, (byte) 255}, 1010, new byte[] {1,2,3,4}), 888,
                Arrays.asList(new Capability("bzz", (byte) 0),
                        new Capability("shh", (byte) 202)));
        final byte[] encoded = m1.getEncoded();
        final BzzStatusMessage m2 = new BzzStatusMessage(encoded);
        System.out.println(m1);
        System.out.println(m2);

        Assert.assertEquals(m1.getVersion(), m2.getVersion());
        Assert.assertEquals(m1.getId(), m2.getId());
        Assert.assertTrue(Arrays.equals(m1.getAddr().getIp(), m2.getAddr().getIp()));
        Assert.assertTrue(Arrays.equals(m1.getAddr().getId(), m2.getAddr().getId()));
        Assert.assertEquals(m1.getAddr().getPort(), m2.getAddr().getPort());
        Assert.assertEquals(m1.getNetworkId(), m2.getNetworkId());
        Assert.assertEquals(m1.getCapabilities(), m2.getCapabilities());
    }


}
