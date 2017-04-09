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

package org.ethereum.core

import org.ethereum.crypto.HashUtil
import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex

/**
 * @author Roman Mandeleil
 * *
 * @since 20.11.2014
 */
class BloomTest {


    @Test /// based on http://bit.ly/1MtXxFg
    fun test1() {

        val address = Hex.decode("095e7baea6a6c7c4c2dfeb977efac326af552d87")
        val addressBloom = Bloom.create(HashUtil.sha3(address))

        val topic = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")
        val topicBloom = Bloom.create(HashUtil.sha3(topic))

        val totalBloom = Bloom()
        totalBloom.or(addressBloom)
        totalBloom.or(topicBloom)


        Assert.assertEquals(
                "00000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000040000000000000000000000000000000000000000000000000000000",
                totalBloom.toString()
        )

        Assert.assertTrue(totalBloom.matches(addressBloom))
        Assert.assertTrue(totalBloom.matches(topicBloom))
        Assert.assertFalse(totalBloom.matches(Bloom.create(HashUtil.sha3(Hex.decode("1000000000000000000000000000000000000000000000000000000000000000")))))
        Assert.assertFalse(totalBloom.matches(Bloom.create(HashUtil.sha3(Hex.decode("195e7baea6a6c7c4c2dfeb977efac326af552d87")))))
    }


    @Test
    fun test2() {
        // todo: more testing
    }

    @Test
    fun test3() {
        // todo: more testing
    }


    @Test
    fun test4() {
        // todo: more testing
    }

}
