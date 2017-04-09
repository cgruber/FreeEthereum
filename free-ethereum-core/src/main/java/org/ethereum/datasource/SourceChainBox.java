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

package org.ethereum.datasource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chain of Sources as a single Source
 * All calls to this Source are delegated to the last Source in the chain
 * On flush all Sources in chain are flushed in reverse order
 *
 * Created by Anton Nashatyrev on 07.12.2016.
 */
public class SourceChainBox<Key, Value, SourceKey, SourceValue>
        extends AbstractChainedSource<Key, Value, SourceKey, SourceValue> {

    private final List<Source> chain = new ArrayList<>();
    private Source<Key, Value> lastSource;

    protected SourceChainBox(final Source<SourceKey, SourceValue> source) {
        super(source);
    }

    /**
     * Adds next Source in the chain to the collection
     * Sources should be added from most bottom (connected to the backing Source)
     * All calls to the SourceChainBox will be delegated to the last added
     * Source
     */
    protected void add(final Source src) {
        chain.add(src);
        lastSource = src;
    }

    @Override
    public void put(final Key key, final Value val) {
        lastSource.put(key, val);
    }

    @Override
    public Value get(final Key key) {
        return lastSource.get(key);
    }

    @Override
    public void delete(final Key key) {
        lastSource.delete(key);
    }

//    @Override
//    public boolean flush() {
////        boolean ret = false;
////        for (int i = chain.size() - 1; i >= 0 ; i--) {
////            ret |= chain.get(i).flush();
////        }
//        return lastSource.flush();
//    }

    @Override
    protected boolean flushImpl() {
        return lastSource.flush();
    }
}
