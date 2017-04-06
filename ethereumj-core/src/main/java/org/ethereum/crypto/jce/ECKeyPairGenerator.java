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

import java.security.*;
import java.security.spec.ECGenParameterSpec;

public final class ECKeyPairGenerator {

    private static final String ALGORITHM = "EC";
    private static final String CURVE_NAME = "secp256k1";

  private static final String algorithmAssertionMsg =
      "Assumed JRE supports EC key pair generation";

  private static final String keySpecAssertionMsg =
      "Assumed correct key spec statically";

  private static final ECGenParameterSpec SECP256K1_CURVE
      = new ECGenParameterSpec(CURVE_NAME);

  private ECKeyPairGenerator() { }

  public static KeyPair generateKeyPair() {
    return Holder.INSTANCE.generateKeyPair();
  }

  public static KeyPairGenerator getInstance(final String provider, final SecureRandom random) throws NoSuchProviderException {
    try {
      final KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM, provider);
      gen.initialize(SECP256K1_CURVE, random);
      return gen;
    } catch (final NoSuchAlgorithmException ex) {
      throw new AssertionError(algorithmAssertionMsg, ex);
    } catch (final InvalidAlgorithmParameterException ex) {
      throw new AssertionError(keySpecAssertionMsg, ex);
    }
  }

  public static KeyPairGenerator getInstance(final Provider provider, final SecureRandom random) {
    try {
      final KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM, provider);
      gen.initialize(SECP256K1_CURVE, random);
      return gen;
    } catch (final NoSuchAlgorithmException ex) {
      throw new AssertionError(algorithmAssertionMsg, ex);
    } catch (final InvalidAlgorithmParameterException ex) {
      throw new AssertionError(keySpecAssertionMsg, ex);
    }
  }

    private static class Holder {
        private static final KeyPairGenerator INSTANCE;

        static {
            try {
                INSTANCE = KeyPairGenerator.getInstance(ALGORITHM);
                INSTANCE.initialize(SECP256K1_CURVE);
            } catch (final NoSuchAlgorithmException ex) {
                throw new AssertionError(algorithmAssertionMsg, ex);
            } catch (final InvalidAlgorithmParameterException ex) {
                throw new AssertionError(keySpecAssertionMsg, ex);
            }
        }
    }
}
