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

import org.ethereum.vm.DataWord;
import org.ethereum.vm.program.listener.ProgramListenerAdaptor;

public class ProgramTraceListener extends ProgramListenerAdaptor {

    private final boolean enabled;
    private OpActions actions = new OpActions();

    public ProgramTraceListener(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onMemoryExtend(final int delta) {
        if (enabled) actions.addMemoryExtend(delta);
    }

    @Override
    public void onMemoryWrite(final int address, final byte[] data, final int size) {
        if (enabled) actions.addMemoryWrite(address, data, size);
    }

    @Override
    public void onStackPop() {
        if (enabled) actions.addStackPop();
    }

    @Override
    public void onStackPush(final DataWord value) {
        if (enabled) actions.addStackPush(value);
    }

    @Override
    public void onStackSwap(final int from, final int to) {
        if (enabled) actions.addStackSwap(from, to);
    }

    @Override
    public void onStoragePut(final DataWord key, final DataWord value) {
        if (enabled) {
            if (value.equals(DataWord.ZERO)) {
                actions.addStorageRemove(key);
            } else {
                actions.addStoragePut(key, value);
            }
        }
    }

    @Override
    public void onStorageClear() {
        if (enabled) actions.addStorageClear();
    }

    public OpActions resetActions() {
        final OpActions actions = this.actions;
        this.actions = new OpActions();
        return actions;
    }
}
