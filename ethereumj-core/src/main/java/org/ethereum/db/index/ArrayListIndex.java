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

package org.ethereum.db.index;

import java.util.*;

/**
 * @author Mikhail Kalinin
 * @since 28.01.2016
 */
public class ArrayListIndex implements Index {
    private final List<Long> index;

    public ArrayListIndex(final Collection<Long> numbers) {
        index = new ArrayList<>(numbers);
        sort();
    }

    @Override
    public synchronized void addAll(final Collection<Long> nums) {
        index.addAll(nums);
        sort();
    }

    @Override
    public synchronized void add(final Long num) {
        index.add(num);
        sort();
    }

    @Override
    public synchronized Long peek() {
        return index.get(0);
    }

    @Override
    public synchronized Long poll() {
        final Long num = index.get(0);
        index.remove(0);
        return num;
    }

    @Override
    public synchronized boolean contains(final Long num) {
        return Collections.binarySearch(index, num) >= 0;
    }

    @Override
    public synchronized boolean isEmpty() {
        return index.isEmpty();
    }

    @Override
    public synchronized int size() {
        return index.size();
    }

    @Override
    public synchronized void clear() {
        index.clear();
    }

    private void sort() {
        Collections.sort(index);
    }

    @Override
    public synchronized Iterator<Long> iterator() {
        return new ArrayList<>(index).iterator();
    }

    public synchronized void removeAll(final Collection<Long> indexes) {
        index.removeAll(indexes);
    }

    @Override
    public synchronized Long peekLast() {

        if (index.isEmpty()) return null;
        return index.get(index.size() - 1);
    }

    @Override
    public synchronized void remove(final Long num) {
        index.remove(num);
    }
}
