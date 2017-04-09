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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A thread-safe version of {@link Map} in which all operations that change the
 * Map are implemented by making a new copy of the underlying Map.
 *
 * While the creation of a new Map can be expensive, this class is designed for
 * cases in which the primary function is to read data from the Map, not to
 * modify the Map.  Therefore the operations that do not cause a change to this
 * class happen quickly and concurrently.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class CopyOnWriteMap<K, V> implements Map<K, V>, Cloneable {
    private volatile Map<K, V> internalMap;

    /**
     * Creates a new instance of CopyOnWriteMap.
     *
     */
    public CopyOnWriteMap() {
        internalMap = new HashMap<>();
    }

    /**
     * Creates a new instance of CopyOnWriteMap with the specified initial size
     *
     * @param initialCapacity
     *  The initial size of the Map.
     */
    public CopyOnWriteMap(final int initialCapacity) {
        internalMap = new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new instance of CopyOnWriteMap in which the
     * initial data being held by this map is contained in
     * the supplied map.
     *
     * @param data
     *  A Map containing the initial contents to be placed into
     *  this class.
     */
    public CopyOnWriteMap(final Map<K, V> data) {
        internalMap = new HashMap<>(data);
    }

    /**
     * Adds the provided key and value to this map.
     *
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put(final K key, final V value) {
        synchronized (this) {
            final Map<K, V> newMap = new HashMap<>(internalMap);
            final V val = newMap.put(key, value);
            internalMap = newMap;
            return val;
        }
    }

    /**
     * Removed the value and key from this map based on the
     * provided key.
     *
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(final Object key) {
        synchronized (this) {
            final Map<K, V> newMap = new HashMap<>(internalMap);
            final V val = newMap.remove(key);
            internalMap = newMap;
            return val;
        }
    }

    /**
     * Inserts all the keys and values contained in the
     * provided map to this map.
     *
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map<? extends K, ? extends V> newData) {
        synchronized (this) {
            final Map<K, V> newMap = new HashMap<>(internalMap);
            newMap.putAll(newData);
            internalMap = newMap;
        }
    }

    /**
     * Removes all entries in this map.
     *
     * @see java.util.Map#clear()
     */
    public void clear() {
        synchronized (this) {
            internalMap = new HashMap<>();
        }
    }

    // 
    //  Below are methods that do not modify 
    //          the internal Maps            
    /**
     * Returns the number of key/value pairs in this map.
     *
     * @see java.util.Map#size()
     */
    public int size() {
        return internalMap.size();
    }

    /**
     * Returns true if this map is empty, otherwise false.
     *
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    /**
     * Returns true if this map contains the provided key, otherwise
     * this method return false.
     *
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(final Object key) {
        return internalMap.containsKey(key);
    }

    /**
     * Returns true if this map contains the provided value, otherwise
     * this method returns false.
     *
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(final Object value) {
        return internalMap.containsValue(value);
    }

    /**
     * Returns the value associated with the provided key from this
     * map.
     *
     * @see java.util.Map#get(java.lang.Object)
     */
    public V get(final Object key) {
        return internalMap.get(key);
    }

    /**
     * This method will return a read-only {@link Set}.
     */
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    /**
     * This method will return a read-only {@link Collection}.
     */
    public Collection<V> values() {
        return internalMap.values();
    }

    /**
     * This method will return a read-only {@link Set}.
     */
    public Set<Entry<K, V>> entrySet() {
        return internalMap.entrySet();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}