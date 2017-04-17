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
import org.ethereum.db.SlowHashMapDb;
import org.ethereum.db.StateSource;
import org.ethereum.util.Utils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

import static org.ethereum.util.ByteUtil.intToBytes;
import static org.spongycastle.util.encoders.Hex.decode;

public class AsyncWriteCacheTest {

    private volatile boolean flushing;

    @Test
    public void simpleTest1() {
        final SlowHashMapDb<String> db = new SlowHashMapDb<String>().withDelay(100);
        final AsyncWriteCache<byte[], String> cache = new AsyncWriteCache<byte[], String>(db) {
            @Override
            protected WriteCache<byte[], String> createCache(final Source<byte[], String> source) {
                return new WriteCache.BytesKey<String>(source, WriteCache.CacheType.SIMPLE) {
                    @Override
                    public boolean flush() {
                        flushing = true;
                        System.out.println("Flushing started");
                        final boolean ret = super.flush();
                        System.out.println("Flushing complete");
                        flushing = false;
                        return ret;
                    }
                };
            }
        };

        cache.put(decode("1111"), "1111");
        cache.flush();
        assert Objects.equals(cache.get(decode("1111")), "1111");

        while (!flushing);

        System.out.println("get");
        assert Objects.equals(cache.get(decode("1111")), "1111");
        System.out.println("put");
        cache.put(decode("2222"), "2222");
        System.out.println("get");
        assert flushing;

        while (flushing) {
            assert Objects.equals(cache.get(decode("2222")), "2222");
            assert Objects.equals(cache.get(decode("1111")), "1111");
        }
        assert Objects.equals(cache.get(decode("2222")), "2222");
        assert Objects.equals(cache.get(decode("1111")), "1111");

        cache.put(decode("1111"), "1112");

        cache.flush();
        assert Objects.equals(cache.get(decode("1111")), "1112");
        assert Objects.equals(cache.get(decode("2222")), "2222");

        while (!flushing);

        System.out.println("Second flush");
        cache.flush();
        System.out.println("Second flush complete");

        assert Objects.equals(cache.get(decode("1111")), "1112");
        assert Objects.equals(cache.get(decode("2222")), "2222");

        System.out.println("put");
        cache.put(decode("3333"), "3333");

        assert Objects.equals(cache.get(decode("1111")), "1112");
        assert Objects.equals(cache.get(decode("2222")), "2222");
        assert Objects.equals(cache.get(decode("3333")), "3333");

        System.out.println("Second flush");
        cache.flush();
        System.out.println("Second flush complete");

        assert Objects.equals(cache.get(decode("1111")), "1112");
        assert Objects.equals(cache.get(decode("2222")), "2222");
        assert Objects.equals(cache.get(decode("3333")), "3333");
        assert Objects.equals(db.get(decode("1111")), "1112");
        assert Objects.equals(db.get(decode("2222")), "2222");
        assert Objects.equals(db.get(decode("3333")), "3333");
    }

    @Ignore
    @Test
    public void highLoadTest1() throws InterruptedException {
        final SlowHashMapDb<byte[]> db = new SlowHashMapDb<byte[]>() {
            @Override
            public void updateBatch(final Map<byte[], byte[]> rows) {
                Utils.sleep(10000);
                super.updateBatch(rows);
            }
        };
        final StateSource stateSource = new StateSource(db, false);
        stateSource.getReadCache().withMaxCapacity(1);

        stateSource.put(HashUtil.INSTANCE.sha3(intToBytes(1)), intToBytes(1));
        stateSource.put(HashUtil.INSTANCE.sha3(intToBytes(2)), intToBytes(2));

        System.out.println("Flush...");
        stateSource.flush();
        System.out.println("Flush!");

        Thread.sleep(100);
        System.out.println("Get...");
        final byte[] bytes1 = stateSource.get(HashUtil.INSTANCE.sha3(intToBytes(1)));
        System.out.println("Get!: " + bytes1);
        final byte[] bytes2 = stateSource.get(HashUtil.INSTANCE.sha3(intToBytes(2)));
        System.out.println("Get!: " + bytes2);


//        int cnt = 0;
//        while(true) {
//            for (int i = 0; i < 1000; i++) {
//                stateSource.put(sha3(intToBytes(cnt)), intToBytes(cnt));
//                cnt++;
//            }
//
//            stateSource.getWriteCache().flush();
//
////            for (int i = 0; i < 200; i++) {
////                stateSource.put(sha3(intToBytes(cnt)), intToBytes(cnt));
////                cnt++;
////            }
//
//            Thread.sleep(800);
//
//                for (int i = max(0, cnt - 1000); i < cnt; i++) {
//
//                    byte[] bytes = stateSource.get(sha3(intToBytes(i)));
//                    assert Arrays.equals(bytes, intToBytes(i));
//                }
//            System.err.println("Iteration done");
//        }
//
//
    }
}
