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

package org.ethereum.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * @author Mikhail Kalinin
 * *
 * @since 15.10.2015
 */
class BIUtilTest {

    @Test
    fun testIsIn20PercentRange() {

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(20000), BigInteger.valueOf(24000)))

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(24000), BigInteger.valueOf(20000)))

        assertFalse(BIUtil.isIn20PercentRange(BigInteger.valueOf(20000), BigInteger.valueOf(25000)))

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(20), BigInteger.valueOf(24)))

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(24), BigInteger.valueOf(20)))

        assertFalse(BIUtil.isIn20PercentRange(BigInteger.valueOf(20), BigInteger.valueOf(25)))

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.ZERO, BigInteger.ZERO))

        assertFalse(BIUtil.isIn20PercentRange(BigInteger.ZERO, BigInteger.ONE))

        assertTrue(BIUtil.isIn20PercentRange(BigInteger.ONE, BigInteger.ZERO))
    }

    @Test // test isIn20PercentRange
    fun test1() {
        assertFalse(BIUtil.isIn20PercentRange(BigInteger.ONE, BigInteger.valueOf(5)))
        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(5), BigInteger.ONE))
        assertTrue(BIUtil.isIn20PercentRange(BigInteger.valueOf(5), BigInteger.valueOf(6)))
        assertFalse(BIUtil.isIn20PercentRange(BigInteger.valueOf(5), BigInteger.valueOf(7)))
    }
}
