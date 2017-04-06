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

package org.ethereum.manager;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 11.12.2014
 */
@Component
public class AdminInfo {
    private static final int ExecTimeListLimit = 10000;
    private final List<Long> blockExecTime = new LinkedList<>();
    private long startupTimeStamp;
    private boolean consensus = true;

    @PostConstruct
    public void init() {
        startupTimeStamp = System.currentTimeMillis();
    }

    public long getStartupTimeStamp() {
        return startupTimeStamp;
    }

    public boolean isConsensus() {
        return consensus;
    }

    public void lostConsensus() {
        consensus = false;
    }

    public void addBlockExecTime(final long time) {
        while (blockExecTime.size() > ExecTimeListLimit) {
            blockExecTime.remove(0);
        }
        blockExecTime.add(time);
    }

    public Long getExecAvg(){

        if (blockExecTime.isEmpty()) return 0L;

        long sum = 0;
        for (final Long aBlockExecTime : blockExecTime) {
            sum += aBlockExecTime;
        }

        return sum / blockExecTime.size();
    }

    public List<Long> getBlockExecTime(){
        return blockExecTime;
    }
}
