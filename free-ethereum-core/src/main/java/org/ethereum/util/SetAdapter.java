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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class SetAdapter<E> implements Set<E> {
    private static final Object DummyValue = new Object();
    private final Map<E, Object> delegate;

    public SetAdapter(final Map<E, ?> delegate) {
        this.delegate = (Map<E, Object>) delegate;
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
        return delegate.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return delegate.keySet().toArray(a);
    }

    @Override
    public boolean add(final E e) {
        return delegate.put(e, DummyValue) == null;
    }

    @Override
    public boolean remove(final Object o) {
        return delegate.remove(o) != null;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return delegate.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        boolean ret = false;
        for (final E e : c) {
            ret |= add(e);
        }
        return ret;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new RuntimeException("Not implemented"); // TODO add later if required
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean ret = false;
        for (final Object e : c) {
            ret |= remove(e);
        }
        return ret;
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
