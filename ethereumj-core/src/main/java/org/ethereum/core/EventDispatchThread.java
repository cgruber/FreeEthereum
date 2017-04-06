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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.*;

/**
 * The class intended to serve as an 'Event Bus' where all EthereumJ events are
 * dispatched asynchronously from component to component or from components to
 * the user event handlers.
 *
 * This made for decoupling different components which are intended to work
 * asynchronously and to avoid complex synchronisation and deadlocks between them
 *
 * Created by Anton Nashatyrev on 29.12.2015.
 */
@Component
public class EventDispatchThread {
    private static final Logger logger = LoggerFactory.getLogger("blockchain");
    private static final int[] queueSizeWarnLevels = new int[]{0, 10_000, 50_000, 100_000, 250_000, 500_000, 1_000_000, 10_000_000};
    private static EventDispatchThread eventDispatchThread;
    private final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, executorQueue, new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "EDT");
        }
    });

    private long taskStart;
    private Runnable lastTask;
    private int lastQueueSizeWarnLevel = 0;
    private int counter;

    /**
     * Returns the default instance for initialization of Autowired instances
     * to be used in tests
     */
    public static EventDispatchThread getDefault() {
        if (eventDispatchThread == null) {
            eventDispatchThread = new EventDispatchThread() {
                @Override
                public void invokeLater(final Runnable r) {
                    r.run();
                }
            };
        }
        return eventDispatchThread;
    }

    private static int getSizeWarnLevel(final int size) {
        final int idx = Arrays.binarySearch(queueSizeWarnLevels, size);
        return idx >= 0 ? idx : -(idx + 1) - 1;
    }

    public void invokeLater(final Runnable r) {
        if (executor.isShutdown()) return;
        if (counter++ % 1000 == 0) logStatus();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lastTask = r;
                    taskStart = System.nanoTime();
                    r.run();
                    final long t = (System.nanoTime() - taskStart) / 1_000_000;
                    taskStart = 0;
                    if (t > 1000) {
                        logger.warn("EDT task executed in more than 1 sec: " + t + "ms, " +
                        "Executor queue size: " + executorQueue.size());

                    }
                } catch (final Exception e) {
                    logger.error("EDT task exception", e);
                }
            }
        });
    }

    // monitors EDT queue size and prints warning if exceeds thresholds
    private void logStatus() {
        final int curLevel = getSizeWarnLevel(executorQueue.size());
        if (lastQueueSizeWarnLevel == curLevel) return;

        synchronized (this) {
            if (curLevel > lastQueueSizeWarnLevel) {
                final long t = taskStart == 0 ? 0 : (System.nanoTime() - taskStart) / 1_000_000;
                final String msg = "EDT size grown up to " + executorQueue.size() + " (last task executing for " + t + " ms: " + lastTask;
                if (curLevel < 3) {
                    logger.info(msg);
                } else {
                    logger.warn(msg);
                }
            } else if (curLevel < lastQueueSizeWarnLevel) {
                logger.info("EDT size shrunk down to " + executorQueue.size());
            }
            lastQueueSizeWarnLevel = curLevel;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            logger.warn("shutdown: executor interrupted: {}", e.getMessage());
        }
    }
}
