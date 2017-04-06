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

package org.ethereum.net.rlpx.discover;

import org.junit.Test;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anton Nashatyrev on 03.08.2015.
 */
public class QueueTest {

    private boolean exception = false;

    @Test
    public void simple() throws Exception {
        final PeerConnectionTester.MutablePriorityQueue<String, String> queue =
                new PeerConnectionTester.MutablePriorityQueue<>(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        });

        final int threadCnt = 8;
        final int elemCnt = 1000;

        final Runnable adder = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Adding...");
                    for (int i = 0; i < elemCnt && !exception; i++) {
                        queue.add("aaa" + i);
                        if (i % 100 == 0) Thread.sleep(10);
                    }
                    System.out.println("Done.");
                } catch (final Exception e) {
                    exception = true;
                    e.printStackTrace();
                }
            }
        };

        final ThreadGroup tg = new ThreadGroup("test");

        final Thread[] t1 = new Thread[threadCnt];

        for (int i = 0; i < t1.length; i++) {
            t1[i] = new Thread(tg, adder);
            t1[i].start();
        }


        final Runnable taker = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Taking...");
                    for (int i = 0; i < elemCnt && !exception; i++) {
                        queue.poll(1, TimeUnit.SECONDS);
                    }
                    System.out.println("OK: " + queue.size());
                } catch (final Exception e) {
                    exception = true;
                    e.printStackTrace();
                }
            }
        };
        final Thread[] t2 = new Thread[threadCnt];

        for (int i = 0; i < t2.length; i++) {
            t2[i] = new Thread(tg, taker);
            t2[i].start();
        }

        for (final Thread thread : t1) {
            thread.join();
        }
        for (final Thread thread : t2) {
            thread.join();
        }

        if (exception) throw new RuntimeException("Test failed");
    }
}
