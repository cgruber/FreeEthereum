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
 * Abstract Source implementation with underlying backing Source
 * The class has control whether the backing Source should be flushed
 * in 'cascade' manner
 *
 * Created by Anton Nashatyrev on 06.12.2016.
 */
public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements Source<Key, Value> {

    boolean flushSource;
    private Source<SourceKey, SourceValue> source;

    /**
     * Intended for subclasses which wishes to initialize the source
     * later via {@link #setSource(Source)} method
     */
    AbstractChainedSource() {
    }

    AbstractChainedSource(final Source<SourceKey, SourceValue> source) {
        this.source = source;
    }

    public Source<SourceKey, SourceValue> getSource() {
        return source;
    }

    /**
     * Intended for subclasses which wishes to initialize the source later
     */
    protected void setSource(final Source<SourceKey, SourceValue> src) {
        source = src;
    }

    public void setFlushSource(final boolean flushSource) {
        this.flushSource = flushSource;
    }

    /**
     * Invokes {@link #flushImpl()} and does backing Source flush if required
     * @return true if this or source flush did any changes
     */
    @Override
    public synchronized boolean flush() {
        boolean ret = flushImpl();
        if (flushSource) {
            ret |= getSource().flush();
        }
        return ret;
    }

    /**
     * Should be overridden to do actual source flush
     */
    protected abstract boolean flushImpl();
}
