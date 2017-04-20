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

package org.ethereum.manager;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.DbFlushManager;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListener;
import org.ethereum.net.client.PeerClient;
import org.ethereum.net.rlpx.discover.NodeManager;
import org.ethereum.net.rlpx.discover.UDPListener;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.sync.FastSyncManager;
import org.ethereum.sync.SyncManager;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * WorldManager is a singleton containing references to different parts of the system.
 *
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
@Component
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger("general");
    private final SystemProperties config;
    private final EthereumListener listener;
    private final Blockchain blockchain;
    private final Repository repository;
    private final BlockStore blockStore;
    private final PeerClient activePeer;
    private final ChannelManager channelManager;
    private final AdminInfo adminInfo;
    private final NodeManager nodeManager;
    private final SyncManager syncManager;
    private final FastSyncManager fastSyncManager;
    private final SyncPool pool;
    private final PendingState pendingState;
    private final UDPListener discoveryUdpListener;
    private final EventDispatchThread eventDispatchThread;
    private final DbFlushManager dbFlushManager;
    private final ApplicationContext ctx;

    @Autowired
    public WorldManager(final SystemProperties config, final Repository repository,
                        final EthereumListener listener, final Blockchain blockchain,
                        final BlockStore blockStore, ApplicationContext ctx, DbFlushManager dbFlushManager, EventDispatchThread eventDispatchThread, UDPListener discoveryUdpListener, PendingState pendingState, SyncPool pool, FastSyncManager fastSyncManager, SyncManager syncManager, NodeManager nodeManager, AdminInfo adminInfo, ChannelManager channelManager, PeerClient activePeer) {
        this.listener = listener;
        this.blockchain = blockchain;
        this.repository = repository;
        this.blockStore = blockStore;
        this.config = config;
        loadBlockchain();
        this.ctx = ctx;
        this.dbFlushManager = dbFlushManager;
        this.eventDispatchThread = eventDispatchThread;
        this.discoveryUdpListener = discoveryUdpListener;
        this.pendingState = pendingState;
        this.pool = pool;
        this.fastSyncManager = fastSyncManager;
        this.syncManager = syncManager;
        this.nodeManager = nodeManager;
        this.adminInfo = adminInfo;
        this.channelManager = channelManager;
        this.activePeer = activePeer;
    }

    @PostConstruct
    private void init() {
        syncManager.init(channelManager, pool);
    }

    public void addListener(final EthereumListener listener) {
        logger.info("Ethereum listener added");
        ((CompositeEthereumListener) this.listener).addListener(listener);
    }

    public void startPeerDiscovery() {
    }

    public void stopPeerDiscovery() {
        discoveryUdpListener.close();
        nodeManager.close();
    }

    public void initSyncing() {
        config.setSyncEnabled(true);
        syncManager.init(channelManager, pool);
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public EthereumListener getListener() {
        return listener;
    }

    public org.ethereum.facade.Repository getRepository() {
        return repository;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public PeerClient getActivePeer() {
        return activePeer;
    }

    public BlockStore getBlockStore() {
        return blockStore;
    }

    public PendingState getPendingState() {
        return pendingState;
    }

    private void loadBlockchain() {

        if (!config.databaseReset() || config.databaseResetBlock() != 0)
            blockStore.load();

        if (blockStore.getBestBlock() == null) {
            logger.info("DB is empty - adding Genesis");

            final Genesis genesis = Genesis.getInstance(config);
            Genesis.populateRepository(repository, genesis);

//            repository.commitBlock(genesis.getHeader());
            repository.commit();

            blockStore.saveBlock(Genesis.getInstance(config), Genesis.getInstance(config).getCumulativeDifficulty(), true);

            blockchain.setBestBlock(Genesis.getInstance(config));
            blockchain.setTotalDifficulty(Genesis.getInstance(config).getCumulativeDifficulty());

            listener.onBlock(new BlockSummary(Genesis.getInstance(config), new HashMap<>(), new ArrayList<>(), new ArrayList<>()));
//            repository.dumpState(Genesis.getInstance(config), 0, 0, null);

            logger.info("Genesis block loaded");
        } else {

            if (!config.databaseReset() &&
                    !Arrays.equals(blockchain.getBlockByNumber(0).getHash(), config.getGenesis().getHash())) {
                // fatal exit
                Utils.showErrorAndExit("*** DB is incorrect, 0 block in DB doesn't match genesis");
            }

            Block bestBlock = blockStore.getBestBlock();
            if (config.databaseReset() && config.databaseResetBlock() > 0) {
                if (config.databaseResetBlock() > bestBlock.getNumber()) {
                    logger.error("*** Can't reset to block [{}] since block store is at block [{}].", config.databaseResetBlock(), bestBlock);
                    throw new RuntimeException("Reset block ahead of block store.");
                }
                bestBlock = blockStore.getChainBlockByNumber(config.databaseResetBlock());

                final Repository snapshot = repository.getSnapshotTo(bestBlock.getStateRoot());
                if (false) { // TODO: some way to tell if the snapshot hasn't been pruned
                    logger.error("*** Could not reset database to block [{}] with stateRoot [{}], since state information is " +
                            "unavailable.  It might have been pruned from the database.");
                    throw new RuntimeException("State unavailable for reset block.");
                }
            }

            blockchain.setBestBlock(bestBlock);

            final BigInteger totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlock.getHash());
            blockchain.setTotalDifficulty(totalDifficulty);

            logger.info("*** Loaded up to block [{}] totalDifficulty [{}] with stateRoot [{}]",
                    blockchain.getBestBlock().getNumber(),
                    blockchain.getTotalDifficulty().toString(),
                    Hex.toHexString(blockchain.getBestBlock().getStateRoot()));
        }

        if (config.rootHashStart() != null) {

            // update world state by dummy hash
            final byte[] rootHash = Hex.decode(config.rootHashStart());
            logger.info("Loading root hash from property file: [{}]", config.rootHashStart());
            this.repository.syncToRoot(rootHash);

        } else {

            // Update world state to latest loaded block from db
            // if state is not generated from empty premine list
            // todo this is just a workaround, move EMPTY_TRIE_HASH logic to Trie implementation
            if (!Arrays.equals(blockchain.getBestBlock().getStateRoot(), HashUtil.INSTANCE.getEMPTY_TRIE_HASH())) {
                this.repository.syncToRoot(blockchain.getBestBlock().getStateRoot());
            }
        }

/* todo: return it when there is no state conflicts on the chain
        boolean dbValid = this.repository.getWorldState().validate() || bestBlock.isGenesis();
        if (!dbValid){
            logger.error("The DB is not valid for that blockchain");
            System.exit(-1); //  todo: reset the repository and blockchain
        }
*/
    }

    public void close() {
        logger.info("close: stopping peer discovery ...");
        stopPeerDiscovery();
        logger.info("close: stopping ChannelManager ...");
        channelManager.close();
        logger.info("close: stopping SyncManager ...");
        syncManager.close();
        logger.info("close: stopping PeerClient ...");
        activePeer.close();
        logger.info("close: shutting down event dispatch thread used by EventBus ...");
        eventDispatchThread.shutdown();
        logger.info("close: closing Blockchain instance ...");
        blockchain.close();
        logger.info("close: closing main repository ...");
        repository.close();
        logger.info("close: database flush manager ...");
        dbFlushManager.close();
    }

}
