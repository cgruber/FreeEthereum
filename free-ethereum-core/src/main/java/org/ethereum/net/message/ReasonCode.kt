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

package org.ethereum.net.message

import java.util.*

/**
 * Reason is an optional integer specifying one
 * of a number of reasons for disconnect
 */
enum class ReasonCode constructor(private val reason: Int) {

    /**
     * [0x00] Disconnect request by other peer
     */
    REQUESTED(0x00),

    /**
     * [0x01]
     */
    TCP_ERROR(0x01),

    /**
     * [0x02] Packets can not be parsed
     */
    BAD_PROTOCOL(0x02),

    /**
     * [0x03] This peer is too slow or delivers unreliable data
     */
    USELESS_PEER(0x03),

    /**
     * [0x04] Already too many connections with other peers
     */
    TOO_MANY_PEERS(0x04),


    /**
     * [0x05] Already have a running connection with this peer
     */
    DUPLICATE_PEER(0x05),

    /**
     * [0x06] Version of the p2p protocol is not the same as ours
     */
    INCOMPATIBLE_PROTOCOL(0x06),

    /**
     * [0x07]
     */
    NULL_IDENTITY(0x07),

    /**
     * [0x08] Peer quit voluntarily
     */
    PEER_QUITING(0x08),

    UNEXPECTED_IDENTITY(0x09),

    LOCAL_IDENTITY(0x0A),

    PING_TIMEOUT(0x0B),

    USER_REASON(0x10),

    /**
     * [0xFF] Reason not specified
     */
    UNKNOWN(0xFF);

    fun asByte(): Byte {
        return reason.toByte()
    }

    companion object {

        private val intToTypeMap = HashMap<Int, ReasonCode>()

        init {
            for (type in ReasonCode.values()) {
                intToTypeMap.put(type.reason, type)
            }
        }

        fun fromInt(i: Int): ReasonCode {
            val type = intToTypeMap[i] ?: return ReasonCode.UNKNOWN
            return type
        }
    }
}
