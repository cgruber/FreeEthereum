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

package org.ethereum.util;

import org.ethereum.db.ByteArrayWrapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ByteArraySet implements Set<byte[]> {
    private final Set<ByteArrayWrapper> delegate;

    public ByteArraySet() {
        this(new HashSet<>());
    }

    ByteArraySet(final Set<ByteArrayWrapper> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return delegate.contains(new ByteArrayWrapper((byte[]) o));
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<byte[]>() {

            final Iterator<ByteArrayWrapper> it = delegate.iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public byte[] next() {
                return it.next().getData();
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public Object[] toArray() {
        final byte[][] ret = new byte[size()][];

        final ByteArrayWrapper[] arr = delegate.toArray(new ByteArrayWrapper[size()]);
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i].getData();
        }
        return ret;
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(final byte[] bytes) {
        return delegate.add(new ByteArrayWrapper(bytes));
    }

    @Override
    public boolean remove(final Object o) {
        return delegate.remove(new ByteArrayWrapper((byte[]) o));
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean addAll(final Collection<? extends byte[]> c) {
        boolean ret = false;
        for (final byte[] bytes : c) {
            ret |= add(bytes);
        }
        return ret;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(final Object o) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Not implemented");
    }
}
