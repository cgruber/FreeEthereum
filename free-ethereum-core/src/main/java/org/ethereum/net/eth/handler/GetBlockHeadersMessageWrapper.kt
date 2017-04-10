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

package org.ethereum.net.eth.handler

import com.google.common.util.concurrent.SettableFuture
import org.ethereum.core.BlockHeader
import org.ethereum.net.eth.message.GetBlockHeadersMessage

/**
 * Wraps [GetBlockHeadersMessage],
 * adds some additional info required by get headers queue

 * @author Mikhail Kalinin
 * *
 * @since 16.02.2016
 */
class GetBlockHeadersMessageWrapper {

    val message: GetBlockHeadersMessage
    val futureHeaders = SettableFuture.create<List<BlockHeader>>()
    var isNewHashesHandling = false
    var isSent = false
        private set

    constructor(message: GetBlockHeadersMessage) {
        this.message = message
    }

    constructor(message: GetBlockHeadersMessage, newHashesHandling: Boolean) {
        this.message = message
        this.isNewHashesHandling = newHashesHandling
    }

    fun send() {
        this.isSent = true
    }
}
