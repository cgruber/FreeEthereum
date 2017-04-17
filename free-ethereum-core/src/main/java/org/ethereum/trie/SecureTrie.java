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

package org.ethereum.trie;

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.Source;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

public class SecureTrie extends TrieImpl {

    public SecureTrie(final byte[] root) {
        super(root);
    }

    public SecureTrie(final Source<byte[], byte[]> cache) {
        super(cache, null);
    }

    public SecureTrie(final Source<byte[], byte[]> cache, final byte[] root) {
        super(cache, root);
    }

    @Override
    public byte[] get(final byte[] key) {
        return super.get(HashUtil.INSTANCE.sha3(key));
    }

    @Override
    public void put(final byte[] key, final byte[] value) {
        super.put(HashUtil.INSTANCE.sha3(key), value);
    }

    @Override
    public void delete(final byte[] key) {
        put(key, EMPTY_BYTE_ARRAY);
    }
}
