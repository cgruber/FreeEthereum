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

package org.ethereum.longrun

import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.googlecode.jsonrpc4j.ProxyUtil
import com.typesafe.config.ConfigFactory
import org.apache.commons.lang3.tuple.Pair
import org.apache.commons.lang3.tuple.Triple
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.TransactionReceipt
import org.ethereum.facade.EthereumFactory
import org.ethereum.jsonrpc.JsonRpc
import org.ethereum.jsonrpc.TransactionResultDTO
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.util.ByteArrayMap
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import org.springframework.context.annotation.Bean
import java.net.URL
import java.util.*

/**
 * Matches pending transactions from EthereumJ and any other JSON-RPC client

 * Created by Anton Nashatyrev on 15.02.2017.
 */
@Ignore
class PendingTxMonitor : BasicNode("sampleNode") {
    private val localTxs = ByteArrayMap<Triple<Long, TransactionReceipt, EthereumListener.PendingTransactionState>>()
    private var remoteTxs: ByteArrayMap<Pair<Long, TransactionResultDTO>>? = null

    override fun run() {
        try {
            setupRemoteRpc()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        super.run()
    }

    @Throws(Exception::class)
    private fun setupRemoteRpc() {
        println("Creating RPC interface...")
        val httpClient = JsonRpcHttpClient(URL("http://localhost:8545"))
        val jsonRpc = ProxyUtil.createClientProxy(javaClass.classLoader, JsonRpc::class.java, httpClient)
        println("Pinging remote RPC...")
        val protocolVersion = jsonRpc.eth_protocolVersion()
        println("Remote OK. Version: " + protocolVersion)

        val pTxFilterId = jsonRpc.eth_newPendingTransactionFilter()

        Thread(Runnable {
            try {
                while (java.lang.Boolean.TRUE) {
                    val changes = jsonRpc.eth_getFilterChanges(pTxFilterId)
                    if (changes.size > 0) {
                        for (change in changes) {
                            val tx = jsonRpc.eth_getTransactionByHash(change as String)
                            newRemotePendingTx(tx)
                        }
                    }
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    override fun onSyncDoneImpl(state: EthereumListener.SyncState) {
        super.onSyncDoneImpl(state)
        if (remoteTxs == null) {
            remoteTxs = ByteArrayMap<Pair<Long, TransactionResultDTO>>()
            println("Sync Done!!!")
            ethereum!!.addListener(object : EthereumListenerAdapter() {
                override fun onPendingTransactionUpdate(txReceipt: TransactionReceipt, state: EthereumListener.PendingTransactionState, block: Block) {
                    this@PendingTxMonitor.onPendingTransactionUpdate(txReceipt, state, block)
                }
            })
        }
    }

    private fun checkUnmatched() {
        for (txHash in HashSet(localTxs.keys)) {
            val tx = localTxs[txHash]
            if (System.currentTimeMillis() - tx?.left as Long > 60000) {
                localTxs.remove(txHash)
                System.err.println("Local tx doesn't match: " + tx.middle?.transaction)
            }
        }

        for (txHash in HashSet(remoteTxs!!.keys)) {
            val tx = remoteTxs!![txHash]
            if ((System.currentTimeMillis() - tx?.left as Long) > 60000) {
                remoteTxs!!.remove(txHash)
                System.err.println("Remote tx doesn't match: " + tx.right)
            }
        }

    }

    private fun onPendingTransactionUpdate(txReceipt: TransactionReceipt, state: EthereumListener.PendingTransactionState, block: Block) {
        val txHash = txReceipt.transaction.hash
        val removed = remoteTxs!!.remove(txHash)
        if (state == EthereumListener.PendingTransactionState.DROPPED) {
            if (localTxs.remove(txHash) != null) {
                println("Dropped due to timeout (matchned: " + (removed != null) + "): " + Hex.toHexString(txHash))
            } else {
                if (remoteTxs!!.containsKey(txHash)) {
                    System.err.println("Dropped but matching: " + Hex.toHexString(txHash) + ": \n" + txReceipt)
                }
            }
        } else if (state == EthereumListener.PendingTransactionState.NEW_PENDING) {
            println("Local: " + Hex.toHexString(txHash))
            if (removed == null) {
                localTxs.put(txHash, Triple.of<Long, TransactionReceipt, EthereumListener.PendingTransactionState>(System.currentTimeMillis(), txReceipt, state))
            } else {
                println("Tx matched: " + Hex.toHexString(txHash))
            }
        }
        checkUnmatched()
    }

    private fun newRemotePendingTx(tx: TransactionResultDTO) {
        val txHash = Hex.decode(tx.hash.substring(2))
        if (remoteTxs == null) return
        println("Remote: " + Hex.toHexString(txHash))
        val removed = localTxs.remove(txHash)
        if (removed == null) {
            remoteTxs!!.put(txHash, Pair.of(System.currentTimeMillis(), tx))
        } else {
            println("Tx matched: " + Hex.toHexString(txHash))
        }
        checkUnmatched()
    }

    @Test
    @Throws(Exception::class)
    fun test() {
        testLogger.info("Starting EthereumJ regular instance!")
        EthereumFactory.createEthereum(RegularConfig::class.java)

        Thread.sleep(100000000000L)
    }

    /**
     * Spring configuration class for the Regular peer
     */
    private class RegularConfig {

        @Bean
        fun node(): PendingTxMonitor {
            return PendingTxMonitor()
        }

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        fun systemProperties(): SystemProperties {
            val props = SystemProperties()
            props.overrideParams(ConfigFactory.parseString(
                    "peer.discovery.enabled = true\n" +
                            "sync.enabled = true\n" +
                            "sync.fast.enabled = true\n" +
                            "database.dir = database-test-ptx\n" +
                            "database.reset = false\n"
            ))
            return props
        }
    }

    companion object {
        private val testLogger = LoggerFactory.getLogger("TestLogger")
    }
}
