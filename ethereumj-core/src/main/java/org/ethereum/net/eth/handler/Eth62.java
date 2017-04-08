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
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.*;
import org.ethereum.net.message.ReasonCode;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.submit.TransactionExecutor;
import org.ethereum.net.submit.TransactionTask;
import org.ethereum.sync.PeerState;
import org.ethereum.sync.SyncManager;
import org.ethereum.sync.SyncStatistics;
import org.ethereum.util.ByteUtil;
import org.ethereum.validator.BlockHeaderRule;
import org.ethereum.validator.BlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static org.ethereum.net.eth.EthVersion.V62;
import static org.ethereum.net.message.ReasonCode.USELESS_PEER;
import static org.ethereum.sync.PeerState.*;
import static org.ethereum.util.Utils.longToTimePeriod;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * Eth 62
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
@Component("Eth62")
@Scope("prototype")
public class Eth62 extends EthHandler {

    final static Logger logger = LoggerFactory.getLogger("sync");
    private static final int MAX_HASHES_TO_SEND = 65536;
    private final static Logger loggerNet = LoggerFactory.getLogger("net");
    private static final EthVersion version = V62;
    final long connectedTime = System.currentTimeMillis();
    /**
     * Header list sent in GET_BLOCK_BODIES message,
     * used to create blocks from headers and bodies
     * also, is useful when returned BLOCK_BODIES msg doesn't cover all sent hashes
     * or in case when peer is disconnected
     */
    private final List<BlockHeaderWrapper> sentHeaders = Collections.synchronizedList(new ArrayList<BlockHeaderWrapper>());
    private final SyncStatistics syncStats = new SyncStatistics();
    @Autowired
    protected NodeManager nodeManager;
    @Autowired
    BlockStore blockstore;
    @Autowired
    SyncManager syncManager;
    PeerState peerState = IDLE;
    boolean syncDone = false;
    long lastReqSentTime;
    long processingTime = 0;
    @Autowired
    private PendingState pendingState;
    private EthState ethState = EthState.INIT;
    /**
     * Number and hash of best known remote block
     */
    private BlockIdentifier bestKnownBlock;
    private SettableFuture<List<Block>> futureBlocks;
    private GetBlockHeadersMessageWrapper headerRequest;
    private BigInteger totalDifficulty;
    private Map<Long, BlockHeaderValidator> validatorMap;

    public Eth62() {
        this(version);
    }

    Eth62(final EthVersion version) {
        super(version);
    }

    @Autowired
    public Eth62(final SystemProperties config, final Blockchain blockchain,
                 final BlockStore blockStore, final CompositeEthereumListener ethereumListener) {
        this(version, config, blockchain, blockStore, ethereumListener);
    }

    Eth62(final EthVersion version, final SystemProperties config,
          final Blockchain blockchain, final BlockStore blockStore,
          final CompositeEthereumListener ethereumListener) {
        super(version, config, blockchain, blockStore, ethereumListener);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final EthMessage msg) throws InterruptedException {

        super.channelRead0(ctx, msg);

        switch (msg.getCommand()) {
            case STATUS:
                processStatus((StatusMessage) msg, ctx);
                break;
            case NEW_BLOCK_HASHES:
                processNewBlockHashes((NewBlockHashesMessage) msg);
                break;
            case TRANSACTIONS:
                processTransactions((TransactionsMessage) msg);
                break;
            case GET_BLOCK_HEADERS:
                processGetBlockHeaders((GetBlockHeadersMessage) msg);
                break;
            case BLOCK_HEADERS:
                processBlockHeaders((BlockHeadersMessage) msg);
                break;
            case GET_BLOCK_BODIES:
                processGetBlockBodies((GetBlockBodiesMessage) msg);
                break;
            case BLOCK_BODIES:
                processBlockBodies((BlockBodiesMessage) msg);
                break;
            case NEW_BLOCK:
                processNewBlock((NewBlockMessage) msg);
                break;
            default:
                break;
        }
    }

    /*************************
     *    Message Sending    *
     *************************/

    @Override
    public synchronized void sendStatus() {
        final byte protocolVersion = getVersion().getCode();
        final int networkId = config.networkId();

        final BigInteger totalDifficulty;
        final byte[] bestHash;

        if (syncManager.isFastSyncRunning()) {
            // while fastsync is not complete reporting block #0
            // until all blocks/receipts are downloaded
            bestHash = blockstore.getBlockHashByNumber(0);
            final Block genesis = blockstore.getBlockByHash(bestHash);
            totalDifficulty = genesis.getDifficultyBI();
        } else {
            // Getting it from blockstore, not blocked by blockchain sync
            bestHash = blockstore.getBestBlock().getHash();
            totalDifficulty = blockchain.getTotalDifficulty();
        }

        final StatusMessage msg = new StatusMessage(protocolVersion, networkId,
                ByteUtil.bigIntegerToBytes(totalDifficulty), bestHash, config.getGenesis().getHash());
        sendMessage(msg);

        ethState = EthState.STATUS_SENT;

        sendNextHeaderRequest();
    }

    @Override
    public synchronized void sendNewBlockHashes(final Block block) {

        final BlockIdentifier identifier = new BlockIdentifier(block.getHash(), block.getNumber());
        final NewBlockHashesMessage msg = new NewBlockHashesMessage(singletonList(identifier));
        sendMessage(msg);
    }

    @Override
    public synchronized void sendTransaction(final List<Transaction> txs) {
        final TransactionsMessage msg = new TransactionsMessage(txs);
        sendMessage(msg);
    }

    @Override
    public synchronized ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(final long blockNumber, final int maxBlocksAsk, final boolean reverse) {

        if (ethState == EthState.STATUS_SUCCEEDED && peerState != IDLE) return null;

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: queue GetBlockHeaders, blockNumber [{}], maxBlocksAsk [{}]",
                channel.getPeerIdShort(),
                blockNumber,
                maxBlocksAsk
        );

        if (headerRequest != null) {
            throw new RuntimeException("The peer is waiting for headers response: " + this);
        }

        final GetBlockHeadersMessage headersRequest = new GetBlockHeadersMessage(blockNumber, null, maxBlocksAsk, 0, reverse);
        final GetBlockHeadersMessageWrapper messageWrapper = new GetBlockHeadersMessageWrapper(headersRequest);
        headerRequest = messageWrapper;

        sendNextHeaderRequest();

        return messageWrapper.getFutureHeaders();
    }

    @Override
    public synchronized ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(final byte[] blockHash, final int maxBlocksAsk, final int skip, final boolean reverse) {
        return sendGetBlockHeaders(blockHash, maxBlocksAsk, skip, reverse, false);
    }

    synchronized void sendGetNewBlockHeaders(final byte[] blockHash, final int maxBlocksAsk, final int skip, final boolean reverse) {
        sendGetBlockHeaders(blockHash, maxBlocksAsk, skip, reverse, true);
    }

    private synchronized ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(final byte[] blockHash, final int maxBlocksAsk, final int skip, final boolean reverse, final boolean newHashes) {

        if (peerState != IDLE) return null;

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: queue GetBlockHeaders, blockHash [{}], maxBlocksAsk [{}], skip[{}], reverse [{}]",
                channel.getPeerIdShort(),
                "0x" + toHexString(blockHash).substring(0, 8),
                maxBlocksAsk, skip, reverse
        );

        if (headerRequest != null) {
            throw new RuntimeException("The peer is waiting for headers response: " + this);
        }

        final GetBlockHeadersMessage headersRequest = new GetBlockHeadersMessage(0, blockHash, maxBlocksAsk, skip, reverse);
        final GetBlockHeadersMessageWrapper messageWrapper = new GetBlockHeadersMessageWrapper(headersRequest, newHashes);
        headerRequest = messageWrapper;

        sendNextHeaderRequest();
        lastReqSentTime = System.currentTimeMillis();

        return messageWrapper.getFutureHeaders();
    }

    @Override
    public synchronized ListenableFuture<List<Block>> sendGetBlockBodies(final List<BlockHeaderWrapper> headers) {
        if (peerState != IDLE) return null;

        peerState = BLOCK_RETRIEVING;
        sentHeaders.clear();
        sentHeaders.addAll(headers);

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send GetBlockBodies, hashes.count [{}]",
                channel.getPeerIdShort(),
                sentHeaders.size()
        );

        final List<byte[]> hashes = new ArrayList<>(headers.size());
        for (final BlockHeaderWrapper header : headers) {
            hashes.add(header.getHash());
        }

        final GetBlockBodiesMessage msg = new GetBlockBodiesMessage(hashes);

        sendMessage(msg);
        lastReqSentTime = System.currentTimeMillis();

        futureBlocks = SettableFuture.create();
        return futureBlocks;
    }

    @Override
    public synchronized void sendNewBlock(final Block block) {
        final BigInteger parentTD = blockstore.getTotalDifficultyForHash(block.getParentHash());
        final byte[] td = ByteUtil.bigIntegerToBytes(parentTD.add(new BigInteger(1, block.getDifficulty())));
        final NewBlockMessage msg = new NewBlockMessage(block, td);
        sendMessage(msg);
    }

    /*************************
     *  Message Processing   *
     *************************/

    private synchronized void processStatus(final StatusMessage msg, final ChannelHandlerContext ctx) throws InterruptedException {

        try {

            if (!Arrays.equals(msg.getGenesisHash(), config.getGenesis().getHash())) {
                if (!peerDiscoveryMode) {
                    loggerNet.debug("Removing EthHandler for {} due to protocol incompatibility", ctx.channel().remoteAddress());
                }
                ethState = EthState.STATUS_FAILED;
                disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
                ctx.pipeline().remove(this); // Peer is not compatible for the 'eth' sub-protocol
                return;
            }

            if (msg.getNetworkId() != config.networkId()) {
                ethState = EthState.STATUS_FAILED;
                disconnect(ReasonCode.NULL_IDENTITY);
                return;
            }

            // basic checks passed, update statistics
            channel.getNodeStatistics().ethHandshake(msg);
            ethereumListener.onEthStatusUpdated(channel, msg);

            if (peerDiscoveryMode) {
                loggerNet.trace("Peer discovery mode: STATUS received, disconnecting...");
                disconnect(ReasonCode.REQUESTED);
                ctx.close().sync();
                ctx.disconnect().sync();
                return;
            }

            // update bestKnownBlock info
            sendGetBlockHeaders(msg.getBestHash(), 1, 0, false);

        } catch (final NoSuchElementException e) {
            loggerNet.debug("EthHandler already removed");
        }
    }

    synchronized void processNewBlockHashes(final NewBlockHashesMessage msg) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing NewBlockHashes, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockIdentifiers().size()
        );

        final List<BlockIdentifier> identifiers = msg.getBlockIdentifiers();

        if (identifiers.isEmpty()) return;

        updateBestBlock(identifiers);

        // queueing new blocks doesn't make sense
        // while Long sync is in progress
        if (!syncDone) return;

        if (peerState != HEADER_RETRIEVING) {
            long firstBlockAsk = Long.MAX_VALUE;
            long lastBlockAsk = 0;
            byte[] firstBlockHash = null;
            for (final BlockIdentifier identifier : identifiers) {
                final long blockNumber = identifier.getNumber();
                if (blockNumber < firstBlockAsk) {
                    firstBlockAsk = blockNumber;
                    firstBlockHash = identifier.getHash();
                }
                if (blockNumber > lastBlockAsk)  {
                    lastBlockAsk = blockNumber;
                }
            }
            final long maxBlocksAsk = lastBlockAsk - firstBlockAsk + 1;
            if (firstBlockHash != null && maxBlocksAsk > 0 && maxBlocksAsk < MAX_HASHES_TO_SEND) {
                sendGetNewBlockHeaders(firstBlockHash, (int) maxBlocksAsk, 0, false);
            }
        }
    }

    private synchronized void processTransactions(final TransactionsMessage msg) {
        if(!processTransactions) {
            return;
        }

        final List<Transaction> txSet = msg.getTransactions();
        final List<Transaction> newPending = pendingState.addPendingTransactions(txSet);
        if (!newPending.isEmpty()) {
            final TransactionTask transactionTask = new TransactionTask(newPending, channel.getChannelManager(), channel);
            TransactionExecutor.Companion.getInstance().submitTransaction(transactionTask);
        }
    }

    protected synchronized void processGetBlockHeaders(final GetBlockHeadersMessage msg) {
        final List<BlockHeader> headers = blockchain.getListOfHeadersStartFrom(
                msg.getBlockIdentifier(),
                msg.getSkipBlocks(),
                min(msg.getMaxHeaders(), MAX_HASHES_TO_SEND),
                msg.isReverse()
        );

        final BlockHeadersMessage response = new BlockHeadersMessage(headers);
        sendMessage(response);
    }

    private synchronized void processBlockHeaders(final BlockHeadersMessage msg) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing BlockHeaders, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockHeaders().size()
        );

        final GetBlockHeadersMessageWrapper request = headerRequest;
        headerRequest = null;

        if (!isValid(msg, request)) {

            dropConnection();
            return;
        }

        final List<BlockHeader> received = msg.getBlockHeaders();

        if (ethState == EthState.STATUS_SENT || ethState == EthState.HASH_CONSTRAINTS_CHECK)
            processInitHeaders(received);
        else {
            syncStats.addHeaders(received.size());
            request.getFutureHeaders().set(received);
        }

        processingTime += lastReqSentTime > 0 ? (System.currentTimeMillis() - lastReqSentTime) : 0;
        lastReqSentTime = 0;
        peerState = IDLE;
    }

    protected synchronized void processGetBlockBodies(final GetBlockBodiesMessage msg) {
        final List<byte[]> bodies = blockchain.getListOfBodiesByHashes(msg.getBlockHashes());

        final BlockBodiesMessage response = new BlockBodiesMessage(bodies);
        sendMessage(response);
    }

    private synchronized void processBlockBodies(final BlockBodiesMessage msg) {

        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: process BlockBodies, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockBodies().size()
        );

        if (!isValid(msg)) {

            dropConnection();
            return;
        }

        syncStats.addBlocks(msg.getBlockBodies().size());

        List<Block> blocks = null;
        try {
            blocks = validateAndMerge(msg);
        } catch (final Exception e) {
            logger.info("Fatal validation error while processing block bodies from peer {}", channel.getPeerIdShort());
        }

        if (blocks == null) {
            // headers will be returned by #onShutdown()
            dropConnection();
            return;
        }

        futureBlocks.set(blocks);
        futureBlocks = null;

        processingTime += (System.currentTimeMillis() - lastReqSentTime);
        lastReqSentTime = 0;
        peerState = IDLE;
    }

    private synchronized void processNewBlock(final NewBlockMessage newBlockMessage) {

        final Block newBlock = newBlockMessage.getBlock();

        logger.debug("New block received: block.index [{}]", newBlock.getNumber());

        updateTotalDifficulty(newBlockMessage.getDifficultyAsBigInt());

        updateBestBlock(newBlock);

        if (!syncManager.validateAndAddNewBlock(newBlock, channel.getNodeId())) {
            dropConnection();
        }
    }

    /*************************
     *    Sync Management    *
     *************************/

    @Override
    public synchronized void onShutdown() {
    }

    @Override
    public synchronized void fetchBodies(final List<BlockHeaderWrapper> headers) {
        syncStats.reset();
        sendGetBlockBodies(headers);
    }

    private synchronized void sendNextHeaderRequest() {

        // do not send header requests if status hasn't been passed yet
        if (ethState == EthState.INIT) return;

        final GetBlockHeadersMessageWrapper wrapper = headerRequest;

        if (wrapper == null || wrapper.isSent()) return;

        peerState = HEADER_RETRIEVING;

        wrapper.send();
        sendMessage(wrapper.getMessage());
        lastReqSentTime = System.currentTimeMillis();
    }

    private synchronized void processInitHeaders(final List<BlockHeader> received) {

        final BlockHeader blockHeader = received.get(0);
        final long blockNumber = blockHeader.getNumber();

        if (ethState == EthState.STATUS_SENT) {
            updateBestBlock(blockHeader);

            logger.trace("Peer {}: init request succeeded, best known block {}",
                    channel.getPeerIdShort(), bestKnownBlock);

            // checking if the peer has expected block hashes
            ethState = EthState.HASH_CONSTRAINTS_CHECK;

            validatorMap = Collections.synchronizedMap(new HashMap<Long, BlockHeaderValidator>());
            final List<Pair<Long, BlockHeaderValidator>> validators = config.getBlockchainConfig().
                    getConfigForBlock(blockNumber).headerValidators();
            for (final Pair<Long, BlockHeaderValidator> validator : validators) {
                if (validator.getLeft() <= getBestKnownBlock().getNumber()) {
                    validatorMap.put(validator.getLeft(), validator.getRight());
                }
            }

            logger.trace("Peer " + channel.getPeerIdShort() + ": Requested " + validatorMap.size() +
                    " headers for hash check: " + validatorMap.keySet());
            requestNextHashCheck();

        } else {
            final BlockHeaderValidator validator = validatorMap.get(blockNumber);
            if (validator != null) {
                final BlockHeaderRule.ValidationResult result = validator.validate(blockHeader);
                if (result.success) {
                    validatorMap.remove(blockNumber);
                    requestNextHashCheck();
                } else {
                    logger.debug("Peer {}: wrong fork ({}). Drop the peer and reduce reputation.", channel.getPeerIdShort(), result.error);
                    channel.getNodeStatistics().wrongFork = true;
                    dropConnection();
                }
            }
        }

        if (validatorMap.isEmpty()) {
            ethState = EthState.STATUS_SUCCEEDED;

            logger.trace("Peer {}: all validations passed", channel.getPeerIdShort());
        }
    }

    private void requestNextHashCheck() {
       if (!validatorMap.isEmpty()) {
            final Long checkHeader = validatorMap.keySet().iterator().next();
            sendGetBlockHeaders(checkHeader, 1, false);
            logger.trace("Peer {}: Requested #{} header for hash check.", channel.getPeerIdShort(), checkHeader);
        }
    }


    private void updateBestBlock(final Block block) {
        updateBestBlock(block.getHeader());
    }

    private void updateBestBlock(final BlockHeader header) {
        if (bestKnownBlock == null || header.getNumber() > bestKnownBlock.getNumber()) {
            bestKnownBlock = new BlockIdentifier(header.getHash(), header.getNumber());
        }
    }

    private void updateBestBlock(final List<BlockIdentifier> identifiers) {

        for (final BlockIdentifier id : identifiers)
            if (bestKnownBlock == null || id.getNumber() > bestKnownBlock.getNumber()) {
                bestKnownBlock = id;
            }
    }

    @Override
    public BlockIdentifier getBestKnownBlock() {
        return bestKnownBlock;
    }

    private void updateTotalDifficulty(final BigInteger totalDiff) {
        channel.getNodeStatistics().setEthTotalDifficulty(totalDiff);
        this.totalDifficulty = totalDiff;
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty != null ? totalDifficulty : channel.getNodeStatistics().getEthTotalDifficulty();
    }

    /*************************
     *   Getters, setters    *
     *************************/

    @Override
    public boolean isHashRetrievingDone() {
        return peerState == DONE_HASH_RETRIEVING;
    }

    @Override
    public boolean isHashRetrieving() {
        return peerState == HEADER_RETRIEVING;
    }

    @Override
    public boolean hasStatusPassed() {
        return ethState.ordinal() > EthState.HASH_CONSTRAINTS_CHECK.ordinal();
    }

    @Override
    public boolean hasStatusSucceeded() {
        return ethState == EthState.STATUS_SUCCEEDED;
    }

    @Override
    public boolean isIdle() {
        return peerState == IDLE;
    }

    @Override
    public void enableTransactions() {
        processTransactions = true;
    }

    @Override
    public void disableTransactions() {
        processTransactions = false;
    }

    @Override
    public SyncStatistics getStats() {
        return syncStats;
    }

    @Override
    public void onSyncDone(final boolean done) {
        syncDone = done;
    }

    /*************************
     *       Validation      *
     *************************/

    @Nullable
    private List<Block> validateAndMerge(final BlockBodiesMessage response) {
        // merging received block bodies with requested headers
        // the assumption is the following:
        // - response may miss any bodies present in the request
        // - response may not contain non-requested bodies
        // - order of response bodies should be preserved
        // Otherwise the response is assumed invalid and all bodies are dropped

        final List<byte[]> bodyList = response.getBlockBodies();

        final Iterator<byte[]> bodies = bodyList.iterator();
        final Iterator<BlockHeaderWrapper> wrappers = sentHeaders.iterator();

        final List<Block> blocks = new ArrayList<>(bodyList.size());
        final List<BlockHeaderWrapper> coveredHeaders = new ArrayList<>(sentHeaders.size());

        boolean blockMerged = true;
        byte[] body = null;
        while (bodies.hasNext() && wrappers.hasNext()) {

            final BlockHeaderWrapper wrapper = wrappers.next();
            if (blockMerged) {
                body = bodies.next();
            }

            final Block b = new Block.Builder()
                    .withHeader(wrapper.getHeader())
                    .withBody(body)
                    .create();

            if (b == null) {
                blockMerged = false;
            } else {
                blockMerged = true;

                coveredHeaders.add(wrapper);
                blocks.add(b);
            }
        }

        if (bodies.hasNext()) {
            logger.info("Peer {}: invalid BLOCK_BODIES response: at least one block body doesn't correspond to any of requested headers: ",
                    channel.getPeerIdShort(), Hex.toHexString(bodies.next()));
            return null;
        }

        // remove headers covered by response
        sentHeaders.removeAll(coveredHeaders);

        return blocks;
    }

    private boolean isValid(final BlockBodiesMessage response) {
        return response.getBlockBodies().size() <= sentHeaders.size();
    }

    boolean isValid(final BlockHeadersMessage response, final GetBlockHeadersMessageWrapper requestWrapper) {

        final GetBlockHeadersMessage request = requestWrapper.getMessage();
        final List<BlockHeader> headers = response.getBlockHeaders();

        // max headers
        if (headers.size() > request.getMaxHeaders()) {

            if (logger.isInfoEnabled()) logger.info(
                    "Peer {}: invalid response to {}, exceeds maxHeaders limit, headers count={}",
                    channel.getPeerIdShort(), request, headers.size()
            );
            return false;
        }

        // emptiness against best known block
        if (headers.isEmpty()) {

            // initial call after handshake
            if (ethState == EthState.STATUS_SENT || ethState == EthState.HASH_CONSTRAINTS_CHECK) {
                if (logger.isInfoEnabled()) logger.info(
                        "Peer {}: invalid response to initial {}, empty",
                        channel.getPeerIdShort(), request
                );
                return false;
            }

            if (request.getBlockHash() == null &&
                    request.getBlockNumber() <= bestKnownBlock.getNumber()) {

                if (logger.isInfoEnabled()) logger.info(
                        "Peer {}: invalid response to {}, it's empty while bestKnownBlock is {}",
                        channel.getPeerIdShort(), request, bestKnownBlock
                );
                return false;
            }

            return true;
        }

        // first header
        final BlockHeader first = headers.get(0);

        if (request.getBlockHash() != null) {
            if (!Arrays.equals(request.getBlockHash(), first.getHash())) {

                if (logger.isInfoEnabled()) logger.info(
                        "Peer {}: invalid response to {}, first header is invalid {}",
                        channel.getPeerIdShort(), request, first
                );
                return false;
            }
        } else {
            if (request.getBlockNumber() != first.getNumber()) {

                if (logger.isInfoEnabled()) logger.info(
                        "Peer {}: invalid response to {}, first header is invalid {}",
                        channel.getPeerIdShort(), request, first
                );
                return false;
            }
        }

        // skip following checks in case of NEW_BLOCK_HASHES handling
        if (requestWrapper.isNewHashesHandling()) return true;

        // numbers and ancestors
        int offset = 1 + request.getSkipBlocks();
        if (request.isReverse()) offset = -offset;

        for (int i = 1; i < headers.size(); i++) {

            final BlockHeader cur = headers.get(i);
            final BlockHeader prev = headers.get(i - 1);

            final long num = cur.getNumber();
            final long expectedNum = prev.getNumber() + offset;

            if (num != expectedNum) {
                if (logger.isInfoEnabled()) logger.info(
                        "Peer {}: invalid response to {}, got #{}, expected #{}",
                        channel.getPeerIdShort(), request, num, expectedNum
                );
                return false;
            }

            if (request.getSkipBlocks() == 0) {
                final BlockHeader parent;
                final BlockHeader child;
                if (request.isReverse()) {
                    parent = cur;
                    child = prev;
                } else {
                    parent = prev;
                    child = cur;
                }
                if (!Arrays.equals(child.getParentHash(), parent.getHash())) {
                    if (logger.isInfoEnabled()) logger.info(
                            "Peer {}: invalid response to {}, got parent hash {} for #{}, expected {}",
                            channel.getPeerIdShort(), request, toHexString(child.getParentHash()),
                            prev.getNumber(), toHexString(parent.getHash())
                    );
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public synchronized void dropConnection() {
        logger.info("Peer {}: is a bad one, drop", channel.getPeerIdShort());
        disconnect(USELESS_PEER);
    }

    /*************************
     *       Logging         *
     *************************/

    @Override
    public String getSyncStats() {
        final int waitResp = lastReqSentTime > 0 ? (int) (System.currentTimeMillis() - lastReqSentTime) / 1000 : 0;
        final long lifeTime = System.currentTimeMillis() - connectedTime;
        return String.format(
                "Peer %s: [ %s, %18s, ping %6s ms, difficulty %s, best block %s%s]: (idle %s of %s) %s",
                getVersion(),
                channel.getPeerIdShort(),
                peerState,
                (int)channel.getPeerStats().getAvgLatency(),
                getTotalDifficulty(),
                getBestKnownBlock().getNumber(),
                waitResp > 5 ? ", wait " + waitResp + "s" : " ",
                longToTimePeriod(lifeTime - processingTime),
                longToTimePeriod(lifeTime),
                channel.getNodeStatistics().getClientId());
    }

    protected enum EthState {
        INIT,
        STATUS_SENT,
        HASH_CONSTRAINTS_CHECK,
        STATUS_SUCCEEDED,
        STATUS_FAILED
    }
}
