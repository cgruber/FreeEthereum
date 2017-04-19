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

package org.ethereum.net.rlpx.discover

import org.ethereum.net.rlpx.Node
import org.ethereum.net.rlpx.discover.table.KademliaOptions
import org.slf4j.LoggerFactory
import java.util.*

open class DiscoverTask(private val nodeManager: NodeManager) : Runnable {

    private val nodeId: ByteArray = nodeManager.homeNode.id

    override fun run() {
        discover(nodeId, 0, ArrayList<Node>())
    }

    @Synchronized fun discover(nodeId: ByteArray, round: Int, prevTried: List<Node>) {

        try {
            //        if (!channel.isOpen() || round == KademliaOptions.MAX_STEPS) {
            //            logger.info("{}", String.format("Nodes discovered %d ", table.getAllNodes().size()));
            //            return;
            //        }

            if (round == KademliaOptions.MAX_STEPS) {
                logger.debug("Node table contains [{}] peers", nodeManager.getTable().nodesCount)
                logger.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover after %d rounds.", round))
                logger.trace("{}\n{}", String.format("Nodes discovered %d ", nodeManager.getTable().nodesCount), dumpNodes())
                return
            }

            val closest = nodeManager.getTable().getClosestNodes(nodeId)
            val tried = ArrayList<Node>()

            for (n in closest) {
                if (!tried.contains(n) && !prevTried.contains(n)) {
                    try {
                        nodeManager.getNodeHandler(n).sendFindNode(nodeId)
                        tried.add(n)
                        Thread.sleep(50)
                    } catch (ignored: InterruptedException) {
                    } catch (ex: Exception) {
                        logger.error("Unexpected Exception " + ex, ex)
                    }

                }
                if (tried.size == KademliaOptions.ALPHA) {
                    break
                }
            }

            //            channel.flush();

            if (tried.isEmpty()) {
                logger.debug("{}", String.format("(tried.isEmpty()) Terminating discover after %d rounds.", round))
                logger.trace("{}\n{}", String.format("Nodes discovered %d ", nodeManager.getTable().nodesCount), dumpNodes())
                return
            }

            tried.addAll(prevTried)

            discover(nodeId, round + 1, tried)
        } catch (ex: Exception) {
            logger.info("{}", ex)
        }

    }

    private fun dumpNodes(): String {
        val ret = StringBuilder()
        for (entry in nodeManager.getTable().allNodes) {
            ret.append("    ").append(entry.node).append("\n")
        }
        return ret.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger("discover")
    }
}
