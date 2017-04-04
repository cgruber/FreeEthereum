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

import org.ethereum.vm.DataWord
import org.ethereum.vm.program.listener.ProgramListener
import org.ethereum.vm.program.listener.ProgramListenerAware

class Stack : java.util.Stack<DataWord>(), ProgramListenerAware {

    private var programListener: ProgramListener? = null

    override fun setProgramListener(listener: ProgramListener) {
        this.programListener = listener
    }

    @Synchronized override fun pop(): DataWord {
        if (programListener != null) programListener!!.onStackPop()
        return super.pop()
    }

    override fun push(item: DataWord): DataWord {
        if (programListener != null) programListener!!.onStackPush(item)
        return super.push(item)
    }

    fun swap(from: Int, to: Int) {
        if (isAccessible(from) && isAccessible(to) && from != to) {
            if (programListener != null) programListener!!.onStackSwap(from, to)
            val tmp = get(from)
            set(from, set(to, tmp))
        }
    }

    private fun isAccessible(from: Int): Boolean {
        return from >= 0 && from < size
    }
}
