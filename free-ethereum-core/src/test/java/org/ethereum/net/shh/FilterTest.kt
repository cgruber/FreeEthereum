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

package org.ethereum.net.shh

import org.ethereum.crypto.ECKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterTest {

    private val to = WhisperImpl.toIdentity(ECKey())
    private val from = WhisperImpl.toIdentity(ECKey())
    private val topics = arrayOf("topic1", "topic2", "topic3", "topic4")

    @Test
    fun test1() {
        val matcher = FilterStub()
        assertTrue(matcher.match(to, from, Topic.createTopics(*topics)))
    }

    @Test
    fun test2() {
        val matcher = FilterStub().setTo(to)
        assertTrue(matcher.match(to, from, Topic.createTopics(*topics)))
    }

    @Test
    fun test3() {
        val matcher = FilterStub().setTo(to)
        assertFalse(matcher.match(null, from, Topic.createTopics(*topics)))
    }

    @Test
    fun test4() {
        val matcher = FilterStub().setFrom(from)
        assertTrue(matcher.match(null, from, Topic.createTopics(*topics)))
    }

    @Test
    fun test5() {
        val matcher = FilterStub().setFrom(from)
        assertTrue(!matcher.match(to, null, Topic.createTopics(*topics)))
    }

    @Test
    fun test6() {
        val matcher = FilterStub(null, from, Topic.createTopics(*topics))
        assertTrue(matcher.match(to, from, Topic.createTopics(*topics)))
    }

    @Test
    fun test7() {
        val matcher = FilterStub(null, null, Topic.createTopics(*topics))
        assertTrue(!matcher.match(to, from, Topic.createTopics()))
    }

    internal inner class FilterStub : MessageWatcher {
        constructor()

        constructor(to: String?, from: String?, filterTopics: Array<Topic>) : super(to, from, filterTopics)

        override fun newMessage(msg: WhisperMessage) {

        }
    }
}
