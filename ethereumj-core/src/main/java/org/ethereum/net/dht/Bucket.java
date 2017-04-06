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

package org.ethereum.net.dht;

import java.util.ArrayList;
import java.util.List;

public class Bucket {

    private static final int MAX_KADEMLIA_K = 5;
    private final String name;
    // if bit = 1 go left
    private Bucket left;
    // if bit = 0 go right
    private Bucket right;
    private List<Peer> peers = new ArrayList<>();


    private Bucket(final String name) {
        this.name = name;
    }


    private void add(final Peer peer) {

        if (peer == null) throw new Error("Not a leaf");

        if ( peers == null){

            if (peer.nextBit(name) == 1)
                left.add(peer);
            else
                right.add(peer);

            return;
        }

        peers.add(peer);

        if (peers.size() > MAX_KADEMLIA_K)
            splitBucket();
    }

    private void splitBucket() {
        left = new Bucket(name + "1");
        right = new Bucket(name + "0");

        for (final Peer id : peers) {
            if (id.nextBit(name) == 1)
                left.add(id);
            else
                right.add(id);
        }

        this.peers = null;
    }


    public Bucket left() {
        return left;
    }

    public Bucket right() {
        return right;
    }


    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        sb.append(name).append("\n");

        if (peers == null) return sb.toString();

        for (final Peer id : peers)
            sb.append(id.toBinaryString()).append("\n");

        return sb.toString();
    }


    public void traverseTree(final DoOnTree doOnTree) {

        if (left  != null) left.traverseTree(doOnTree);
        if (right != null) right.traverseTree(doOnTree);

        doOnTree.call(this);
    }


    /********************/
     // tree operations //
    public String getName() {
        return name;
    }

    public List<Peer> getPeers() {
        return peers;
    }

    /********************/

    public interface DoOnTree {

        void call(Bucket bucket);
    }

    public static class SaveLeaf implements DoOnTree {

        List<Bucket> leafs = new ArrayList<>();

        @Override
        public void call(final Bucket bucket) {
            if (bucket.peers != null) leafs.add(bucket);
        }

        public List<Bucket> getLeafs() {
            return leafs;
        }

        public void setLeafs(final List<Bucket> leafs) {
            this.leafs = leafs;
        }
    }
}
