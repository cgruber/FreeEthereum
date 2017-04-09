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

package org.ethereum.net.dht

import java.util.*

class Bucket private constructor(
        /** */
        // tree operations //
        val name: String) {
    // if bit = 1 go left
    private var left: Bucket? = null
    // if bit = 0 go right
    private var right: Bucket? = null
    internal var peers: MutableList<Peer>? = ArrayList()


    private fun add(peer: Peer?) {

        if (peer == null) throw Error("Not a leaf")

        if (peers == null) {

            if (peer.nextBit(name).toInt() == 1)
                left!!.add(peer)
            else
                right!!.add(peer)

            return
        }

        peers!!.add(peer)

        if (peers!!.size > MAX_KADEMLIA_K)
            splitBucket()
    }

    private fun splitBucket() {
        left = Bucket(name + "1")
        right = Bucket(name + "0")

        for (id in peers!!) {
            if (id.nextBit(name).toInt() == 1)
                left!!.add(id)
            else
                right!!.add(id)
        }

        this.peers = null
    }


    fun left(): Bucket {
        return left!!
    }

    fun right(): Bucket {
        return right!!
    }


    override fun toString(): String {

        val sb = StringBuilder()

        sb.append(name).append("\n")

        if (peers == null) return sb.toString()

        for (id in peers!!)
            sb.append(id.toBinaryString()).append("\n")

        return sb.toString()
    }


    fun traverseTree(doOnTree: DoOnTree) {

        if (left != null) left!!.traverseTree(doOnTree)
        if (right != null) right!!.traverseTree(doOnTree)

        doOnTree.call(this)
    }

//    fun getPeers(): MutableList<Peer> {
//        return peers!!
//    }

    /** */

    interface DoOnTree {

        fun call(bucket: Bucket)
    }

    class SaveLeaf : DoOnTree {

        internal var leafs: MutableList<Bucket> = ArrayList()

        override fun call(bucket: Bucket) {
            if (bucket.peers != null) leafs.add(bucket)
        }

        fun getLeafs(): List<Bucket> {
            return leafs
        }

        fun setLeafs(leafs: MutableList<Bucket>) {
            this.leafs = leafs
        }
    }

    companion object {

        private val MAX_KADEMLIA_K = 5
    }
}
