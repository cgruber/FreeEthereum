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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.ethereum.net.swarm.Util;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ethereum.util.RLP.decode2OneItem;

public class FrameCodec {
    private final StreamCipher enc;
    private final StreamCipher dec;
    private final KeccakDigest egressMac;
    private final KeccakDigest ingressMac;
    private final byte[] mac;
    private boolean isHeadRead;
    private int totalBodySize;
    private int contextId = -1;
    private int totalFrameSize = -1;

    public FrameCodec(final EncryptionHandshake.Secrets secrets) {
        this.mac = secrets.getMac();
        final int blockSize = secrets.getAes().length * 8;
        enc = new SICBlockCipher(new AESFastEngine());
        enc.init(true, new ParametersWithIV(new KeyParameter(secrets.getAes()), new byte[blockSize / 8]));
        dec = new SICBlockCipher(new AESFastEngine());
        dec.init(false, new ParametersWithIV(new KeyParameter(secrets.getAes()), new byte[blockSize / 8]));
        egressMac = secrets.getEgressMac();
        ingressMac = secrets.getIngressMac();
    }

    private AESFastEngine makeMacCipher() {
        // Stateless AES encryption
        final AESFastEngine macc = new AESFastEngine();
        macc.init(true, new KeyParameter(mac));
        return macc;
    }

    public void writeFrame(final Frame frame, final ByteBuf buf) throws IOException {
        writeFrame(frame, new ByteBufOutputStream(buf));
    }

    public void writeFrame(final Frame frame, final OutputStream out) throws IOException {
        final byte[] headBuffer = new byte[32];
        final byte[] ptype = RLP.encodeInt((int) frame.type); // FIXME encodeLong
        final int totalSize = frame.size + ptype.length;
        headBuffer[0] = (byte)(totalSize >> 16);
        headBuffer[1] = (byte)(totalSize >> 8);
        headBuffer[2] = (byte)(totalSize);

        final List<byte[]> headerDataElems = new ArrayList<>();
        headerDataElems.add(RLP.encodeInt(0));
        if (frame.contextId >= 0) headerDataElems.add(RLP.encodeInt(frame.contextId));
        if (frame.totalFrameSize >= 0) headerDataElems.add(RLP.encodeInt(frame.totalFrameSize));

        final byte[] headerData = RLP.encodeList(headerDataElems.toArray(new byte[0][]));
        System.arraycopy(headerData, 0, headBuffer, 3, headerData.length);

        enc.processBytes(headBuffer, 0, 16, headBuffer, 0);

        // Header MAC
        updateMac(egressMac, headBuffer, 0, headBuffer, 16, true);

        final byte[] buff = new byte[256];
        out.write(headBuffer);
        enc.processBytes(ptype, 0, ptype.length, buff, 0);
        out.write(buff, 0, ptype.length);
        egressMac.update(buff, 0, ptype.length);
        while (true) {
            final int n = frame.payload.read(buff);
            if (n <= 0) break;
            enc.processBytes(buff, 0, n, buff, 0);
            egressMac.update(buff, 0, n);
            out.write(buff, 0, n);
        }
        final int padding = 16 - (totalSize % 16);
        final byte[] pad = new byte[16];
        if (padding < 16) {
            enc.processBytes(pad, 0, padding, buff, 0);
            egressMac.update(buff, 0, padding);
            out.write(buff, 0, padding);
        }

        // Frame MAC
        final byte[] macBuffer = new byte[egressMac.getDigestSize()];
        doSum(egressMac, macBuffer); // fmacseed
        updateMac(egressMac, macBuffer, 0, macBuffer, 0, true);
        out.write(macBuffer, 0, 16);
    }

    public List<Frame> readFrames(final ByteBuf buf) throws IOException {
        try (ByteBufInputStream bufInputStream = new ByteBufInputStream(buf)) {
            return readFrames(bufInputStream);
        }
    }

    public List<Frame> readFrames(final DataInput inp) throws IOException {
        if (!isHeadRead) {
            final byte[] headBuffer = new byte[32];
            try {
                inp.readFully(headBuffer);
            } catch (final EOFException e) {
                return null;
            }

            // Header MAC
            updateMac(ingressMac, headBuffer, 0, headBuffer, 16, false);

            dec.processBytes(headBuffer, 0, 16, headBuffer, 0);
            totalBodySize = headBuffer[0] & 0xFF;
            totalBodySize = (totalBodySize << 8) + (headBuffer[1] & 0xFF);
            totalBodySize = (totalBodySize << 8) + (headBuffer[2] & 0xFF);

            final RLPList rlpList = (RLPList) decode2OneItem(headBuffer, 3);

            final int protocol = Util.rlpDecodeInt(rlpList.get(0));
            contextId = -1;
            totalFrameSize = -1;
            if (rlpList.size() > 1) {
                contextId = Util.rlpDecodeInt(rlpList.get(1));
                if (rlpList.size() > 2) {
                    totalFrameSize = Util.rlpDecodeInt(rlpList.get(2));
                }
            }

            isHeadRead = true;
        }

        int padding = 16 - (totalBodySize % 16);
        if (padding == 16) padding = 0;
        final int macSize = 16;
        final byte[] buffer = new byte[totalBodySize + padding + macSize];
        try {
            inp.readFully(buffer);
        } catch (final EOFException e) {
            return null;
        }
        final int frameSize = buffer.length - macSize;
        ingressMac.update(buffer, 0, frameSize);
        dec.processBytes(buffer, 0, frameSize, buffer, 0);
        int pos = 0;
        final long type = RLP.decodeInt(buffer, pos); // FIXME long
        pos = RLP.getNextElementIndex(buffer, pos);
        final InputStream payload = new ByteArrayInputStream(buffer, pos, totalBodySize - pos);
        final int size = totalBodySize - pos;
        final byte[] macBuffer = new byte[ingressMac.getDigestSize()];

        // Frame MAC
        doSum(ingressMac, macBuffer); // fmacseed
        updateMac(ingressMac, macBuffer, 0, buffer, frameSize, false);

        isHeadRead = false;
        final Frame frame = new Frame(type, size, payload);
        frame.contextId = contextId;
        frame.totalFrameSize = totalFrameSize;
        return Collections.singletonList(frame);
    }

    private byte[] updateMac(final KeccakDigest mac, final byte[] seed, final int offset, final byte[] out, final int outOffset, final boolean egress) throws IOException {
        final byte[] aesBlock = new byte[mac.getDigestSize()];
        doSum(mac, aesBlock);
        makeMacCipher().processBlock(aesBlock, 0, aesBlock, 0);
        // Note that although the mac digest size is 32 bytes, we only use 16 bytes in the computation
        final int length = 16;
        for (int i = 0; i < length; i++) {
            aesBlock[i] ^= seed[i + offset];
        }
        mac.update(aesBlock, 0, length);
        final byte[] result = new byte[mac.getDigestSize()];
        doSum(mac, result);
        if (egress) {
            System.arraycopy(result, 0, out, outOffset, length);
        } else {
            for (int i = 0; i < length; i++) {
                if (out[i + outOffset] != result[i]) {
                    throw new IOException("MAC mismatch");
                }
            }
        }
        return result;
    }

    private void doSum(final KeccakDigest mac, final byte[] out) {
        // doFinal without resetting the MAC by using clone of digest state
        new KeccakDigest(mac).doFinal(out, 0);
    }

    public static class Frame {
        final long type;
        final int size;
        final InputStream payload;

        int totalFrameSize = -1;
        int contextId = -1;

        public Frame(final long type, final int size, final InputStream payload) {
            this.type = type;
            this.size = size;
            this.payload = payload;
        }

        public Frame(final int type, final byte[] payload) {
            this.type = type;
            this.size = payload.length;
            this.payload = new ByteArrayInputStream(payload);
        }

        public int getSize() {
            return size;
        }

        public long getType() {
            return type;
        }

        public InputStream getStream() {
            return payload;
        }

        public boolean isChunked() {
            return contextId >= 0;
        }

    }

}
