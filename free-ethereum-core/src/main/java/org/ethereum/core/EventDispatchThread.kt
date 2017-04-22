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

package org.ethereum.core

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * The class intended to serve as an 'Event Bus' where all EthereumJ events are
 * dispatched asynchronously from component to component or from components to
 * the user event handlers.

 * This made for decoupling different components which are intended to work
 * asynchronously and to avoid complex synchronisation and deadlocks between them

 * Created by Anton Nashatyrev on 29.12.2015.
 */
@Component
open class EventDispatchThread {
    private val executorQueue = LinkedBlockingQueue<Runnable>()
    private val executor = ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, executorQueue) { r -> Thread(r, "EDT") }

    private var taskStart: Long = 0
    private var lastTask: Runnable? = null
    private var lastQueueSizeWarnLevel = 0
    private var counter: Int = 0

    open fun invokeLater(r: Runnable) {
        if (executor.isShutdown) return
        if (counter++ % 1000 == 0) logStatus()

        executor.submit {
            try {
                lastTask = r
                taskStart = System.nanoTime()
                r.run()
                val t = (System.nanoTime() - taskStart) / 1000000
                taskStart = 0
                if (t > 1000) {
                    logger.warn("EDT task executed in more than 1 sec: " + t + "ms, " +
                            "Executor queue size: " + executorQueue.size)

                }
            } catch (e: Exception) {
                logger.error("EDT task exception", e)
            }
        }
    }

    // monitors EDT queue size and prints warning if exceeds thresholds
    private fun logStatus() {
        val curLevel = getSizeWarnLevel(executorQueue.size)
        if (lastQueueSizeWarnLevel == curLevel) return

        synchronized(this) {
            if (curLevel > lastQueueSizeWarnLevel) {
                val t = if (taskStart.equals(0)) 0 else (System.nanoTime() - taskStart) / 1000000
                val msg = "EDT size grown up to " + executorQueue.size + " (last task executing for " + t + " ms: " + lastTask
                if (curLevel < 3) {
                    logger.info(msg)
                } else {
                    logger.warn(msg)
                }
            } else if (curLevel < lastQueueSizeWarnLevel) {
                logger.info("EDT size shrunk down to " + executorQueue.size)
            }
            lastQueueSizeWarnLevel = curLevel
        }
    }

    fun shutdown() {
        executor.shutdownNow()
        try {
            executor.awaitTermination(10L, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.warn("shutdown: executor interrupted: {}", e.message)
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger("blockchain")
        private val queueSizeWarnLevels = intArrayOf(0, 10000, 50000, 100000, 250000, 500000, 1000000, 10000000)
        private var eventDispatchThread: EventDispatchThread? = null

        /**
         * Returns the default instance for initialization of Autowired instances
         * to be used in tests
         */
        val default: EventDispatchThread
            get() {
                if (eventDispatchThread == null) {
                    eventDispatchThread = object : EventDispatchThread() {
                        override fun invokeLater(r: Runnable) {
                            r.run()
                        }
                    }
                }
                return eventDispatchThread!!
            }

        private fun getSizeWarnLevel(size: Int): Int {
            val idx = Arrays.binarySearch(queueSizeWarnLevels, size)
            return if (idx >= 0) idx else -(idx + 1) - 1
        }
    }
}
