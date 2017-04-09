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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;

public final class ECSignatureFactory {

    private static final String RAW_ALGORITHM = "NONEwithECDSA";

  private static final String rawAlgorithmAssertionMsg =
      "Assumed the JRE supports NONEwithECDSA signatures";

  private ECSignatureFactory() { }

  public static Signature getRawInstance() {
    try {
      return Signature.getInstance(RAW_ALGORITHM);
    } catch (final NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }

  public static Signature getRawInstance(final String provider) throws NoSuchProviderException {
    try {
      return Signature.getInstance(RAW_ALGORITHM, provider);
    } catch (final NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }

  public static Signature getRawInstance(final Provider provider) {
    try {
      return Signature.getInstance(RAW_ALGORITHM, provider);
    } catch (final NoSuchAlgorithmException ex) {
      throw new AssertionError(rawAlgorithmAssertionMsg, ex);
    }
  }
}
