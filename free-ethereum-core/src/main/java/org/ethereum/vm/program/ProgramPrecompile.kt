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

package org.ethereum.vm.program

import org.ethereum.util.ByteUtil
import org.ethereum.util.RLP
import org.ethereum.util.RLPList
import org.ethereum.vm.OpCode
import java.util.*

class ProgramPrecompile {

    private val jumpdest = HashSet<Int>()

    fun serialize(): ByteArray {
        val jdBytes = arrayOfNulls<ByteArray>(jumpdest.size + 1)
        var cnt = 0
        jdBytes[cnt++] = RLP.encodeInt(version)
        for (dst in jumpdest) {
            jdBytes[cnt++] = RLP.encodeInt(dst)
        }

        return RLP.encodeList(*jdBytes)
    }

    fun hasJumpDest(pc: Int): Boolean {
        return jumpdest.contains(pc)
    }

    companion object {
        private val version = 1

        fun deserialize(stream: ByteArray): ProgramPrecompile? {
            val l = RLP.decode2(stream)[0] as RLPList
            val ver = ByteUtil.byteArrayToInt(l[0].rlpData)
            if (ver != version) return null
            val ret = ProgramPrecompile()
            for (i in 1..l.size - 1) {
                ret.jumpdest.add(ByteUtil.byteArrayToInt(l[i].rlpData))
            }
            return ret
        }

        fun compile(ops: ByteArray): ProgramPrecompile {
            val ret = ProgramPrecompile()
            var i = 0
            while (i < ops.size) {

                val op = OpCode.code(ops[i])
                if (op == null) {
                    ++i
                    continue
                }

                if (op == OpCode.JUMPDEST) ret.jumpdest.add(i)

                if (op.asInt() >= OpCode.PUSH1.asInt() && op.asInt() <= OpCode.PUSH32.asInt()) {
                    i += op.asInt() - OpCode.PUSH1.asInt() + 1
                }
                ++i
            }
            return ret
        }

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            val pp = ProgramPrecompile()
            pp.jumpdest.add(100)
            pp.jumpdest.add(200)
            val bytes = pp.serialize()

            val pp1 = ProgramPrecompile.deserialize(bytes)
            println(pp1!!.jumpdest)
        }
    }
}
