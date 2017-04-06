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

package org.ethereum.facade;

import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.core.PendingState;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.listener.GasPriceTracker;
import org.ethereum.manager.AdminInfo;
import org.ethereum.manager.BlockLoader;
import org.ethereum.manager.WorldManager;
import org.ethereum.mine.BlockMiner;
import org.ethereum.net.client.PeerClient;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.shh.Whisper;
import org.ethereum.net.submit.TransactionExecutor;
import org.ethereum.net.submit.TransactionTask;
import org.ethereum.sync.SyncManager;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FutureAdapter;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Roman Mandeleil
 * @since 27.07.2014
 */
@Component
public class EthereumImpl implements Ethereum, SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger("facade");
    private static final Logger gLogger = LoggerFactory.getLogger("general");
    private final SystemProperties config;
    private final GasPriceTracker gasPriceTracker = new GasPriceTracker();
    @Autowired
    private
    WorldManager worldManager;
    @Autowired
    private
    AdminInfo adminInfo;
    @Autowired
    private
    ChannelManager channelManager;
    @Autowired
    private
    ApplicationContext ctx;
    @Autowired
    private
    BlockLoader blockLoader;
    @Autowired
    private
    ProgramInvokeFactory programInvokeFactory;
    @Autowired
    private
    Whisper whisper;
    @Autowired
    private
    PendingState pendingState;
    @Autowired
    private
    SyncManager syncManager;
    @Autowired
    private
    CommonConfig commonConfig = CommonConfig.getDefault();

    @Autowired
    public EthereumImpl(final SystemProperties config, final CompositeEthereumListener compositeEthereumListener) {
        final CompositeEthereumListener compositeEthereumListener1 = compositeEthereumListener;
        this.config = config;
        System.out.println();
        compositeEthereumListener1.addListener(gasPriceTracker);
        gLogger.info("EthereumJ node started: enode://" + Hex.toHexString(config.nodeId()) + "@" + config.externalIp() + ":" + config.listenPort());
    }

    @Override
    public void startPeerDiscovery() {
        worldManager.startPeerDiscovery();
    }

    @Override
    public void stopPeerDiscovery() {
        worldManager.stopPeerDiscovery();
    }

    @Override
    public void connect(final InetAddress addr, final int port, final String remoteId) {
        connect(addr.getHostName(), port, remoteId);
    }

    @Override
    public void connect(final String ip, final int port, final String remoteId) {
        logger.debug("Connecting to: {}:{}", ip, port);
        worldManager.getActivePeer().connectAsync(ip, port, remoteId, false);
    }

    @Override
    public void connect(final Node node) {
        connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
    }

    @Override
    public org.ethereum.facade.Blockchain getBlockchain() {
        return (org.ethereum.facade.Blockchain) worldManager.getBlockchain();
    }

    public ImportResult addNewMinedBlock(final Block block) {
        final ImportResult importResult = worldManager.getBlockchain().tryToConnect(block);
        if (importResult == ImportResult.IMPORTED_BEST) {
            channelManager.sendNewBlock(block);
        }
        return importResult;
    }

    @Override
    public BlockMiner getBlockMiner() {
        return ctx.getBean(BlockMiner.class);
    }

    @Override
    public void addListener(final EthereumListener listener) {
        worldManager.addListener(listener);
    }

    @Override
    public void close() {
        logger.info("### Shutdown initiated ### ");
        ((AbstractApplicationContext) getApplicationContext()).close();
    }

    @Override
    public SyncStatus getSyncStatus() {
        return syncManager.getSyncStatus();
    }

    @Override
    public PeerClient getDefaultPeer() {
        return worldManager.getActivePeer();
    }

    @Override
    public boolean isConnected() {
        return worldManager.getActivePeer() != null;
    }

    @Override
    public Transaction createTransaction(final BigInteger nonce,
                                         final BigInteger gasPrice,
                                         final BigInteger gas,
                                         final byte[] receiveAddress,
                                         final BigInteger value, final byte[] data) {

        final byte[] nonceBytes = ByteUtil.bigIntegerToBytes(nonce);
        final byte[] gasPriceBytes = ByteUtil.bigIntegerToBytes(gasPrice);
        final byte[] gasBytes = ByteUtil.bigIntegerToBytes(gas);
        final byte[] valueBytes = ByteUtil.bigIntegerToBytes(value);

        return new Transaction(nonceBytes, gasPriceBytes, gasBytes,
                receiveAddress, valueBytes, data, getChainIdForNextBlock());
    }


    @Override
    public Future<Transaction> submitTransaction(final Transaction transaction) {

        final TransactionTask transactionTask = new TransactionTask(transaction, channelManager);

        final Future<List<Transaction>> listFuture =
                TransactionExecutor.instance.submitTransaction(transactionTask);

        pendingState.addPendingTransaction(transaction);

        return new FutureAdapter<Transaction, List<Transaction>>(listFuture) {
            @Override
            protected Transaction adapt(final List<Transaction> adapteeResult) throws ExecutionException {
                return adapteeResult.get(0);
            }
        };
    }

    @Override
    public TransactionReceipt callConstant(final Transaction tx, final Block block) {
        if (tx.getSignature() == null) {
            tx.sign(ECKey.fromPrivate(new byte[32]));
        }
        return callConstantImpl(tx, block).getReceipt();
    }

    public BlockSummary replayBlock(final Block block) {
        final List<TransactionReceipt> receipts = new ArrayList<>();
        final List<TransactionExecutionSummary> summaries = new ArrayList<>();

        final Repository repository = ((Repository) worldManager.getRepository())
                .getSnapshotTo(block.getStateRoot())
                .startTracking();

        try {
            for (final Transaction tx : block.getTransactionsList()) {
                final org.ethereum.core.TransactionExecutor executor = new org.ethereum.core.TransactionExecutor(
                        tx, block.getCoinbase(), repository, worldManager.getBlockStore(),
                        programInvokeFactory, block, worldManager.getListener(), 0)
                        .withCommonConfig(commonConfig);

                executor.setLocalCall(true);
                executor.init();
                executor.execute();
                executor.go();

                final TransactionExecutionSummary summary = executor.finalization();
                final TransactionReceipt receipt = executor.getReceipt();
                // TODO: change to repository.getRoot() after RepositoryTrack implementation
                receipt.setPostTxState(ArrayUtils.EMPTY_BYTE_ARRAY);
                receipts.add(receipt);
                summaries.add(summary);
            }
        } finally {
            repository.rollback();
        }

        return new BlockSummary(block, new HashMap<>(), receipts, summaries);
    }

    private org.ethereum.core.TransactionExecutor callConstantImpl(final Transaction tx, final Block block) {

        final Repository repository = ((Repository) worldManager.getRepository())
                .getSnapshotTo(block.getStateRoot())
                .startTracking();

        try {
            final org.ethereum.core.TransactionExecutor executor = new org.ethereum.core.TransactionExecutor
                    (tx, block.getCoinbase(), repository, worldManager.getBlockStore(),
                            programInvokeFactory, block, new EthereumListenerAdapter(), 0)
                    .withCommonConfig(commonConfig)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return executor;
        } finally {
            repository.rollback();
        }
    }

    @Override
    public ProgramResult callConstantFunction(final String receiveAddress,
                                              final CallTransaction.Function function, final Object... funcArgs) {
        return callConstantFunction(receiveAddress, ECKey.fromPrivate(new byte[32]), function, funcArgs);
    }

    @Override
    public ProgramResult callConstantFunction(final String receiveAddress, final ECKey senderPrivateKey,
                                              final CallTransaction.Function function, final Object... funcArgs) {
        final Transaction tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L,
                receiveAddress, 0, function, funcArgs);
        tx.sign(senderPrivateKey);
        final Block bestBlock = worldManager.getBlockchain().getBestBlock();

        return callConstantImpl(tx, bestBlock).getResult();
    }

    @Override
    public org.ethereum.facade.Repository getRepository() {
        return worldManager.getRepository();
    }

    @Override
    public org.ethereum.facade.Repository getLastRepositorySnapshot() {
        return getSnapshotTo(getBlockchain().getBestBlock().getStateRoot());
    }

    @Override
    public org.ethereum.facade.Repository getPendingState() {
        return worldManager.getPendingState().getRepository();
    }

    @Override
    public org.ethereum.facade.Repository getSnapshotTo(final byte[] root) {

        final Repository repository = (Repository) worldManager.getRepository();
        final org.ethereum.facade.Repository snapshot = repository.getSnapshotTo(root);

        return snapshot;
    }

    @Override
    public AdminInfo getAdminInfo() {
        return adminInfo;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }


    @Override
    public List<Transaction> getWireTransactions() {
        return worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public List<Transaction> getPendingStateTransactions() {
        return worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public BlockLoader getBlockLoader() {
        return blockLoader;
    }

    @Override
    public Whisper getWhisper() {
        return whisper;
    }

    @Override
    public long getGasPrice() {
        return gasPriceTracker.getGasPrice();
    }

    @Override
    public Integer getChainIdForNextBlock() {
        final BlockchainConfig nextBlockConfig = config.getBlockchainConfig().getConfigForBlock(getBlockchain()
                .getBestBlock().getNumber() + 1);
        return nextBlockConfig.getChainId();
    }

    @Override
    public void exitOn(final long number) {
        worldManager.getBlockchain().setExitOn(number);
    }

    @Override
    public void initSyncing() {
        worldManager.initSyncing();
    }


    /**
     * For testing purposes and 'hackers'
     */
    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    /**
     * Shutting down all app beans
     */
    @Override
    public void stop(final Runnable callback) {
        logger.info("Shutting down Ethereum instance...");
        worldManager.close();
        callback.run();
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isRunning() {
        return true;
    }

    /**
     * Called first on shutdown lifecycle
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
