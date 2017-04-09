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

package org.ethereum.net.rlpx.discover.table;

import org.ethereum.net.rlpx.Node;

import java.util.*;

public class NodeTable {

    private final Node node;  // our node
    private transient NodeBucket[] buckets;
    private transient List<NodeEntry> nodes;
    private Map<Node, Node> evictedCandidates = new HashMap<>();
    private Map<Node, Date> expectedPongs = new HashMap<>();

    public NodeTable(final Node n) {
        this(n, true);
    }

    public NodeTable(final Node n, final boolean includeHomeNode) {
        this.node = n;
        initialize();
        if (includeHomeNode) {
            addNode(this.node);
        }
    }

    public Node getNode() {
        return node;
    }

    private void initialize()
    {
        nodes = new ArrayList<>();
        buckets = new NodeBucket[KademliaOptions.BINS];
        for (int i = 0; i < KademliaOptions.BINS; i++)
        {
            buckets[i] = new NodeBucket(i);
        }
    }

    public synchronized Node addNode(final Node n) {
        final NodeEntry e = new NodeEntry(node.getId(), n);
        final NodeEntry lastSeen = buckets[getBucketId(e)].addNode(e);
        if (lastSeen != null) {
            return lastSeen.getNode();
        }
        if (!nodes.contains(e)) {
            nodes.add(e);
        }
        return null;
    }

    public synchronized void dropNode(final Node n) {
        final NodeEntry e = new NodeEntry(node.getId(), n);
        buckets[getBucketId(e)].dropNode(e);
        nodes.remove(e);
    }

    public synchronized boolean contains(final Node n) {
        final NodeEntry e = new NodeEntry(node.getId(), n);
        for (final NodeBucket b : buckets) {
            if (b.getNodes().contains(e)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void touchNode(final Node n) {
        final NodeEntry e = new NodeEntry(node.getId(), n);
        for (final NodeBucket b : buckets) {
            if (b.getNodes().contains(e)) {
                b.getNodes().get(b.getNodes().indexOf(e)).touch();
                break;
            }
        }
    }

    public int getBucketsCount() {
        int i = 0;
        for (final NodeBucket b : buckets) {
            if (b.getNodesCount() > 0) {
                i++;
            }
        }
        return i;
    }

    public synchronized NodeBucket[] getBuckets() {
        return buckets;
    }

    private int getBucketId(final NodeEntry e) {
        final int id = e.getDistance() - 1;
        return id < 0 ? 0 : id;
    }

    public synchronized int getNodesCount() {
        return nodes.size();
    }

    public synchronized List<NodeEntry> getAllNodes()
    {
        final List<NodeEntry> nodes = new ArrayList<>();

        for (final NodeBucket b : buckets)
        {
//            nodes.addAll(b.getNodes());
            for (final NodeEntry e : b.getNodes())
            {
                if (!e.getNode().equals(node)) {
                    nodes.add(e);
                }
            }
        }

//        boolean res = nodes.remove(node);
        return nodes;
    }

    public synchronized List<Node> getClosestNodes(final byte[] targetId) {
        List<NodeEntry> closestEntries = getAllNodes();
        final List<Node> closestNodes = new ArrayList<>();
        closestEntries.sort(new DistanceComparator(targetId));
        if (closestEntries.size() > KademliaOptions.BUCKET_SIZE) {
            closestEntries = closestEntries.subList(0, KademliaOptions.BUCKET_SIZE);
        }

        for (final NodeEntry e : closestEntries) {
            if (!e.getNode().isDiscoveryNode()) {
                closestNodes.add(e.getNode());
            }
        }
        return closestNodes;
    }
}
