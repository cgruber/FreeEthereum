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

package org.ethereum.vm.program.listener;

import org.ethereum.vm.DataWord;

import java.util.ArrayList;
import java.util.List;


public class CompositeProgramListener implements ProgramListener {

    private final List<ProgramListener> listeners = new ArrayList<>();

    @Override
    public void onMemoryExtend(final int delta) {
        for (final ProgramListener listener : listeners) {
            listener.onMemoryExtend(delta);
        }
    }

    @Override
    public void onMemoryWrite(final int address, final byte[] data, final int size) {
        for (final ProgramListener listener : listeners) {
            listener.onMemoryWrite(address, data, size);
        }
    }

    @Override
    public void onStackPop() {
        for (final ProgramListener listener : listeners) {
            listener.onStackPop();
        }
    }

    @Override
    public void onStackPush(final DataWord value) {
        for (final ProgramListener listener : listeners) {
            listener.onStackPush(value);
        }
    }

    @Override
    public void onStackSwap(final int from, final int to) {
        for (final ProgramListener listener : listeners) {
            listener.onStackSwap(from, to);
        }
    }

    @Override
    public void onStoragePut(final DataWord key, final DataWord value) {
        for (final ProgramListener listener : listeners) {
            listener.onStoragePut(key, value);
        }
    }

    @Override
    public void onStorageClear() {
        for (final ProgramListener listener : listeners) {
            listener.onStorageClear();
        }
    }

    public void addListener(final ProgramListener listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
