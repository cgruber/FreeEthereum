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

package org.ethereum.net.eth.handler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.db.StateSource;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.*;
import org.ethereum.sync.PeerState;
import org.ethereum.util.ByteArraySet;
import org.ethereum.util.Value;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.ethereum.net.eth.EthVersion.V63;

/**
 * Fast synchronization (PV63) Handler
 */
@Component("Eth63")
@Scope("prototype")
public class Eth63 extends Eth62 {

    private static final EthVersion version = V63;

    private StateSource stateSource;

    private List<byte[]> requestedReceipts;
    private SettableFuture<List<List<TransactionReceipt>>> requestReceiptsFuture;
    private Set<byte[]> requestedNodes;
    private SettableFuture<List<Pair<byte[], byte[]>>> requestNodesFuture;

    public Eth63() {
        super(version);
    }

    @Autowired
    public Eth63(final SystemProperties config, final Blockchain blockchain, final BlockStore blockStore,
                 final CompositeEthereumListener ethereumListener, StateSource stateSource) {
        super(version, config, blockchain, blockStore, ethereumListener);
        this.stateSource = stateSource;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final EthMessage msg) throws InterruptedException {

        super.channelRead0(ctx, msg);

        // Only commands that were added in V63, V62 are handled in child
        switch (msg.getCommand()) {
            case GET_NODE_DATA:
                processGetNodeData((GetNodeDataMessage) msg);
                break;
            case NODE_DATA:
                processNodeData((NodeDataMessage) msg);
                break;
            case GET_RECEIPTS:
                processGetReceipts((GetReceiptsMessage) msg);
                break;
            case RECEIPTS:
                processReceipts((ReceiptsMessage) msg);
                break;
            default:
                break;
        }
    }

    private synchronized void processGetNodeData(final GetNodeDataMessage msg) {

        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing GetNodeData, size [{}]",
                channel.getPeerIdShort(),
                msg.getNodeKeys().size()
        );

        final List<Value> nodeValues = new ArrayList<>();
        for (final byte[] nodeKey : msg.getNodeKeys()) {
            final byte[] rawNode = stateSource.get(nodeKey);
            if (rawNode != null) {
                final Value value = new Value(rawNode);
                nodeValues.add(value);
                logger.trace("Eth63: " + Hex.toHexString(nodeKey).substring(0, 8) + " -> " + value);
            }
        }

        sendMessage(new NodeDataMessage(nodeValues));
    }

    private synchronized void processGetReceipts(final GetReceiptsMessage msg) {

        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing GetReceipts, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockHashes().size()
        );

        final List<List<TransactionReceipt>> receipts = new ArrayList<>();
        for (final byte[] blockHash : msg.getBlockHashes()) {
            final Block block = blockchain.getBlockByHash(blockHash);
            if (block == null) continue;

            final List<TransactionReceipt> blockReceipts = new ArrayList<>();
            for (final Transaction transaction : block.getTransactionsList()) {
                final TransactionInfo transactionInfo = blockchain.getTransactionInfo(transaction.getHash());
                if (transactionInfo == null) break;
                blockReceipts.add(transactionInfo.getReceipt());
            }
            receipts.add(blockReceipts);
        }

        sendMessage(new ReceiptsMessage(receipts));
    }

    public synchronized ListenableFuture<List<Pair<byte[], byte[]>>> requestTrieNodes(final List<byte[]> hashes) {
        if (peerState != PeerState.IDLE) return null;

        final GetNodeDataMessage msg = new GetNodeDataMessage(hashes);
        requestedNodes = new ByteArraySet();
        requestedNodes.addAll(hashes);

        requestNodesFuture = SettableFuture.create();
        sendMessage(msg);
        lastReqSentTime = System.currentTimeMillis();

        peerState = PeerState.NODE_RETRIEVING;
        return requestNodesFuture;
    }

    public synchronized ListenableFuture<List<List<TransactionReceipt>>> requestReceipts(final List<byte[]> hashes) {
        if (peerState != PeerState.IDLE) return null;

        final GetReceiptsMessage msg = new GetReceiptsMessage(hashes);
        requestedReceipts = hashes;
        peerState = PeerState.RECEIPT_RETRIEVING;

        requestReceiptsFuture = SettableFuture.create();
        sendMessage(msg);
        lastReqSentTime = System.currentTimeMillis();

        return requestReceiptsFuture;
    }

    private synchronized void processNodeData(final NodeDataMessage msg) {
        if (requestedNodes == null) {
            logger.debug("Received NodeDataMessage when requestedNodes == null. Dropping peer " + channel);
            dropConnection();
        }

        final List<Pair<byte[], byte[]>> ret = new ArrayList<>();
        if(msg.getDataList().isEmpty()) {
            final String err = "Received NodeDataMessage contains empty node data. Dropping peer " + channel;
            dropUselessPeer(err);
            return;
        }

        for (final Value nodeVal : msg.getDataList()) {
            final byte[] hash = nodeVal.hash();
            if (!requestedNodes.contains(hash)) {
                final String err = "Received NodeDataMessage contains non-requested node with hash :" + Hex.toHexString(hash) + " . Dropping peer " + channel;
                dropUselessPeer(err);
                return;
            }
            ret.add(Pair.of(hash, nodeVal.encode()));
        }
        requestNodesFuture.set(ret);

        requestedNodes = null;
        requestNodesFuture = null;
        processingTime += (System.currentTimeMillis() - lastReqSentTime);
        lastReqSentTime = 0;
        peerState = PeerState.IDLE;
    }

    private synchronized void processReceipts(final ReceiptsMessage msg) {
        if (requestedReceipts == null) {
            logger.debug("Received ReceiptsMessage when requestedReceipts == null. Dropping peer " + channel);
            dropConnection();
        }


        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing Receipts, size [{}]",
                channel.getPeerIdShort(),
                msg.getReceipts().size()
        );

        final List<List<TransactionReceipt>> receipts = msg.getReceipts();

        requestReceiptsFuture.set(receipts);

        requestedReceipts = null;
        requestReceiptsFuture = null;
        processingTime += (System.currentTimeMillis() - lastReqSentTime);
        lastReqSentTime = 0;
        peerState = PeerState.IDLE;
    }


    private void dropUselessPeer(final String err) {
        logger.debug(err);
        requestNodesFuture.setException(new RuntimeException(err));
        dropConnection();
    }

    @Override
    public String getSyncStats() {
        final double nodesPerSec = 1000d * channel.getNodeStatistics().eth63NodesReceived.get() / channel.getNodeStatistics().eth63NodesRetrieveTime.get();
        final double missNodesRatio = 1 - (double) channel.getNodeStatistics().eth63NodesReceived.get() / channel.getNodeStatistics().eth63NodesRequested.get();
        final long lifeTime = System.currentTimeMillis() - connectedTime;
        return super.getSyncStats() + String.format("\tNodes/sec: %1$.2f, miss: %2$.2f", nodesPerSec, missNodesRatio);
    }
}
