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

package org.ethereum.crypto;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.DerivationFunction;
import org.spongycastle.crypto.DerivationParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.params.MGFParameters;

/**
 * This class is borrowed from spongycastle project
 * The only change made is addition of 'counterStart' parameter to
 * conform to Crypto++ capabilities
 */
class MGF1BytesGeneratorExt implements DerivationFunction {
    private final Digest digest;
    private final int hLen;
    private final int counterStart;
    private byte[] seed;

    public MGF1BytesGeneratorExt(final Digest digest, final int counterStart) {
        this.digest = digest;
        this.hLen = digest.getDigestSize();
        this.counterStart = counterStart;
    }

    public void init(final DerivationParameters param) {
        if(!(param instanceof MGFParameters)) {
            throw new IllegalArgumentException("MGF parameters required for MGF1Generator");
        } else {
            final MGFParameters p = (MGFParameters) param;
            this.seed = p.getSeed();
        }
    }

    public Digest getDigest() {
        return this.digest;
    }

    private void ItoOSP(final int i, final byte[] sp) {
        sp[0] = (byte)(i >>> 24);
        sp[1] = (byte)(i >>> 16);
        sp[2] = (byte)(i >>> 8);
        sp[3] = (byte) (i);
    }

    public int generateBytes(final byte[] out, final int outOff, final int len) throws DataLengthException, IllegalArgumentException {
        if(out.length - len < outOff) {
            throw new DataLengthException("output buffer too small");
        } else {
            final byte[] hashBuf = new byte[this.hLen];
            final byte[] C = new byte[4];
            int counter = 0;
            int hashCounter = counterStart;
            this.digest.reset();
            if(len > this.hLen) {
                do {
                    this.ItoOSP(hashCounter++, C);
                    this.digest.update(this.seed, 0, this.seed.length);
                    this.digest.update(C, 0, C.length);
                    this.digest.doFinal(hashBuf, 0);
                    System.arraycopy(hashBuf, 0, out, outOff + counter * this.hLen, this.hLen);
                    ++counter;
                } while(counter < len / this.hLen);
            }

            if(counter * this.hLen < len) {
                this.ItoOSP(hashCounter, C);
                this.digest.update(this.seed, 0, this.seed.length);
                this.digest.update(C, 0, C.length);
                this.digest.doFinal(hashBuf, 0);
                System.arraycopy(hashBuf, 0, out, outOff + counter * this.hLen, len - counter * this.hLen);
            }

            return len;
        }
    }
}
