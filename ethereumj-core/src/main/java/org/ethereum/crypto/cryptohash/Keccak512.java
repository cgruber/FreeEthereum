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

// $Id: Keccak512.java 189 2010-05-14 21:21:46Z tp $

package org.ethereum.crypto.cryptohash;

public class Keccak512 extends KeccakCore {

	/**
	 * Create the engine.
	 */
	public Keccak512()
	{
		super("eth-keccak-512");
	}

	/** @see Digest */
	public Digest copy()
	{
		return copyState(new Keccak512());
	}

	/** @see Digest */
	public int engineGetDigestLength()
	{
		return 64;
	}

	@Override
	protected byte[] engineDigest() {
		return null;
	}

	@Override
    protected void engineUpdate(final byte input) {
    }

	@Override
    protected void engineUpdate(final byte[] input, final int offset, final int len) {
    }
}
