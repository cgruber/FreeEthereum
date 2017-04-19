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

import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.TransactionStore;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListener.PendingTransactionState;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.ethereum.listener.EthereumListener.PendingTransactionState.*;

/**
 * Keeps logic providing pending state management
 *
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
@Component
public class PendingStateImpl implements PendingState {

    private static final Logger logger = LoggerFactory.getLogger("pending");
    private final List<PendingTransaction> pendingTransactions = new ArrayList<>();
    // to filter out the transactions we have already processed
    // transactions could be sent by peers even if they were already included into blocks
    private final Map<ByteArrayWrapper, Object> receivedTxs = new LRUMap<>(100000);
    private final Object dummyObject = new Object();
    @Autowired
    private SystemProperties config = SystemProperties.getDefault();
    @Autowired
    private
    CommonConfig commonConfig = CommonConfig.getDefault();
    @Autowired
    private EthereumListener listener;
    @Autowired
    private BlockchainImpl blockchain;
    @Autowired
    private BlockStore blockStore;

    //    private Repository repository;
    @Autowired
    private TransactionStore transactionStore;
    @Autowired
    private ProgramInvokeFactory programInvokeFactory;
    private Repository pendingState;
    private Block best = null;

    @Autowired
    public PendingStateImpl(final EthereumListener listener, final BlockchainImpl blockchain) {
        this.listener = listener;
        this.blockchain = blockchain;
//        this.repository = blockchain.getRepository();
        this.blockStore = blockchain.getBlockStore();
        this.programInvokeFactory = blockchain.getProgramInvokeFactory();
        this.transactionStore = blockchain.getTransactionStore();
    }

    private void init() {
        this.pendingState = getOrigRepository().startTracking();
    }

    private Repository getOrigRepository() {
        return blockchain.getRepositorySnapshot();
    }

    @Override
    public synchronized Repository getRepository() {
        if (pendingState == null) {
            init();
        }
        return pendingState;
    }

    @Override
    public synchronized List<Transaction> getPendingTransactions() {

        final List<Transaction> txs = new ArrayList<>();

        for (final PendingTransaction tx : pendingTransactions) {
            txs.add(tx.getTransaction());
        }

        return txs;
    }

    public Block getBestBlock() {
        if (best == null) {
            best = blockchain.getBestBlock();
        }
        return best;
    }

    private boolean addNewTxIfNotExist(final Transaction tx) {
        return receivedTxs.put(new ByteArrayWrapper(tx.getHash()), dummyObject) == null;
    }

    @Override
    public void addPendingTransaction(final Transaction tx) {
        addPendingTransactions(Collections.singletonList(tx));
    }

    @Override
//    public synchronized List<Transaction> addPendingTransactions(final List<Transaction> transactions) {
    public synchronized List<Transaction> addPendingTransactions(@NotNull final List<? extends Transaction> transactions) {
        int unknownTx = 0;
        final List<Transaction> newPending = new ArrayList<>();
        for (final Transaction tx : transactions) {
            if (addNewTxIfNotExist(tx)) {
                unknownTx++;
                if (addPendingTransactionImpl(tx)) {
                    newPending.add(tx);
                }
            }
        }

        logger.debug("Wire transaction list added: total: {}, new: {}, valid (added to pending): {} (current #of known txs: {})",
                transactions.size(), unknownTx, newPending, receivedTxs.size());

        if (!newPending.isEmpty()) {
            listener.onPendingTransactionsReceived(newPending);
            listener.onPendingStateChanged(PendingStateImpl.this);
        }

        return newPending;
    }

    public synchronized void trackTransaction(final Transaction tx) {
        final List<TransactionInfo> infos = transactionStore.get(tx.getHash());
        if (!infos.isEmpty()) {
            for (final TransactionInfo info : infos) {
                final Block txBlock = blockStore.getBlockByHash(info.getBlockHash());
                if (txBlock.isEqual(blockStore.getChainBlockByNumber(txBlock.getNumber()))) {
                    // transaction included to the block on main chain
                    info.getReceipt().setTransaction(tx);
                    fireTxUpdate(info.getReceipt(), INCLUDED, txBlock);
                    return;
                }
            }
        }
        addPendingTransaction(tx);
    }

    private void fireTxUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("PendingTransactionUpdate: (Tot: %3s) %12s : %s %8s %s [%s]",
                    getPendingTransactions().size(),
                    state, Hex.toHexString(txReceipt.getTransaction().getSender()).substring(0, 8),
                    ByteUtil.byteArrayToLong(txReceipt.getTransaction().getNonce()),
                    block.getShortDescr(), txReceipt.getError()));
        }
        listener.onPendingTransactionUpdate(txReceipt, state, block);
    }

    /**
     * Executes pending tx on the latest best block
     * Fires pending state update
     * @param tx    Transaction
     * @return True if transaction gets NEW_PENDING state, False if DROPPED
     */
    private boolean addPendingTransactionImpl(final Transaction tx) {
        final TransactionReceipt newReceipt = new TransactionReceipt();
        newReceipt.setTransaction(tx);

        final String err = validate(tx);

        final TransactionReceipt txReceipt;
        if (err != null) {
            txReceipt = createDroppedReceipt(tx, err);
        } else {
            txReceipt = executeTx(tx);
        }

        if (!txReceipt.isValid()) {
            fireTxUpdate(txReceipt, DROPPED, getBestBlock());
        } else {
            pendingTransactions.add(new PendingTransaction(tx, getBestBlock().getNumber()));
            fireTxUpdate(txReceipt, NEW_PENDING, getBestBlock());
        }
        return txReceipt.isValid();
    }

    private TransactionReceipt createDroppedReceipt(final Transaction tx, final String error) {
        final TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setTransaction(tx);
        txReceipt.setError(error);
        return txReceipt;
    }

    // validations which are not performed within executeTx
    private String validate(final Transaction tx) {
        try {
            tx.verify();
        } catch (final Exception e) {
            return String.format("Invalid transaction: %s", e.getMessage());
        }

        if (config.getMineMinGasPrice().compareTo(ByteUtil.bytesToBigInteger(tx.getGasPrice())) > 0) {
            return "Too low gas price for transaction: " + ByteUtil.bytesToBigInteger(tx.getGasPrice());
        }

        return null;
    }

    private Block findCommonAncestor(Block b1, Block b2) {
        while(!b1.isEqual(b2)) {
            if (b1.getNumber() >= b2.getNumber()) {
                b1 = blockchain.getBlockByHash(b1.getParentHash());
            }

            if (b1.getNumber() < b2.getNumber()) {
                b2 = blockchain.getBlockByHash(b2.getParentHash());
            }
            if (b1 == null || b2 == null) {
                // shouldn't happen
                throw new RuntimeException("Pending state can't find common ancestor: one of blocks has a gap");
            }
        }
        return b1;
    }

    @Override
//    public synchronized void processBest(final Block newBlock, final List<TransactionReceipt> receipts) {
    public synchronized void processBest(@NotNull final Block newBlock, @NotNull final List<? extends TransactionReceipt> receipts) {

        if (getBestBlock() != null && !getBestBlock().isParentOf(newBlock)) {
            // need to switch the state to another fork

            final Block commonAncestor = findCommonAncestor(getBestBlock(), newBlock);

            if (logger.isDebugEnabled()) logger.debug("New best block from another fork: "
                    + newBlock.getShortDescr() + ", old best: " + getBestBlock().getShortDescr()
                    + ", ancestor: " + commonAncestor.getShortDescr());

            // first return back the transactions from forked blocks
            Block rollback = getBestBlock();
            while(!rollback.isEqual(commonAncestor)) {
                final List<PendingTransaction> blockTxs = new ArrayList<>();
                for (final Transaction tx : rollback.getTransactionsList()) {
                    logger.trace("Returning transaction back to pending: " + tx);
                    blockTxs.add(new PendingTransaction(tx, commonAncestor.getNumber()));
                }
                pendingTransactions.addAll(0, blockTxs);
                rollback = blockchain.getBlockByHash(rollback.getParentHash());
            }

            // rollback the state snapshot to the ancestor
            pendingState = getOrigRepository().getSnapshotTo(commonAncestor.getStateRoot()).startTracking();

            // next process blocks from new fork
            Block main = newBlock;
            final List<Block> mainFork = new ArrayList<>();
            while(!main.isEqual(commonAncestor)) {
                mainFork.add(main);
                main = blockchain.getBlockByHash(main.getParentHash());
            }

            // processing blocks from ancestor to new block
            for (int i = mainFork.size() - 1; i >= 0; i--) {
                processBestInternal(mainFork.get(i), null);
            }
        } else {
            logger.debug("PendingStateImpl.processBest: " + newBlock.getShortDescr());
            processBestInternal(newBlock, (List<TransactionReceipt>) receipts);
        }

        best = newBlock;

        updateState(newBlock);

        listener.onPendingStateChanged(PendingStateImpl.this);
    }

    private void processBestInternal(final Block block, final List<TransactionReceipt> receipts) {

        clearPending(block, receipts);

        clearOutdated(block.getNumber());
    }

    private void clearOutdated(final long blockNumber) {
        final List<PendingTransaction> outdated = new ArrayList<>();

        for (final PendingTransaction tx : pendingTransactions) {
            if (blockNumber - tx.getBlockNumber() > config.txOutdatedThreshold()) {
                outdated.add(tx);

                fireTxUpdate(createDroppedReceipt(tx.getTransaction(),
                        "Tx was not included into last " + config.txOutdatedThreshold() + " blocks"),
                        DROPPED, getBestBlock());
            }
        }

        if (outdated.isEmpty()) return;

        if (logger.isDebugEnabled())
            for (final PendingTransaction tx : outdated)
                logger.trace(
                        "Clear outdated pending transaction, block.number: [{}] hash: [{}]",
                        tx.getBlockNumber(),
                        Hex.toHexString(tx.getHash())
                );

        pendingTransactions.removeAll(outdated);
    }

    private void clearPending(final Block block, final List<TransactionReceipt> receipts) {
        for (int i = 0; i < block.getTransactionsList().size(); i++) {
            final Transaction tx = block.getTransactionsList().get(i);
            final PendingTransaction pend = new PendingTransaction(tx);

            if (pendingTransactions.remove(pend)) {
                try {
                    logger.trace("Clear pending transaction, hash: [{}]", Hex.toHexString(tx.getHash()));
                    final TransactionReceipt receipt;
                    if (receipts != null) {
                        receipt = receipts.get(i);
                    } else {
                        final TransactionInfo info = getTransactionInfo(tx.getHash(), block.getHash());
                        receipt = info.getReceipt();
                    }
                    fireTxUpdate(receipt, INCLUDED, block);
                } catch (final Exception e) {
                    logger.error("Exception creating onPendingTransactionUpdate (block: " + block.getShortDescr() + ", tx: " + i, e);
                }
            }
        }
    }

    private TransactionInfo getTransactionInfo(final byte[] txHash, final byte[] blockHash) {
        final TransactionInfo info = transactionStore.get(txHash, blockHash);
        final Transaction tx = blockchain.getBlockByHash(info.getBlockHash()).getTransactionsList().get(info.getIndex());
        info.getReceipt().setTransaction(tx);
        return info;
    }

    private void updateState(final Block block) {

        pendingState = getOrigRepository().startTracking();

        for (final PendingTransaction tx : pendingTransactions) {
            final TransactionReceipt receipt = executeTx(tx.getTransaction());
            fireTxUpdate(receipt, PENDING, block);
        }
    }

    private TransactionReceipt executeTx(final Transaction tx) {

        logger.trace("Apply pending state tx: {}", Hex.toHexString(tx.getHash()));

        final Block best = getBestBlock();

        final TransactionExecutor executor = new TransactionExecutor(
                tx, best.getCoinbase(), getRepository(),
                blockStore, programInvokeFactory, createFakePendingBlock(), new EthereumListenerAdapter(), 0)
                .withCommonConfig(commonConfig);

        executor.init();
        executor.execute();
        executor.go();
        executor.finalization();

        return executor.getReceipt();
    }

    private Block createFakePendingBlock() {
        // creating fake lightweight calculated block with no hashes calculations
        final Block block = new Block(best.getHash(),
                BlockchainImpl.EMPTY_LIST_HASH, // uncleHash
                new byte[32],
                new byte[32], // log bloom - from tx receipts
                new byte[0], // difficulty computed right after block creation
                best.getNumber() + 1,
                ByteUtil.longToBytesNoLeadZeroes(Long.MAX_VALUE), // max Gas Limit
                0,  // gas used
                best.getTimestamp() + 1,  // block time
                new byte[0],  // extra data
                new byte[0],  // mixHash (to mine)
                new byte[0],  // nonce   (to mine)
                new byte[32],  // receiptsRoot
                new byte[32],    // TransactionsRoot
                new byte[32], // stateRoot
                Collections.emptyList(), // tx list
                Collections.emptyList());  // uncle list
        return block;
    }

    public void setBlockchain(final BlockchainImpl blockchain) {
        this.blockchain = blockchain;
    }

//    @NotNull
//    @Override
//    public List<Transaction> addPendingTransactions(@NotNull List<? extends Transaction> transactions) {
//        return null;
//    }
//
//    @Override
//    public void processBest(@NotNull Block block, @NotNull List<? extends TransactionReceipt> receipts) {
//
//    }

    public static class TransactionSortedSet extends TreeSet<Transaction> {
        public TransactionSortedSet() {
            super((tx1, tx2) -> {
                final long nonceDiff = ByteUtil.byteArrayToLong(tx1.getNonce()) -
                        ByteUtil.byteArrayToLong(tx2.getNonce());
                if (nonceDiff != 0) {
                    return nonceDiff > 0 ? 1 : -1;
                }
                return FastByteComparisons.compareTo(tx1.getHash(), 0, 32, tx2.getHash(), 0, 32);
            });
        }
    }
}
