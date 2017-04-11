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

package org.ethereum.net.rlpx

import org.ethereum.net.rlpx.discover.table.KademliaOptions
import org.ethereum.net.rlpx.discover.table.NodeTable
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.util.*

class KademliaTest {

    private val nodeId: ByteArray
        get() {
            val gen = Random()
            val id = ByteArray(64)
            gen.nextBytes(id)
            return id
        }

    private fun getNode(id: ByteArray, i: Int): Node {
        id[0] = id[0].plus(i).toByte()
        val n = node
        n.id = id
        return n
    }

    private val node: Node
        get() = Node(nodeId, "127.0.0.1", 30303)

    private fun getTestNodeTable(nodesQuantity: Int): NodeTable {
        val testTable = NodeTable(node)

        for (i in 0..nodesQuantity - 1) {
            testTable.addNode(node)
        }

        return testTable
    }

    private fun showBuckets(t: NodeTable) {
        t.buckets
                .asSequence()
                .filter { it.nodesCount > 0 }
                .forEach { println(String.format("Bucket %d nodes %d depth %d", it.depth, it.nodesCount, it.depth)) }
    }

    private fun containsNode(t: NodeTable, n: Node): Boolean {
        return t.allNodes.any { it.node.toString() == n.toString() }
    }

    @Ignore
    @Test
    fun test1() {
        //init table with one home node
        val t = getTestNodeTable(0)
        val homeNode = t.node

        //table should contain the home node only
        assertEquals(t.allNodes.size.toLong(), 1)

        val bucketNode = t.allNodes[0].node

        assertEquals(homeNode, bucketNode)

    }

    @Test
    fun test2() {
        val t = getTestNodeTable(0)
        val n = node
        t.addNode(n)

        assertTrue(containsNode(t, n))

        t.dropNode(n)

        assertFalse(containsNode(t, n))
    }

    @Test
    fun test3() {
        val t = getTestNodeTable(1000)
        showBuckets(t)

        val closest1 = t.getClosestNodes(t.node.id)
        val closest2 = t.getClosestNodes(nodeId)

        assertNotEquals(closest1, closest2)
    }

    @Test
    fun test4() {
        val t = getTestNodeTable(0)
        val homeNode = t.node

        //t.getBucketsCount() returns non empty buckets
        assertEquals(t.bucketsCount.toLong(), 1)

        //creates very close nodes
        (1..KademliaOptions.BUCKET_SIZE - 1)
                .asSequence()
                .map { getNode(homeNode.id, it) }
                .forEach { t.addNode(it) }

        assertEquals(t.bucketsCount.toLong(), 1)
        assertEquals(t.buckets[0].nodesCount.toLong(), KademliaOptions.BUCKET_SIZE.toLong())
    }
}
