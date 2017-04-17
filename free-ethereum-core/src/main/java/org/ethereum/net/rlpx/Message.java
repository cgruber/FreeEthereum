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
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.FastByteComparisons;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;

import static org.ethereum.util.ByteUtil.merge;

public abstract class Message {

    private byte[] wire;

    private byte[] mdc;
    private byte[] signature;
    private byte[] type;
    private byte[] data;

    public static Message decode(final byte[] wire) {

        if (wire.length < 98) throw new RuntimeException("Bad message");

        final byte[] mdc = new byte[32];
        System.arraycopy(wire, 0, mdc, 0, 32);

        final byte[] signature = new byte[65];
        System.arraycopy(wire, 32, signature, 0, 65);

        final byte[] type = new byte[1];
        type[0] = wire[97];

        final byte[] data = new byte[wire.length - 98];
        System.arraycopy(wire, 98, data, 0, data.length);

        final byte[] mdcCheck = HashUtil.INSTANCE.sha3(wire, 32, wire.length - 32);

        final int check = FastByteComparisons.compareTo(mdc, 0, mdc.length, mdcCheck, 0, mdcCheck.length);

        if (check != 0) throw new RuntimeException("MDC check failed");

        final Message msg;
        if (type[0] == 1) msg = new PingMessage();
        else if (type[0] == 2) msg = new PongMessage();
        else if (type[0] == 3) msg = new FindNodeMessage();
        else if (type[0] == 4) msg = new NeighborsMessage();
        else throw new RuntimeException("Unknown RLPx message: " + type[0]);

        msg.mdc = mdc;
        msg.signature = signature;
        msg.type = type;
        msg.data = data;
        msg.wire = wire;

        msg.parse(data);

        return msg;
    }


    Message encode(final byte[] type, final byte[] data, final ECKey privKey) {

        /* [1] Calc keccak - prepare for sig */
        final byte[] payload = new byte[type.length + data.length];
        payload[0] = type[0];
        System.arraycopy(data, 0, payload, 1, data.length);
        final byte[] forSig = HashUtil.INSTANCE.sha3(payload);

        /* [2] Crate signature*/
        final ECKey.ECDSASignature signature = privKey.sign(forSig);

        signature.v -= 27;

        final byte[] sigBytes =
                merge(BigIntegers.asUnsignedByteArray(32, signature.r),
                        BigIntegers.asUnsignedByteArray(32, signature.s), new byte[]{signature.v});

        // [3] calculate MDC
        final byte[] forSha = merge(sigBytes, type, data);
        final byte[] mdc = HashUtil.INSTANCE.sha3(forSha);

        // wrap all the data in to the packet
        this.mdc = mdc;
        this.signature = sigBytes;
        this.type = type;
        this.data = data;

        this.wire = merge(this.mdc, this.signature, this.type, this.data);

        return this;
    }

    public ECKey getKey() {

        final byte[] r = new byte[32];
        final byte[] s = new byte[32];
        byte v = signature[64];

        // todo: remove this when cpp conclude what they do here
        if (v == 1) v = 28;
        if (v == 0) v = 27;

        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);

        final ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v);
        final byte[] msgHash = HashUtil.INSTANCE.sha3(wire, 97, wire.length - 97);

        ECKey outKey = null;
        try {
            outKey = ECKey.signatureToKey(msgHash, signature);
        } catch (final SignatureException e) {
            e.printStackTrace();
        }

        return outKey;
    }

    public byte[] getNodeId() {
        return getKey().getNodeId();
    }

    public byte[] getPacket() {
        return wire;
    }

    public byte[] getMdc() {
        return mdc;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    protected abstract void parse(byte[] data);

    @Override
    public String toString() {
        return "{" +
                "mdc=" + Hex.toHexString(mdc) +
                ", signature=" + Hex.toHexString(signature) +
                ", type=" + Hex.toHexString(type) +
                ", data=" + Hex.toHexString(data) +
                '}';
    }
}
