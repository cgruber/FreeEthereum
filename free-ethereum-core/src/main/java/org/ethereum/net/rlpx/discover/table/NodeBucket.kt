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

package org.ethereum.net.rlpx.discover.table

import java.util.*

class NodeBucket internal constructor(val depth: Int) {
    private val nodes = ArrayList<NodeEntry>()

    @Synchronized fun addNode(e: NodeEntry): NodeEntry? {
        if (!nodes.contains(e)) {
            if (nodes.size >= KademliaOptions.BUCKET_SIZE) {
                return lastSeen
            } else {
                nodes.add(e)
            }
        }

        return null
    }

    private val lastSeen: NodeEntry
        get() {
            val sorted = nodes
            sorted.sortWith(TimeComparator())
            return sorted[0]
        }

    @Synchronized fun dropNode(entry: NodeEntry) {
        for (e in nodes) {
            if (e.id == entry.id) {
                nodes.remove(e)
                break
            }
        }
    }

    val nodesCount: Int
        get() = nodes.size

    fun getNodes(): List<NodeEntry> {
        //        List<NodeEntry> nodes = new ArrayList<>();
        //        for (NodeEntry e : this.nodes) {
        //            nodes.add(e);
        //        }
        return nodes
    }
}
