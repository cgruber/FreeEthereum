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

package org.ethereum.mine;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections4.CollectionUtils;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.mine.MinerIfc.MiningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Math.max;

/**
 * Manages embedded CPU mining and allows to use external miners.
 *
 * Created by Anton Nashatyrev on 10.12.2015.
 */
@Component
public class BlockMiner {
    private static final Logger logger = LoggerFactory.getLogger("mine");

    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Queue<ListenableFuture<MiningResult>> currentMiningTasks = new ConcurrentLinkedQueue<>();
    private final PendingState pendingState;
    private final Blockchain blockchain;
    private final BlockStore blockStore;
    private final SystemProperties config;
    private final List<MinerListener> listeners = new CopyOnWriteArrayList<>();
    private final long minBlockTimeout;
    private final int UNCLE_LIST_LIMIT;
    private final int UNCLE_GENERATION_LIMIT;
    @Autowired
    private Ethereum ethereum;
    private BigInteger minGasPrice;
    private int cpuThreads;
    private boolean fullMining = true;
    private volatile boolean isLocalMining;
    private Block miningBlock;
    private volatile MinerIfc externalMiner;
    private long lastBlockMinedTime;

    @Autowired
    public BlockMiner(final SystemProperties config, final CompositeEthereumListener listener,
                      final Blockchain blockchain, final BlockStore blockStore,
                      final PendingState pendingState) {
        CompositeEthereumListener listener1 = listener;
        this.config = config;
        this.blockchain = blockchain;
        this.blockStore = blockStore;
        this.pendingState = pendingState;
        UNCLE_LIST_LIMIT = config.getBlockchainConfig().getCommonConstants().getUncleListLimit();
        UNCLE_GENERATION_LIMIT = config.getBlockchainConfig().getCommonConstants().getUncleGenerationLimit();
        minGasPrice = config.getMineMinGasPrice();
        minBlockTimeout = config.getMineMinBlockTimeoutMsec();
        cpuThreads = config.getMineCpuThreads();
        fullMining = config.isMineFullDataset();
        listener.addListener(new EthereumListenerAdapter() {
            @Override
            public void onPendingStateChanged(PendingState pendingState) {
                BlockMiner.this.onPendingStateChanged();
            }

            @Override
            public void onSyncDone(SyncState state) {
                if (config.minerStart() && config.isSyncEnabled()) {
                    logger.info("Sync complete, start mining...");
                    startMining();
                }
            }
        });

        if (config.minerStart() && !config.isSyncEnabled()) {
            logger.info("Sync disabled, start mining now...");
            startMining();
        }
    }

    public void setFullMining(boolean fullMining) {
        this.fullMining = fullMining;
    }

    public void setCpuThreads(int cpuThreads) {
        this.cpuThreads = cpuThreads;
    }

    public void setMinGasPrice(BigInteger minGasPrice) {
        this.minGasPrice = minGasPrice;
    }

    public void setExternalMiner(MinerIfc miner) {
        externalMiner = miner;
        restartMining();
    }

    public void startMining() {
        isLocalMining = true;
        fireMinerStarted();
        logger.info("Miner started");
        restartMining();
    }

    public void stopMining() {
        isLocalMining = false;
        cancelCurrentBlock();
        fireMinerStopped();
        logger.info("Miner stopped");
    }

    private List<Transaction> getAllPendingTransactions() {
        PendingStateImpl.TransactionSortedSet ret = new PendingStateImpl.TransactionSortedSet();
        ret.addAll(pendingState.getPendingTransactions());
        Iterator<Transaction> it = ret.iterator();
        while(it.hasNext()) {
            Transaction tx = it.next();
            if (!isAcceptableTx(tx)) {
                logger.debug("Miner excluded the transaction: {}", tx);
                it.remove();
            }
        }
        return new ArrayList<>(ret);
    }

    private void onPendingStateChanged() {
        if (!isLocalMining && externalMiner == null) return;

        logger.debug("onPendingStateChanged()");
        if (miningBlock == null) {
            restartMining();
        } else if (miningBlock.getNumber() <= ((PendingStateImpl) pendingState).getBestBlock().getNumber()) {
            logger.debug("Restart mining: new best block: " + blockchain.getBestBlock().getShortDescr());
            restartMining();
        } else if (!CollectionUtils.isEqualCollection(miningBlock.getTransactionsList(), getAllPendingTransactions())) {
            logger.debug("Restart mining: pending transactions changed");
            restartMining();
        } else {
            if (logger.isDebugEnabled()) {
                StringBuilder s = new StringBuilder("onPendingStateChanged() event, but pending Txs the same as in currently mining block: ");
                for (Transaction tx : getAllPendingTransactions()) {
                    s.append("\n    ").append(tx);
                }
                logger.debug(s.toString());
            }
        }
    }

    private boolean isAcceptableTx(Transaction tx) {
        return minGasPrice.compareTo(new BigInteger(1, tx.getGasPrice())) <= 0;
    }

    private synchronized void cancelCurrentBlock() {
        for (ListenableFuture<MiningResult> task : currentMiningTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
            }
        }
        currentMiningTasks.clear();

        if (miningBlock != null) {
            fireBlockCancelled(miningBlock);
            logger.debug("Tainted block mining cancelled: {}", miningBlock.getShortDescr());
            miningBlock = null;
        }
    }

    private List<BlockHeader> getUncles(Block mineBest) {
        List<BlockHeader> ret = new ArrayList<>();
        long miningNum = mineBest.getNumber() + 1;
        Block mineChain = mineBest;

        long limitNum = max(0, miningNum - UNCLE_GENERATION_LIMIT);
        Set<ByteArrayWrapper> ancestors = BlockchainImpl.getAncestors(blockStore, mineBest, UNCLE_GENERATION_LIMIT + 1, true);
        Set<ByteArrayWrapper> knownUncles = ((BlockchainImpl)blockchain).getUsedUncles(blockStore, mineBest, true);
        knownUncles.addAll(ancestors);
        knownUncles.add(new ByteArrayWrapper(mineBest.getHash()));

        if (blockStore instanceof IndexedBlockStore) {
            outer:
            while (mineChain.getNumber() > limitNum) {
                List<Block> genBlocks = ((IndexedBlockStore) blockStore).getBlocksByNumber(mineChain.getNumber());
                if (genBlocks.size() > 1) {
                    for (Block uncleCandidate : genBlocks) {
                        if (!knownUncles.contains(new ByteArrayWrapper(uncleCandidate.getHash())) &&
                                ancestors.contains(new ByteArrayWrapper(blockStore.getBlockByHash(uncleCandidate.getParentHash()).getHash()))) {

                            ret.add(uncleCandidate.getHeader());
                            if (ret.size() >= UNCLE_LIST_LIMIT) {
                                break outer;
                            }
                        }
                    }
                }
                mineChain = blockStore.getBlockByHash(mineChain.getParentHash());
            }
        } else {
            logger.warn("BlockStore is not instance of IndexedBlockStore: miner can't include uncles");
        }
        return ret;
    }

    private Block getNewBlockForMining() {
        Block bestBlockchain = blockchain.getBestBlock();
        Block bestPendingState = ((PendingStateImpl) pendingState).getBestBlock();

        logger.debug("getNewBlockForMining best blocks: PendingState: " + bestPendingState.getShortDescr() +
                ", Blockchain: " + bestBlockchain.getShortDescr());

        Block newMiningBlock = blockchain.createNewBlock(bestPendingState, getAllPendingTransactions(),
                getUncles(bestPendingState));
        return newMiningBlock;
    }

    private void restartMining() {
        Block newMiningBlock = getNewBlockForMining();

        synchronized(this) {
            cancelCurrentBlock();
            miningBlock = newMiningBlock;

            if (externalMiner != null) {
                currentMiningTasks.add(externalMiner.mine(cloneBlock(miningBlock)));
            }
            if (isLocalMining) {
                MinerIfc localMiner = config.getBlockchainConfig()
                        .getConfigForBlock(miningBlock.getNumber())
                        .getMineAlgorithm(config);
                currentMiningTasks.add(localMiner.mine(cloneBlock(miningBlock)));
            }

            for (final ListenableFuture<MiningResult> task : currentMiningTasks) {
                task.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // wow, block mined!
                            final Block minedBlock = task.get().block;
                            blockMined(minedBlock);
                        } catch (InterruptedException | CancellationException e) {
                            // OK, we've been cancelled, just exit
                        } catch (Exception e) {
                            logger.warn("Exception during mining: ", e);
                        }
                    }
                }, MoreExecutors.sameThreadExecutor());
            }
        }
        fireBlockStarted(newMiningBlock);
        logger.debug("New block mining started: {}", newMiningBlock.getShortHash());
    }

    /**
     * Block cloning is required before passing block to concurrent miner env.
     * In success result miner will modify this block instance.
     */
    private Block cloneBlock(Block block) {
        return new Block(block.getEncoded());
    }

    private void blockMined(Block newBlock) throws InterruptedException {
        long t = System.currentTimeMillis();
        if (t - lastBlockMinedTime < minBlockTimeout) {
            long sleepTime = minBlockTimeout - (t - lastBlockMinedTime);
            logger.debug("Last block was mined " + (t - lastBlockMinedTime) + " ms ago. Sleeping " +
                    sleepTime + " ms before importing...");
            Thread.sleep(sleepTime);
        }

        fireBlockMined(newBlock);
        logger.info("Wow, block mined !!!: {}", newBlock.toString());

        lastBlockMinedTime = t;
        miningBlock = null;
        // cancel all tasks
        cancelCurrentBlock();

        // broadcast the block
        logger.debug("Importing newly mined block {} {} ...", newBlock.getShortHash(), newBlock.getNumber());
        ImportResult importResult = ((EthereumImpl) ethereum).addNewMinedBlock(newBlock);
        logger.debug("Mined block import result is " + importResult);
    }

    public boolean isMining() {
        return isLocalMining || externalMiner != null;
    }

    /*****  Listener boilerplate  ******/

    public void addListener(MinerListener l) {
        listeners.add(l);
    }

    public void removeListener(MinerListener l) {
        listeners.remove(l);
    }

    private void fireMinerStarted() {
        for (MinerListener l : listeners) {
            l.miningStarted();
        }
    }

    private void fireMinerStopped() {
        for (MinerListener l : listeners) {
            l.miningStopped();
        }
    }

    private void fireBlockStarted(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningStarted(b);
        }
    }

    private void fireBlockCancelled(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningCanceled(b);
        }
    }

    private void fireBlockMined(Block b) {
        for (MinerListener l : listeners) {
            l.blockMined(b);
        }
    }
}
