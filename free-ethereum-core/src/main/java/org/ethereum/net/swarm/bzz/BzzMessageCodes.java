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

package org.ethereum.net.swarm.bzz;

import java.util.HashMap;
import java.util.Map;

public enum BzzMessageCodes {

    /**
     * Handshake BZZ message
     */
    STATUS(0x00),

    /**
     * Request to store a {@link org.ethereum.net.swarm.Chunk}
     */
    STORE_REQUEST(0x01),

    /**
     * Used for several purposes
     * - the main is to ask for a {@link org.ethereum.net.swarm.Chunk} with the specified hash
     * - ask to send back {#PEERS} message with the known nodes nearest to the specified hash
     * - initial request after handshake with zero hash. On this request the nearest known
     *   neighbours are sent back with the {#PEERS} message.
     */
    RETRIEVE_REQUEST(0x02),

    /**
     * The message is the immediate response on the {#RETRIEVE_REQUEST} with the nearest known nodes
     * of the requested hash.
     */
    PEERS(0x03);

    private static final Map<Integer, BzzMessageCodes> intToTypeMap = new HashMap<>();

    static {
        for (final BzzMessageCodes type : BzzMessageCodes.values()) {
            intToTypeMap.put(type.cmd, type);
        }
    }

    private int cmd;

    BzzMessageCodes(final int cmd) {
        this.cmd = cmd;
    }

    public static BzzMessageCodes fromByte(final byte i) {
        return intToTypeMap.get((int) i);
    }

    public static boolean inRange(final byte code) {
        return code >= STATUS.asByte() && code <= PEERS.asByte();
    }

    public byte asByte() {
        return (byte) (cmd);
    }
}
