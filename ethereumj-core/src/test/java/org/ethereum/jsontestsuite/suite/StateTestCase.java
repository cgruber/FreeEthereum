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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.jsontestsuite.suite.model.AccountTck;
import org.ethereum.jsontestsuite.suite.model.EnvTck;
import org.ethereum.jsontestsuite.suite.model.LogTck;
import org.ethereum.jsontestsuite.suite.model.TransactionTck;

import java.util.List;
import java.util.Map;

public class StateTestCase {


    private EnvTck env;
    private List<LogTck> logs;
    private String out;
    private Map<String, AccountTck> pre;
    private String postStateRoot;
    private Map<String, AccountTck> post;
    private TransactionTck transaction;


    public StateTestCase() {
    }

    public EnvTck getEnv() {
        return env;
    }

    public void setEnv(final EnvTck env) {
        this.env = env;
    }

    public List<LogTck> getLogs() {
        return logs;
    }

    public void setLogs(final List<LogTck> logs) {
        this.logs = logs;
    }

    public String getOut() {
        return out;
    }

    public void setOut(final String out) {
        this.out = out;
    }

    public Map<String, AccountTck> getPre() {
        return pre;
    }

    public void setPre(final Map<String, AccountTck> pre) {
        this.pre = pre;
    }

    public String getPostStateRoot() {
        return postStateRoot;
    }

    public void setPostStateRoot(final String postStateRoot) {
        this.postStateRoot = postStateRoot;
    }

    public Map<String, AccountTck> getPost() {
        return post;
    }

    public void setPost(final Map<String, AccountTck> post) {
        this.post = post;
    }

    public TransactionTck getTransaction() {
        return transaction;
    }

    public void setTransaction(final TransactionTck transaction) {
        this.transaction = transaction;
    }
}
