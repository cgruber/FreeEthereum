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

package org.ethereum.net.client

/**
 * The protocols and versions of those protocols that this peer support
 */
class Capability(val name: String?, val version: Byte) : Comparable<Capability> {

    val isEth: Boolean
        get() = ETH == name

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj !is Capability) return false

        val other = obj
        if (this.name == null)
            return other.name == null
        else
            return this.name == other.name && this.version == other.version
    }

    override fun compareTo(o: Capability): Int {
        val cmp = this.name!!.compareTo(o.name!!)
        if (cmp != 0) {
            return cmp
        } else {
            return java.lang.Byte.valueOf(this.version)!!.compareTo(o.version)
        }
    }

    override fun hashCode(): Int {
        var result = name!!.hashCode()
        result = 31 * result + version.toInt()
        return result
    }

    override fun toString(): String {
        return name + ":" + version
    }

    companion object {

        val P2P = "p2p"
        val ETH = "eth"
        val SHH = "shh"
        val BZZ = "bzz"
    }
}