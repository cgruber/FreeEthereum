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

package org.ethereum.util.blockchain;

import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.FrontierConfig;
import org.ethereum.core.*;
import org.ethereum.core.genesis.GenesisLoader;
import org.ethereum.crypto.ECKey;
import org.ethereum.datasource.CountingBytesSource;
import org.ethereum.datasource.JournalSource;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.PruneManager;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.mine.Ethash;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.CompilationResult.ContractMetadata;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.sync.SyncManager;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.validator.DependentBlockHeaderRuleAdapter;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.iq80.leveldb.DBException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static org.ethereum.util.ByteUtil.wrap;

public class StandaloneBlockchain implements LocalBlockchain {

    private final List<PendingTx> submittedTxes = new CopyOnWriteArrayList<>();
    private Genesis genesis;
    private byte[] coinbase;
    private BlockchainImpl blockchain;
    private PendingStateImpl pendingState;
    private CompositeEthereumListener listener;
    private ECKey txSender;
    private long gasPrice;
    private long gasLimit;
    private boolean autoBlock;
    private long dbDelay = 0;
    private long totalDbHits = 0;
    private BlockchainNetConfig netConfig;
    private int blockGasIncreasePercent = 0;
    private long time = 0;
    private JournalSource<byte[]> pruningStateDS;
    private HashMapDB<byte[]> stateDS;
    private BlockSummary lastSummary;

    public StandaloneBlockchain() {
        final Genesis genesis = GenesisLoader.INSTANCE.loadGenesis(
                getClass().getResourceAsStream("/genesis/genesis-light-sb.json"));

        withGenesis(genesis);
        withGasPrice(50_000_000_000L);
        withGasLimit(5_000_000L);
        withMinerCoinbase(Hex.decode("ffffffffffffffffffffffffffffffffffffffff"));
        setSender(ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c")));
//        withAccountBalance(txSender.getAddress(), new BigInteger("100000000000000000000000000"));
    }

    // Override blockchain net config for fast mining
    public static FrontierConfig getEasyMiningConfig() {
        return new FrontierConfig(new FrontierConfig.FrontierConstants() {
            @Override
            public BigInteger getMinimumDifficulty() {
                return BigInteger.ONE;
            }
        });
    }

    public StandaloneBlockchain withGenesis(final Genesis genesis) {
        this.genesis = genesis;
        return this;
    }

    private StandaloneBlockchain withMinerCoinbase(final byte[] coinbase) {
        this.coinbase = coinbase;
        return this;
    }

    public StandaloneBlockchain withNetConfig(final BlockchainNetConfig netConfig) {
        this.netConfig = netConfig;
        return this;
    }

    public StandaloneBlockchain withAccountBalance(final byte[] address, final BigInteger weis) {
        final AccountState state = new AccountState(BigInteger.ZERO, weis);
        genesis.addPremine(wrap(address), state);
        genesis.setStateRoot(GenesisLoader.INSTANCE.generateRootHash(genesis.getPremine()));

        return this;
    }


    public StandaloneBlockchain withGasPrice(final long gasPrice) {
        this.gasPrice = gasPrice;
        return this;
    }

    public StandaloneBlockchain withGasLimit(final long gasLimit) {
        this.gasLimit = gasLimit;
        return this;
    }

    public StandaloneBlockchain withAutoblock(final boolean autoblock) {
        this.autoBlock = autoblock;
        return this;
    }

    public StandaloneBlockchain withCurrentTime(final Date date) {
        this.time = date.getTime() / 1000;
        return this;
    }

    /**
     * [-100, 100]
     * 0 - the same block gas limit as parent
     * 100 - max available increase from parent gas limit
     * -100 - max available decrease from parent gas limit
     */
    public StandaloneBlockchain withBlockGasIncrease(final int blockGasIncreasePercent) {
        this.blockGasIncreasePercent = blockGasIncreasePercent;
        return this;
    }

    public StandaloneBlockchain withDbDelay(final long dbDelay) {
        this.dbDelay = dbDelay;
        return this;
    }

    private Map<PendingTx, Transaction> createTransactions(final Block parent) {
        final Map<PendingTx, Transaction> txes = new LinkedHashMap<>();
        final Map<ByteArrayWrapper, Long> nonces = new HashMap<>();
        final Repository repoSnapshot = getBlockchain().getRepository().getSnapshotTo(parent.getStateRoot());
        for (final PendingTx tx : submittedTxes) {
            final Transaction transaction;
            if (tx.customTx == null) {
                final ByteArrayWrapper senderW = new ByteArrayWrapper(tx.sender.getAddress());
                Long nonce = nonces.get(senderW);
                if (nonce == null) {
                    final BigInteger bcNonce = repoSnapshot.getNonce(tx.sender.getAddress());
                    nonce = bcNonce.longValue();
                }
                nonces.put(senderW, nonce + 1);

                final byte[] toAddress = tx.targetContract != null ? tx.targetContract.getAddress() : tx.toAddress;

                transaction = createTransaction(tx.sender, nonce, toAddress, tx.value, tx.data);

                if (tx.createdContract != null) {
                    tx.createdContract.setAddress(transaction.getContractAddress());
                }
            } else {
                transaction = tx.customTx;
            }

            txes.put(tx, transaction);
        }
        return txes;
    }

    public PendingStateImpl getPendingState() {
        return pendingState;
    }

    public void generatePendingTransactions() {
        pendingState.addPendingTransactions(new ArrayList<>(createTransactions(getBlockchain().getBestBlock()).values()));
    }

    @Override
    public Block createBlock() {
        return createForkBlock(getBlockchain().getBestBlock());
    }

    @Override
    public Block createForkBlock(final Block parent) {
        try {
            final Map<PendingTx, Transaction> txes = createTransactions(parent);

            long timeIncrement = 13;
            time += timeIncrement;
            final Block b = getBlockchain().createNewBlock(parent, new ArrayList<>(txes.values()), Collections.emptyList(), time);

            final int GAS_LIMIT_BOUND_DIVISOR = SystemProperties.getDefault().getBlockchainConfig().
                    getCommonConstants().getGasLimitBoundDivisor();
            final BigInteger newGas = ByteUtil.bytesToBigInteger(parent.getGasLimit())
                    .multiply(BigInteger.valueOf(GAS_LIMIT_BOUND_DIVISOR * 100 + blockGasIncreasePercent))
                    .divide(BigInteger.valueOf(GAS_LIMIT_BOUND_DIVISOR * 100));
            b.getHeader().setGasLimit(ByteUtil.bigIntegerToBytes(newGas));

            Ethash.Companion.getForBlock(SystemProperties.getDefault(), b.getNumber()).mineLight(b).get();
            final ImportResult importResult = getBlockchain().tryToConnect(b);
            if (importResult != ImportResult.IMPORTED_BEST && importResult != ImportResult.IMPORTED_NOT_BEST) {
                throw new RuntimeException("Invalid block import result " + importResult + " for block " + b);
            }

            final List<PendingTx> pendingTxes = new ArrayList<>(txes.keySet());
            for (int i = 0; i < lastSummary.getReceipts().size(); i++) {
                pendingTxes.get(i).txResult.receipt = lastSummary.getReceipts().get(i);
                pendingTxes.get(i).txResult.executionSummary = getTxSummary(lastSummary, i);
            }

            submittedTxes.clear();
            return b;
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private TransactionExecutionSummary getTxSummary(final BlockSummary bs, final int idx) {
        final TransactionReceipt txReceipt = bs.getReceipts().get(idx);
        for (final TransactionExecutionSummary summary : bs.getSummaries()) {
            if (FastByteComparisons.equal(txReceipt.getTransaction().getHash(), summary.getTransaction().getHash())) {
                return summary;
            }
        }
        return null;
    }

    public Transaction createTransaction(final long nonce, final byte[] toAddress, final long value, final byte[] data) {
        return createTransaction(getSender(), nonce, toAddress, BigInteger.valueOf(value), data);
    }

    public Transaction createTransaction(final ECKey sender, final long nonce, final byte[] toAddress, final BigInteger value, final byte[] data) {
        final Transaction transaction = new Transaction(ByteUtil.longToBytesNoLeadZeroes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(gasPrice),
                ByteUtil.longToBytesNoLeadZeroes(gasLimit),
                toAddress, ByteUtil.bigIntegerToBytes(value),
                data,
                null);
        transaction.sign(sender);
        return transaction;
    }

    public void resetSubmittedTransactions() {
        submittedTxes.clear();
    }

    public ECKey getSender() {
        return txSender;
    }

    @Override
    public void setSender(final ECKey senderPrivateKey) {
        txSender = senderPrivateKey;
//        if (!getBlockchain().getRepository().isExist(senderPrivateKey.getAddress())) {
//            Repository repository = getBlockchain().getRepository();
//            Repository track = repository.startTracking();
//            track.createAccount(senderPrivateKey.getAddress());
//            track.commit();
//        }
    }

    @Override
    public void sendEther(final byte[] toAddress, final BigInteger weis) {
        submitNewTx(new PendingTx(toAddress, weis, new byte[0]));
    }

    public void submitTransaction(final Transaction tx) {
        submitNewTx(new PendingTx(tx));
    }

    @Override
    public SolidityContract submitNewContract(final String soliditySrc, final Object... constructorArgs) {
        return submitNewContract(soliditySrc, null, constructorArgs);
    }

    @Override
    public SolidityContract submitNewContract(final String soliditySrc, final String contractName, final Object... constructorArgs) {
        final SolidityContractImpl contract = createContract(soliditySrc, contractName);
        return submitNewContract(contract, constructorArgs);
    }

    @Override
    public SolidityContract submitNewContractFromJson(final String json, final Object... constructorArgs) {
        return submitNewContractFromJson(json, null, constructorArgs);
    }

    @Override
    public SolidityContract submitNewContractFromJson(final String json, final String contractName, final Object... constructorArgs) {
        final SolidityContractImpl contract;
        try {
			contract = createContractFromJson(contractName, json);
			return submitNewContract(contract, constructorArgs);
        } catch (final IOException e) {
            throw new RuntimeException(e);
		}
    }

    @Override
    public SolidityContract submitNewContract(final ContractMetadata contractMetaData, final Object... constructorArgs) {
        final SolidityContractImpl contract = new SolidityContractImpl(contractMetaData);
        return submitNewContract(contract, constructorArgs);
	}

    private SolidityContract submitNewContract(final SolidityContractImpl contract, final Object... constructorArgs) {
        final CallTransaction.Function constructor = contract.contract.getConstructor();
        if (constructor == null && constructorArgs.length > 0) {
			throw new RuntimeException("No constructor with params found");
		}
        final byte[] argsEncoded = constructor == null ? new byte[0] : constructor.encodeArguments(constructorArgs);
        submitNewTx(new PendingTx(new byte[0], BigInteger.ZERO,
				ByteUtil.merge(Hex.decode(contract.getBinary()), argsEncoded), contract, null,
				new TransactionResult()));
		return contract;
	}

    private SolidityContractImpl createContract(final String soliditySrc, final String contractName) {
        try {
            final SolidityCompiler.Result compileRes = SolidityCompiler.compile(soliditySrc.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            if (compileRes.isFailed()) throw new RuntimeException("Compile result: " + compileRes.errors);
			return createContractFromJson(contractName, compileRes.output);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SolidityContractImpl createContractFromJson(String contractName, final String json) throws IOException {
        final CompilationResult result = CompilationResult.parse(json);
        if (contractName == null) {
		    if (result.contracts.size() > 1) {
		        throw new RuntimeException("Source contains more than 1 contact (" + result.contracts.keySet() + "). Please specify the contract name");
		    } else {
		        contractName = result.contracts.keySet().iterator().next();
		    }
		}

		return createContract(contractName, result);
	}

	/**
	 * @param contractName
	 * @param result
	 * @return
	 */
    private SolidityContractImpl createContract(final String contractName, final CompilationResult result) {
        final ContractMetadata cMetaData = result.contracts.get(contractName);
        final SolidityContractImpl contract = createContract(cMetaData);

        for (final CompilationResult.ContractMetadata metadata : result.contracts.values()) {
            contract.addRelatedContract(metadata.abi);
		}
		return contract;
	}

    private SolidityContractImpl createContract(final ContractMetadata contractData) {
        final SolidityContractImpl contract = new SolidityContractImpl(contractData);
        return contract;
	}

    @Override
    public SolidityContract createExistingContractFromSrc(final String soliditySrc, final String contractName, final byte[] contractAddress) {
        final SolidityContractImpl contract = createContract(soliditySrc, contractName);
        contract.setAddress(contractAddress);
        return contract;
    }

    @Override
    public SolidityContract createExistingContractFromSrc(final String soliditySrc, final byte[] contractAddress) {
        return createExistingContractFromSrc(soliditySrc, null, contractAddress);
    }

    @Override
    public SolidityContract createExistingContractFromABI(final String ABI, final byte[] contractAddress) {
        final SolidityContractImpl contract = new SolidityContractImpl(ABI);
        contract.setAddress(contractAddress);
        return contract;
    }

    @Override
    public BlockchainImpl getBlockchain() {
        if (blockchain == null) {
            blockchain = createBlockchain(genesis);
            blockchain.setMinerCoinbase(coinbase);
            addEthereumListener(new EthereumListenerAdapter() {
                @Override
                public void onBlock(final BlockSummary blockSummary) {
                    lastSummary = blockSummary;
                }
            });
        }
        return blockchain;
    }

    public void addEthereumListener(final EthereumListener listener) {
        getBlockchain();
        this.listener.addListener(listener);
    }

    private void submitNewTx(final PendingTx tx) {
        getBlockchain();
        submittedTxes.add(tx);
        if (autoBlock) {
            createBlock();
        }
    }

    public HashMapDB<byte[]> getStateDS() {
        return stateDS;
    }

    public Source<byte[], byte[]> getPruningStateDS() {
        return pruningStateDS;
    }

    public long getTotalDbHits() {
        return totalDbHits;
    }

    private BlockchainImpl createBlockchain(final Genesis genesis) {
        SystemProperties.getDefault().setBlockchainConfig(netConfig != null ? netConfig : getEasyMiningConfig());

        final IndexedBlockStore blockStore = new IndexedBlockStore();
        blockStore.init(new HashMapDB<>(), new HashMapDB<>());

        stateDS = new HashMapDB<>();
        pruningStateDS = new JournalSource<>(new CountingBytesSource(stateDS));
        PruneManager pruneManager = new PruneManager(blockStore, pruningStateDS, SystemProperties.getDefault().databasePruneDepth());

        final RepositoryRoot repository = new RepositoryRoot(pruningStateDS);

        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();
        listener = new CompositeEthereumListener();

        final BlockchainImpl blockchain = new BlockchainImpl(blockStore, repository)
                .withEthereumListener(listener)
                .withSyncManager(new SyncManager());
        blockchain.setParentHeaderValidator(new DependentBlockHeaderRuleAdapter());
        blockchain.setProgramInvokeFactory(programInvokeFactory);
        blockchain.setPruneManager(pruneManager);

        blockchain.byTest = true;

        pendingState = new PendingStateImpl(listener, blockchain);

        pendingState.setBlockchain(blockchain);
        blockchain.setPendingState(pendingState);

        Genesis.populateRepository(repository, genesis);

        repository.commit();

        blockStore.saveBlock(genesis, genesis.getCumulativeDifficulty(), true);

        blockchain.setBestBlock(genesis);
        blockchain.setTotalDifficulty(genesis.getCumulativeDifficulty());

        pruneManager.blockCommitted(genesis.getHeader());

        return blockchain;
    }

    class PendingTx {
        ECKey sender;
        byte[] toAddress;
        BigInteger value;
        byte[] data;

        SolidityContractImpl createdContract;
        SolidityContractImpl targetContract;

        Transaction customTx;

        TransactionResult txResult = new TransactionResult();

        public PendingTx(final byte[] toAddress, final BigInteger value, final byte[] data) {
            this.sender = txSender;
            this.toAddress = toAddress;
            this.value = value;
            this.data = data;
        }

        public PendingTx(final byte[] toAddress, final BigInteger value, final byte[] data,
                         final SolidityContractImpl createdContract, final SolidityContractImpl targetContract, final TransactionResult res) {
            this.sender = txSender;
            this.toAddress = toAddress;
            this.value = value;
            this.data = data;
            this.createdContract = createdContract;
            this.targetContract = targetContract;
            this.txResult = res;
        }

        public PendingTx(final Transaction customTx) {
            this.customTx = customTx;
        }
    }

    public class SolidityFunctionImpl implements SolidityFunction {
        final SolidityContractImpl contract;
        final CallTransaction.Function abi;

        public SolidityFunctionImpl(final SolidityContractImpl contract, final CallTransaction.Function abi) {
            this.contract = contract;
            this.abi = abi;
        }

        @Override
        public SolidityContract getContract() {
            return contract;
        }

        @Override
        public CallTransaction.Function getInterface() {
            return abi;
        }
    }

    public class SolidityContractImpl implements SolidityContract {
        public final CallTransaction.Contract contract;
        public final List<CallTransaction.Contract> relatedContracts = new ArrayList<>();
        public CompilationResult.ContractMetadata compiled;
        byte[] address;

        public SolidityContractImpl(final String abi) {
            contract = new CallTransaction.Contract(abi);
        }

        public SolidityContractImpl(final CompilationResult.ContractMetadata result) {
            this(result.abi);
            compiled = result;
        }

        public void addRelatedContract(final String abi) {
            final CallTransaction.Contract c = new CallTransaction.Contract(abi);
            relatedContracts.add(c);
        }

        @Override
        public byte[] getAddress() {
            if (address == null) {
                throw new RuntimeException("Contract address will be assigned only after block inclusion. Call createBlock() first.");
            }
            return address;
        }

        void setAddress(final byte[] address) {
            this.address = address;
        }

        @Override
        public SolidityCallResult callFunction(final String functionName, final Object... args) {
            return callFunction(0, functionName, args);
        }

        @Override
        public SolidityCallResult callFunction(final long value, final String functionName, final Object... args) {
            final CallTransaction.Function function = contract.getByName(functionName);
            final byte[] data = function.encode(convertArgs(args));
            final SolidityCallResult res = new SolidityCallResultImpl(this, function);
            submitNewTx(new PendingTx(null, BigInteger.valueOf(value), data, null, this, res));
            return res;
        }

        @Override
        public Object[] callConstFunction(final String functionName, final Object... args) {
            return callConstFunction(getBlockchain().getBestBlock(), functionName, args);
        }

        @Override
        public Object[] callConstFunction(final Block callBlock, final String functionName, final Object... args) {

            final CallTransaction.Function func = contract.getByName(functionName);
            if (func == null) throw new RuntimeException("No function with name '" + functionName + "'");
            final Transaction tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L,
                    Hex.toHexString(getAddress()), 0, func, convertArgs(args));
            tx.sign(new byte[32]);

            final Repository repository = getBlockchain().getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking();

            try {
                final org.ethereum.core.TransactionExecutor executor = new org.ethereum.core.TransactionExecutor
                        (tx, callBlock.getCoinbase(), repository, getBlockchain().getBlockStore(),
                                getBlockchain().getProgramInvokeFactory(), callBlock)
                        .setLocalCall(true);

                executor.init();
                executor.execute();
                executor.go();
                executor.finalization();

                return func.decodeResult(executor.getResult().getHReturn());
            } finally {
                repository.rollback();
            }
        }

        private Object[] convertArgs(final Object[] args) {
            final Object[] ret = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof SolidityFunction) {
                    final SolidityFunction f = (SolidityFunction) args[i];
                    ret[i] = ByteUtil.merge(f.getContract().getAddress(), f.getInterface().encodeSignature());
                } else {
                    ret[i] = args[i];
                }
            }
            return ret;
        }

        @Override
        public SolidityStorage getStorage() {
            return new SolidityStorageImpl(getAddress());
        }

        @Override
        public String getABI() {
            return compiled.abi;
        }

        @Override
        public String getBinary() {
            return compiled.bin;
        }

        @Override
        public void call(final byte[] callData) {
            // for this we need cleaner separation of EasyBlockchain to
            // Abstract and Solidity specific
            throw new UnsupportedOperationException();
        }

        @Override
        public SolidityFunction getFunction(final String name) {
            return new SolidityFunctionImpl(this, contract.getByName(name));
        }
    }

    public class SolidityCallResultImpl extends SolidityCallResult {
        final SolidityContractImpl contract;
        final CallTransaction.Function function;

        SolidityCallResultImpl(final SolidityContractImpl contract, final CallTransaction.Function function) {
            this.contract = contract;
            this.function = function;
        }

        @Override
        public CallTransaction.Function getFunction() {
            return function;
        }

        public List<CallTransaction.Invocation> getEvents() {
            final List<CallTransaction.Invocation> ret = new ArrayList<>();
            for (final LogInfo logInfo : getReceipt().getLogInfoList()) {
                for (final CallTransaction.Contract c : contract.relatedContracts) {
                    final CallTransaction.Invocation event = c.parseEvent(logInfo);
                    if (event != null) ret.add(event);
                }
            }
            return ret;
        }

        @Override
        public String toString() {
            String ret = "SolidityCallResult{" +
                    function + ": " +
                    (isIncluded() ? "EXECUTED" : "PENDING") + ", ";
            if (isIncluded()) {
                ret += isSuccessful() ? "SUCCESS" : ("ERR (" + getReceipt().getError() + ")");
                ret += ", ";
                if (isSuccessful()) {
                    ret += "Ret: " + Arrays.toString(getReturnValues()) + ", ";
                    ret += "Events: " + getEvents() + ", ";
                }
            }
            return ret + "}";
        }
    }

    class SolidityStorageImpl implements SolidityStorage {
        final byte[] contractAddr;

        public SolidityStorageImpl(final byte[] contractAddr) {
            this.contractAddr = contractAddr;
        }

        @Override
        public byte[] getStorageSlot(final long slot) {
            return getStorageSlot(new DataWord(slot).getData());
        }

        @Override
        public byte[] getStorageSlot(final byte[] slot) {
            final DataWord ret = getBlockchain().getRepository().getContractDetails(contractAddr).get(new DataWord(slot));
            return ret.getData();
        }
    }

     class SlowHashMapDB extends HashMapDB<byte[]> {
         private void sleep(final int cnt) {
            totalDbHits += cnt;
            if (dbDelay == 0) return;
            try {
                Thread.sleep(dbDelay * cnt);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized void delete(final byte[] arg0) throws DBException {
            super.delete(arg0);
            sleep(1);
        }

        @Override
        public synchronized byte[] get(final byte[] arg0) throws DBException {
            sleep(1);
            return super.get(arg0);
        }

        @Override
        public synchronized void put(final byte[] key, final byte[] value) throws DBException {
            sleep(1);
            super.put(key, value);
        }

        @Override
        public synchronized void updateBatch(final Map<byte[], byte[]> rows) {
            sleep(rows.size() / 2);
            super.updateBatch(rows);
        }
    }
}
