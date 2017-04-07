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

package org.ethereum.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.AbstractKeyValue;
import org.ethereum.util.Functional;
import org.ethereum.vm.DataWord;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TransactionTouchedStorage {

    private final Map<DataWord, Entry> entries = new HashMap<>();

    public TransactionTouchedStorage() {
    }

    @JsonCreator
    public TransactionTouchedStorage(final Collection<Entry> entries) {
        for (final Entry entry : entries) {
            add(entry);
        }
    }

    @JsonValue
    public Collection<Entry> getEntries() {
        return entries.values();
    }

    public Entry add(final Entry entry) {
        return entries.put(entry.getKey(), entry);
    }

    private Entry add(final Map.Entry<DataWord, DataWord> entry, final boolean changed) {
        return add(new Entry(entry.getKey(), entry.getValue(), changed));
    }

    void addReading(final Map<DataWord, DataWord> entries) {
        if (MapUtils.isEmpty(entries)) return;

        for (final Map.Entry<DataWord, DataWord> entry : entries.entrySet()) {
            if (!this.entries.containsKey(entry.getKey())) add(entry, false);
        }
    }

    void addWriting(final Map<DataWord, DataWord> entries) {
        if (MapUtils.isEmpty(entries)) return;

        for (final Map.Entry<DataWord, DataWord> entry : entries.entrySet()) {
            add(entry, true);
        }
    }

    private Map<DataWord, DataWord> keyValues(final Functional.Function<Entry, Boolean> filter) {
        final Map<DataWord, DataWord> result = new HashMap<>();
        for (final Entry entry : getEntries()) {
            if (filter == null || filter.apply(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;

    }

    public Map<DataWord, DataWord> getChanged() {
        return keyValues(Entry::isChanged);
    }

    public Map<DataWord, DataWord> getReadOnly() {
        return keyValues(new Functional.Function<Entry, Boolean>() {
            @Override
            public Boolean apply(final Entry entry) {
                return !entry.isChanged();
            }
        });
    }

    public Map<DataWord, DataWord> getAll() {
        return keyValues(null);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public static class Entry extends AbstractKeyValue<DataWord, DataWord> {

        private boolean changed;

        public Entry(final DataWord key, final DataWord value, final boolean changed) {
            super(key, value);
            this.changed = changed;
        }

        public Entry() {
            super(null, null);
        }

        @Override
        protected DataWord setKey(final DataWord key) {
            return super.setKey(key);
        }

        @Override
        protected DataWord setValue(final DataWord value) {
            return super.setValue(value);
        }

        public boolean isChanged() {
            return changed;
        }

        public void setChanged(final boolean changed) {
            this.changed = changed;
        }
    }
}
