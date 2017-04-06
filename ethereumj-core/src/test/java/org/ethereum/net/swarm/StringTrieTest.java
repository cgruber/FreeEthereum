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

package org.ethereum.net.swarm;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Admin on 11.06.2015.
 */
public class StringTrieTest {

    @Test
    public void testAdd() {
        final T trie = new T();

        trie.add("aaa");
        trie.add("bbb");
        trie.add("aad");
        trie.add("aade");
        trie.add("aadd");

        System.out.println(Util.dumpTree(trie.rootNode));

        Assert.assertEquals("aaa", trie.get("aaa").getAbsolutePath());
        Assert.assertEquals("bbb", trie.get("bbb").getAbsolutePath());
        Assert.assertEquals("aad", trie.get("aad").getAbsolutePath());
        Assert.assertEquals("aa", trie.get("aaqqq").getAbsolutePath());
        Assert.assertEquals("", trie.get("bbe").getAbsolutePath());
    }

    @Test
    public void testAddRootLeaf() {
        final T trie = new T();

        trie.add("ax");
        trie.add("ay");
        trie.add("a");

        System.out.println(Util.dumpTree(trie.rootNode));
    }

    @Test
    public void testAddDuplicate() {
        final T trie = new T();

        final A a = trie.add("a");
        final A ay = trie.add("ay");
        final A a1 = trie.add("a");
        Assert.assertTrue(a == a1);
        final A ay1 = trie.add("ay");
        Assert.assertTrue(ay == ay1);
    }

    @Test
    public void testAddLeafRoot() {
        final T trie = new T();

        trie.add("a");
        trie.add("ax");

        System.out.println(Util.dumpTree(trie.rootNode));
    }

    @Test
    public void testAddDelete() {
        final T trie = new T();

        trie.add("aaaa");
        trie.add("aaaaxxxx");
        trie.add("aaaaxxxxeeee");
        System.out.println(Util.dumpTree(trie.rootNode));
        trie.delete("aaaa");
        System.out.println(Util.dumpTree(trie.rootNode));
        trie.delete("aaaaxxxx");
        System.out.println(Util.dumpTree(trie.rootNode));
    }

    class A extends StringTrie.TrieNode<A> {
        String id;

        public A() {
        }

        public A(final A parent, final String relPath) {
            super(parent, relPath);
        }

        public void setId(final String id) {
            this.id = id;
        }

        @Override
        protected A createNode(final A parent, final String path) {
            return new A(parent, path);
        }

        @Override
        public String toString() {
            return "A[" + (id != null ? id : "") + "]";
        }
    }

    class T extends StringTrie<A> {

        public T() {
            super(new A());
        }

        @Override
        public A add(final String path) {
            final A ret = super.add(path);
            ret.setId(path);
            return ret;
        }
    }
}
