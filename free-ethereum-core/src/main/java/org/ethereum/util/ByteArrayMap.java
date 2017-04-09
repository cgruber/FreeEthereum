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

import java.util.*;

public class ByteArrayMap<V> implements Map<byte[], V> {
    private final Map<ByteArrayWrapper, V> delegate;

    public ByteArrayMap() {
        this(new HashMap<>());
    }

    public ByteArrayMap(final Map<ByteArrayWrapper, V> delegate) {
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
    public boolean containsKey(final Object key) {
        return delegate.containsKey(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        return delegate.get(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public V put(final byte[] key, final V value) {
        return delegate.put(new ByteArrayWrapper(key), value);
    }

    @Override
    public V remove(final Object key) {
        return delegate.remove(new ByteArrayWrapper((byte[]) key));
    }

    @Override
    public void putAll(final Map<? extends byte[], ? extends V> m) {
        for (final Entry<? extends byte[], ? extends V> entry : m.entrySet()) {
            delegate.put(new ByteArrayWrapper(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<byte[]> keySet() {
        return new ByteArraySet(new SetAdapter<>(delegate));
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<byte[], V>> entrySet() {
        return new MapEntrySet(delegate.entrySet());
    }

    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private class MapEntrySet implements Set<Map.Entry<byte[], V>> {
        private final Set<Map.Entry<ByteArrayWrapper, V>> delegate;

        private MapEntrySet(final Set<Entry<ByteArrayWrapper, V>> delegate) {
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
            throw new RuntimeException("Not implemented");
        }

        @Override
        public Iterator<Entry<byte[], V>> iterator() {
            final Iterator<Entry<ByteArrayWrapper, V>> it = delegate.iterator();
            return new Iterator<Entry<byte[], V>>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<byte[], V> next() {
                    final Entry<ByteArrayWrapper, V> next = it.next();
                    return new AbstractMap.SimpleImmutableEntry(next.getKey().getData(), next.getValue());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public Object[] toArray() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean add(final Entry<byte[], V> vEntry) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean remove(final Object o) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean addAll(final Collection<? extends Entry<byte[], V>> c) {
            throw new RuntimeException("Not implemented");
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
            throw new RuntimeException("Not implemented");

        }
    }
}
