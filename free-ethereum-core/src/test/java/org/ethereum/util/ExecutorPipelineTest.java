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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ExecutorPipelineTest {

    @Test
    public void joinTest() throws InterruptedException {
        final ExecutorPipeline<Integer, Integer> exec1 = new ExecutorPipeline<>(8, 100, true, integer -> {
            try {
                Thread.sleep(2);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            return integer;
        }, Throwable::printStackTrace);

        final List<Integer> consumed = new ArrayList<>();

        final ExecutorPipeline<Integer, Void> exec2 = exec1.add(1, 100, consumed::add);

        final int cnt = 1000;
        for (int i = 0; i < cnt; i++) {
            exec1.push(i);
        }
        exec1.join();

        Assert.assertEquals(cnt, consumed.size());
    }
}
