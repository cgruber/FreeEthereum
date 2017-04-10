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

package org.ethereum.net.swarm

import org.ethereum.net.client.Capability
import org.ethereum.net.swarm.bzz.BzzStatusMessage
import org.ethereum.net.swarm.bzz.PeerAddress
import org.junit.Assert
import org.junit.Test
import java.util.*

class MessageTest {

    @Test
    fun statusMessageTest() {
        val m1 = BzzStatusMessage(777, "IdString",
                PeerAddress(byteArrayOf(127, 0, 0, 255.toByte()), 1010, byteArrayOf(1, 2, 3, 4)), 888,
                Arrays.asList(Capability("bzz", 0.toByte()),
                        Capability("shh", 202.toByte())))
        val encoded = m1.encoded
        val m2 = BzzStatusMessage(encoded)
        println(m1)
        println(m2)

        Assert.assertEquals(m1.version, m2.version)
        Assert.assertEquals(m1.id, m2.id)
        Assert.assertTrue(Arrays.equals(m1.addr.ip, m2.addr.ip))
        Assert.assertTrue(Arrays.equals(m1.addr.id, m2.addr.id))
        Assert.assertEquals(m1.addr.port.toLong(), m2.addr.port.toLong())
        Assert.assertEquals(m1.networkId, m2.networkId)
        Assert.assertEquals(m1.capabilities, m2.capabilities)
    }


}
