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

package org.ethereum.mine

import com.google.common.util.concurrent.MoreExecutors
import org.junit.Test
import java.util.concurrent.*

/**
 * Created by Anton Nashatyrev on 17.12.2015.
 */
class FutureTest {

    @Test
    @Throws(InterruptedException::class)
    fun interruptTest() {
        val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
        val future = executor.submit(Callable<Any> {
            //                try {
            println("Waiting")
            Thread.sleep(10000)
            println("Complete")
            null
            //                } catch (Exception e) {
            //                    e.printStackTrace();
            //                    throw e;
            //                }
        })
        future.addListener(Runnable {
            println("Listener: " + future.isCancelled + ", " + future.isDone)
            try {
                future.get()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            }
        }, MoreExecutors.sameThreadExecutor())

        Thread.sleep(1000)
        future.cancel(true)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun guavaExecutor() {
        //        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
        //                new ThreadPoolExecutor(2, 16, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
        val executor = ThreadPoolExecutor(16, 16, 1L, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(1))
        var future: Future<Any>? = null
        for (i in 0..3) {
            val ii = i
            future = executor.submit(Callable<Any> {
                try {
                    println("Waiting " + ii)
                    Thread.sleep(5000)
                    println("Complete " + ii)
                    return@Callable null
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            })
        }
        future!!.get()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun anyFutureTest() {
        val executor = MoreExecutors.listeningDecorator(
                ThreadPoolExecutor(16, 16, 1L, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()))
        var anyFuture: AnyFuture<Int> = object : AnyFuture<Int>() {
            override fun postProcess(integer: Int?) {
                println("FutureTest.postProcess:integer = [$integer]")
            }
        }
        for (i in 0..3) {
            val ii = i
            val future = executor.submit(Callable {
                println("Waiting " + ii)
                Thread.sleep((5000 - ii * 500).toLong())
                println("Complete " + ii)
                ii
            })
            anyFuture.add(future)
        }
        Thread.sleep(1000)
        anyFuture.cancel(true)
        println("Getting anyFuture...")
        println("anyFuture: " + anyFuture.isCancelled + ", " + anyFuture.isDone)

        anyFuture = object : AnyFuture<Int>() {
            override fun postProcess(integer: Int?) {
                println("FutureTest.postProcess:integer = [$integer]")
            }
        }
        for (i in 0..3) {
            val ii = i
            val future = executor.submit(Callable {
                println("Waiting " + ii)
                Thread.sleep((5000 - ii * 500).toLong())
                println("Complete " + ii)
                ii
            })
            anyFuture.add(future)
        }
        println("Getting anyFuture...")
        println("anyFuture.get(): " + anyFuture.get() + ", " + anyFuture.isCancelled() + ", " + anyFuture.isDone())
    }
}
