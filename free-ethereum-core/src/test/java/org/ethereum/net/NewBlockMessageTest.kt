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

import org.ethereum.net.eth.message.NewBlockMessage
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

class NewBlockMessageTest {


    /* NEW_BLOCK */

    @Test
    fun test_1() {

        val payload = Hex.decode("f90144f9013Bf90136a0d8faffbc4c4213d35db9007de41cece45d95db7fd6c0f129e158baa888c48eefa01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d4934794baedba0480e1b882b606cd302d8c4f5701cabac7a0c7d4565fb7b3d98e54a0dec8b76f8c001a784a5689954ce0aedcc1bbe8d13095a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b8400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000083063477825fc88609184e72a0008301e8488084543ffee680a00de0b9d4a0f0c23546d31f1f70db00d25cf6a7af79365b4e058e4a6a3b69527bc0c0850177ddbebe")

        val newBlockMessage = NewBlockMessage(payload)
        logger.trace("{}", newBlockMessage)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
    }


}
