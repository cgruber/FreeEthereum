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

package org.ethereum.crypto.jce;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;

final class ECAlgorithmParameters {

    private static final String ALGORITHM = "EC";
    private static final String CURVE_NAME = "secp256k1";

  private ECAlgorithmParameters() { }

    public static ECParameterSpec getParameterSpec() {
        try {
            return Holder.INSTANCE.getParameterSpec(ECParameterSpec.class);
        } catch (final InvalidParameterSpecException ex) {
            throw new AssertionError(
                    "Assumed correct key spec statically", ex);
        }
    }

    public static byte[] getASN1Encoding() {
        try {
            return Holder.INSTANCE.getEncoded();
        } catch (final IOException ex) {
            throw new AssertionError(
                    "Assumed algo params has been initialized", ex);
        }
    }

  private static class Holder {
    private static final AlgorithmParameters INSTANCE;

    private static final ECGenParameterSpec SECP256K1_CURVE
        = new ECGenParameterSpec(CURVE_NAME);

    static {
      try {
        INSTANCE = AlgorithmParameters.getInstance(ALGORITHM);
        INSTANCE.init(SECP256K1_CURVE);
      } catch (final NoSuchAlgorithmException ex) {
        throw new AssertionError(
            "Assumed the JRE supports EC algorithm params", ex);
      } catch (final InvalidParameterSpecException ex) {
        throw new AssertionError(
            "Assumed correct key spec statically", ex);
      }
    }
  }
}
