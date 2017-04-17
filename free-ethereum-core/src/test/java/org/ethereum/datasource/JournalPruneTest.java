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
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.util.ByteUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JournalPruneTest {

    private void checkDb(final StringJDS db, final String... keys) {
        assertEquals(keys.length, db.mapDB.keys().size());
        for (final String key : keys) {
            assertTrue(db.get(key.getBytes()) != null);
        }
    }

    private void putKeys(final StringJDS db, final String... keys) {
        for (final String key : keys) {
            db.put(key.getBytes(), key.getBytes());
        }
    }

    @Test
    public void simpleTest() {
        final StringJDS jds = new StringJDS();

        putKeys(jds, "a1", "a2");

        jds.put("a3");
        jds.delete("a2");
        jds.commitUpdates(hashInt(1));
        jds.put("a2");
        jds.delete("a3");
        jds.commitUpdates(hashInt(2));
        jds.delete("a2");
        jds.commitUpdates(hashInt(3));

        jds.persistUpdate(hashInt(1));
        checkDb(jds, "a1", "a2", "a3");

        jds.persistUpdate(hashInt(2));
        checkDb(jds, "a1", "a2");

        jds.persistUpdate(hashInt(3));
        checkDb(jds, "a1");

        assertEquals(0, ((HashMapDB) jds.journal).getStorage().size());
    }

    @Test
    public void forkTest1() {
        final StringJDS jds = new StringJDS();

        putKeys(jds, "a1", "a2", "a3");

        jds.put("a4");
        jds.put("a1");
        jds.delete("a2");
        jds.commitUpdates(hashInt(1));
        jds.put("a5");
        jds.delete("a3");
        jds.put("a2");
        jds.put("a1");
        jds.commitUpdates(hashInt(2));

        checkDb(jds, "a1", "a2", "a3", "a4", "a5");

        jds.persistUpdate(hashInt(1));
        jds.revertUpdate(hashInt(2));
        checkDb(jds, "a1", "a3", "a4");

        assertEquals(0, ((HashMapDB) jds.journal).getStorage().size());
    }

    @Test
    public void forkTest2() {
        final StringJDS jds = new StringJDS();

        putKeys(jds, "a1", "a2", "a3");

        jds.delete("a1");
        jds.delete("a3");
        jds.commitUpdates(hashInt(1));
        jds.put("a4");
        jds.commitUpdates(hashInt(2));
        jds.commitUpdates(hashInt(3));
        jds.put("a1");
        jds.delete("a2");
        jds.commitUpdates(hashInt(4));
        jds.put("a4");
        jds.commitUpdates(hashInt(5));
        jds.put("a3");
        jds.commitUpdates(hashInt(6));

        checkDb(jds, "a1", "a2", "a3", "a4");

        jds.persistUpdate(hashInt(1));
        jds.revertUpdate(hashInt(2));
        checkDb(jds, "a1", "a2", "a3", "a4");

        jds.persistUpdate(hashInt(3));
        jds.revertUpdate(hashInt(4));
        jds.revertUpdate(hashInt(5));
        checkDb(jds, "a2", "a3");

        jds.persistUpdate(hashInt(6));
        checkDb(jds, "a2", "a3");

        assertEquals(0, ((HashMapDB) jds.journal).getStorage().size());
    }

    @Test
    public void forkTest3() {
        final StringJDS jds = new StringJDS();

        putKeys(jds, "a1");

        jds.put("a2");
        jds.commitUpdates(hashInt(1));
        jds.put("a1");
        jds.put("a2");
        jds.put("a3");
        jds.commitUpdates(hashInt(2));
        jds.put("a1");
        jds.put("a2");
        jds.put("a3");
        jds.commitUpdates(hashInt(3));

        checkDb(jds, "a1", "a2", "a3");

        jds.persistUpdate(hashInt(1));
        jds.revertUpdate(hashInt(2));
        jds.revertUpdate(hashInt(3));
        checkDb(jds, "a1", "a2");

        assertEquals(0, ((HashMapDB) jds.journal).getStorage().size());
    }

    private byte[] hashInt(final int i) {
        return HashUtil.INSTANCE.sha3(ByteUtil.intToBytes(i));
    }

    class StringJDS extends JournalSource<byte[]> {
        final HashMapDB<byte[]> mapDB;
        final Source<byte[], byte[]> db;

        public StringJDS() {
            this(new HashMapDB<>());
        }

        private StringJDS(final HashMapDB<byte[]> mapDB) {
            this(mapDB, new CountingBytesSource(mapDB));
        }

        private StringJDS(final HashMapDB<byte[]> mapDB, final Source<byte[], byte[]> db) {
            super(db);
            this.db = db;
            this.mapDB = mapDB;
        }

        public synchronized void put(final String key) {
            super.put(key.getBytes(), key.getBytes());
        }

        public synchronized void delete(final String key) {
            super.delete(key.getBytes());
        }

        public String get(final String key) {
            return new String(super.get(key.getBytes()));
        }
    }
}
