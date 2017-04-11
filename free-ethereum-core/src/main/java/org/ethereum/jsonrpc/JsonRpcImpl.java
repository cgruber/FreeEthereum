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

package org.ethereum.jsonrpc;

import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.db.TransactionStore;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.manager.WorldManager;
import org.ethereum.mine.BlockMiner;
import org.ethereum.net.client.Capability;
import org.ethereum.net.client.ConfigCapabilities;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.sync.SyncManager;
import org.ethereum.util.BuildInfo;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;
import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.jsonrpc.TypeConverter.*;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.util.ByteUtil.bigIntegerToBytes;

@Component
@Lazy
public class JsonRpcImpl implements JsonRpc {
    private static final Logger logger = LoggerFactory.getLogger("jsonrpc");
    private final BlockchainImpl blockchain;
    private final long initialBlockNumber;
    private final Map<ByteArrayWrapper, Account> accounts = new HashMap<>();
    private final AtomicInteger filterCounter = new AtomicInteger(1);
    private final Map<Integer, Filter> installedFilters = new Hashtable<>();
    private final Map<ByteArrayWrapper, TransactionReceipt> pendingReceipts = Collections.synchronizedMap(new LRUMap<ByteArrayWrapper, TransactionReceipt>(1024));
    @Autowired
    TransactionStore txStore;
    @Autowired
    TransactionStore transactionStore;
    @Autowired
    private WorldManager worldManager;
    @Autowired
    private Repository repository;
    @Autowired
    private
    SystemProperties config;
    @Autowired
    private
    ConfigCapabilities configCapabilities;
    @Autowired
    private
    Ethereum eth;
    @Autowired
    private
    PeerServer peerServer;
    @Autowired
    private
    SyncManager syncManager;
    @Autowired
    private
    ChannelManager channelManager;
    @Autowired
    private
    BlockMiner blockMiner;
    @Autowired
    private
    PendingStateImpl pendingState;
    @Autowired
    private
    SolidityCompiler solidityCompiler;
    @Autowired
    private
    ProgramInvokeFactory programInvokeFactory;
    @Autowired
    private
    CommonConfig commonConfig = CommonConfig.getDefault();
    @Autowired
    public JsonRpcImpl(final BlockchainImpl blockchain, final CompositeEthereumListener compositeEthereumListener) {
        this.blockchain = blockchain;
        final CompositeEthereumListener compositeEthereumListener1 = compositeEthereumListener;
        initialBlockNumber = blockchain.getBestBlock().getNumber();

        compositeEthereumListener.addListener(new EthereumListenerAdapter() {
            @Override
            public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
                for (final Filter filter : installedFilters.values()) {
                    filter.newBlockReceived(block);
                }
            }

            @Override
            public void onPendingTransactionsReceived(final List<Transaction> transactions) {
                for (final Filter filter : installedFilters.values()) {
                    for (final Transaction tx : transactions) {
                        filter.newPendingTx(tx);
                    }
                }
            }

            @Override
            public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {
                final ByteArrayWrapper txHashW = new ByteArrayWrapper(txReceipt.getTransaction().getHash());
                if (state.isPending() || state == PendingTransactionState.DROPPED) {
                    pendingReceipts.put(txHashW, txReceipt);
                } else {
                    pendingReceipts.remove(txHashW);
                }
            }
        });

    }

    private long JSonHexToLong(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Long.parseLong(x, 16);
    }

    private int JSonHexToInt(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Integer.parseInt(x, 16);
    }

    private String JSonHexToHex(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return x;
    }

    private Block getBlockByJSonHash(final String blockHash) throws Exception {
        final byte[] bhash = TypeConverter.StringHexToByteArray(blockHash);
        return worldManager.getBlockchain().getBlockByHash(bhash);
    }

    private Block getByJsonBlockId(final String id) {
        if ("earliest".equalsIgnoreCase(id)) {
            return blockchain.getBlockByNumber(0);
        } else if ("latest".equalsIgnoreCase(id)) {
            return blockchain.getBestBlock();
        } else if ("pending".equalsIgnoreCase(id)) {
            return null;
        } else {
            final long blockNumber = StringHexToBigInteger(id).longValue();
            return blockchain.getBlockByNumber(blockNumber);
        }
    }

    private Repository getRepoByJsonBlockId(final String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getRepository();
        } else {
            final Block block = getByJsonBlockId(id);
            return this.repository.getSnapshotTo(block.getStateRoot());
        }
    }

    private List<Transaction> getTransactionsByJsonBlockId(final String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getPendingTransactions();
        } else {
            final Block block = getByJsonBlockId(id);
            return block != null ? block.getTransactionsList() : null;
        }
    }

    private Account getAccount(final String address) throws Exception {
        return accounts.get(new ByteArrayWrapper(StringHexToByteArray(address)));
    }

    private Account addAccount(final String seed) {
        return addAccount(ECKey.fromPrivate(sha3(seed.getBytes())));
    }

    private Account addAccount(final ECKey key) {
        final Account account = new Account();
        account.init(key);
        accounts.put(new ByteArrayWrapper(account.getAddress()), account);
        return account;
    }

    public String web3_clientVersion() {

        final String s = "EthereumJ" + "/v" + config.projectVersion() + "/" +
                System.getProperty("os.name") + "/Java1.7/" + config.projectVersionModifier() + "-" + BuildInfo.buildHash;
        if (logger.isDebugEnabled()) logger.debug("web3_clientVersion(): " + s);
        return s;
    }

    public String web3_sha3(final String data) throws Exception {
        String s = null;
        try {
            final byte[] result = HashUtil.sha3(TypeConverter.StringHexToByteArray(data));
            return s = TypeConverter.toJsonHex(result);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("web3_sha3(" + data + "): " + s);
        }
    }

    public String net_version() {
        String s = null;
        try {
            return s = eth_protocolVersion();
        } finally {
            if (logger.isDebugEnabled()) logger.debug("net_version(): " + s);
        }
    }

    public String net_peerCount(){
        String s = null;
        try {
            final int n = channelManager.getActivePeers().size();
            return s = TypeConverter.toJsonHex(n);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("net_peerCount(): " + s);
        }
    }

    public boolean net_listening() {
        Boolean s = null;
        try {
            return s = peerServer.isListening();
        }finally {
            if (logger.isDebugEnabled()) logger.debug("net_listening(): " + s);
        }
    }

    public String eth_protocolVersion(){
        String s = null;
        try {
            int version = 0;
            for (final Capability capability : configCapabilities.getConfigCapabilities()) {
                if (capability.isEth()) {
                    version = max(version, capability.getVersion());
                }
            }
            return s = Integer.toString(version);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_protocolVersion(): " + s);
        }
    }

    public SyncingResult eth_syncing(){
        final SyncingResult s = new SyncingResult();
        try {
            s.setStartingBlock(TypeConverter.toJsonHex(initialBlockNumber));
            s.setCurrentBlock(TypeConverter.toJsonHex(blockchain.getBestBlock().getNumber()));
            s.setHighestBlock(TypeConverter.toJsonHex(syncManager.getLastKnownBlockNumber()));

            return s;
        }finally {
            if (logger.isDebugEnabled()) logger.debug("eth_syncing(): " + s);
        }
    }

    public String eth_coinbase() {
        String s = null;
        try {
            return s = toJsonHex(blockchain.getMinerCoinbase());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_coinbase(): " + s);
        }
    }

    public boolean eth_mining() {
        Boolean s = null;
        try {
            return s = blockMiner.isMining();
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_mining(): " + s);
        }
    }

    public String eth_hashrate() {
        String s = null;
        try {
            return s = null;
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_hashrate(): " + s);
        }
    }

    public String eth_gasPrice(){
        String s = null;
        try {
            return s = TypeConverter.toJsonHex(eth.getGasPrice());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_gasPrice(): " + s);
        }
    }

    public String[] eth_accounts() {
        String[] s = null;
        try {
            return s = personal_listAccounts();
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_accounts(): " + Arrays.toString(s));
        }
    }

    public String eth_blockNumber(){
        String s = null;
        try {
            final Block bestBlock = blockchain.getBestBlock();
            long b = 0;
            if (bestBlock != null) {
                b = bestBlock.getNumber();
            }
            return s = TypeConverter.toJsonHex(b);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_blockNumber(): " + s);
        }
    }

    public String eth_getBalance(final String address, final String blockId) throws Exception {
        String s = null;
        try {
            final byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
            final BigInteger balance = getRepoByJsonBlockId(blockId).getBalance(addressAsByteArray);
            return s = TypeConverter.toJsonHex(balance);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBalance(" + address + ", " + blockId + "): " + s);
        }
    }

    public String eth_getBalance(final String address) throws Exception {
        String s = null;
        try {
            return s = eth_getBalance(address, "latest");
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBalance(" + address + "): " + s);
        }
    }

    @Override
    public String eth_getStorageAt(final String address, final String storageIdx, final String blockId) throws Exception {
        String s = null;
        try {
            final byte[] addressAsByteArray = StringHexToByteArray(address);
            final DataWord storageValue = getRepoByJsonBlockId(blockId).
                    getStorageValue(addressAsByteArray, new DataWord(StringHexToByteArray(storageIdx)));
            return s = TypeConverter.toJsonHex(storageValue.getData());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getStorageAt(" + address + ", " + storageIdx + ", " + blockId + "): " + s);
        }
    }

    @Override
    public String eth_getTransactionCount(final String address, final String blockId) throws Exception {
        String s = null;
        try {
            final byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
            final BigInteger nonce = getRepoByJsonBlockId(blockId).getNonce(addressAsByteArray);
            return s = TypeConverter.toJsonHex(nonce);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionCount(" + address + ", " + blockId + "): " + s);
        }
    }

    public String eth_getBlockTransactionCountByHash(final String blockHash) throws Exception {
        String s = null;
        try {
            final Block b = getBlockByJSonHash(blockHash);
            if (b == null) return null;
            final long n = b.getTransactionsList().size();
            return s = TypeConverter.toJsonHex(n);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBlockTransactionCountByHash(" + blockHash + "): " + s);
        }
    }

    public String eth_getBlockTransactionCountByNumber(final String bnOrId) throws Exception {
        String s = null;
        try {
            final List<Transaction> list = getTransactionsByJsonBlockId(bnOrId);
            if (list == null) return null;
            final long n = list.size();
            return s = TypeConverter.toJsonHex(n);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBlockTransactionCountByNumber(" + bnOrId + "): " + s);
        }
    }

    public String eth_getUncleCountByBlockHash(final String blockHash) throws Exception {
        String s = null;
        try {
            final Block b = getBlockByJSonHash(blockHash);
            if (b == null) return null;
            final long n = b.getUncleList().size();
            return s = TypeConverter.toJsonHex(n);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getUncleCountByBlockHash(" + blockHash + "): " + s);
        }
    }

    public String eth_getUncleCountByBlockNumber(final String bnOrId) throws Exception {
        String s = null;
        try {
            final Block b = getByJsonBlockId(bnOrId);
            if (b == null) return null;
            final long n = b.getUncleList().size();
            return s = TypeConverter.toJsonHex(n);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getUncleCountByBlockNumber(" + bnOrId + "): " + s);
        }
    }

    public String eth_getCode(final String address, final String blockId) throws Exception {
        String s = null;
        try {
            final byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
            final byte[] code = getRepoByJsonBlockId(blockId).getCode(addressAsByteArray);
            return s = TypeConverter.toJsonHex(code);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getCode(" + address + ", " + blockId + "): " + s);
        }
    }

    public String eth_sign(final String addr, final String data) throws Exception {
        String s = null;
        try {
            final String ha = JSonHexToHex(addr);
            final Account account = getAccount(ha);

            if (account==null)
                throw new Exception("Inexistent account");

            // Todo: is not clear from the spec what hash function must be used to sign
            final byte[] masgHash = HashUtil.sha3(TypeConverter.StringHexToByteArray(data));
            final ECKey.ECDSASignature signature = account.getEcKey().sign(masgHash);
            // Todo: is not clear if result should be RlpEncoded or serialized by other means
            final byte[] rlpSig = RLP.encode(signature);
            return s = TypeConverter.toJsonHex(rlpSig);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_sign(" + addr + ", " + data + "): " + s);
        }
    }

    public String eth_sendTransaction(final CallArguments args) throws Exception {

        String s = null;
        try {
            final Account account = getAccount(JSonHexToHex(args.getFrom()));

            if (account == null)
                throw new Exception("From address private key could not be found in this node");

            if (args.getData() != null && args.getData().startsWith("0x"))
                args.setData(args.getData().substring(2));

            final Transaction tx = new Transaction(
                    args.getNonce() != null ? StringHexToByteArray(args.getNonce()) : bigIntegerToBytes(pendingState.getRepository().getNonce(account.getAddress())),
                    args.getGasPrice() != null ? StringHexToByteArray(args.getGasPrice()) : ByteUtil.longToBytesNoLeadZeroes(eth.getGasPrice()),
                    args.getGas() != null ? StringHexToByteArray(args.getGas()) : ByteUtil.longToBytes(90_000),
                    args.getTo() != null ? StringHexToByteArray(args.getTo()) : EMPTY_BYTE_ARRAY,
                    args.getValue() != null ? StringHexToByteArray(args.getValue()) : EMPTY_BYTE_ARRAY,
                    args.getData() != null ? StringHexToByteArray(args.getData()) : EMPTY_BYTE_ARRAY,
                    eth.getChainIdForNextBlock());
            tx.sign(account.getEcKey().getPrivKeyBytes());

            eth.submitTransaction(tx);

            return s = TypeConverter.toJsonHex(tx.getHash());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_sendTransaction(" + args + "): " + s);
        }
    }

    public String eth_sendTransaction(final String from, final String to, final String gas,
                                      final String gasPrice, final String value, final String data, final String nonce) throws Exception {
        String s = null;
        try {
            final Transaction tx = new Transaction(
                    TypeConverter.StringHexToByteArray(nonce),
                    TypeConverter.StringHexToByteArray(gasPrice),
                    TypeConverter.StringHexToByteArray(gas),
                    TypeConverter.StringHexToByteArray(to), /*receiveAddress*/
                    TypeConverter.StringHexToByteArray(value),
                    TypeConverter.StringHexToByteArray(data),
                    eth.getChainIdForNextBlock());

            final Account account = getAccount(from);
            if (account == null) throw new RuntimeException("No account " + from);

            tx.sign(account.getEcKey());

            eth.submitTransaction(tx);

            return s = TypeConverter.toJsonHex(tx.getHash());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_sendTransaction(" +
                    "from = [" + from + "], to = [" + to + "], gas = [" + gas + "], gasPrice = [" + gasPrice +
                    "], value = [" + value + "], data = [" + data + "], nonce = [" + nonce + "]" + "): " + s);
        }
    }

    public String eth_sendRawTransaction(final String rawData) throws Exception {
        String s = null;
        try {
            final Transaction tx = new Transaction(StringHexToByteArray(rawData));
            tx.verify();

            eth.submitTransaction(tx);

            return s = TypeConverter.toJsonHex(tx.getHash());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_sendRawTransaction(" + rawData + "): " + s);
        }
    }

    private TransactionReceipt createCallTxAndExecute(final CallArguments args, final Block block) throws Exception {
        final Repository repository = ((Repository) worldManager.getRepository())
                .getSnapshotTo(block.getStateRoot())
                .startTracking();

        return createCallTxAndExecute(args, block, repository, worldManager.getBlockStore());
    }

    private TransactionReceipt createCallTxAndExecute(final CallArguments args, final Block block, final Repository repository, final BlockStore blockStore) throws Exception {
        final BinaryCallArguments bca = new BinaryCallArguments();
        bca.setArguments(args);
        final Transaction tx = CallTransaction.createRawTransaction(0,
                bca.gasPrice,
                bca.gasLimit,
                bca.toAddress,
                bca.value,
                bca.data);

        // put mock signature if not present
        if (tx.getSignature() == null) {
            tx.sign(ECKey.fromPrivate(new byte[32]));
        }

        try {
            final TransactionExecutor executor = new TransactionExecutor
                    (tx, block.getCoinbase(), repository, blockStore,
                            programInvokeFactory, block, new EthereumListenerAdapter(), 0)
                    .withCommonConfig(commonConfig)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return executor.getReceipt();
        } finally {
            repository.rollback();
        }
    }

    public String eth_call(final CallArguments args, final String bnOrId) throws Exception {

        String s = null;
        try {
            final TransactionReceipt res;
            if ("pending".equals(bnOrId)) {
                final Block pendingBlock = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.emptyList());
                res = createCallTxAndExecute(args, pendingBlock, pendingState.getRepository(), worldManager.getBlockStore());
            } else {
                res = createCallTxAndExecute(args, getByJsonBlockId(bnOrId));
            }
            return s = TypeConverter.toJsonHex(res.getExecutionResult());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_call(" + args + "): " + s);
        }
    }

    public String eth_estimateGas(final CallArguments args) throws Exception {
        String s = null;
        try {
            final TransactionReceipt res = createCallTxAndExecute(args, blockchain.getBestBlock());
            return s = TypeConverter.toJsonHex(res.getGasUsed());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_estimateGas(" + args + "): " + s);
        }
    }

    private BlockResult getBlockResult(final Block b, final boolean fullTx) {
        if (b==null)
            return null;
        final boolean isPending = ByteUtil.byteArrayToLong(b.getNonce()) == 0;
        final BlockResult br = new BlockResult();
        br.setNumber(isPending ? null : TypeConverter.toJsonHex(b.getNumber()));
        br.setHash(isPending ? null : TypeConverter.toJsonHex(b.getHash()));
        br.setParentHash(TypeConverter.toJsonHex(b.getParentHash()));
        br.setNonce(isPending ? null : TypeConverter.toJsonHex(b.getNonce()));
        br.setSha3Uncles(TypeConverter.toJsonHex(b.getUnclesHash()));
        br.setLogsBloom(isPending ? null : TypeConverter.toJsonHex(b.getLogBloom()));
        br.setTransactionsRoot(TypeConverter.toJsonHex(b.getTxTrieRoot()));
        br.setStateRoot(TypeConverter.toJsonHex(b.getStateRoot()));
        br.setReceiptsRoot(TypeConverter.toJsonHex(b.getReceiptsRoot()));
        br.setMiner(isPending ? null : TypeConverter.toJsonHex(b.getCoinbase()));
        br.setDifficulty(TypeConverter.toJsonHex(b.getDifficulty()));
        br.setTotalDifficulty(TypeConverter.toJsonHex(blockchain.getTotalDifficulty()));
        if (b.getExtraData() != null)
            br.setExtraData(TypeConverter.toJsonHex(b.getExtraData()));
        br.setSize(TypeConverter.toJsonHex(b.getEncoded().length));
        br.setGasLimit(TypeConverter.toJsonHex(b.getGasLimit()));
        br.setGasUsed(TypeConverter.toJsonHex(b.getGasUsed()));
        br.setTimestamp(TypeConverter.toJsonHex(b.getTimestamp()));

        final List<Object> txes = new ArrayList<>();
        if (fullTx) {
            for (int i = 0; i < b.getTransactionsList().size(); i++) {
                txes.add(new TransactionResultDTO(b, i, b.getTransactionsList().get(i)));
            }
        } else {
            for (final Transaction tx : b.getTransactionsList()) {
                txes.add(toJsonHex(tx.getHash()));
            }
        }
        br.setTransactions(txes.toArray());

        final List<String> ul = new ArrayList<>();
        for (final BlockHeader header : b.getUncleList()) {
            ul.add(toJsonHex(header.getHash()));
        }
        br.setUncles(ul.toArray(new String[ul.size()]));

        return br;
    }

    public BlockResult eth_getBlockByHash(final String blockHash, final Boolean fullTransactionObjects) throws Exception {
        final BlockResult s = null;
        try {
            final Block b = getBlockByJSonHash(blockHash);
            return getBlockResult(b, fullTransactionObjects);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBlockByHash(" +  blockHash + ", " + fullTransactionObjects + "): " + s);
        }
    }

    public BlockResult eth_getBlockByNumber(final String bnOrId, final Boolean fullTransactionObjects) throws Exception {
        BlockResult s = null;
        try {
            final Block b;
            if ("pending".equalsIgnoreCase(bnOrId)) {
                b = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.emptyList());
            } else {
                b = getByJsonBlockId(bnOrId);
            }
            return s = (b == null ? null : getBlockResult(b, fullTransactionObjects));
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getBlockByNumber(" +  bnOrId + ", " + fullTransactionObjects + "): " + s);
        }
    }

    public TransactionResultDTO eth_getTransactionByHash(final String transactionHash) throws Exception {
        TransactionResultDTO s = null;
        try {
            final byte[] txHash = StringHexToByteArray(transactionHash);
            Block block = null;

            TransactionInfo txInfo = blockchain.getTransactionInfo(txHash);

            if (txInfo == null) {
                final TransactionReceipt receipt = pendingReceipts.get(new ByteArrayWrapper(txHash));

                if (receipt == null) {
                    return null;
                }
                txInfo = new TransactionInfo(receipt);
            } else {
                block = blockchain.getBlockByHash(txInfo.getBlockHash());
                // need to return txes only from main chain
                final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
                    return null;
                }
                txInfo.setTransaction(block.getTransactionsList().get(txInfo.getIndex()));
            }

            return s = new TransactionResultDTO(block, txInfo.getIndex(), txInfo.getReceipt().getTransaction());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionByHash(" + transactionHash + "): " + s);
        }
    }

    public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(final String blockHash, final String index) throws Exception {
        TransactionResultDTO s = null;
        try {
            final Block b = getBlockByJSonHash(blockHash);
            if (b == null) return null;
            final int idx = JSonHexToInt(index);
            if (idx >= b.getTransactionsList().size()) return null;
            final Transaction tx = b.getTransactionsList().get(idx);
            return s = new TransactionResultDTO(b, idx, tx);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionByBlockHashAndIndex(" + blockHash + ", " + index + "): " + s);
        }
    }

    public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(final String bnOrId, final String index) throws Exception {
        TransactionResultDTO s = null;
        try {
            final Block b = getByJsonBlockId(bnOrId);
            final List<Transaction> txs = getTransactionsByJsonBlockId(bnOrId);
            if (txs == null) return null;
            final int idx = JSonHexToInt(index);
            if (idx >= txs.size()) return null;
            final Transaction tx = txs.get(idx);
            return s = new TransactionResultDTO(b, idx, tx);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionByBlockNumberAndIndex(" + bnOrId + ", " + index + "): " + s);
        }
    }

    public TransactionReceiptDTO eth_getTransactionReceipt(final String transactionHash) throws Exception {
        TransactionReceiptDTO s = null;
        try {
            final byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);

            final TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));

            final TransactionInfo txInfo;
            final Block block;

            if (pendingReceipt != null) {
                txInfo = new TransactionInfo(pendingReceipt);
                block = null;
            } else {
                txInfo = blockchain.getTransactionInfo(hash);

                if (txInfo == null)
                    return null;

                block = blockchain.getBlockByHash(txInfo.getBlockHash());

                // need to return txes only from main chain
                final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
                    return null;
                }
            }

            return s = new TransactionReceiptDTO(block, txInfo);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionReceipt(" + transactionHash + "): " + s);
        }
    }

    @Override
    public TransactionReceiptDTOExt ethj_getTransactionReceipt(final String transactionHash) throws Exception {
        TransactionReceiptDTOExt s = null;
        try {
            final byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);

            final TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));

            final TransactionInfo txInfo;
            final Block block;

            if (pendingReceipt != null) {
                txInfo = new TransactionInfo(pendingReceipt);
                block = null;
            } else {
                txInfo = blockchain.getTransactionInfo(hash);

                if (txInfo == null)
                    return null;

                block = blockchain.getBlockByHash(txInfo.getBlockHash());

                // need to return txes only from main chain
                final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
                if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
                    return null;
                }
            }

            return s = new TransactionReceiptDTOExt(block, txInfo);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getTransactionReceipt(" + transactionHash + "): " + s);
        }
    }

    @Override
    public BlockResult eth_getUncleByBlockHashAndIndex(final String blockHash, final String uncleIdx) throws Exception {
        BlockResult s = null;
        try {
            final Block block = blockchain.getBlockByHash(StringHexToByteArray(blockHash));
            if (block == null) return null;
            final int idx = JSonHexToInt(uncleIdx);
            if (idx >= block.getUncleList().size()) return null;
            final BlockHeader uncleHeader = block.getUncleList().get(idx);
            Block uncle = blockchain.getBlockByHash(uncleHeader.getHash());
            if (uncle == null) {
                uncle = new Block(uncleHeader, Collections.emptyList(), Collections.emptyList());
            }
            return s = getBlockResult(uncle, false);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getUncleByBlockHashAndIndex(" + blockHash + ", " + uncleIdx + "): " + s);
        }
    }

    @Override
    public BlockResult eth_getUncleByBlockNumberAndIndex(final String blockId, final String uncleIdx) throws Exception {
        BlockResult s = null;
        try {
            final Block block = getByJsonBlockId(blockId);
            return s = block == null ? null :
                    eth_getUncleByBlockHashAndIndex(toJsonHex(block.getHash()), uncleIdx);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getUncleByBlockNumberAndIndex(" + blockId + ", " + uncleIdx + "): " + s);
        }
    }

    @Override
    public String[] eth_getCompilers() {
        String[] s = null;
        try {
            return s = new String[] {"solidity"};
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getCompilers(): " + Arrays.toString(s));
        }
    }

    @Override
    public CompilationResult eth_compileLLL(final String contract) {
        throw new UnsupportedOperationException("LLL compiler not supported");
    }

    @Override
    public CompilationResult eth_compileSolidity(final String contract) throws Exception {
        CompilationResult s = null;
        try {
            final SolidityCompiler.Result res = solidityCompiler.compileSrc(
                    contract.getBytes(), true, true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
            if (res.isFailed()) {
                throw new RuntimeException("Compilation error: " + res.errors);
            }
            final org.ethereum.solidity.compiler.CompilationResult result = org.ethereum.solidity.compiler.CompilationResult.parse(res.output);
            final CompilationResult ret = new CompilationResult();
            final org.ethereum.solidity.compiler.CompilationResult.ContractMetadata contractMetadata = result.contracts.values().iterator().next();
            ret.setCode(toJsonHex(contractMetadata.bin));
            ret.setInfo(new CompilationInfo());
            ret.getInfo().setSource(contract);
            ret.getInfo().setLanguage("Solidity");
            ret.getInfo().setLanguageVersion("0");
            ret.getInfo().setCompilerVersion(result.version);
            ret.getInfo().setAbiDefinition(new CallTransaction.Contract(contractMetadata.abi).functions);
            return s = ret;
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_compileSolidity(" + contract + ")" + s);
        }
    }

    @Override
    public CompilationResult eth_compileSerpent(final String contract) {
        throw new UnsupportedOperationException("Serpent compiler not supported");
    }

    @Override
    public String eth_resend() {
        throw new UnsupportedOperationException("JSON RPC method eth_resend not implemented yet");
    }

    @Override
    public String eth_pendingTransactions() {
        throw new UnsupportedOperationException("JSON RPC method eth_pendingTransactions not implemented yet");
    }

    @Override
    public String eth_newFilter(final FilterRequest fr) throws Exception {
        String str = null;
        try {
            final LogFilter logFilter = new LogFilter();

            if (fr.getAddress() instanceof String) {
                logFilter.withContractAddress(StringHexToByteArray((String) fr.getAddress()));
            } else if (fr.getAddress() instanceof String[]) {
                final List<byte[]> addr = new ArrayList<>();
                for (final String s : ((String[]) fr.getAddress())) {
                    addr.add(StringHexToByteArray(s));
                }
                logFilter.withContractAddress(addr.toArray(new byte[0][]));
            }

            if (fr.getTopics() != null) {
                for (final Object topic : fr.getTopics()) {
                    if (topic == null) {
                        logFilter.withTopic(null);
                    } else if (topic instanceof String) {
                        logFilter.withTopic(new DataWord(StringHexToByteArray((String) topic)).getData());
                    } else if (topic instanceof String[]) {
                        final List<byte[]> t = new ArrayList<>();
                        for (final String s : ((String[]) topic)) {
                            t.add(new DataWord(StringHexToByteArray(s)).getData());
                        }
                        logFilter.withTopic(t.toArray(new byte[0][]));
                    }
                }
            }

            final JsonLogFilter filter = new JsonLogFilter(logFilter);
            final int id = filterCounter.getAndIncrement();
            installedFilters.put(id, filter);

            final Block blockFrom = fr.getFromBlock() == null ? null : getByJsonBlockId(fr.getFromBlock());
            Block blockTo = fr.getToBlock() == null ? null : getByJsonBlockId(fr.getToBlock());

            if (blockFrom != null) {
                // need to add historical data
                blockTo = blockTo == null ? blockchain.getBestBlock() : blockTo;
                for (long blockNum = blockFrom.getNumber(); blockNum <= blockTo.getNumber(); blockNum++) {
                    filter.onBlock(blockchain.getBlockByNumber(blockNum));
                }
            }

            // the following is not precisely documented
            if ("pending".equalsIgnoreCase(fr.getFromBlock()) || "pending".equalsIgnoreCase(fr.getToBlock())) {
                filter.onPendingTx = true;
            } else if ("latest".equalsIgnoreCase(fr.getFromBlock()) || "latest".equalsIgnoreCase(fr.getToBlock())) {
                filter.onNewBlock = true;
            }

            return str = toJsonHex(id);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_newFilter(" + fr + "): " + str);
        }
    }

    @Override
    public String eth_newBlockFilter() {
        String s = null;
        try {
            final int id = filterCounter.getAndIncrement();
            installedFilters.put(id, new NewBlockFilter());
            return s = toJsonHex(id);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_newBlockFilter(): " + s);
        }
    }

    @Override
    public String eth_newPendingTransactionFilter() {
        String s = null;
        try {
            final int id = filterCounter.getAndIncrement();
            installedFilters.put(id, new PendingTransactionFilter());
            return s = toJsonHex(id);
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_newPendingTransactionFilter(): " + s);
        }
    }

    @Override
    public boolean eth_uninstallFilter(final String id) {
        Boolean s = null;
        try {
            if (id == null) return false;
            return s = installedFilters.remove(StringHexToBigInteger(id).intValue()) != null;
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_uninstallFilter(" + id + "): " + s);
        }
    }

    @Override
    public Object[] eth_getFilterChanges(final String id) {
        Object[] s = null;
        try {
            final Filter filter = installedFilters.get(StringHexToBigInteger(id).intValue());
            if (filter == null) return null;
            return s = filter.poll();
        } finally {
            if (logger.isDebugEnabled()) logger.debug("eth_getFilterChanges(" + id + "): " + Arrays.toString(s));
        }
    }

    @Override
    public Object[] eth_getFilterLogs(final String id) {
        logger.debug("eth_getFilterLogs ...");
        return eth_getFilterChanges(id);
    }

    @Override
    public Object[] eth_getLogs(final FilterRequest fr) throws Exception {
        logger.debug("eth_getLogs ...");
        final String id = eth_newFilter(fr);
        final Object[] ret = eth_getFilterChanges(id);
        eth_uninstallFilter(id);
        return ret;
    }

    @Override
    public String eth_getWork() {
        throw new UnsupportedOperationException("JSON RPC method eth_getWork not implemented yet");
    }

    @Override
    public String eth_submitWork() {
        throw new UnsupportedOperationException("JSON RPC method eth_submitWork not implemented yet");
    }

    @Override
    public String eth_submitHashrate() {
        throw new UnsupportedOperationException("JSON RPC method eth_submitHashrate not implemented yet");
    }

    @Override
    public String db_putString() {
        throw new UnsupportedOperationException("JSON RPC method db_putString not implemented yet");
    }

    @Override
    public String db_getString() {
        throw new UnsupportedOperationException("JSON RPC method db_getString not implemented yet");
    }

    @Override
    public String db_putHex() {
        throw new UnsupportedOperationException("JSON RPC method db_putHex not implemented yet");
    }

    @Override
    public String db_getHex() {
        throw new UnsupportedOperationException("JSON RPC method db_getHex not implemented yet");
    }

    @Override
    public String shh_post() {
        throw new UnsupportedOperationException("JSON RPC method shh_post not implemented yet");
    }

    @Override
    public String shh_version() {
        throw new UnsupportedOperationException("JSON RPC method shh_version not implemented yet");
    }

    @Override
    public String shh_newIdentity() {
        throw new UnsupportedOperationException("JSON RPC method shh_newIdentity not implemented yet");
    }

    @Override
    public String shh_hasIdentity() {
        throw new UnsupportedOperationException("JSON RPC method shh_hasIdentity not implemented yet");
    }

    @Override
    public String shh_newGroup() {
        throw new UnsupportedOperationException("JSON RPC method shh_newGroup not implemented yet");
    }

    @Override
    public String shh_addToGroup() {
        throw new UnsupportedOperationException("JSON RPC method shh_addToGroup not implemented yet");
    }

    @Override
    public String shh_newFilter() {
        throw new UnsupportedOperationException("JSON RPC method shh_newFilter not implemented yet");
    }

    @Override
    public String shh_uninstallFilter() {
        throw new UnsupportedOperationException("JSON RPC method shh_uninstallFilter not implemented yet");
    }

    @Override
    public String shh_getFilterChanges() {
        throw new UnsupportedOperationException("JSON RPC method shh_getFilterChanges not implemented yet");
    }

    @Override
    public String shh_getMessages() {
        throw new UnsupportedOperationException("JSON RPC method shh_getMessages not implemented yet");
    }

    @Override
    public boolean admin_addPeer(final String s) {
        eth.connect(new Node(s));
        return true;
    }

    @Override
    public String admin_exportChain() {
        throw new UnsupportedOperationException("JSON RPC method admin_exportChain not implemented yet");
    }

    @Override
    public String admin_importChain() {
        throw new UnsupportedOperationException("JSON RPC method admin_importChain not implemented yet");
    }

    @Override
    public String admin_sleepBlocks() {
        throw new UnsupportedOperationException("JSON RPC method admin_sleepBlocks not implemented yet");
    }

    @Override
    public String admin_verbosity() {
        throw new UnsupportedOperationException("JSON RPC method admin_verbosity not implemented yet");
    }

    @Override
    public String admin_setSolc() {
        throw new UnsupportedOperationException("JSON RPC method admin_setSolc not implemented yet");
    }

    @Override
    public String admin_startRPC() {
        throw new UnsupportedOperationException("JSON RPC method admin_startRPC not implemented yet");
    }

    @Override
    public String admin_stopRPC() {
        throw new UnsupportedOperationException("JSON RPC method admin_stopRPC not implemented yet");
    }

    @Override
    public String admin_setGlobalRegistrar() {
        throw new UnsupportedOperationException("JSON RPC method admin_setGlobalRegistrar not implemented yet");
    }

    @Override
    public String admin_setHashReg() {
        throw new UnsupportedOperationException("JSON RPC method admin_setHashReg not implemented yet");
    }

    @Override
    public String admin_setUrlHint() {
        throw new UnsupportedOperationException("JSON RPC method admin_setUrlHint not implemented yet");
    }

    @Override
    public String admin_saveInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_saveInfo not implemented yet");
    }

    @Override
    public String admin_register() {
        throw new UnsupportedOperationException("JSON RPC method admin_register not implemented yet");
    }

    @Override
    public String admin_registerUrl() {
        throw new UnsupportedOperationException("JSON RPC method admin_registerUrl not implemented yet");
    }

    @Override
    public String admin_startNatSpec() {
        throw new UnsupportedOperationException("JSON RPC method admin_startNatSpec not implemented yet");
    }

    @Override
    public String admin_stopNatSpec() {
        throw new UnsupportedOperationException("JSON RPC method admin_stopNatSpec not implemented yet");
    }

    @Override
    public String admin_getContractInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_getContractInfo not implemented yet");
    }

    @Override
    public String admin_httpGet() {
        throw new UnsupportedOperationException("JSON RPC method admin_httpGet not implemented yet");
    }

    @Override
    public String admin_nodeInfo() {
        throw new UnsupportedOperationException("JSON RPC method admin_nodeInfo not implemented yet");
    }

    @Override
    public String admin_peers() {
        throw new UnsupportedOperationException("JSON RPC method admin_peers not implemented yet");
    }

    @Override
    public String admin_datadir() {
        throw new UnsupportedOperationException("JSON RPC method admin_datadir not implemented yet");
    }

    @Override
    public String net_addPeer() {
        throw new UnsupportedOperationException("JSON RPC method net_addPeer not implemented yet");
    }

    @Override
    public boolean miner_start() {
        blockMiner.startMining();
        return true;
    }

    @Override
    public boolean miner_stop() {
        blockMiner.stopMining();
        return true;
    }

    @Override
    public boolean miner_setEtherbase(final String coinBase) throws Exception {
        blockchain.setMinerCoinbase(TypeConverter.StringHexToByteArray(coinBase));
        return true;
    }

    @Override
    public boolean miner_setExtra(final String data) throws Exception {
        blockchain.setMinerExtraData(TypeConverter.StringHexToByteArray(data));
        return true;
    }

    @Override
    public boolean miner_setGasPrice(final String newMinGasPrice) {
        blockMiner.setMinGasPrice(TypeConverter.StringHexToBigInteger(newMinGasPrice));
        return true;
    }

    @Override
    public boolean miner_startAutoDAG() {
        return false;
    }

    @Override
    public boolean miner_stopAutoDAG() {
        return false;
    }

    @Override
    public boolean miner_makeDAG() {
        return false;
    }

    @Override
    public String miner_hashrate() {
        return "0x01";
    }

    @Override
    public String debug_printBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_printBlock not implemented yet");
    }

    @Override
    public String debug_getBlockRlp() {
        throw new UnsupportedOperationException("JSON RPC method debug_getBlockRlp not implemented yet");
    }

    @Override
    public String debug_setHead() {
        throw new UnsupportedOperationException("JSON RPC method debug_setHead not implemented yet");
    }

    @Override
    public String debug_processBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_processBlock not implemented yet");
    }

    @Override
    public String debug_seedHash() {
        throw new UnsupportedOperationException("JSON RPC method debug_seedHash not implemented yet");
    }

    @Override
    public String debug_dumpBlock() {
        throw new UnsupportedOperationException("JSON RPC method debug_dumpBlock not implemented yet");
    }

    @Override
    public String debug_metrics() {
        throw new UnsupportedOperationException("JSON RPC method debug_metrics not implemented yet");
    }

    @Override
    public String personal_newAccount(final String seed) {
        String s = null;
        try {
            final Account account = addAccount(seed);
            return s = toJsonHex(account.getAddress());
        } finally {
            if (logger.isDebugEnabled()) logger.debug("personal_newAccount(*****): " + s);
        }
    }

    @Override
    public boolean personal_unlockAccount(final String addr, final String pass, final String duration) {
        final String s = null;
        try {
            return true;
        } finally {
            if (logger.isDebugEnabled()) logger.debug("personal_unlockAccount(" + addr + ", ***, " + duration + "): " + s);
        }
    }

    @Override
    public String[] personal_listAccounts() {
        final String[] ret = new String[accounts.size()];
        try {
            int i = 0;
            for (final ByteArrayWrapper addr : accounts.keySet()) {
                ret[i++] = toJsonHex(addr.getData());
            }
            return ret;
        } finally {
            if (logger.isDebugEnabled()) logger.debug("personal_listAccounts(): " + Arrays.toString(ret));
        }
    }

    static class Filter {
        static final int MAX_EVENT_COUNT = 1024; // prevent OOM when Filers are forgotten
        final List<FilterEvent> events = new LinkedList<>();

        public synchronized boolean hasNew() {
            return !events.isEmpty();
        }

        public synchronized Object[] poll() {
            final Object[] ret = new Object[events.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i).getJsonEventObject();
            }
            this.events.clear();
            return ret;
        }

        synchronized void add(final FilterEvent evt) {
            events.add(evt);
            if (events.size() > MAX_EVENT_COUNT) events.remove(0);
        }

        public void newBlockReceived(final Block b) {
        }

        public void newPendingTx(final Transaction tx) {
        }

        static abstract class FilterEvent {
            public abstract Object getJsonEventObject();
        }
    }

    static class NewBlockFilter extends Filter {
        public void newBlockReceived(final Block b) {
            add(new NewBlockFilterEvent(b));
        }

        class NewBlockFilterEvent extends FilterEvent {
            public final Block b;

            NewBlockFilterEvent(final Block b) {
                this.b = b;
            }

            @Override
            public String getJsonEventObject() {
                return toJsonHex(b.getHash());
            }
        }
    }

    static class PendingTransactionFilter extends Filter {
        public void newPendingTx(final Transaction tx) {
            add(new PendingTransactionFilterEvent(tx));
        }

        class PendingTransactionFilterEvent extends FilterEvent {
            private final Transaction tx;

            PendingTransactionFilterEvent(final Transaction tx) {
                this.tx = tx;
            }

            @Override
            public String getJsonEventObject() {
                return toJsonHex(tx.getHash());
            }
        }
    }

    public class BinaryCallArguments {
        public long nonce;
        public long gasPrice;
        public long gasLimit;
        public String toAddress;
        public long value;
        public byte[] data;

        public void setArguments(final CallArguments args) throws Exception {
            nonce = 0;
            if (args.getNonce() != null && args.getNonce().length() != 0)
                nonce = JSonHexToLong(args.getNonce());

            gasPrice = 0;
            if (args.getGasPrice() != null && args.getGasPrice().length() != 0)
                gasPrice = JSonHexToLong(args.getGasPrice());

            gasLimit = 4_000_000;
            if (args.getGas() != null && args.getGas().length() != 0)
                gasLimit = JSonHexToLong(args.getGas());

            toAddress = null;
            if (args.getTo() != null && !args.getTo().isEmpty())
                toAddress = JSonHexToHex(args.getTo());

            value = 0;
            if (args.getValue() != null && args.getValue().length() != 0)
                value = JSonHexToLong(args.getValue());

            data = null;

            if (args.getData() != null && args.getData().length() != 0)
                data = TypeConverter.StringHexToByteArray(args.getData());
        }
    }

    class JsonLogFilter extends Filter {
        final LogFilter logFilter;
        boolean onNewBlock;
        boolean onPendingTx;

        public JsonLogFilter(final LogFilter logFilter) {
            this.logFilter = logFilter;
        }

        void onLogMatch(final LogInfo logInfo, final Block b, final int txIndex, final Transaction tx, final int logIdx) {
            add(new LogFilterEvent(new LogFilterElement(logInfo, b, txIndex, tx, logIdx)));
        }

        void onTransactionReceipt(final TransactionReceipt receipt, final Block b, final int txIndex) {
            if (logFilter.matchBloom(receipt.getBloomFilter())) {
                int logIdx = 0;
                for (final LogInfo logInfo : receipt.getLogInfoList()) {
                    if (logFilter.matchBloom(logInfo.getBloom()) && logFilter.matchesExactly(logInfo)) {
                        onLogMatch(logInfo, b, txIndex, receipt.getTransaction(), logIdx);
                    }
                    logIdx++;
                }
            }
        }

        void onTransaction(final Transaction tx, final Block b, final int txIndex) {
            if (logFilter.matchesContractAddress(tx.getReceiveAddress())) {
                final TransactionInfo txInfo = blockchain.getTransactionInfo(tx.getHash());
                onTransactionReceipt(txInfo.getReceipt(), b, txIndex);
            }
        }

        void onBlock(final Block b) {
            if (logFilter.matchBloom(new Bloom(b.getLogBloom()))) {
                int txIdx = 0;
                for (final Transaction tx : b.getTransactionsList()) {
                    onTransaction(tx, b, txIdx);
                    txIdx++;
                }
            }
        }

        @Override
        public void newBlockReceived(final Block b) {
            if (onNewBlock) onBlock(b);
        }

        @Override
        public void newPendingTx(final Transaction tx) {
            // TODO add TransactionReceipt for PendingTx
//            if (onPendingTx)
        }

        class LogFilterEvent extends FilterEvent {
            private final LogFilterElement el;

            LogFilterEvent(final LogFilterElement el) {
                this.el = el;
            }

            @Override
            public LogFilterElement getJsonEventObject() {
                return el;
            }
        }
    }
}
