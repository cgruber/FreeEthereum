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

package org.ethereum.net.rlpx.discover

import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Anton Nashatyrev on 03.08.2015.
 */
class QueueTest {

    private var exception = false

    @Test
    @Throws(Exception::class)
    fun simple() {
        val queue = PeerConnectionTester.MutablePriorityQueue(Comparator<String> { o1, o2 -> o1.compareTo(o2) })

        val threadCnt = 8
        val elemCnt = 1000

        val adder = Runnable {
            try {
                println("Adding...")
                var i = 0
                while (i < elemCnt && !exception) {
                    queue.add("aaa" + i)
                    if (i % 100 == 0) Thread.sleep(10)
                    i++
                }
                println("Done.")
            } catch (e: Exception) {
                exception = true
                e.printStackTrace()
            }
        }

        val tg = ThreadGroup("test")

        val t1 = arrayOfNulls<Thread>(threadCnt)

        for (i in t1.indices) {
            t1[i] = Thread(tg, adder)
            t1[i]?.start()
        }


        val taker = Runnable {
            try {
                println("Taking...")
                var i = 0
                while (i < elemCnt && !exception) {
                    queue.poll(1, TimeUnit.SECONDS)
                    i++
                }
                println("OK: " + queue.size)
            } catch (e: Exception) {
                exception = true
                e.printStackTrace()
            }
        }
        val t2 = arrayOfNulls<Thread>(threadCnt)

        for (i in t2.indices) {
            t2[i] = Thread(tg, taker)
            t2[i]?.start()
        }

        for (thread in t1) {
            thread?.join()
        }
        for (thread in t2) {
            thread?.join()
        }

        if (exception) throw RuntimeException("Test failed")
    }
}
