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

public class NodeEntry {
    private final Node node;
    private final byte[] ownerId;
    private final String entryId;
    private final int distance;
    private long modified;

    public NodeEntry(final Node n) {
        this.node = n;
        this.ownerId = n.getId();
        entryId = n.toString();
        distance = distance(ownerId, n.getId());
        touch();
    }

    public NodeEntry(final byte[] ownerId, final Node n) {
        this.node = n;
        this.ownerId = ownerId;
        entryId = n.toString();
        distance = distance(ownerId, n.getId());
        touch();
    }

    public static int distance(final byte[] ownerId, final byte[] targetId) {
//        byte[] h1 = keccak(targetId);
//        byte[] h2 = keccak(ownerId);
        final byte[] h1 = targetId;
        final byte[] h2 = ownerId;

        final byte[] hash = new byte[Math.min(h1.length, h2.length)];

        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) (((int) h1[i]) ^ ((int) h2[i]));
        }

        int d = KademliaOptions.BINS;

        for (final byte b : hash)
        {
            if (b == 0)
            {
                d -= 8;
            }
            else
            {
                int count = 0;
                for (int i = 7; i >= 0; i--)
                {
                    final boolean a = (b & (1 << i)) == 0;
                    if (a)
                    {
                        count++;
                    }
                    else
                    {
                        break;
                    }
                }

                d -= count;

                break;
            }
        }
        return d;
    }

    public void touch() {
        modified = System.currentTimeMillis();
    }

    public int getDistance() {
        return distance;
    }

    public String getId() {
        return entryId;
    }

    public Node getNode() {
        return node;
    }

    public long getModified() {
        return modified;
    }

    @Override
    public boolean equals(final Object o) {
        boolean ret = false;

        if (o instanceof NodeEntry) {
            final NodeEntry e = (NodeEntry) o;
            ret = this.getId().equals(e.getId());
        }

        return ret;
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }
}
