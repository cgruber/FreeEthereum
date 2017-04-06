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

package org.ethereum.net.shh;

import org.ethereum.crypto.ECKey;
import org.springframework.stereotype.Component;

/**
 * Created by Anton Nashatyrev on 25.09.2015.
 */
@Component
public abstract class Whisper {

    public abstract String addIdentity(ECKey key);

    public abstract String newIdentity();

    public abstract void watch(MessageWatcher f);

    public abstract void unwatch(MessageWatcher f);

    public void send(final byte[] payload, final Topic[] topics) {
        send(null, null, payload, topics, 50, 50);
    }

    public void send(final byte[] payload, final Topic[] topics, final int ttl, final int workToProve) {
        send(null, null, payload, topics, ttl, workToProve);
    }

    public void send(final String toIdentity, final byte[] payload, final Topic[] topics) {
        send(null, toIdentity, payload, topics, 50, 50);
    }

    public void send(final String toIdentity, final byte[] payload, final Topic[] topics, final int ttl, final int workToProve) {
        send(null, toIdentity, payload, topics, ttl, workToProve);
    }

    public void send(final String fromIdentity, final String toIdentity, final byte[] payload, final Topic[] topics) {
        send(fromIdentity, toIdentity, payload, topics, 50, 50);
    }

    protected abstract void send(String fromIdentity, String toIdentity, byte[] payload, Topic[] topics, int ttl, int workToProve);
}
