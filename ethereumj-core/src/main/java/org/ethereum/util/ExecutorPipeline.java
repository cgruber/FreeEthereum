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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Queues execution tasks into a single pipeline where some tasks can be executed in parallel
 * but preserve 'messages' order so the next task process messages on a single thread in
 * the same order they were added to the previous executor
 *
 * Created by Anton Nashatyrev on 23.02.2016.
 */
public class ExecutorPipeline <In, Out>{

    private static final AtomicInteger pipeNumber = new AtomicInteger(1);
    private final BlockingQueue<Runnable> queue;
    private final ThreadPoolExecutor exec;
    private final Functional.Function<In, Out> processor;
    private final Functional.Consumer<Throwable> exceptionHandler;
    private final AtomicLong orderCounter = new AtomicLong();
    private final Map<Long, Out> orderMap = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private boolean preserveOrder = false;
    private ExecutorPipeline <Out, ?> next;
    private long nextOutTaskNumber = 0;
    private String threadPoolName;

    public ExecutorPipeline(final int threads, final int queueSize, final boolean preserveOrder, final Functional.Function<In, Out> processor,
                            final Functional.Consumer<Throwable> exceptionHandler) {
        queue = new LimitedQueue<>(queueSize);
        exec = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, queue, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, threadPoolName + "-" + threadNumber.getAndIncrement());
            }
        });
        this.preserveOrder = preserveOrder;
        this.processor = processor;
        this.exceptionHandler = exceptionHandler;
        this.threadPoolName = "pipe-" + pipeNumber.getAndIncrement();
    }

    public ExecutorPipeline<Out, Void> add(final int threads, final int queueSize, final Functional.Consumer<Out> consumer) {
        return add(threads, queueSize, false, new Functional.Function<Out, Void>() {
            @Override
            public Void apply(final Out out) {
                consumer.accept(out);
                return null;
            }
        });
    }

    private <NextOut> ExecutorPipeline<Out, NextOut> add(final int threads, final int queueSize, final boolean preserveOrder,
                                                         final Functional.Function<Out, NextOut> processor) {
        final ExecutorPipeline<Out, NextOut> ret = new ExecutorPipeline<>(threads, queueSize, preserveOrder, processor, exceptionHandler);
        next = ret;
        return ret;
    }

    private void pushNext(final long order, final Out res) {
        if (next != null) {
            if (!preserveOrder) {
                next.push(res);
            } else {
                lock.lock();
                try {
                    if (order == nextOutTaskNumber) {
                        next.push(res);
                        while(true) {
                            nextOutTaskNumber++;
                            final Out out = orderMap.remove(nextOutTaskNumber);
                            if (out == null) break;
                            next.push(out);
                        }
                    } else {
                        orderMap.put(order, res);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public void push(final In in) {
        final long order = orderCounter.getAndIncrement();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    pushNext(order, processor.apply(in));
                } catch (final Throwable e) {
                    exceptionHandler.accept(e);
                }
            }
        });
    }

    public void pushAll(final List<In> list) {
        for (final In in : list) {
            push(in);
        }
    }

    public ExecutorPipeline<In, Out> setThreadPoolName(final String threadPoolName) {
        this.threadPoolName = threadPoolName;
        return this;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    public Map<Long, Out> getOrderMap() {
        return orderMap;
    }

    public void shutdown() {
        try {
            exec.shutdown();
        } catch (final Exception e) {
        }
        if (next != null) {
            exec.shutdown();
        }
    }

    public boolean isShutdown() {
        return exec.isShutdown();
    }

    /**
     * Shutdowns executors and waits until all pipeline
     * submitted tasks complete
     * @throws InterruptedException
     */
    public void join() throws InterruptedException {
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.MINUTES);
        if (next != null) next.join();
    }

    private static class LimitedQueue<E> extends LinkedBlockingQueue<E> {
        public LimitedQueue(final int maxSize) {
            super(maxSize);
        }

        @Override
        public boolean offer(final E e) {
            // turn offer() and add() into a blocking calls (unless interrupted)
            try {
                put(e);
                return true;
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
