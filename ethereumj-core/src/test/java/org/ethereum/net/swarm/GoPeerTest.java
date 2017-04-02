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

package org.ethereum.net.swarm;

import org.ethereum.Start;
import org.junit.Ignore;
import org.junit.Test;

import static org.ethereum.crypto.HashUtil.sha3;

/**
 * Created by Admin on 06.07.2015.
 */
public class GoPeerTest {

    @Ignore
    @Test
    // TODO to be done at some point: run Go peer and connect to it
    public void putTest() throws Exception {
        System.out.println("Starting Java peer...");
        Start.main(new String[]{});
        System.out.println("Warming up...");
        Thread.sleep(5000);
        System.out.println("Sending a chunk...");

        Key key = new Key(sha3(new byte[]{0x22, 0x33}));
//            stdout.setFilter(Hex.toHexString(key.getBytes()));
        Chunk chunk = new Chunk(key, new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 77, 88});

        NetStore.getInstance().put(chunk);
    }
}
