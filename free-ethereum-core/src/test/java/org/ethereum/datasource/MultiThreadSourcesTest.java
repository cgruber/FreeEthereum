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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.StateSource;
import org.ethereum.mine.AnyFuture;
import org.ethereum.util.ALock;
import org.ethereum.util.ByteArrayMap;
import org.ethereum.util.Utils;
import org.ethereum.vm.DataWord;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;
import static java.lang.Thread.sleep;
import static org.ethereum.util.ByteUtil.intToBytes;
import static org.ethereum.util.ByteUtil.longToBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Testing different sources and chain of sources in multi-thread environment
 */
public class MultiThreadSourcesTest {

    private volatile int maxConcurrency = 0;
    private volatile int maxWriteConcurrency = 0;
    private volatile int maxReadWriteConcurrency = 0;

    private static byte[] key(final int key) {
        return HashUtil.INSTANCE.sha3(intToBytes(key));
    }

    private byte[] intToKey(final int i) {
        return HashUtil.INSTANCE.sha3(longToBytes(i));
    }

    private byte[] intToValue(final int i) {
        return (new DataWord(i)).getData();
    }

    private String str(final Object obj) {
        if (obj == null) return null;
        return Hex.toHexString((byte[]) obj);
    }

    @Test
    public void testWriteCache() throws InterruptedException {
        final Source<byte[], byte[]> src = new HashMapDB<>();
        final WriteCache writeCache = new WriteCache.BytesKey<>(src, WriteCache.CacheType.SIMPLE);

        final TestExecutor testExecutor = new TestExecutor(writeCache);
        testExecutor.run(5);
    }

    @Test
    public void testReadCache() throws InterruptedException {
        final Source<byte[], byte[]> src = new HashMapDB<>();
        final ReadCache readCache = new ReadCache.BytesKey<>(src);

        final TestExecutor testExecutor = new TestExecutor(readCache);
        testExecutor.run(5);
    }

    @Test
    public void testReadWriteCache() throws InterruptedException {
        final Source<byte[], byte[]> src = new HashMapDB<>();
        final ReadWriteCache readWriteCache = new ReadWriteCache.BytesKey<>(src, WriteCache.CacheType.SIMPLE);

        final TestExecutor testExecutor = new TestExecutor(readWriteCache);
        testExecutor.run(5);
    }

    @Test
    public void testAsyncWriteCache() throws InterruptedException, TimeoutException, ExecutionException {
        final Source<byte[], byte[]> src = new HashMapDB<>();

        final AsyncWriteCache<byte[], byte[]> cache = new AsyncWriteCache<byte[], byte[]>(src) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(final Source<byte[], byte[]> source) {
                return new WriteCache.BytesKey<byte[]>(source, WriteCache.CacheType.SIMPLE) {
                    @Override
                    public boolean flush() {
//                        System.out.println("Flushing started");
                        final boolean ret = super.flush();
//                        System.out.println("Flushing complete");
                        return ret;
                    }
                };
            }
        };

//        TestExecutor testExecutor = new TestExecutor(cache);
        final TestExecutor1 testExecutor = new TestExecutor1(cache);
        testExecutor.start(5);
    }

    @Test
    public void testStateSource() throws Exception {
        final HashMapDB<byte[]> src = new HashMapDB<>();
//        LevelDbDataSource ldb = new LevelDbDataSource("test");
//        ldb.init();
        final StateSource stateSource = new StateSource(src, false);
        stateSource.getReadCache().withMaxCapacity(10);

        final TestExecutor1 testExecutor = new TestExecutor1(stateSource);
        testExecutor.start(10);
    }

    @Test
    public void testStateSourceConcurrency() throws Exception {
        final HashMapDB<byte[]> src = new HashMapDB<byte[]>() {
            final AtomicInteger concurrentReads = new AtomicInteger(0);
            final AtomicInteger concurrentWrites = new AtomicInteger(0);

            void checkConcurrency(final boolean write) {
                maxConcurrency = max(concurrentReads.get() + concurrentWrites.get(), maxConcurrency);
                if (write) {
                    maxWriteConcurrency = max(concurrentWrites.get(), maxWriteConcurrency);
                } else {
                    maxReadWriteConcurrency = max(concurrentWrites.get(), maxReadWriteConcurrency);
                }
            }

            @Override
            public void put(final byte[] key, final byte[] val) {
                final int i1 = concurrentWrites.incrementAndGet();
                checkConcurrency(true);
                super.put(key, val);
                final int i2 = concurrentWrites.getAndDecrement();
            }

            @Override
            public byte[] get(final byte[] key) {
//                Utils.sleep(60_000);
                final int i1 = concurrentReads.incrementAndGet();
                checkConcurrency(false);
                Utils.sleep(1);
                final byte[] ret = super.get(key);
                final int i2 = concurrentReads.getAndDecrement();
                return ret;
            }

            @Override
            public void delete(final byte[] key) {
                final int i1 = concurrentWrites.incrementAndGet();
                checkConcurrency(true);
                super.delete(key);
                final int i2 = concurrentWrites.getAndDecrement();
            }
        };
        final StateSource stateSource = new StateSource(src, false);
        stateSource.getReadCache().withMaxCapacity(10);

        new Thread(() -> stateSource.get(key(1))).start();

        stateSource.get(key(2));


        final TestExecutor1 testExecutor = new TestExecutor1(stateSource);
//        testExecutor.writerThreads = 0;
        testExecutor.start(3);

        System.out.println("maxConcurrency = " + maxConcurrency + ", maxWriteConcurrency = " + maxWriteConcurrency +
                ", maxReadWriteConcurrency = " + maxReadWriteConcurrency);
    }

    @Test
    public void testCountingWriteCache() throws InterruptedException {
        final Source<byte[], byte[]> parentSrc = new HashMapDB<>();
        final Source<byte[], byte[]> src = new CountingBytesSource(parentSrc);
        final WriteCache writeCache = new WriteCache.BytesKey<>(src, WriteCache.CacheType.COUNTING);

        final TestExecutor testExecutor = new TestExecutor(writeCache, true);
        testExecutor.run(10);
    }

    private class TestExecutor {

        final CountDownLatch failSema = new CountDownLatch(1);
        final AtomicInteger putCnt = new AtomicInteger(1);
        final AtomicInteger delCnt = new AtomicInteger(1);
        final AtomicInteger checkCnt = new AtomicInteger(0);
        private final Source<byte[], byte[]> cache;
        boolean isCounting = false;
        boolean noDelete = false;
        boolean running = true;
        final Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(running) {
                        final int curMax = putCnt.get() - 1;
                        if (checkCnt.get() >= curMax) {
                            sleep(10);
                            continue;
                        }
                        assertEquals(str(intToValue(curMax)), str(cache.get(intToKey(curMax))));
                        checkCnt.set(curMax);
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                    failSema.countDown();
                }
            }
        });
        final Thread delThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(running) {
                        final int toDelete = delCnt.get();
                        final int curMax = putCnt.get() - 1;

                        if (toDelete > checkCnt.get() || toDelete >= curMax) {
                            sleep(10);
                            continue;
                        }
                        assertEquals(str(intToValue(toDelete)), str(cache.get(intToKey(toDelete))));

                        if (isCounting) {
                            for (int i = 0; i < (toDelete % 5); ++i) {
                                cache.delete(intToKey(toDelete));
                                assertEquals(str(intToValue(toDelete)), str(cache.get(intToKey(toDelete))));
                            }
                        }

                        cache.delete(intToKey(toDelete));
                        if (isCounting) cache.flush();
                        assertNull(cache.get(intToKey(toDelete)));
                        delCnt.getAndIncrement();
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                    failSema.countDown();
                }
            }
        });

        public TestExecutor(final Source cache) {
            this.cache = cache;
        }

        public TestExecutor(final Source cache, final boolean isCounting) {
            this.cache = cache;
            this.isCounting = isCounting;
        }

        public void setNoDelete(final boolean noDelete) {
            this.noDelete = noDelete;
        }

        public void run(final long timeout) {
            new Thread(() -> {
                try {
                    while (running) {
                        final int curCnt = putCnt.get();
                        cache.put(intToKey(curCnt), intToValue(curCnt));
                        if (isCounting) {
                            for (int i = 0; i < (curCnt % 5); ++i) {
                                cache.put(intToKey(curCnt), intToValue(curCnt));
                            }
                        }
                        putCnt.getAndIncrement();
                        if (curCnt == 1) {
                            readThread.start();
                            if (!noDelete) {
                                delThread.start();
                            }
                        }
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                    failSema.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    while (running) {
                        sleep(10);
                        cache.flush();
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                    failSema.countDown();
                }
            }).start();

            try {
                failSema.await(timeout, TimeUnit.SECONDS);
            } catch (final InterruptedException ex) {
                running = false;
                throw new RuntimeException("Thrown interrupted exception", ex);
            }

            // Shutdown carefully
            running = false;

            if (failSema.getCount() == 0) {
                throw new RuntimeException("Test failed.");
            } else {
                System.out.println("Test passed, put counter: " + putCnt.get() + ", delete counter: " + delCnt.get());
            }
        }
    }

    private class TestExecutor1 {
        public final int writerThreads = 4;
        public final int readerThreads = 8;
        public final int deleterThreads = 0;
        public final int flusherThreads = 2;
        public final int maxKey = 10000;
        final Map<byte[], byte[]> map = Collections.synchronizedMap(new ByteArrayMap<byte[]>());
        final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        final ALock rLock = new ALock(rwLock.readLock());
        final ALock wLock = new ALock(rwLock.writeLock());
        private final Source<byte[], byte[]> cache;
        boolean stopped;

        public TestExecutor1(final Source<byte[], byte[]> cache) {
            this.cache = cache;
        }

        public void start(final long time) throws InterruptedException, ExecutionException {
            final List<Callable<Object>> all = new ArrayList<>();

            for (int i = 0; i < writerThreads; i++) {
                all.add(Executors.callable(new Writer()));
            }
            for (int i = 0; i < readerThreads; i++) {
                all.add(Executors.callable(new Reader()));
            }
            for (int i = 0; i < deleterThreads; i++) {
                all.add(Executors.callable(new Deleter()));
            }
            for (int i = 0; i < flusherThreads; i++) {
                all.add(Executors.callable(new Flusher()));
            }

            final ExecutorService executor = Executors.newFixedThreadPool(all.size());
            final ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(executor);
            final AnyFuture<Object> anyFuture = new AnyFuture<>();
            for (final Callable<Object> callable : all) {
                final ListenableFuture<Object> future = listeningExecutorService.submit(callable);
                anyFuture.add(future);
            }

            try {
                anyFuture.get(time, TimeUnit.SECONDS);
            } catch (final TimeoutException e) {
                System.out.println("Passed.");
            }

            stopped = true;
        }

        class Writer implements Runnable {
            public void run() {
                final Random rnd = new Random(0);
                while (!stopped) {
                    final byte[] key = key(rnd.nextInt(maxKey));
                    try (ALock l = wLock.lock()) {
                        map.put(key, key);
                        cache.put(key, key);
                    }
                    Utils.sleep(rnd.nextInt(1));
                }
            }
        }

        class Reader implements Runnable {
            public void run() {
                final Random rnd = new Random(0);
                while (!stopped) {
                    final byte[] key = key(rnd.nextInt(maxKey));
                    try (ALock l = rLock.lock()) {
                        final byte[] expected = map.get(key);
                        final byte[] actual = cache.get(key);
                        Assert.assertArrayEquals(expected, actual);
                    }
                }
            }
        }

        class Deleter implements Runnable {
            public void run() {
                final Random rnd = new Random(0);
                while (!stopped) {
                    final byte[] key = key(rnd.nextInt(maxKey));
                    try (ALock l = wLock.lock()) {
                        map.remove(key);
                        cache.delete(key);
                    }
                }
            }
        }

        class Flusher implements Runnable {
            public void run() {
                final Random rnd = new Random(0);
                while (!stopped) {
                    Utils.sleep(rnd.nextInt(50));
                    cache.flush();
                }
            }
        }
    }
}
