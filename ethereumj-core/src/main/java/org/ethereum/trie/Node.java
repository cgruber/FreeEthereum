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

package org.ethereum.trie;

import org.ethereum.util.Value;


/**
 * A Node in a Merkle Patricia Tree is one of the following:
 *
 * - NULL (represented as the empty string)
 * - A two-item array [ key, value ] (1 key for 2-item array)
 * - A 17-item array [ v0 ... v15, vt ] (16 keys for 17-item array)
 *
 * The idea is that in the event that there is a long path of nodes
 * each with only one element, we shortcut the descent by setting up
 * a [ key, value ] node, where the key gives the hexadecimal path
 * to descend, in the compact encoding described above, and the value
 * is just the hash of the node like in the standard radix tree.
 *
 *                               R
 *                              / \
 *                             /   \
 *                            N     N
 *                           / \   / \
 *                          L   L L   L
 *
 * Also, we add another conceptual change: internal nodes can no longer
 * have values, only leaves with no children of their own can; however,
 * since to be fully generic we want the key/value store to be able
 * store keys like 'dog' and 'doge' at the same time, we simply add
 * a terminator symbol (16) to the alphabet so there is never a value
 * "en-route" to another value.
 *
 * Where a node is referenced inside a node, what is included is:
 *
 *      H(rlp.encode(x)) where H(x) = keccak(x) if len(x) &gt;= 32 else x
 *
 * Note that when updating a trie, you will need to store the key/value pair (keccak(x), x)
 * in a persistent lookup table when you create a node with length &gt;= 32,
 * but if the node is shorter than that then you do not need to store anything
 * when length &lt; 32 for the obvious reason that the function f(x) = x is reversible.
 *
 * @author Nick Savers
 * @since 20.05.2014
 */
public class Node {

    /* RLP encoded value of the Trie-node */
    private final Value value;
    private boolean dirty;

    public Node(final Value val) {
        this(val, false);
    }

    private Node(final Value val, final boolean dirty) {
        this.value = val;
        this.dirty = dirty;
    }

    public Node copy() {
        return new Node(this.value, this.dirty);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "[" + dirty + ", " + value + "]";
    }
}
