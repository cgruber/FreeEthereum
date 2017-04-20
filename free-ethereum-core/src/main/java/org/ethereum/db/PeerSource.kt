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

package org.ethereum.db

import org.apache.commons.lang3.tuple.Pair
import org.ethereum.datasource.DataSourceArray
import org.ethereum.datasource.ObjectDataSource
import org.ethereum.datasource.Serializer
import org.ethereum.datasource.Source
import org.ethereum.datasource.leveldb.LevelDbDataSource
import org.ethereum.net.rlpx.Node
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.slf4j.LoggerFactory
import java.math.BigInteger

/**
 * Source for [org.ethereum.net.rlpx.Node] also known as Peers
 */
class PeerSource(private val src: Source<ByteArray, ByteArray>) {
    var nodes: DataSourceArray<Pair<Node, Int>>? = null
        private set


    init {
        val INST = this
        this.nodes = DataSourceArray(
                ObjectDataSource(src, NODE_SERIALIZER, 512))
    }

    fun clear() {
        if (src is LevelDbDataSource) {
            src.reset()
            this.nodes = DataSourceArray(
                    ObjectDataSource(src, NODE_SERIALIZER, 512))
        } else {
            throw RuntimeException("Not supported")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("db")
        private val NODE_SERIALIZER = object : Serializer<Pair<Node, Int>, ByteArray> {

            override fun serialize(value: Pair<Node, Int>): ByteArray {
                val nodeRlp = value.left.rlp
                val nodeIsDiscovery = RLP.encodeByte(if (value.left.isDiscoveryNode) 1.toByte() else 0)
                val savedReputation = RLP.encodeBigInteger(BigInteger.valueOf(value.right.toLong()))

                return RLP.encodeList(nodeRlp, nodeIsDiscovery, savedReputation)
            }

            override fun deserialize(bytes: ByteArray?): Pair<Node, Int>? {
                if (bytes == null) return null

                val nodeElement = RLP.decode2(bytes)[0] as RLPList
                val nodeRlp = nodeElement[0].rlpData
                val nodeIsDiscovery = nodeElement[1].rlpData
                val savedReputation = nodeElement[2].rlpData
                val node = Node(nodeRlp)
                node.isDiscoveryNode = nodeIsDiscovery != null

                return Pair.of(node, if (savedReputation == null) 0 else BigInteger(1, savedReputation).toInt())
            }
        }
    }
}
