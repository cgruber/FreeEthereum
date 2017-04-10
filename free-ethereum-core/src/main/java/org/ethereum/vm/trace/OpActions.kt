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

package org.ethereum.vm.trace

import com.fasterxml.jackson.annotation.JsonInclude
import org.ethereum.util.ByteUtil.toHexString
import org.ethereum.vm.DataWord
import java.util.*

class OpActions {

    private var stack: MutableList<Action> = ArrayList()
    private var memory: MutableList<Action> = ArrayList()
    private var storage: MutableList<Action> = ArrayList()

    private fun addAction(container: MutableList<Action>, name: Action.Name): Action {
        val action = Action()
        action.name = name

        container.add(action)

        return action
    }

    fun getStack(): List<Action> {
        return stack
    }

    fun setStack(stack: MutableList<Action>) {
        this.stack = stack
    }

    fun getMemory(): List<Action> {
        return memory
    }

    fun setMemory(memory: MutableList<Action>) {
        this.memory = memory
    }

    fun getStorage(): List<Action> {
        return storage
    }

    fun setStorage(storage: MutableList<Action>) {
        this.storage = storage
    }

    fun addStackPop(): Action {
        return addAction(stack, Action.Name.pop)
    }

    fun addStackPush(value: DataWord): Action {
        return addAction(stack, Action.Name.push)
                .addParam("value", value)
    }

    fun addStackSwap(from: Int, to: Int): Action {
        return addAction(stack, Action.Name.swap)
                .addParam("from", from)
                .addParam("to", to)
    }

    fun addMemoryExtend(delta: Long): Action {
        return addAction(memory, Action.Name.extend)
                .addParam("delta", delta)
    }

    fun addMemoryWrite(address: Int, data: ByteArray, size: Int): Action {
        return addAction(memory, Action.Name.write)
                .addParam("address", address)
                .addParam("data", toHexString(data).substring(0, size))
    }

    fun addStoragePut(key: DataWord, value: DataWord): Action {
        return addAction(storage, Action.Name.put)
                .addParam("key", key)
                .addParam("value", value)
    }

    fun addStorageRemove(key: DataWord): Action {
        return addAction(storage, Action.Name.remove)
                .addParam("key", key)
    }

    fun addStorageClear(): Action {
        return addAction(storage, Action.Name.clear)
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    class Action {

        var name: Name? = null
        private var params: MutableMap<String, Any>? = null

        fun getParams(): Map<String, Any> {
            return params!!
        }

        fun setParams(params: MutableMap<String, Any>) {
            this.params = params
        }

        internal fun addParam(name: String, value: Any?): Action {
            if (value != null) {
                if (params == null) {
                    params = HashMap<String, Any>()
                }
                params!!.put(name, value.toString())
            }
            return this
        }

        enum class Name {
            pop,
            push,
            swap,
            extend,
            write,
            put,
            remove,
            clear
        }
    }
}
