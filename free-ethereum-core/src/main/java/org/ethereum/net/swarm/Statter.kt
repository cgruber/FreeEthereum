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

package org.ethereum.net.swarm

/**
 * The interface for gathering statistical information.
 * Used similar to loggers and assumed to have minimal latency to
 * not affect the performance in production.
 * The implementation might be substituted to allow some advanced
 * information processing like streaming it to the database
 * or aggregating and displaying as graphics

 * Created by Anton Nashatyrev on 01.07.2015.
 */
abstract class Statter {

    abstract fun add(value: Double)

    class SimpleStatter(val name: String) : Statter() {
        @Volatile var last: Double = 0.toDouble()
            private set
        @Volatile var sum: Double = 0.toDouble()
            private set
        @Volatile var count: Int = 0
            private set

        override fun add(value: Double) {
            last = value
            sum += value
            count++
        }

        val avrg: Double
            get() = sum / count

    }

    companion object {

        /**
         * Used as a factory to create statters.

         * @param name Normally the name is assumed to be a hierarchical path with '.' delimiters
         * *             similar to full Java class names.
         */
        fun create(name: String): Statter {
            return SimpleStatter(name)
        }
    }
}
