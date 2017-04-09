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

package org.ethereum.config

import org.ethereum.net.rlpx.Node
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

class NodeFilter {
    private val entries = ArrayList<Entry>()

    fun add(nodeId: ByteArray, hostIpPattern: String) {
        entries.add(Entry(nodeId, hostIpPattern))
    }

    fun accept(node: Node): Boolean {
        return entries.any { it.accept(node) }
    }

    fun accept(nodeAddr: InetAddress): Boolean {
        return entries.any { it.accept(nodeAddr) }
    }

    private inner class Entry(internal val nodeId: ByteArray?, hostIpPattern: String?) {
        internal val hostIpPattern: String?

        init {
            var hostIpPattern = hostIpPattern
            if (hostIpPattern != null) {
                val idx = hostIpPattern.indexOf("*")
                if (idx > 0) {
                    hostIpPattern = hostIpPattern.substring(0, idx)
                }
            }
            this.hostIpPattern = hostIpPattern
        }

        fun accept(nodeAddr: InetAddress): Boolean {
            val ip = nodeAddr.hostAddress
            return hostIpPattern != null && ip.startsWith(hostIpPattern)
        }

        fun accept(node: Node): Boolean {
            try {
                return (nodeId == null || Arrays.equals(node.id, nodeId)) && (hostIpPattern == null || accept(InetAddress.getByName(node.host)))
            } catch (e: UnknownHostException) {
                return false
            }
        }
    }
}
