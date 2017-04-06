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

import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.AbstractList;

/**
 * Stores List structure in Source structure
 *
 * Created by Anton Nashatyrev on 17.03.2016.
 */
public class DataSourceArray<V> extends AbstractList<V> {
    private static final byte[] SIZE_KEY = Hex.decode("FFFFFFFFFFFFFFFF");
    private final ObjectDataSource<V> src;
    private int size = -1;

    public DataSourceArray(final ObjectDataSource<V> src) {
        this.src = src;
    }

    public synchronized boolean flush() {
        return src.flush();
    }

    @Override
    public synchronized V set(final int idx, final V value) {
        if (idx >= size()) {
            setSize(idx + 1);
        }
        src.put(ByteUtil.intToBytes(idx), value);
        return value;
    }

    @Override
    public synchronized void add(final int index, final V element) {
        set(index, element);
    }

    @Override
    public synchronized V remove(final int index) {
        throw new RuntimeException("Not supported yet.");
    }

    @Override
    public synchronized V get(final int idx) {
        if (idx < 0 || idx >= size()) throw new IndexOutOfBoundsException(idx + " > " + size);
        return src.get(ByteUtil.intToBytes(idx));
    }

    @Override
    public synchronized int size() {
        if (size < 0) {
            final byte[] sizeBB = src.getSource().get(SIZE_KEY);
            size = sizeBB == null ? 0 : ByteUtil.byteArrayToInt(sizeBB);
        }
        return size;
    }

    private synchronized void setSize(final int newSize) {
        size = newSize;
        src.getSource().put(SIZE_KEY, ByteUtil.intToBytes(newSize));
    }
}
