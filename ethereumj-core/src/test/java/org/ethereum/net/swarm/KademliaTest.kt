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

import org.ethereum.net.rlpx.Node
import org.ethereum.net.rlpx.discover.table.NodeTable
import org.junit.Ignore
import org.junit.Test

import java.util.*

/**
 * Created by Admin on 01.07.2015.
 */
class KademliaTest {

    @Ignore
    @Test
    fun nodesConnectivityTest() {
        val nameMap = IdentityHashMap<Node, Int>()
        val nodes = arrayOfNulls<Node>(300)
        val table = testNodeTable
        for (i in nodes.indices) {
            nodes[i] = getNode(1000 + i)
            table.addNode(nodes[i])
            nameMap.put(nodes[i], i)
        }

        val reachable = IdentityHashMap<Node, Set<Node>>()

        for (node in nodes) {
            val reachnodes = IdentityHashMap<Node, Any>()
            reachable.put(node, reachnodes.keys)
            val closestNodes = table.getClosestNodes(node!!.id)
            var max = 16
            for (closestNode in closestNodes) {
                reachnodes.put(closestNode, null)
                if (--max == 0) break
            }
        }

        for (node in nodes) {
            println("Closing node " + nameMap[node])
            val closure = IdentityHashMap<Node, Any>()
            addAll(reachable, reachable[node]!!, closure.keys)
            reachable.put(node, closure.keys)
        }

        for ((key, value) in reachable) {
            println("Node " + nameMap[key]
                    + " has " + value.size + " neighbours")
            //            for (Node nb : entry.getValue()) {
            //                System.out.println("    " + nameMap.get(nb));
            //            }
        }
    }

    private fun addAll(reachableMap: Map<Node, Set<Node>>, reachable: Set<Node>, ret: MutableSet<Node>) {
        for (node in reachable) {
            if (!ret.contains(node)) {
                ret.add(node)
                addAll(reachableMap, reachableMap[node]!!, ret)
            }
        }
    }

    companion object {

        private val gen = Random(0)

        private val nodeId: ByteArray
            get() {
                val id = ByteArray(64)
                gen.nextBytes(id)
                return id
            }

        private fun getNode(port: Int): Node {
            return Node(nodeId, "127.0.0.1", port)
        }

        private val testNodeTable: NodeTable
            get() {
                val testTable = NodeTable(getNode(3333))
                return testTable
            }
    }
}
