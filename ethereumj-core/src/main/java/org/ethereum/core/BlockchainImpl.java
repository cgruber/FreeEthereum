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

package org.ethereum.core;

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.*;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.manager.AdminInfo;
import org.ethereum.sync.SyncManager;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.util.*;
import org.ethereum.validator.DependentBlockHeaderRule;
import org.ethereum.validator.ParentBlockHeaderValidator;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.emptyList;
import static org.ethereum.core.Denomination.SZABO;
import static org.ethereum.core.ImportResult.*;
import static org.ethereum.crypto.HashUtil.sha3;

/**
 * The Ethereum blockchain is in many ways similar to the Bitcoin blockchain,
 * although it does have some differences.
 * <p>
 * The main difference between Ethereum and Bitcoin with regard to the blockchain architecture
 * is that, unlike Bitcoin, Ethereum blocks contain a copy of both the transaction list
 * and the most recent state. Aside from that, two other values, the block number and
 * the difficulty, are also stored in the block.
 * </p>
 * The block validation algorithm in Ethereum is as follows:
 * <ol>
 * <li>Check if the previous block referenced exists and is valid.</li>
 * <li>Check that the timestamp of the block is greater than that of the referenced previous block and less than 15 minutes into the future</li>
 * <li>Check that the block number, difficulty, transaction root, uncle root and gas limit (various low-level Ethereum-specific concepts) are valid.</li>
 * <li>Check that the proof of work on the block is valid.</li>
 * <li>Let S[0] be the STATE_ROOT of the previous block.</li>
 * <li>Let TX be the block's transaction list, with n transactions.
 * For all in in 0...n-1, set S[i+1] = APPLY(S[i],TX[i]).
 * If any applications returns an error, or if the total gas consumed in the block
 * up until this point exceeds the GASLIMIT, return an error.</li>
 * <li>Let S_FINAL be S[n], but adding the block reward paid to the miner.</li>
 * <li>Check if S_FINAL is the same as the STATE_ROOT. If it is, the block is valid; otherwise, it is not valid.</li>
 * </ol>
 * See <a href="https://github.com/ethereum/wiki/wiki/White-Paper#blockchain-and-mining">Ethereum Whitepaper</a>
 *
 * @author Roman Mandeleil
 * @author Nick Savers
 * @since 20.05.2014
 */
@Component
public class BlockchainImpl implements Blockchain, org.ethereum.facade.Blockchain {


    public static final byte[] EMPTY_LIST_HASH = sha3(RLP.encodeList(new byte[0]));
    private static final Logger logger = LoggerFactory.getLogger("blockchain");
    private static final Logger stateLogger = LoggerFactory.getLogger("state");
    // to avoid using minGasPrice=0 from Genesis for the wallet
    private static final long INITIAL_MIN_GAS_PRICE = 10 * SZABO.longValue();
    private static final int MAGIC_REWARD_OFFSET = 8;
    private final List<Chain> altChains = new ArrayList<>();
    private final List<Block> garbage = new ArrayList<>();
    private final Stack<State> stateStack = new Stack<>();
    public boolean byTest = false;
    @Autowired
    protected BlockStore blockStore;
    @Autowired
    StateSource stateDataSource;
    @Autowired
    private
    ProgramInvokeFactory programInvokeFactory;
    @Autowired
    private
    EventDispatchThread eventDispatchThread;
    @Autowired
    private
    CommonConfig commonConfig = CommonConfig.getDefault();
    @Autowired
    private
    SyncManager syncManager;
    @Autowired
    private
    PruneManager pruneManager;
    @Autowired
    private
    DbFlushManager dbFlushManager;
    private SystemProperties config = SystemProperties.getDefault();
    private long exitOn = Long.MAX_VALUE;
    @Autowired
    @Qualifier("defaultRepository")
    private Repository repository;
    @Autowired
    private TransactionStore transactionStore;
    private Block bestBlock;
    private BigInteger totalDifficulty = ZERO;
    @Autowired
    private EthereumListener listener;
    @Autowired
    private AdminInfo adminInfo;
    @Autowired
    private DependentBlockHeaderRule parentHeaderValidator;
    @Autowired
    private PendingState pendingState;
    private boolean fork = false;
    private byte[] minerCoinbase;
    private byte[] minerExtraData;
    private BigInteger BLOCK_REWARD;
    private BigInteger INCLUSION_REWARD;
    private int UNCLE_LIST_LIMIT;
    private int UNCLE_GENERATION_LIMIT;

    /** Tests only **/
    public BlockchainImpl() {
    }

    @Autowired
    public BlockchainImpl(final SystemProperties config) {
        this.config = config;
        initConst(config);
    }

    //todo: autowire over constructor
    public BlockchainImpl(final BlockStore blockStore, final Repository repository) {
        this.blockStore = blockStore;
        this.repository = repository;
        this.adminInfo = new AdminInfo();
        this.listener = new EthereumListenerAdapter();
        this.parentHeaderValidator = null;
        this.transactionStore = new TransactionStore(new HashMapDB());
        this.eventDispatchThread = EventDispatchThread.getDefault();
        this.programInvokeFactory = new ProgramInvokeFactoryImpl();
        initConst(SystemProperties.getDefault());
    }

    public static byte[] calcTxTrie(final List<Transaction> transactions) {

        final Trie txsState = new TrieImpl();

        if (transactions == null || transactions.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < transactions.size(); i++) {
            txsState.put(RLP.encodeInt(i), transactions.get(i).getEncoded());
        }
        return txsState.getRootHash();
    }

    public static byte[] calcReceiptsTrie(final List<TransactionReceipt> receipts) {
        final Trie receiptsTrie = new TrieImpl();

        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.put(RLP.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.getRootHash();
    }

    public static Set<ByteArrayWrapper> getAncestors(final BlockStore blockStore, final Block testedBlock, int limitNum, final boolean isParentBlock) {
        final Set<ByteArrayWrapper> ret = new HashSet<>();
        limitNum = (int) max(0, testedBlock.getNumber() - limitNum);
        Block it = testedBlock;
        if (!isParentBlock) {
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        while (it != null && it.getNumber() >= limitNum) {
            ret.add(new ByteArrayWrapper(it.getHash()));
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        return ret;
    }

    public BlockchainImpl withTransactionStore(final TransactionStore transactionStore) {
        this.transactionStore = transactionStore;
        return this;
    }

    public BlockchainImpl withAdminInfo(final AdminInfo adminInfo) {
        this.adminInfo = adminInfo;
        return this;
    }

    public BlockchainImpl withEthereumListener(final EthereumListener listener) {
        this.listener = listener;
        return this;
    }

    public BlockchainImpl withSyncManager(final SyncManager syncManager) {
        this.syncManager = syncManager;
        return this;
    }

    public BlockchainImpl withParentBlockHeaderValidator(final ParentBlockHeaderValidator parentHeaderValidator) {
        this.parentHeaderValidator = parentHeaderValidator;
        return this;
    }

    private void initConst(final SystemProperties config) {
        minerCoinbase = config.getMinerCoinbase();
        minerExtraData = config.getMineExtraData();
        BLOCK_REWARD = config.getBlockchainConfig().getCommonConstants().getBlockReward();
        INCLUSION_REWARD = BLOCK_REWARD.divide(BigInteger.valueOf(32));
        UNCLE_LIST_LIMIT = config.getBlockchainConfig().getCommonConstants().getUncleListLimit();
        UNCLE_GENERATION_LIMIT = config.getBlockchainConfig().getCommonConstants().getUncleGenerationLimit();
    }

    @Override
    public byte[] getBestBlockHash() {
        return getBestBlock().getHash();
    }

    @Override
    public long getSize() {
        return bestBlock.getNumber() + 1;
    }

    @Override
    public Block getBlockByNumber(final long blockNr) {
        return blockStore.getChainBlockByNumber(blockNr);
    }

    @Override
    public TransactionInfo getTransactionInfo(final byte[] hash) {

        final List<TransactionInfo> infos = transactionStore.get(hash);

        if (infos == null || infos.isEmpty())
            return null;

        TransactionInfo txInfo = null;
        if (infos.size() == 1) {
            txInfo = infos.get(0);
        } else {
            // pick up the receipt from the block on the main chain
            for (final TransactionInfo info : infos) {
                final Block block = blockStore.getBlockByHash(info.getBlockHash());
                final Block mainBlock = blockStore.getChainBlockByNumber(block.getNumber());
                if (FastByteComparisons.equal(info.getBlockHash(), mainBlock.getHash())) {
                    txInfo = info;
                    break;
                }
            }
        }
        if (txInfo == null) {
            logger.warn("Can't find block from main chain for transaction " + Hex.toHexString(hash));
            return null;
        }

        final Transaction tx = this.getBlockByHash(txInfo.getBlockHash()).getTransactionsList().get(txInfo.getIndex());
        txInfo.setTransaction(tx);

        return txInfo;
    }

    @Override
    public Block getBlockByHash(final byte[] hash) {
        return blockStore.getBlockByHash(hash);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFrom(final byte[] hash, final int qty) {
        return blockStore.getListHashesEndWith(hash, qty);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFromBlock(final long blockNumber, int qty) {
        final long bestNumber = bestBlock.getNumber();

        if (blockNumber > bestNumber) {
            return emptyList();
        }

        if (blockNumber + qty - 1 > bestNumber) {
            qty = (int) (bestNumber - blockNumber + 1);
        }

        final long endNumber = blockNumber + qty - 1;

        final Block block = getBlockByNumber(endNumber);

        final List<byte[]> hashes = blockStore.getListHashesEndWith(block.getHash(), qty);

        // asc order of hashes is required in the response
        Collections.reverse(hashes);

        return hashes;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public Repository getRepositorySnapshot() {
        return repository.getSnapshotTo(blockStore.getBestBlock().getStateRoot());
    }

    @Override
    public BlockStore getBlockStore() {
        return blockStore;
    }

    public ProgramInvokeFactory getProgramInvokeFactory() {
        return programInvokeFactory;
    }

    public void setProgramInvokeFactory(final ProgramInvokeFactory factory) {
        this.programInvokeFactory = factory;
    }

    private State pushState(final byte[] bestBlockHash) {
        final State push = stateStack.push(new State());
        this.bestBlock = blockStore.getBlockByHash(bestBlockHash);
        totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlockHash);
        this.repository = this.repository.getSnapshotTo(this.bestBlock.getStateRoot());
        return push;
    }

    private void popState() {
        final State state = stateStack.pop();
        this.repository = repository.getSnapshotTo(state.root);
        this.bestBlock = state.savedBest;
        this.totalDifficulty = state.savedTD;
    }

    private void dropState() {
        stateStack.pop();
    }

    private synchronized BlockSummary tryConnectAndFork(final Block block) {
        final State savedState = pushState(block.getParentHash());
        this.fork = true;

        final BlockSummary summary;
        Repository repo;
        try {

            // FIXME: adding block with no option for flush
            final Block parentBlock = getBlockByHash(block.getParentHash());
            repo = repository.getSnapshotTo(parentBlock.getStateRoot());
            summary = add(repo, block);
            if (summary == null) {
                return null;
            }
        } catch (final Throwable th) {
            logger.error("Unexpected error: ", th);
            return null;
        } finally {
            this.fork = false;
        }

        if (BIUtil.INSTANCE.isMoreThan(this.totalDifficulty, savedState.savedTD)) {

            logger.info("Rebranching: {} ~> {}", savedState.savedBest.getShortHash(), block.getShortHash());

            // main branch become this branch
            // cause we proved that total difficulty
            // is greateer
            blockStore.reBranch(block);

            // The main repository rebranch
            this.repository = repo;
//            this.repository.syncToRoot(block.getStateRoot());

            dropState();
        } else {
            // Stay on previous branch
            popState();
        }

        return summary;
    }

    public synchronized ImportResult tryToConnect(final Block block) {

        if (logger.isDebugEnabled())
            logger.debug("Try connect block hash: {}, number: {}",
                    Hex.toHexString(block.getHash()).substring(0, 6),
                    block.getNumber());

        if (blockStore.getMaxNumber() >= block.getNumber() &&
                blockStore.isBlockExist(block.getHash())) {

            if (logger.isDebugEnabled())
                logger.debug("Block already exist hash: {}, number: {}",
                        Hex.toHexString(block.getHash()).substring(0, 6),
                        block.getNumber());

            // retry of well known block
            return EXIST;
        }

        final ImportResult ret;

        // The simple case got the block
        // to connect to the main chain
        final BlockSummary summary;
        if (bestBlock.isParentOf(block)) {
            recordBlock(block);
//            Repository repoSnap = repository.getSnapshotTo(bestBlock.getStateRoot());
            summary = add(repository, block);

            ret = summary == null ? INVALID_BLOCK : IMPORTED_BEST;
        } else {

            if (blockStore.isBlockExist(block.getParentHash())) {
                final BigInteger oldTotalDiff = getTotalDifficulty();

                recordBlock(block);
                summary = tryConnectAndFork(block);

                ret = summary == null ? INVALID_BLOCK :
                        (BIUtil.INSTANCE.isMoreThan(getTotalDifficulty(), oldTotalDiff) ? IMPORTED_BEST : IMPORTED_NOT_BEST);
            } else {
                summary = null;
                ret = NO_PARENT;
            }

        }

        if (ret.isSuccessful()) {
            listener.onBlock(summary);
            listener.trace(String.format("Block chain size: [ %d ]", this.getSize()));

            if (ret == IMPORTED_BEST) {
                eventDispatchThread.invokeLater(() -> pendingState.processBest(block, summary.getReceipts()));
            }
        }

        return ret;
    }

    public synchronized Block createNewBlock(final Block parent, final List<Transaction> txs, final List<BlockHeader> uncles) {
        long time = System.currentTimeMillis() / 1000;
        // adjust time to parent block this may happen due to system clocks difference
        if (parent.getTimestamp() >= time) time = parent.getTimestamp() + 1;

        return createNewBlock(parent, txs, uncles, time);
    }

    public synchronized Block createNewBlock(final Block parent, final List<Transaction> txs, final List<BlockHeader> uncles, final long time) {
        final long blockNumber = parent.getNumber() + 1;

        final byte[] extraData = config.getBlockchainConfig().getConfigForBlock(blockNumber).getExtraData(minerExtraData, blockNumber);

        final Block block = new Block(parent.getHash(),
                EMPTY_LIST_HASH, // uncleHash
                minerCoinbase,
                new byte[0], // log bloom - from tx receipts
                new byte[0], // difficulty computed right after block creation
                blockNumber,
                parent.getGasLimit(), // (add to config ?)
                0,  // gas used - computed after running all transactions
                time,  // block time
                extraData,  // extra data
                new byte[0],  // mixHash (to mine)
                new byte[0],  // nonce   (to mine)
                new byte[0],  // receiptsRoot - computed after running all transactions
                calcTxTrie(txs),    // TransactionsRoot - computed after running all transactions
                new byte[] {0}, // stateRoot - computed after running all transactions
                txs,
                null);  // uncle list

        for (final BlockHeader uncle : uncles) {
            block.addUncle(uncle);
        }

        block.getHeader().setDifficulty(ByteUtil.bigIntegerToBytes(block.getHeader().
                calcDifficulty(config.getBlockchainConfig(), parent.getHeader())));

        final Repository track = repository.getSnapshotTo(parent.getStateRoot());
        final BlockSummary summary = applyBlock(track, block);
        final List<TransactionReceipt> receipts = summary.getReceipts();
        block.setStateRoot(track.getRoot());

        final Bloom logBloom = new Bloom();
        for (final TransactionReceipt receipt : receipts) {
            logBloom.or(receipt.getBloomFilter());
        }
        block.getHeader().setLogsBloom(logBloom.getData());
        block.getHeader().setGasUsed(receipts.size() > 0 ? receipts.get(receipts.size() - 1).getCumulativeGasLong() : 0);
        block.getHeader().setReceiptsRoot(calcReceiptsTrie(receipts));

        return block;
    }

    @Override
    public BlockSummary add(final Block block) {
        throw new RuntimeException("Not supported");
    }

    //    @Override
    private synchronized BlockSummary add(final Repository repo, final Block block) {
        final BlockSummary summary = addImpl(repo, block);

        if (summary == null) {
            stateLogger.warn("Trying to reimport the block for debug...");
            try {
                Thread.sleep(50);
            } catch (final InterruptedException e) {
            }
            final BlockSummary summary1 = addImpl(repo.getSnapshotTo(getBestBlock().getStateRoot()), block);
            stateLogger.warn("Second import trial " + (summary1 == null ? "FAILED" : "OK"));
            if (summary1 != null) {
                stateLogger.error("Inconsistent behavior, exiting...");
                System.exit(-1);
            }
        }
        return summary;
    }

    private synchronized BlockSummary addImpl(final Repository repo, final Block block) {

        if (exitOn < block.getNumber()) {
            System.out.print("Exiting after block.number: " + bestBlock.getNumber());
            dbFlushManager.flushSync();
            System.exit(-1);
        }


        if (!isValid(repo, block)) {
            logger.warn("Invalid block with number: {}", block.getNumber());
            return null;
        }

//        Repository track = repo.startTracking();
        final byte[] origRoot = repo.getRoot();

        if (block == null)
            return null;

        // keep chain continuity
//        if (!Arrays.equals(bestBlock.getHash(),
//                block.getParentHash())) return null;

        if (block.getNumber() >= config.traceStartBlock() && config.traceStartBlock() != -1) {
            AdvancedDeviceUtils.adjustDetailedTracing(config, block.getNumber());
        }

        BlockSummary summary = processBlock(repo, block);
        final List<TransactionReceipt> receipts = summary.getReceipts();

        // Sanity checks

        if (!FastByteComparisons.equal(block.getReceiptsRoot(), calcReceiptsTrie(receipts))) {
            logger.warn("Block's given Receipt Hash doesn't match: {} != {}", Hex.toHexString(block.getReceiptsRoot()), Hex.toHexString(calcReceiptsTrie(receipts)));
            logger.warn("Calculated receipts: " + receipts);
            repo.rollback();
            summary = null;
        }

        if (!FastByteComparisons.equal(block.getLogBloom(), calcLogBloom(receipts))) {
            logger.warn("Block's given logBloom Hash doesn't match: {} != {}", Hex.toHexString(block.getLogBloom()), Hex.toHexString(calcLogBloom(receipts)));
            repo.rollback();
            summary = null;
        }

        if (!FastByteComparisons.equal(block.getStateRoot(), repo.getRoot())) {

            stateLogger.warn("BLOCK: State conflict or received invalid block. block: {} worldstate {} mismatch", block.getNumber(), Hex.toHexString(repo.getRoot()));
            stateLogger.warn("Conflict block dump: {}", Hex.toHexString(block.getEncoded()));

//            track.rollback();
//            repository.rollback();
            repository = repository.getSnapshotTo(origRoot);

            // block is bad so 'rollback' the state root to the original state
//            ((RepositoryImpl) repository).setRoot(origRoot);

//            track.rollback();
            // block is bad so 'rollback' the state root to the original state
//            ((RepositoryImpl) repository).setRoot(origRoot);

            if (config.exitOnBlockConflict()) {
                adminInfo.lostConsensus();
                System.out.println("CONFLICT: BLOCK #" + block.getNumber() + ", dump: " + Hex.toHexString(block.getEncoded()));
                System.exit(1);
            } else {
                summary = null;
            }
        }

        if (summary != null) {
            repo.commit();
            updateTotalDifficulty(block);
            summary.setTotalDifficulty(getTotalDifficulty());

            if (!byTest) {
                dbFlushManager.commit(() -> {
                    storeBlock(block, receipts);
                    repository.commit();
                });
            } else {
                storeBlock(block, receipts);
            }
        }

        return summary;
    }

    @Override
    public void flush() {
//        repository.flush();
//        stateDataSource.flush();
//        blockStore.flush();
//        transactionStore.flush();
//
//        repository = repository.getSnapshotTo(repository.getRoot());
//
//        if (isMemoryBoundFlush()) {
//            System.gc();
//        }
    }

    private boolean needFlushByMemory(final double maxMemoryPercents) {
        return getRuntime().freeMemory() < (getRuntime().totalMemory() * (1 - maxMemoryPercents));
    }

    private byte[] calcLogBloom(final List<TransactionReceipt> receipts) {

        final Bloom retBloomFilter = new Bloom();

        if (receipts == null || receipts.isEmpty())
            return retBloomFilter.getData();

        for (final TransactionReceipt receipt : receipts) {
            retBloomFilter.or(receipt.getBloomFilter());
        }

        return retBloomFilter.getData();
    }

    private Block getParent(final BlockHeader header) {

        return blockStore.getBlockByHash(header.getParentHash());
    }

    private boolean isValid(final BlockHeader header) {
        if (parentHeaderValidator == null) return true;

        final Block parentBlock = getParent(header);

        if (!parentHeaderValidator.validate(header, parentBlock.getHeader())) {

            if (logger.isErrorEnabled())
                parentHeaderValidator.logErrors(logger);

            return false;
        }

        return true;
    }

    /**
     * This mechanism enforces a homeostasis in terms of the time between blocks;
     * a smaller period between the last two blocks results in an increase in the
     * difficulty level and thus additional computation required, lengthening the
     * likely next period. Conversely, if the period is too large, the difficulty,
     * and expected time to the next block, is reduced.
     */
    private boolean isValid(final Repository repo, final Block block) {

        boolean isValid = true;

        if (!block.isGenesis()) {
            isValid = isValid(block.getHeader());

            // Sanity checks
            final String trieHash = Hex.toHexString(block.getTxTrieRoot());
            final String trieListHash = Hex.toHexString(calcTxTrie(block.getTransactionsList()));


            if (!trieHash.equals(trieListHash)) {
                logger.warn("Block's given Trie Hash doesn't match: {} != {}", trieHash, trieListHash);
                return false;
            }

//            if (!validateUncles(block)) return false;

            final List<Transaction> txs = block.getTransactionsList();
            if (!txs.isEmpty()) {
//                Repository parentRepo = repository;
//                if (!Arrays.equals(bestBlock.getHash(), block.getParentHash())) {
//                    parentRepo = repository.getSnapshotTo(getBlockByHash(block.getParentHash()).getStateRoot());
//                }

                final Map<ByteArrayWrapper, BigInteger> curNonce = new HashMap<>();

                for (final Transaction tx : txs) {
                    final byte[] txSender = tx.getSender();
                    final ByteArrayWrapper key = new ByteArrayWrapper(txSender);
                    BigInteger expectedNonce = curNonce.get(key);
                    if (expectedNonce == null) {
                        expectedNonce = repo.getNonce(txSender);
                    }
                    curNonce.put(key, expectedNonce.add(ONE));
                    final BigInteger txNonce = new BigInteger(1, tx.getNonce());
                    if (!expectedNonce.equals(txNonce)) {
                        logger.warn("Invalid transaction: Tx nonce {} != expected nonce {} (parent nonce: {}): {}",
                                txNonce, expectedNonce, repo.getNonce(txSender), tx);
                        return false;
                    }
                }
            }
        }

        return isValid;
    }

    public boolean validateUncles(final Block block) {
        final String unclesHash = Hex.toHexString(block.getHeader().getUnclesHash());
        final String unclesListHash = Hex.toHexString(HashUtil.sha3(block.getHeader().getUnclesEncoded(block.getUncleList())));

        if (!unclesHash.equals(unclesListHash)) {
            logger.warn("Block's given Uncle Hash doesn't match: {} != {}", unclesHash, unclesListHash);
            return false;
        }


        if (block.getUncleList().size() > UNCLE_LIST_LIMIT) {
            logger.warn("Uncle list to big: block.getUncleList().size() > UNCLE_LIST_LIMIT");
            return false;
        }


        final Set<ByteArrayWrapper> ancestors = getAncestors(blockStore, block, UNCLE_GENERATION_LIMIT + 1, false);
        final Set<ByteArrayWrapper> usedUncles = getUsedUncles(blockStore, block, false);

        for (final BlockHeader uncle : block.getUncleList()) {

            // - They are valid headers (not necessarily valid blocks)
            if (!isValid(uncle)) return false;

            //if uncle's parent's number is not less than currentBlock - UNCLE_GEN_LIMIT, mark invalid
            final boolean isValid = !(getParent(uncle).getNumber() < (block.getNumber() - UNCLE_GENERATION_LIMIT));
            if (!isValid) {
                logger.warn("Uncle too old: generationGap must be under UNCLE_GENERATION_LIMIT");
                return false;
            }

            final ByteArrayWrapper uncleHash = new ByteArrayWrapper(uncle.getHash());
            if (ancestors.contains(uncleHash)) {
                logger.warn("Uncle is direct ancestor: " + Hex.toHexString(uncle.getHash()));
                return false;
            }

            if (usedUncles.contains(uncleHash)) {
                logger.warn("Uncle is not unique: " + Hex.toHexString(uncle.getHash()));
                return false;
            }

            final Block uncleParent = blockStore.getBlockByHash(uncle.getParentHash());
            if (!ancestors.contains(new ByteArrayWrapper(uncleParent.getHash()))) {
                logger.warn("Uncle has no common parent: " + Hex.toHexString(uncle.getHash()));
                return false;
            }
        }

        return true;
    }

    public Set<ByteArrayWrapper> getUsedUncles(final BlockStore blockStore, final Block testedBlock, final boolean isParentBlock) {
        final Set<ByteArrayWrapper> ret = new HashSet<>();
        final long limitNum = max(0, testedBlock.getNumber() - UNCLE_GENERATION_LIMIT);
        Block it = testedBlock;
        if (!isParentBlock) {
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        while(it.getNumber() > limitNum) {
            for (final BlockHeader uncle : it.getUncleList()) {
                ret.add(new ByteArrayWrapper(uncle.getHash()));
            }
            it = blockStore.getBlockByHash(it.getParentHash());
        }
        return ret;
    }

    private BlockSummary processBlock(final Repository track, final Block block) {

        if (!block.isGenesis() && !config.blockChainOnly()) {
            return applyBlock(track, block);
        }
        else {
            return new BlockSummary(block, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    private BlockSummary applyBlock(final Repository track, final Block block) {

        logger.debug("applyBlock: block: [{}] tx.list: [{}]", block.getNumber(), block.getTransactionsList().size());

        config.getBlockchainConfig().getConfigForBlock(block.getNumber()).hardForkTransfers(block, track);

        final long saveTime = System.nanoTime();
        final int i = 1;
        long totalGasUsed = 0;
        final List<TransactionReceipt> receipts = new ArrayList<>();
        final List<TransactionExecutionSummary> summaries = new ArrayList<>();

        for (final Transaction tx : block.getTransactionsList()) {
            stateLogger.debug("apply block: [{}] tx: [{}] ", block.getNumber(), i);

            final Repository txTrack = track.startTracking();
            final TransactionExecutor executor = new TransactionExecutor(tx, block.getCoinbase(),
                    txTrack, blockStore, programInvokeFactory, block, listener, totalGasUsed)
                    .withCommonConfig(commonConfig);

            executor.init();
            executor.execute();
            executor.go();
            final TransactionExecutionSummary summary = executor.finalization();

            totalGasUsed += executor.getGasUsed();

            txTrack.commit();
            final TransactionReceipt receipt = executor.getReceipt();

            receipt.setPostTxState(track.getRoot());

            stateLogger.info("block: [{}] executed tx: [{}] \n  state: [{}]", block.getNumber(), i,
                    Hex.toHexString(track.getRoot()));

            stateLogger.info("[{}] ", receipt.toString());

            if (stateLogger.isInfoEnabled())
                stateLogger.info("tx[{}].receipt: [{}] ", i, Hex.toHexString(receipt.getEncoded()));

            // TODO
//            if (block.getNumber() >= config.traceStartBlock())
//                repository.dumpState(block, totalGasUsed, i++, tx.getHash());

            receipts.add(receipt);
            if (summary != null) {
                summaries.add(summary);
            }
        }

        final Map<byte[], BigInteger> rewards = addReward(track, block, summaries);

        stateLogger.info("applied reward for block: [{}]  \n  state: [{}]",
                block.getNumber(),
                Hex.toHexString(track.getRoot()));


        // TODO
//        if (block.getNumber() >= config.traceStartBlock())
//            repository.dumpState(block, totalGasUsed, 0, null);

        final long totalTime = System.nanoTime() - saveTime;
        adminInfo.addBlockExecTime(totalTime);
        logger.debug("block: num: [{}] hash: [{}], executed after: [{}]nano", block.getNumber(), block.getShortHash(), totalTime);

        return new BlockSummary(block, rewards, receipts, summaries);
    }

    /**
     * Add reward to block- and every uncle coinbase
     * assuming the entire block is valid.
     *
     * @param block object containing the header and uncles
     */
    private Map<byte[], BigInteger> addReward(final Repository track, final Block block, final List<TransactionExecutionSummary> summaries) {

        final Map<byte[], BigInteger> rewards = new HashMap<>();

        // Add extra rewards based on number of uncles
        if (block.getUncleList().size() > 0) {
            for (final BlockHeader uncle : block.getUncleList()) {
                final BigInteger uncleReward = BLOCK_REWARD
                        .multiply(BigInteger.valueOf(MAGIC_REWARD_OFFSET + uncle.getNumber() - block.getNumber()))
                        .divide(BigInteger.valueOf(MAGIC_REWARD_OFFSET));

                track.addBalance(uncle.getCoinbase(),uncleReward);
                rewards.merge(uncle.getCoinbase(), uncleReward, BigInteger::add);
            }
        }

        final BigInteger minerReward = BLOCK_REWARD.add(INCLUSION_REWARD.multiply(BigInteger.valueOf(block.getUncleList().size())));

        BigInteger totalFees = BigInteger.ZERO;
        for (final TransactionExecutionSummary summary : summaries) {
            totalFees = totalFees.add(summary.getFee());
        }

        rewards.put(block.getCoinbase(), minerReward.add(totalFees));
        track.addBalance(block.getCoinbase(), minerReward); // fees are already given to the miner during tx execution
        return rewards;
    }

    @Override
    public synchronized void storeBlock(final Block block, final List<TransactionReceipt> receipts) {

        if (fork)
            blockStore.saveBlock(block, totalDifficulty, false);
        else
            blockStore.saveBlock(block, totalDifficulty, true);

        for (int i = 0; i < receipts.size(); i++) {
            transactionStore.put(new TransactionInfo(receipts.get(i), block.getHash(), i));
        }

        if (pruneManager != null) {
            pruneManager.blockCommitted(block.getHeader());
        }

        logger.debug("Block saved: number: {}, hash: {}, TD: {}",
                block.getNumber(), block.getShortHash(), totalDifficulty);

        setBestBlock(block);

        if (logger.isDebugEnabled())
            logger.debug("block added to the blockChain: index: [{}]", block.getNumber());
        if (block.getNumber() % 100 == 0)
            logger.info("*** Last block added [ #{} ]", block.getNumber());

    }

    public boolean hasParentOnTheChain(final Block block) {
        return getParent(block.getHeader()) != null;
    }

    @Override
    public List<Chain> getAltChains() {
        return altChains;
    }

    @Override
    public List<Block> getGarbage() {
        return garbage;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    @Override
    public synchronized Block getBestBlock() {
        // the method is synchronized since the bestBlock might be
        // temporarily switched to the fork while importing non-best block
        return bestBlock;
    }

    @Override
    public void setBestBlock(final Block block) {
        bestBlock = block;
        repository = repository.getSnapshotTo(block.getStateRoot());
    }

    @Override
    public synchronized void close() {
        blockStore.close();
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    @Override
    public void setTotalDifficulty(final BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    @Override
    public synchronized void updateTotalDifficulty(final Block block) {
        totalDifficulty = totalDifficulty.add(block.getDifficultyBI());
        logger.debug("TD: updated to {}", totalDifficulty);
    }

    private void recordBlock(final Block block) {

        if (!config.recordBlocks()) return;

        final String dumpDir = config.databaseDir() + "/" + config.dumpDir();

        final File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {

            dumpFile.getParentFile().mkdirs();
            if (!dumpFile.exists()) dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            if (bestBlock.isGenesis()) {
                bw.write(Hex.toHexString(bestBlock.getEncoded()));
                bw.write("\n");
            }

            bw.write(Hex.toHexString(block.getEncoded()));
            bw.write("\n");

        } catch (final IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateBlockTotDifficulties(int startFrom) {
        // no synchronization here not to lock instance for long period
        while(true) {
            synchronized (this) {
                ((IndexedBlockStore) blockStore).updateTotDifficulties(startFrom);
                if (startFrom == bestBlock.getNumber()) {
                    totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlock.getHash());
                    break;
                }
                startFrom++;
            }
        }
    }

    public void setExitOn(final long exitOn) {
        this.exitOn = exitOn;
    }

    @Override
    public byte[] getMinerCoinbase() {
        return minerCoinbase;
    }

    public void setMinerCoinbase(final byte[] minerCoinbase) {
        this.minerCoinbase = minerCoinbase;
    }

    public void setMinerExtraData(final byte[] minerExtraData) {
        this.minerExtraData = minerExtraData;
    }

    public boolean isBlockExist(final byte[] hash) {
        return blockStore.isBlockExist(hash);
    }

    public void setParentHeaderValidator(final DependentBlockHeaderRule parentHeaderValidator) {
        this.parentHeaderValidator = parentHeaderValidator;
    }

    public PendingState getPendingState() {
        return pendingState;
    }

    public void setPendingState(final PendingState pendingState) {
        this.pendingState = pendingState;
    }

    /**
     * Returns up to limit headers found with following search parameters
     * [Synchronized only in blockstore, not using any synchronized BlockchainImpl methods]
     * @param identifier        Identifier of start block, by number of by hash
     * @param skip              Number of blocks to skip between consecutive headers
     * @param limit             Maximum number of headers in return
     * @param reverse           Is search reverse or not
     * @return  {@link BlockHeader}'s list or empty list if none found
     */
    @Override
    public List<BlockHeader> getListOfHeadersStartFrom(final BlockIdentifier identifier, final int skip, final int limit, final boolean reverse) {

        // Identifying block we'll move from
        final Block startBlock;
        if (identifier.getHash() != null) {
            startBlock = blockStore.getBlockByHash(identifier.getHash());
        } else {
            startBlock = blockStore.getChainBlockByNumber(identifier.getNumber());
        }

        // If nothing found or provided hash is not on main chain, return empty array
        if (startBlock == null) {
            return emptyList();
        }
        if (identifier.getHash() != null) {
            final Block mainChainBlock = blockStore.getChainBlockByNumber(startBlock.getNumber());
            if (!startBlock.equals(mainChainBlock)) return emptyList();
        }

        final List<BlockHeader> headers;
        if (skip == 0) {
            final long bestNumber = blockStore.getBestBlock().getNumber();
            headers = getContinuousHeaders(bestNumber, startBlock.getNumber(), limit, reverse);
        } else {
            headers = getGapedHeaders(startBlock, skip, limit, reverse);
        }

        return headers;
    }

    /**
     * Finds up to limit blocks starting from blockNumber on main chain
     * @param bestNumber        Number of best block
     * @param blockNumber       Number of block to start search (included in return)
     * @param limit             Maximum number of headers in response
     * @param reverse           Order of search
     * @return  headers found by query or empty list if none
     */
    private List<BlockHeader> getContinuousHeaders(final long bestNumber, final long blockNumber, final int limit, final boolean reverse) {
        final int qty = getQty(blockNumber, bestNumber, limit, reverse);

        final byte[] startHash = getStartHash(blockNumber, qty, reverse);

        if (startHash == null) {
            return emptyList();
        }

        final List<BlockHeader> headers = blockStore.getListHeadersEndWith(startHash, qty);

        // blocks come with falling numbers
        if (!reverse) {
            Collections.reverse(headers);
        }

        return headers;
    }

    /**
     * Gets blocks from main chain with gaps between
     * @param startBlock        Block to start from (included in return)
     * @param skip              Number of blocks skipped between every header in return
     * @param limit             Maximum number of headers in return
     * @param reverse           Order of search
     * @return  headers found by query or empty list if none
     */
    private List<BlockHeader> getGapedHeaders(final Block startBlock, final int skip, final int limit, final boolean reverse) {
        final List<BlockHeader> headers = new ArrayList<>();
        headers.add(startBlock.getHeader());
        int offset = skip + 1;
        if (reverse) offset = -offset;
        long currentNumber = startBlock.getNumber();
        boolean finished = false;

        while(!finished && headers.size() < limit) {
            currentNumber += offset;
            final Block nextBlock = blockStore.getChainBlockByNumber(currentNumber);
            if (nextBlock == null) {
                finished = true;
            } else {
                headers.add(nextBlock.getHeader());
            }
        }

        return headers;
    }

    private int getQty(final long blockNumber, final long bestNumber, final int limit, final boolean reverse) {
        if (reverse) {
            return blockNumber - limit + 1 < 0 ? (int) (blockNumber + 1) : limit;
        } else {
            if (blockNumber + limit - 1 > bestNumber) {
                return (int) (bestNumber - blockNumber + 1);
            } else {
                return limit;
            }
        }
    }

    private byte[] getStartHash(final long blockNumber, final int qty, final boolean reverse) {

        final long startNumber;

        if (reverse) {
            startNumber = blockNumber;
        } else {
            startNumber = blockNumber + qty - 1;
        }

        final Block block = blockStore.getChainBlockByNumber(startNumber);

        if (block == null) {
            return null;
        }

        return block.getHash();
    }

    /**
     * Returns list of block bodies by block hashes, stopping on first not found block
     * [Synchronized only in blockstore, not using any synchronized BlockchainImpl methods]
     * @param hashes List of hashes
     * @return List of RLP encoded block bodies
     */
    @Override
    public List<byte[]> getListOfBodiesByHashes(final List<byte[]> hashes) {
        final List<byte[]> bodies = new ArrayList<>(hashes.size());

        for (final byte[] hash : hashes) {
            final Block block = blockStore.getBlockByHash(hash);
            if (block == null) break;
            bodies.add(block.getEncodedBody());
        }

        return bodies;
    }

    public void setPruneManager(final PruneManager pruneManager) {
        this.pruneManager = pruneManager;
    }

    private class State {
//        Repository savedRepo = repository;
final byte[] root = repository.getRoot();
        final Block savedBest = bestBlock;
        final BigInteger savedTD = totalDifficulty;
    }
}
