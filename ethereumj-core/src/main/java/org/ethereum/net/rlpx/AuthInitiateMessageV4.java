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

import static org.ethereum.util.ByteUtil.merge;
import static org.spongycastle.util.BigIntegers.asUnsignedByteArray;

/**
 * Auth Initiate message defined by EIP-8
 *
 * @author mkalinin
 * @since 17.02.2016
 */
public class AuthInitiateMessageV4 {

    ECKey.ECDSASignature signature; // 65 bytes
    ECPoint publicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    int version = 4; // 4 bytes

    public AuthInitiateMessageV4() {
    }

    static AuthInitiateMessageV4 decode(final byte[] wire) {
        final AuthInitiateMessageV4 message = new AuthInitiateMessageV4();

        final RLPList params = (RLPList) RLP.decode2OneItem(wire, 0);

        final byte[] signatureBytes = params.get(0).getRLPData();
        int offset = 0;
        final byte[] r = new byte[32];
        final byte[] s = new byte[32];
        System.arraycopy(signatureBytes, offset, r, 0, 32);
        offset += 32;
        System.arraycopy(signatureBytes, offset, s, 0, 32);
        offset += 32;
        final int v = signatureBytes[offset] + 27;
        message.signature = ECKey.ECDSASignature.fromComponents(r, s, (byte)v);

        final byte[] publicKeyBytes = params.get(1).getRLPData();
        final byte[] bytes = new byte[65];
        System.arraycopy(publicKeyBytes, 0, bytes, 1, 64);
        bytes[0] = 0x04; // uncompressed
        message.publicKey = ECKey.CURVE.getCurve().decodePoint(bytes);

        message.nonce = params.get(2).getRLPData();

        final byte[] versionBytes = params.get(3).getRLPData();
        message.version = ByteUtil.byteArrayToInt(versionBytes);

        return message;
    }

    public byte[] encode() {

        final byte[] rsigPad = new byte[32];
        final byte[] rsig = asUnsignedByteArray(signature.r);
        System.arraycopy(rsig, 0, rsigPad, rsigPad.length - rsig.length, rsig.length);

        final byte[] ssigPad = new byte[32];
        final byte[] ssig = asUnsignedByteArray(signature.s);
        System.arraycopy(ssig, 0, ssigPad, ssigPad.length - ssig.length, ssig.length);

        final byte[] publicKey = new byte[64];
        System.arraycopy(this.publicKey.getEncoded(false), 1, publicKey, 0, publicKey.length);

        final byte[] sigBytes = RLP.encode(merge(rsigPad, ssigPad, new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)}));
        final byte[] publicBytes = RLP.encode(publicKey);
        final byte[] nonceBytes = RLP.encode(nonce);
        final byte[] versionBytes = RLP.encodeInt(version);

        return RLP.encodeList(sigBytes, publicBytes, nonceBytes, versionBytes);
    }

    @Override
    public String toString() {

        final byte[] sigBytes = merge(asUnsignedByteArray(signature.r),
                asUnsignedByteArray(signature.s), new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        return "AuthInitiateMessage{" +
                "\n  sigBytes=" + Hex.toHexString(sigBytes) +
                "\n  publicKey=" + Hex.toHexString(publicKey.getEncoded(false)) +
                "\n  nonce=" + Hex.toHexString(nonce) +
                "\n  version=" + version +
                "\n}";
    }
}
