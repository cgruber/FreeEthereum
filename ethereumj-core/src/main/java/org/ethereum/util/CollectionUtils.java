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

import java.util.*;

/**
 * @author Mikhail Kalinin
 * @since 14.07.2015
 */
public class CollectionUtils {

    public static <K, V> List<V> collectList(final Collection<K> items, final Functional.Function<K, V> collector) {
        final List<V> collected = new ArrayList<>(items.size());
        for (final K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <K, V> Set<V> collectSet(final Collection<K> items, final Functional.Function<K, V> collector) {
        final Set<V> collected = new HashSet<>();
        for (final K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <T> List<T> truncate(final List<T> items, final int limit) {
        if(limit > items.size()) {
            return new ArrayList<>(items);
        }
        final List<T> truncated = new ArrayList<>(limit);
        for (final T item : items) {
            truncated.add(item);
            if(truncated.size() == limit) {
                break;
            }
        }
        return truncated;
    }

    public static <T> List<T> selectList(final Collection<T> items, final Functional.Predicate<T> predicate) {
        final List<T> selected = new ArrayList<>();
        for (final T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static <T> Set<T> selectSet(final Collection<T> items, final Functional.Predicate<T> predicate) {
        final Set<T> selected = new HashSet<>();
        for (final T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }
}
