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

package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

/**
 * Auth Response message defined by EIP-8
 *
 * @author mkalinin
 * @since 17.02.2016
 */
public class AuthResponseMessageV4 {

    ECPoint ephemeralPublicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    int version = 4; // 4 bytes

    static AuthResponseMessageV4 decode(final byte[] wire) {

        final AuthResponseMessageV4 message = new AuthResponseMessageV4();

        final RLPList params = (RLPList) RLP.decode2OneItem(wire, 0);

        final byte[] pubKeyBytes = params.get(0).getRLPData();

        final byte[] bytes = new byte[65];
        System.arraycopy(pubKeyBytes, 0, bytes, 1, 64);
        bytes[0] = 0x04; // uncompressed
        message.ephemeralPublicKey = ECKey.CURVE.getCurve().decodePoint(bytes);

        message.nonce = params.get(1).getRLPData();

        final byte[] versionBytes = params.get(2).getRLPData();
        message.version = ByteUtil.byteArrayToInt(versionBytes);

        return message;
    }

    public byte[] encode() {

        final byte[] publicKey = new byte[64];
        System.arraycopy(ephemeralPublicKey.getEncoded(false), 1, publicKey, 0, publicKey.length);

        final byte[] publicBytes = RLP.encode(publicKey);
        final byte[] nonceBytes = RLP.encode(nonce);
        final byte[] versionBytes = RLP.encodeInt(version);

        return RLP.encodeList(publicBytes, nonceBytes, versionBytes);
    }

    @Override
    public String toString() {
        return "AuthResponseMessage{" +
                "\n  ephemeralPublicKey=" + ephemeralPublicKey +
                "\n  nonce=" + Hex.toHexString(nonce) +
                "\n  version=" + version +
                '}';
    }
}
