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

import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Data key, just a wrapper for byte array. Normally the SHA(data)
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
public class Key {
    private final byte[] bytes;

    public Key(final byte[] bytes) {
        this.bytes = bytes;
    }

    public Key(final String hexKey) {
        this(Hex.decode(hexKey));
    }

    public static Key zeroKey() {
        return new Key(new byte[0]);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean isZero() {
        if (bytes == null  || bytes.length == 0) return true;
        for (final byte b : bytes) {
            if (b != 0) return false;
        }
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Key key = (Key) o;

        return Arrays.equals(bytes, key.bytes);

    }

    public String getHexString() {
        return Hex.toHexString(getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return getBytes() == null ? "<null>" : ByteUtil.toHexString(getBytes());
    }
}
