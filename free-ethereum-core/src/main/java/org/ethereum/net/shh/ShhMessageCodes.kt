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

package org.ethereum.net.shh

import java.util.*

/**
 * A list of commands for the Whisper network protocol.
 * <br></br>
 * The codes for these commands are the first byte in every packet.

 * @see [
 * https://github.com/ethereum/wiki/wiki/Wire-Protocol](https://github.com/ethereum/wiki/wiki/Wire-Protocol)
 */
enum class ShhMessageCodes constructor(private val cmd: Int) {

    /* Whisper Protocol */

    /**
     * [+0x00]
     */
    STATUS(0x00),

    /**
     * [+0x01]
     */
    MESSAGE(0x01),

    /**
     * [+0x02]
     */
    FILTER(0x02);

    fun asByte(): Byte {
        return cmd.toByte()
    }

    companion object {

        private val intToTypeMap = HashMap<Int, ShhMessageCodes>()

        init {
            for (type in ShhMessageCodes.values()) {
                intToTypeMap.put(type.cmd, type)
            }
        }

        fun fromByte(i: Byte): ShhMessageCodes {
            return intToTypeMap[i.toInt()]!!
        }

        fun inRange(code: Byte): Boolean {
            return code >= STATUS.asByte() && code <= FILTER.asByte()
        }
    }
}
