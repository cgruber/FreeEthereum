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
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import static org.ethereum.util.ByteUtil.merge;
import static org.spongycastle.util.BigIntegers.asUnsignedByteArray;

/**
 * Authentication initiation message, to be wrapped inside
 *
 * Created by devrandom on 2015-04-07.
 */
public class AuthInitiateMessage {
    ECKey.ECDSASignature signature; // 65 bytes
    byte[] ephemeralPublicHash; // 32 bytes
    ECPoint publicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    boolean isTokenUsed; // 1 byte - 0x00 or 0x01

    public AuthInitiateMessage() {
    }

    public static int getLength() {
        return 65+32+64+32+1;
    }

    static AuthInitiateMessage decode(final byte[] wire) {
        final AuthInitiateMessage message = new AuthInitiateMessage();
        int offset = 0;
        final byte[] r = new byte[32];
        final byte[] s = new byte[32];
        System.arraycopy(wire, offset, r, 0, 32);
        offset += 32;
        System.arraycopy(wire, offset, s, 0, 32);
        offset += 32;
        final int v = wire[offset] + 27;
        offset += 1;
        message.signature = ECKey.ECDSASignature.fromComponents(r, s, (byte)v);
        message.ephemeralPublicHash = new byte[32];
        System.arraycopy(wire, offset, message.ephemeralPublicHash, 0, 32);
        offset += 32;
        final byte[] bytes = new byte[65];
        System.arraycopy(wire, offset, bytes, 1, 64);
        offset += 64;
        bytes[0] = 0x04; // uncompressed
        message.publicKey = ECKey.CURVE.getCurve().decodePoint(bytes);
        message.nonce = new byte[32];
        System.arraycopy(wire, offset, message.nonce, 0, 32);
        offset += message.nonce.length;
        final byte tokenUsed = wire[offset];
        offset += 1;
        if (tokenUsed != 0x00 && tokenUsed != 0x01)
            throw new RuntimeException("invalid boolean"); // TODO specific exception
        message.isTokenUsed = (tokenUsed == 0x01);
        return message;
    }

    public byte[] encode() {

        final byte[] rsigPad = new byte[32];
        final byte[] rsig = asUnsignedByteArray(signature.r);
        System.arraycopy(rsig, 0, rsigPad, rsigPad.length - rsig.length, rsig.length);

        final byte[] ssigPad = new byte[32];
        final byte[] ssig = asUnsignedByteArray(signature.s);
        System.arraycopy(ssig, 0, ssigPad, ssigPad.length - ssig.length, ssig.length);

        final byte[] sigBytes = merge(rsigPad, ssigPad, new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        final byte[] buffer = new byte[getLength()];
        int offset = 0;
        System.arraycopy(sigBytes, 0, buffer, offset, sigBytes.length);
        offset += sigBytes.length;
        System.arraycopy(ephemeralPublicHash, 0, buffer, offset, ephemeralPublicHash.length);
        offset += ephemeralPublicHash.length;
        final byte[] publicBytes = publicKey.getEncoded(false);
        System.arraycopy(publicBytes, 1, buffer, offset, publicBytes.length - 1);
        offset += publicBytes.length - 1;
        System.arraycopy(nonce, 0, buffer, offset, nonce.length);
        offset += nonce.length;
        buffer[offset] = (byte)(isTokenUsed ? 0x01 : 0x00);
        offset += 1;
        return buffer;
    }

    @Override
    public String toString() {

        final byte[] sigBytes = merge(asUnsignedByteArray(signature.r),
                asUnsignedByteArray(signature.s), new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        return "AuthInitiateMessage{" +
                "\n  sigBytes=" + Hex.toHexString(sigBytes) +
                "\n  ephemeralPublicHash=" + Hex.toHexString(ephemeralPublicHash) +
                "\n  publicKey=" + Hex.toHexString(publicKey.getEncoded(false)) +
                "\n  nonce=" + Hex.toHexString(nonce) +
                "\n}";
    }
}
