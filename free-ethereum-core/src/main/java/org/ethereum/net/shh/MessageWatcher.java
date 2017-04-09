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


public abstract class MessageWatcher {
    private String to;
    private String from;
    private Topic[] topics = null;

    public MessageWatcher() {
    }

    public MessageWatcher(final String to, final String from, final Topic[] topics) {
        this.to = to;
        this.from = from;
        this.topics = topics;
    }

    public MessageWatcher setFilterTopics(final Topic[] topics) {
        this.topics = topics;
        return this;
    }

    public String getTo() {
        return to;
    }

    public MessageWatcher setTo(final String to) {
        this.to = to;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public MessageWatcher setFrom(final String from) {
        this.from = from;
        return this;
    }

    public Topic[] getTopics() {
        return topics == null ? new Topic[0] : topics;
    }

    boolean match(final String to, final String from, final Topic[] topics) {
        if (this.to != null) {
            if (!this.to.equals(to)) {
                return false;
            }
        }

        if (this.from != null) {
            if (!this.from.equals(from)) {
                return false;
            }
        }

        if (this.topics != null) {
            for (final Topic watchTopic : this.topics) {
                for (final Topic msgTopic : topics) {
                    if (watchTopic.equals(msgTopic)) return true;
                }
            }
            return false;
        }
        return true;
    }

    protected abstract void newMessage(WhisperMessage msg);
}
