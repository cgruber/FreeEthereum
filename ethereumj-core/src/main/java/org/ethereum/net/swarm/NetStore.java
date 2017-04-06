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

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.ethereum.config.SystemProperties;
import org.ethereum.manager.WorldManager;
import org.ethereum.net.swarm.bzz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The main logic of communicating with BZZ peers.
 * The class process local/remote retrieve/store requests and forwards them if necessary
 * to the peers.
 *
 * Magic is happening here!
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
@Component
public class NetStore implements ChunkStore {
    private static NetStore INST;
    // Statistics gathers
    public final Statter statInMsg = Statter.create("net.swarm.bzz.inMessages");
    public final Statter statOutMsg = Statter.create("net.swarm.bzz.outMessages");
    public final Statter statHandshakes = Statter.create("net.swarm.bzz.handshakes");
    private final Statter statInStoreReq = Statter.create("net.swarm.in.storeReq");
    private final Statter statInGetReq = Statter.create("net.swarm.in.getReq");
    private final Statter statOutStoreReq = Statter.create("net.swarm.out.storeReq");
    private final Statter statOutGetReq = Statter.create("net.swarm.out.getReq");
    private final int requesterCount = 3;
    private final int maxSearchPeers = 6;
    private final int timeout = 600 * 1000;
    private final LocalStore localStore;
    private final Hive hive;
    private final Map<Chunk, PeerAddress> chunkSourceAddr = new IdentityHashMap<>();
    private final Map<Key, ChunkRequest> chunkRequestMap = new HashMap<>();
    public int maxStorePeers = 3;
    @Autowired
    WorldManager worldManager;
    private PeerAddress selfAddress;

    @Autowired
    public NetStore(final SystemProperties config) {
        this(new LocalStore(new MemStore(), new MemStore()), new Hive(new PeerAddress(new byte[] {127,0,0,1},
                config.listenPort(), config.nodeId())));
        start(hive.getSelfAddress());
        // FIXME bad dirty hack to workaround Spring DI machinery
        INST = this;
    }

    public NetStore(final LocalStore localStore, final Hive hive) {
        this.localStore = localStore;
        this.hive = hive;
    }

    public synchronized static NetStore getInstance() {
        return INST;
    }

    public void start(final PeerAddress self) {
        this.selfAddress = self;
        hive.start();
    }

    public void stop() {
        hive.stop();
    }

    public Hive getHive() {
        return hive;
    }

    public PeerAddress getSelfAddress() {
        return selfAddress;
    }

    /******************************************
     *    Put methods
     ******************************************/

    // called from dpa, entrypoint for *local* chunk store requests
    @Override
    public synchronized void put(final Chunk chunk) {
//        Chunk localChunk = localStore.get(chunk.getKey()); // ???
        putImpl(chunk);
    }

    // store logic common to local and network chunk store requests
    private void putImpl(final Chunk chunk) {
        localStore.put(chunk);
        if (chunkRequestMap.get(chunk.getKey()) != null &&
                chunkRequestMap.get(chunk.getKey()).status == EntryReqStatus.Searching) {
            // If this is response to retrieve message
            chunkRequestMap.get(chunk.getKey()).status = EntryReqStatus.Found;

            // Resend to all (max [3]) requesters
            propagateResponse(chunk);
            // TODO remove chunkRequest from map (memleak) ???
        } else {
            // If local, broadcast store request to hive peers
            store(chunk);
        }
    }

    // the entrypoint for network store requests
    public void addStoreRequest(final BzzStoreReqMessage msg) {
        statInStoreReq.add(1);

        Chunk chunk = localStore.get(msg.getKey()); // TODO hasKey() should be faster
        if (chunk == null) {
            // If not in local store yet
            // but remember the request source to exclude it from store broadcast
            chunk = new Chunk(msg.getKey(), msg.getData());
            chunkSourceAddr.put(chunk, msg.getPeer().getNode());
            putImpl(chunk);
        }
    }

    // once a chunk is found propagate it its requesters unless timed out
    private synchronized void propagateResponse(final Chunk chunk) {
        final ChunkRequest chunkRequest = chunkRequestMap.get(chunk.getKey());
        for (final Promise<Chunk> localRequester : chunkRequest.localRequesters) {
            localRequester.setSuccess(chunk);
        }

        for (final Map.Entry<Long, Collection<BzzRetrieveReqMessage>> e :
                chunkRequest.requesters.entrySet()) {
            final BzzStoreReqMessage msg = new BzzStoreReqMessage(e.getKey(), chunk.getKey(), chunk.getData());

            int counter = requesterCount;
            for (final BzzRetrieveReqMessage r : e.getValue()) {
                r.getPeer().sendMessage(msg);
                statOutStoreReq.add(1);
                if (--counter < 0) {
                    break;
                }
            }
        }
    }

    // store propagates store requests to specific peers given by the kademlia hive
    // except for peers that the store request came from (if any)
    private void store(final Chunk chunk) {
        final PeerAddress chunkStoreRequestSource = chunkSourceAddr.get(chunk);

        hive.addTask(hive.new HiveTask(chunk.getKey(), timeout, maxStorePeers) {
            @Override
            protected void processPeer(final BzzProtocol peer) {
                if (chunkStoreRequestSource == null || !chunkStoreRequestSource.equals(peer.getNode())) {
                    final BzzStoreReqMessage msg = new BzzStoreReqMessage(chunk.getKey(), chunk.getData());
                    peer.sendMessage(msg);
                }
            }
        });
    }

    /******************************************
     *    Get methods
     ******************************************/

    @Override
    // Get is the entrypoint for local retrieve requests
    // waits for response or times out
    public Chunk get(final Key key) {
        try {
            return getAsync(key).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized Future<Chunk> getAsync(final Key key) {
        final Chunk chunk = localStore.get(key);
        final Promise<Chunk> ret = new DefaultPromise<Chunk>() {
        };
        if (chunk == null) {
//            long timeout = 0; // TODO
            final ChunkRequest chunkRequest = new ChunkRequest();
            chunkRequest.localRequesters.add(ret);
            chunkRequestMap.put(key, chunkRequest);
            startSearch(-1, key, timeout);
        } else {
            ret.setSuccess(chunk);
        }
        return ret;
    }

    // entrypoint for network retrieve requests
    public void addRetrieveRequest(final BzzRetrieveReqMessage req) {
        statInGetReq.add(1);

        final Chunk chunk = localStore.get(req.getKey());

        final ChunkRequest chunkRequest = chunkRequestMap.get(req.getKey());

        if (chunk == null) {
            peers(req, chunk, 0);
            if (chunkRequest == null && !req.getKey().isZero()) {
                chunkRequestMap.put(req.getKey(), new ChunkRequest());
                final long timeout = strategyUpdateRequest(chunkRequestMap.get(req.getKey()), req);
                startSearch(req.getId(), req.getKey(), timeout);
            }
//            req.timeout = +10sec // TODO ???
        } else {

            final long timeout = strategyUpdateRequest(chunkRequestMap.get(req.getKey()), req);
            if (chunkRequest != null) {
                chunkRequest.status = EntryReqStatus.Found;
            }
            deliver(req, chunk);
        }

    }

    // logic propagating retrieve requests to peers given by the kademlia hive
    // it's assumed that caller holds the lock
    private Chunk startSearch(final long id, final Key key, final long timeout) {
        final ChunkRequest chunkRequest = chunkRequestMap.get(key);
        chunkRequest.status = EntryReqStatus.Searching;

        final BzzRetrieveReqMessage req = new BzzRetrieveReqMessage(key/*, timeout*/);
        hive.addTask(hive.new HiveTask(key, timeout, maxSearchPeers) {
            @Override
            protected void processPeer(final BzzProtocol peer) {
                boolean requester = false;
                out:
                for (final Collection<BzzRetrieveReqMessage> chReqColl : chunkRequest.requesters.values()) {
                    for (final BzzRetrieveReqMessage chReq : chReqColl) {
                        if (chReq.getPeer().getNode().equals(peer.getNode())) {
                            requester = true;
                            break out;
                        }
                    }
                }
                if (!requester) {
                    statOutGetReq.add(1);
                    peer.sendMessage(req);
                }
            }
        });

        return null;
    }

    // add peer request the chunk and decides the timeout for the response if still searching
    private long strategyUpdateRequest(final ChunkRequest chunkRequest, final BzzRetrieveReqMessage req) {
        if (chunkRequest != null && chunkRequest.status == EntryReqStatus.Searching) {
            addRequester(chunkRequest, req);
            return searchingTimeout(chunkRequest, req);
        } else {
            return -1;
        }

    }

    /*
    adds a new peer to an existing open request
    only add if less than requesterCount peers forwarded the same request id so far
    note this is done irrespective of status (searching or found)
    */
    private void addRequester(final ChunkRequest rs, final BzzRetrieveReqMessage req) {
        final Collection<BzzRetrieveReqMessage> list = rs.requesters.computeIfAbsent(req.getId(), k -> new ArrayList<>());
        list.add(req);
    }

    // called on each request when a chunk is found,
    // delivery is done by sending a request to the requesting peer
    private void deliver(final BzzRetrieveReqMessage req, final Chunk chunk) {
        final BzzStoreReqMessage msg = new BzzStoreReqMessage(req.getId(), req.getKey(), chunk.getData());
        req.getPeer().sendMessage(msg);
        statOutStoreReq.add(1);
    }

    // the immediate response to a retrieve request,
    // sends relevant peer data given by the kademlia hive to the requester
    private void peers(final BzzRetrieveReqMessage req, final Chunk chunk, final long timeout) {
//        Collection<BzzProtocol> peers = hive.getPeers(req.getKey(), maxSearchPeers/*(int) req.getMaxPeers()*/);
//        List<PeerAddress> peerAddrs = new ArrayList<>();
//        for (BzzProtocol peer : peers) {
//            peerAddrs.add(peer.getNode());
//        }
        Key key = req.getKey();
        if (key.isZero()) {
            key = new Key(req.getPeer().getNode().getId()); // .getAddrKey();
        }
        final Collection<PeerAddress> nodes = hive.getNodes(key, maxSearchPeers);
        final BzzPeersMessage msg = new BzzPeersMessage(new ArrayList<>(nodes), timeout, req.getKey(), req.getId());
        req.getPeer().sendMessage(msg);
    }

    private long searchingTimeout(final ChunkRequest chunkRequest, final BzzRetrieveReqMessage req) {
        // TODO
        return 0;
    }
    private enum EntryReqStatus {
        Searching,
        Found
    }

    private class ChunkRequest {
        final Map<Long, Collection<BzzRetrieveReqMessage>> requesters = new HashMap<>();
        final List<Promise<Chunk>> localRequesters = new ArrayList<>();
        EntryReqStatus status = EntryReqStatus.Searching;
    }
}

