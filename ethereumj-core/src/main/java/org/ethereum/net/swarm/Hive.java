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

import org.ethereum.net.rlpx.Node;
import org.ethereum.net.rlpx.discover.table.NodeEntry;
import org.ethereum.net.rlpx.discover.table.NodeTable;
import org.ethereum.net.swarm.bzz.BzzPeersMessage;
import org.ethereum.net.swarm.bzz.BzzProtocol;
import org.ethereum.net.swarm.bzz.PeerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

/**
 * Serves as an interface to the Kademlia. Manages the database of Nodes reported
 * by all the peers and selects from DB the nearest nodes to the specified hash Key
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
public class Hive {
    private final static Logger LOG = LoggerFactory.getLogger("net.bzz");
    final NodeTable nodeTable;
    private final PeerAddress thisAddress;
    private final Map<Node, BzzProtocol> connectedPeers = new IdentityHashMap<>();
    private final Map<HiveTask, Object> hiveTasks = new IdentityHashMap<>();

    public Hive(final PeerAddress thisAddress) {
        this.thisAddress = thisAddress;
        nodeTable = new NodeTable(thisAddress.toNode());
    }

    public void start() {}

    public void stop() {}

    public PeerAddress getSelfAddress() {
        return thisAddress;
    }

    public void addPeer(final BzzProtocol peer) {
        final Node node = peer.getNode().toNode();
        nodeTable.addNode(node);
        connectedPeers.put(node, peer);
        LOG.info("Hive added a new peer: " + peer);
        peersAdded();
    }

    public void removePeer(final BzzProtocol peer) {
        nodeTable.dropNode(peer.getNode().toNode());
    }

    /**
     * Finds the nodes which are not connected yet
     * TODO review this method later
     */
    public Collection<PeerAddress> getNodes(final Key key, int max) {
        final List<Node> closestNodes = nodeTable.getClosestNodes(key.getBytes());
        final ArrayList<PeerAddress> ret = new ArrayList<>();
        for (final Node node : closestNodes) {
            ret.add(new PeerAddress(node));
            if (--max == 0) break;
        }
        return ret;
    }

    /**
     * Returns the peers in the DB which are closest to the specified key
     * but not more peers than {#maxCount}
     */
    public Collection<BzzProtocol> getPeers(final Key key, int maxCount) {
        final List<Node> closestNodes = nodeTable.getClosestNodes(key.getBytes());
        final ArrayList<BzzProtocol> ret = new ArrayList<>();
        for (final Node node : closestNodes) {
            // TODO connect to Node
//            ret.add(thisPeer.getPeer(new PeerAddress(node)));
            final BzzProtocol peer = connectedPeers.get(node);
            if (peer != null) {
                ret.add(peer);
                if (--maxCount == 0) break;
            } else {
                LOG.info("Hive connects to node " + node);
                NetStore.getInstance().worldManager.getActivePeer().connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
            }
        }
        return ret;
    }

    public void newNodeRecord(final PeerAddress addr) {
    }

    /**
     * Adds the nodes received in the {@link BzzPeersMessage}
     */
    public void addPeerRecords(final BzzPeersMessage req) {
        for (final PeerAddress peerAddress : req.getPeers()) {
            nodeTable.addNode(peerAddress.toNode());
        }
        LOG.debug("Hive added new nodes: " + req.getPeers());
        peersAdded();
    }

    private void peersAdded() {
        for (final HiveTask task : new ArrayList<>(hiveTasks.keySet())) {
            if (!task.peersAdded()) {
                hiveTasks.remove(task);
                LOG.debug("HiveTask removed from queue: " + task);
            }
        }
    }

    /**
     * For testing
     */
    public Map<Node, BzzProtocol> getAllEntries() {
        final Map<Node, BzzProtocol> ret = new LinkedHashMap<>();
        for (final NodeEntry entry : nodeTable.getAllNodes()) {
            final Node node = entry.getNode();
            final BzzProtocol bzz = connectedPeers.get(node);
            ret.put(node, bzz);
        }
        return ret;
    }

    /**
     *  Adds a task with a search Key parameter. The task has a limited life time
     *  ({@link org.ethereum.net.swarm.Hive.HiveTask#expireTime} and a limited number of
     *  peers to process ({@link org.ethereum.net.swarm.Hive.HiveTask#maxPeers}).
     *  Until the task is alive and new Peer(s) is discovered by the Hive this task
     *  is invoked with another one closest Peer.
     *  This task may complete synchronously (i.e. before the method return) if the
     *  number of Peers in the Hive &gt;= maxPeers for that task.
     */
    public void addTask(final HiveTask t) {
        if (t.peersAdded()) {
            LOG.debug("Added a HiveTask to queue: " + t);
            hiveTasks.put(t, null);
        }
    }

    /**
     * The task to be executed when another one closest Peer is discovered
     * until the timeout or maxPeers is reached.
     */
    public abstract class HiveTask {
        final Key targetKey;
        final Map<BzzProtocol, Object> processedPeers = new IdentityHashMap<>();
        final long expireTime;
        final int maxPeers;

        public HiveTask(final Key targetKey, final long timeout, final int maxPeers) {
            this.targetKey = targetKey;
            this.expireTime = Util.curTime() + timeout;
            this.maxPeers = maxPeers;
        }

        /**
         * Notifies the task that new peers were connected.
         * @return true if the task wants to wait further for another peers
         * false if the task is completed
         */
        public boolean peersAdded() {
            if (Util.curTime() > expireTime) return false;
            final Collection<BzzProtocol> peers = getPeers(targetKey, maxPeers);
            for (final BzzProtocol peer : peers) {
                if (!processedPeers.containsKey(peer)) {
                    processPeer(peer);
                    processedPeers.put(peer, null);
                    if (processedPeers.size() > maxPeers) {
                        return false;
                    }
                }
            }
            return true;
        }

        protected abstract void processPeer(BzzProtocol peer);
    }
}
