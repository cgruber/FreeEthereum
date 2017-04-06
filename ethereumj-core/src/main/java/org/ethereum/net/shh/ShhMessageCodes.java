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

package org.ethereum.net.shh;

import java.util.HashMap;
import java.util.Map;

/**
 * A list of commands for the Whisper network protocol.
 * <br>
 * The codes for these commands are the first byte in every packet.
 *
 * @see <a href="https://github.com/ethereum/wiki/wiki/Wire-Protocol">
 * https://github.com/ethereum/wiki/wiki/Wire-Protocol</a>
 */
public enum ShhMessageCodes {

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

    private static final Map<Integer, ShhMessageCodes> intToTypeMap = new HashMap<>();

    static {
        for (final ShhMessageCodes type : ShhMessageCodes.values()) {
            intToTypeMap.put(type.cmd, type);
        }
    }

    private final int cmd;

    ShhMessageCodes(final int cmd) {
        this.cmd = cmd;
    }

    public static ShhMessageCodes fromByte(final byte i) {
        return intToTypeMap.get((int) i);
    }

    public static boolean inRange(final byte code) {
        return code >= STATUS.asByte() && code <= FILTER.asByte();
    }

    public byte asByte() {
        return (byte) (cmd);
    }
}
