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

package org.ethereum.datasource;

import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;

import java.util.Arrays;

/**
 * 'Reference counting' Source. Unlike regular Source if an entry was
 * e.g. 'put' twice it is actually deleted when 'delete' is called twice
 * I.e. each put increments counter and delete decrements counter, the
 * entry is deleted when the counter becomes zero.
 *
 * Please note that the counting mechanism makes sense only for
 * {@link HashedKeySource} like Sources when any taken key can correspond to
 * the only value
 *
 * This Source is constrained to byte[] values only as the counter
 * needs to be encoded to the backing Source value as byte[]
 *
 * Created by Anton Nashatyrev on 08.11.2016.
 */
public class CountingBytesSource extends AbstractChainedSource<byte[], byte[], byte[], byte[]>
        implements HashedKeySource<byte[], byte[]> {

    private final byte[] filterKey = HashUtil.INSTANCE.sha3("countingStateFilter".getBytes());
    private QuotientFilter filter;
    private boolean dirty = false;

    public CountingBytesSource(final Source<byte[], byte[]> src) {
        this(src, false);

    }

    public CountingBytesSource(final Source<byte[], byte[]> src, final boolean bloom) {
        super(src);
        final byte[] filterBytes = src.get(filterKey);
        if (bloom) {
            if (filterBytes != null) {
                filter = QuotientFilter.deserialize(filterBytes);
            } else {
                filter = QuotientFilter.create(5_000_000, 10_000);
            }
        }
    }

    @Override
    public void put(final byte[] key, final byte[] val) {
        if (val == null) {
            delete(key);
            return;
        }

        synchronized (this) {
            final byte[] srcVal = getSource().get(key);
            final int srcCount = decodeCount(srcVal);
            if (srcCount >= 1) {
                if (filter != null) filter.insert(key);
                dirty = true;
            }
            getSource().put(key, encodeCount(val, srcCount + 1));
        }
    }

    @Override
    public byte[] get(final byte[] key) {
        return decodeValue(getSource().get(key));
    }

    @Override
    public void delete(final byte[] key) {
        synchronized (this) {
            final int srcCount;
            byte[] srcVal = null;
            if (filter == null || filter.maybeContains(key)) {
                srcVal = getSource().get(key);
                srcCount = decodeCount(srcVal);
            } else {
                srcCount = 1;
            }
            if (srcCount > 1) {
                getSource().put(key, encodeCount(decodeValue(srcVal), srcCount - 1));
            } else {
                getSource().delete(key);
            }
        }
    }

    @Override
    protected boolean flushImpl() {
        if (filter != null && dirty) {
            final byte[] filterBytes;
            synchronized (this) {
                filterBytes = filter.serialize();
            }
            getSource().put(filterKey, filterBytes);
            dirty = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Extracts value from the backing Source counter + value byte array
     */
    private byte[] decodeValue(final byte[] srcVal) {
        return srcVal == null ? null : Arrays.copyOfRange(srcVal, RLP.decode(srcVal, 0).getPos(), srcVal.length);
    }

    /**
     * Extracts counter from the backing Source counter + value byte array
     */
    private int decodeCount(final byte[] srcVal) {
        return srcVal == null ? 0 : ByteUtil.byteArrayToInt((byte[]) RLP.decode(srcVal, 0).getDecoded());
    }

    /**
     * Composes value and counter into backing Source value
     */
    private byte[] encodeCount(final byte[] val, final int count) {
        return ByteUtil.merge(RLP.encodeInt(count), val);
    }
}
