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

package org.ethereum.net.rlpx;

import org.ethereum.net.rlpx.discover.table.KademliaOptions;
import org.ethereum.net.rlpx.discover.table.NodeBucket;
import org.ethereum.net.rlpx.discover.table.NodeEntry;
import org.ethereum.net.rlpx.discover.table.NodeTable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class KademliaTest {

    private static byte[] getNodeId() {
        final Random gen = new Random();
        final byte[] id = new byte[64];
        gen.nextBytes(id);
        return id;
    }

    private static Node getNode(final byte[] id, final int i) {
        id[0] += (byte) i;
        final Node n = getNode();
        n.setId(id);
        return n;
    }

    private static Node getNode() {
        return new Node(getNodeId(), "127.0.0.1", 30303);
    }

    private static NodeTable getTestNodeTable(final int nodesQuantity) {
        final NodeTable testTable = new NodeTable(getNode());

        for (int i = 0; i < nodesQuantity; i++) {
            testTable.addNode(getNode());
        }

        return testTable;
    }

    private static void showBuckets(final NodeTable t) {
        for (final NodeBucket b : t.getBuckets()) {
            if (b.getNodesCount() > 0) {
                System.out.println(String.format("Bucket %d nodes %d depth %d", b.getDepth(), b.getNodesCount(), b.getDepth()));
            }
        }
    }

    private static boolean containsNode(final NodeTable t, final Node n) {
        for (final NodeEntry e : t.getAllNodes()) {
            if (e.getNode().toString().equals(n.toString())) {
                return true;
            }
        }
        return false;
    }

    @Ignore
    @Test
    public void test1() {
        //init table with one home node
        final NodeTable t = getTestNodeTable(0);
        final Node homeNode = t.getNode();

        //table should contain the home node only
        assertEquals(t.getAllNodes().size(), 1);

        final Node bucketNode = t.getAllNodes().get(0).getNode();

        assertEquals(homeNode, bucketNode);

    }

    @Test
    public void test2() {
        final NodeTable t = getTestNodeTable(0);
        final Node n = getNode();
        t.addNode(n);

        assertTrue(containsNode(t, n));

        t.dropNode(n);

        assertFalse(containsNode(t, n));
    }

    @Test
    public void test3() {
        final NodeTable t = getTestNodeTable(1000);
        showBuckets(t);

        final List<Node> closest1 = t.getClosestNodes(t.getNode().getId());
        final List<Node> closest2 = t.getClosestNodes(getNodeId());

        assertNotEquals(closest1, closest2);
    }

    @Test
    public void test4() {
        final NodeTable t = getTestNodeTable(0);
        final Node homeNode = t.getNode();

        //t.getBucketsCount() returns non empty buckets
        assertEquals(t.getBucketsCount(), 1);

        //creates very close nodes
        for (int i = 1; i < KademliaOptions.BUCKET_SIZE; i++) {
            final Node n = getNode(homeNode.getId(), i);
            t.addNode(n);
        }

        assertEquals(t.getBucketsCount(), 1);
        assertEquals(t.getBuckets()[0].getNodesCount(), KademliaOptions.BUCKET_SIZE);
    }
}
