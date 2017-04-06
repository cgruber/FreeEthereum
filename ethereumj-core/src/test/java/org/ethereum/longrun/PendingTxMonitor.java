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

package org.ethereum.longrun;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.jsonrpc.JsonRpc;
import org.ethereum.jsonrpc.TransactionResultDTO;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.ByteArrayMap;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;

import java.net.URL;
import java.util.HashSet;

/**
 * Matches pending transactions from EthereumJ and any other JSON-RPC client
 *
 * Created by Anton Nashatyrev on 15.02.2017.
 */
@Ignore
public class PendingTxMonitor extends BasicNode {
    private static final Logger testLogger = LoggerFactory.getLogger("TestLogger");
    private final ByteArrayMap<Triple<Long, TransactionReceipt, EthereumListener.PendingTransactionState>> localTxs = new ByteArrayMap<>();
    private ByteArrayMap<Pair<Long, TransactionResultDTO>> remoteTxs;

    public PendingTxMonitor() {
        super("sampleNode");
    }

    @Override
    public void run() {
        try {
            setupRemoteRpc();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        super.run();
    }

    private void setupRemoteRpc() throws Exception {
        System.out.println("Creating RPC interface...");
        final JsonRpcHttpClient httpClient = new JsonRpcHttpClient(new URL("http://localhost:8545"));
        final JsonRpc jsonRpc = ProxyUtil.createClientProxy(getClass().getClassLoader(), JsonRpc.class, httpClient);
        System.out.println("Pinging remote RPC...");
        final String protocolVersion = jsonRpc.eth_protocolVersion();
        System.out.println("Remote OK. Version: " + protocolVersion);

        final String pTxFilterId = jsonRpc.eth_newPendingTransactionFilter();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (Boolean.TRUE) {
                        final Object[] changes = jsonRpc.eth_getFilterChanges(pTxFilterId);
                        if (changes.length > 0) {
                            for (final Object change : changes) {
                                final TransactionResultDTO tx = jsonRpc.eth_getTransactionByHash((String) change);
                                newRemotePendingTx(tx);
                            }
                        }
                        Thread.sleep(100);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onSyncDoneImpl(final EthereumListener.SyncState state) {
        super.onSyncDoneImpl(state);
        if (remoteTxs == null) {
            remoteTxs = new ByteArrayMap<>();
            System.out.println("Sync Done!!!");
            ethereum.addListener(new EthereumListenerAdapter() {
                @Override
                public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state, final Block block) {
                    PendingTxMonitor.this.onPendingTransactionUpdate(txReceipt, state, block);
                }
            });
        }
    }

    private void checkUnmatched() {
        for (final byte[] txHash : new HashSet<>(localTxs.keySet())) {
            final Triple<Long, TransactionReceipt, EthereumListener.PendingTransactionState> tx = localTxs.get(txHash);
            if (System.currentTimeMillis() - tx.getLeft() > 60_000) {
                localTxs.remove(txHash);
                System.err.println("Local tx doesn't match: " + tx.getMiddle().getTransaction());
            }
        }

        for (final byte[] txHash : new HashSet<>(remoteTxs.keySet())) {
            final Pair<Long, TransactionResultDTO> tx = remoteTxs.get(txHash);
            if (System.currentTimeMillis() - tx.getLeft() > 60_000) {
                remoteTxs.remove(txHash);
                System.err.println("Remote tx doesn't match: " + tx.getRight());
            }
        }

    }

    private void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final EthereumListener.PendingTransactionState state, final Block block) {
        final byte[] txHash = txReceipt.getTransaction().getHash();
        final Pair<Long, TransactionResultDTO> removed = remoteTxs.remove(txHash);
        if (state == EthereumListener.PendingTransactionState.DROPPED) {
            if (localTxs.remove(txHash) != null) {
                System.out.println("Dropped due to timeout (matchned: " + (removed != null) + "): " + Hex.toHexString(txHash));
            } else {
                if (remoteTxs.containsKey(txHash)) {
                    System.err.println("Dropped but matching: "  + Hex.toHexString(txHash) + ": \n" + txReceipt);
                }
            }
        } else if (state == EthereumListener.PendingTransactionState.NEW_PENDING) {
            System.out.println("Local: " + Hex.toHexString(txHash));
            if (removed == null) {
                localTxs.put(txHash, Triple.of(System.currentTimeMillis(), txReceipt, state));
            } else {
                System.out.println("Tx matched: " + Hex.toHexString(txHash));
            }
        }
        checkUnmatched();
    }

    private void newRemotePendingTx(final TransactionResultDTO tx) {
        final byte[] txHash = Hex.decode(tx.hash.substring(2));
        if (remoteTxs == null) return;
        System.out.println("Remote: " + Hex.toHexString(txHash));
        final Triple<Long, TransactionReceipt, EthereumListener.PendingTransactionState> removed = localTxs.remove(txHash);
        if (removed == null) {
            remoteTxs.put(txHash, Pair.of(System.currentTimeMillis(), tx));
        } else {
            System.out.println("Tx matched: " + Hex.toHexString(txHash));
        }
        checkUnmatched();
    }

    @Test
    public void test() throws Exception {
        testLogger.info("Starting EthereumJ regular instance!");
        EthereumFactory.createEthereum(RegularConfig.class);

        Thread.sleep(100000000000L);
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private static class RegularConfig {

        @Bean
        public PendingTxMonitor node() {
            return new PendingTxMonitor();
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        public SystemProperties systemProperties() {
            final SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseString(
                    "peer.discovery.enabled = true\n" +
                            "sync.enabled = true\n" +
                            "sync.fast.enabled = true\n" +
                            "database.dir = database-test-ptx\n" +
                            "database.reset = false\n"
            ));
            return props;
        }
    }
}
