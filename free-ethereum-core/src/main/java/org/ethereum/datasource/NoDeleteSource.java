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

/**
 * Just ignores deletes from the backing Source
 * Normally used for testing for Trie backing Sources to
 * not delete older states
 *
 * Created by Anton Nashatyrev on 03.11.2016.
 */
public class NoDeleteSource<Key, Value> extends AbstractChainedSource<Key, Value, Key, Value> {

    public NoDeleteSource(final Source<Key, Value> src) {
        super(src);
        setFlushSource(true);
    }

    @Override
    public void delete(final Key key) {
    }

    @Override
    public void put(final Key key, final Value val) {
        if (val != null) getSource().put(key, val);
    }

    @Override
    public Value get(final Key key) {
        return getSource().get(key);
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }
}
