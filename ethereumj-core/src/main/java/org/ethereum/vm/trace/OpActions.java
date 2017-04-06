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

package org.ethereum.vm.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ethereum.vm.DataWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ethereum.util.ByteUtil.toHexString;

public class OpActions {

    private List<Action> stack = new ArrayList<>();
    private List<Action> memory = new ArrayList<>();
    private List<Action> storage = new ArrayList<>();

    private static Action addAction(final List<Action> container, final Action.Name name) {
        final Action action = new Action();
        action.setName(name);

        container.add(action);

        return action;
    }

    public List<Action> getStack() {
        return stack;
    }

    public void setStack(final List<Action> stack) {
        this.stack = stack;
    }

    public List<Action> getMemory() {
        return memory;
    }

    public void setMemory(final List<Action> memory) {
        this.memory = memory;
    }

    public List<Action> getStorage() {
        return storage;
    }

    public void setStorage(final List<Action> storage) {
        this.storage = storage;
    }

    public Action addStackPop() {
        return addAction(stack, Action.Name.pop);
    }

    public Action addStackPush(final DataWord value) {
        return addAction(stack, Action.Name.push)
                .addParam("value", value);
    }

    public Action addStackSwap(final int from, final int to) {
        return addAction(stack, Action.Name.swap)
                .addParam("from", from)
                .addParam("to", to);
    }

    public Action addMemoryExtend(final long delta) {
        return addAction(memory, Action.Name.extend)
                .addParam("delta", delta);
    }

    public Action addMemoryWrite(final int address, final byte[] data, final int size) {
        return addAction(memory, Action.Name.write)
                .addParam("address", address)
                .addParam("data", toHexString(data).substring(0, size));
    }

    public Action addStoragePut(final DataWord key, final DataWord value) {
        return addAction(storage, Action.Name.put)
                .addParam("key", key)
                .addParam("value", value);
    }

    public Action addStorageRemove(final DataWord key) {
        return addAction(storage, Action.Name.remove)
                .addParam("key", key);
    }

    public Action addStorageClear() {
        return addAction(storage, Action.Name.clear);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Action {

        private Name name;
        private Map<String, Object> params;

        public Name getName() {
            return name;
        }

        public void setName(final Name name) {
            this.name = name;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(final Map<String, Object> params) {
            this.params = params;
        }

        Action addParam(final String name, final Object value) {
            if (value != null) {
                if (params == null) {
                    params = new HashMap<>();
                }
                params.put(name, value.toString());
            }
            return this;
        }

        public enum Name {
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
