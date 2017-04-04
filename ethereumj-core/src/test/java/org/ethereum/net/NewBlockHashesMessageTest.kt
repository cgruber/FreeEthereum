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

import org.ethereum.core.BlockIdentifier
import org.ethereum.net.eth.message.EthMessageCodes
import org.ethereum.net.eth.message.NewBlockHashesMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.spongycastle.util.encoders.Hex.decode
import org.spongycastle.util.encoders.Hex.toHexString
import java.util.*

/**
 * @author Mikhail Kalinin
 * *
 * @since 20.08.2015
 */
class NewBlockHashesMessageTest {

    @Test /* NewBlockHashesMessage 1 from new */
    fun test_1() {

        val identifiers = Arrays.asList(
                BlockIdentifier(decode("4ee6424d776b3f59affc20bc2de59e67f36e22cc07897ff8df152242c921716b"), 1),
                BlockIdentifier(decode("7d2fe4df0dbbc9011da2b3bf177f0c6b7e71a11c509035c5d751efa5cf9b4817"), 2)
        )

        val newBlockHashesMessage = NewBlockHashesMessage(identifiers)
        println(newBlockHashesMessage)

        val expected = "f846e2a04ee6424d776b3f59affc20bc2de59e67f36e22cc07897ff8df152242c921716b01e2a07d2fe4df0dbbc9011da2b3bf177f0c6b7e71a11c509035c5d751efa5cf9b481702"
        assertEquals(expected, toHexString(newBlockHashesMessage.encoded))

        assertEquals(EthMessageCodes.NEW_BLOCK_HASHES, newBlockHashesMessage.command)
        assertEquals(2, newBlockHashesMessage.blockIdentifiers.size.toLong())

        assertEquals(null, newBlockHashesMessage.answerMessage)
    }
}
