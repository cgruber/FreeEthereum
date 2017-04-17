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

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.QuotientFilter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.ethereum.util.ByteUtil.intToBytes;

public class QuotientFilterTest {

    @Ignore
    @Test
    public void perfTest() {
        final QuotientFilter f = QuotientFilter.create(50_000_000, 100_000);
        long s = System.currentTimeMillis();
        for (int i = 0; i < 5_000_000; i++) {
            f.insert(HashUtil.INSTANCE.sha3(intToBytes(i)));

            // inserting duplicate slows down significantly
            if (i % 10 == 0) f.insert(HashUtil.INSTANCE.sha3(intToBytes(0)));

            if (i > 100_000 && i % 2 == 0) {
                f.remove(HashUtil.INSTANCE.sha3(intToBytes(i - 100_000)));
            }
            if (i % 10000 == 0) {
                System.out.println(i + ": " + (System.currentTimeMillis() - s));
                s = System.currentTimeMillis();
            }
        }
    }

    @Test
    public void maxDuplicatesTest() {
        final QuotientFilter f = QuotientFilter.create(50_000_000, 1000).withMaxDuplicates(2);
        f.insert(1);
        Assert.assertTrue(f.maybeContains(1));
        f.remove(1);
        Assert.assertFalse(f.maybeContains(1));

        f.insert(1);
        f.insert(1);
        f.insert(2);
        Assert.assertTrue(f.maybeContains(2));
        f.remove(2);
        Assert.assertFalse(f.maybeContains(2));

        f.remove(1);
        f.remove(1);
        Assert.assertTrue(f.maybeContains(1));

        f.insert(3);
        f.insert(3);
        Assert.assertTrue(f.maybeContains(3));
        f.remove(3);
        f.remove(3);
        Assert.assertTrue(f.maybeContains(3));
    }
}
