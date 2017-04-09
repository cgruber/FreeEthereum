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

package org.ethereum.db;

import org.ethereum.config.SystemProperties;
import org.ethereum.datasource.WriteCache;
import org.ethereum.datasource.inmem.HashMapDB;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.ethereum.datasource.MemSizeEstimator.ByteArrayEstimator;
import static org.ethereum.util.ByteUtil.intToBytes;

/**
 * Created by Anton Nashatyrev on 23.12.2016.
 */
public class FlushDbManagerTest {

    @Test
    public void testConcurrentCommit() throws Throwable {
        // check that FlushManager commit(Runnable) is performed atomically

        final HashMapDB<byte[]> db1 = new HashMapDB<>();
        final HashMapDB<byte[]> db2 = new HashMapDB<>();
        final WriteCache<byte[], byte[]> cache1 = new WriteCache.BytesKey<>(db1, WriteCache.CacheType.SIMPLE);
        cache1.withSizeEstimators(ByteArrayEstimator, ByteArrayEstimator);
        final WriteCache<byte[], byte[]> cache2 = new WriteCache.BytesKey<>(db2, WriteCache.CacheType.SIMPLE);
        cache2.withSizeEstimators(ByteArrayEstimator, ByteArrayEstimator);

        final DbFlushManager dbFlushManager = new DbFlushManager(SystemProperties.getDefault(), Collections.emptySet(), null);
        dbFlushManager.addCache(cache1);
        dbFlushManager.addCache(cache2);
        dbFlushManager.setSizeThreshold(1);

        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] exception = new Throwable[1];

        new Thread(() -> {
            try {
                for (int i = 0; i < 300; i++) {
                    final int i_ = i;
                    dbFlushManager.commit(() -> {
                        cache1.put(intToBytes(i_), intToBytes(i_));
                        try {
                            Thread.sleep(5);
                        } catch (final InterruptedException e) {
                        }
                        cache2.put(intToBytes(i_), intToBytes(i_));
                    });
                }
            } catch (Throwable e) {
                exception[0] = e;
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                while (true) {
                    dbFlushManager.commit();
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                exception[0] = e;
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                int cnt = 0;
                while (true) {
                    synchronized (dbFlushManager) {
                        for (; cnt < 300; cnt++) {
                            byte[] val1 = db1.get(intToBytes(cnt));
                            byte[] val2 = db2.get(intToBytes(cnt));
                            if (val1 == null) {
                                Assert.assertNull(val2);
                                break;
                            } else {
                                Assert.assertNotNull(val2);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                exception[0] = e;
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();


        latch.await();

        if (exception[0] != null) throw exception[0];
    }
}
