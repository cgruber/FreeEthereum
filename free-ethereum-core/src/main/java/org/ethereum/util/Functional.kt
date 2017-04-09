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


interface Functional {

    /**
     * Represents an operation that accepts a single input argument and returns no
     * result. Unlike most other functional interfaces, `Consumer` is expected
     * to operate via side-effects.

     * @param <T> the type of the input to the operation
    </T> */
    interface Consumer<T> {

        /**
         * Performs this operation on the given argument.

         * @param t the input argument
         */
        fun accept(t: T)
    }

    /**
     * Represents an operation that accepts two input arguments and returns no
     * result.  This is the two-arity specialization of [java.util.function.Consumer].
     * Unlike most other functional interfaces, `BiConsumer` is expected
     * to operate via side-effects.

     * @param <T> the type of the first argument to the operation
     * *
     * @param <U> the type of the second argument to the operation
     * *
     * *
     * @see org.ethereum.util.Functional.Consumer
    </U></T> */
    interface BiConsumer<T, U> {

        /**
         * Performs this operation on the given arguments.

         * @param t the first input argument
         * *
         * @param u the second input argument
         */
        fun accept(t: T, u: U)
    }


    /**
     * Represents a function that accepts one argument and produces a result.

     * @param <T> the type of the input to the function
     * *
     * @param <R> the type of the result of the function
    </R></T> */
    interface Function<T, R> {

        /**
         * Applies this function to the given argument.

         * @param t the function argument
         * *
         * @return the function result
         */
        fun apply(t: T): R
    }

    interface Supplier<T> {

        /**
         * Gets a result.

         * @return a result
         */
        fun get(): T
    }

    interface InvokeWrapper {

        operator fun invoke()
    }

    interface InvokeWrapperWithResult<R> {

        operator fun invoke(): R
    }

    interface Predicate<T> {
        fun test(t: T): Boolean
    }

}
