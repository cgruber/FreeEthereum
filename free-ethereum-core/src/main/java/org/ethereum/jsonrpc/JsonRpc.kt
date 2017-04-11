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

package org.ethereum.jsonrpc

import org.ethereum.core.Block
import org.ethereum.core.CallTransaction
import org.ethereum.core.Transaction
import org.ethereum.jsonrpc.TypeConverter.toJsonHex
import org.ethereum.vm.LogInfo
import java.util.*

interface JsonRpc {

    fun web3_clientVersion(): String

    @Throws(Exception::class)
    fun web3_sha3(data: String): String

    fun net_version(): String

    fun net_peerCount(): String

    fun net_listening(): Boolean

    fun eth_protocolVersion(): String

    fun eth_syncing(): SyncingResult

    fun eth_coinbase(): String

    fun eth_mining(): Boolean

    fun eth_hashrate(): String

    fun eth_gasPrice(): String

    fun eth_accounts(): Array<String>

    fun eth_blockNumber(): String

    @Throws(Exception::class)
    fun eth_getBalance(address: String, block: String): String

    @Throws(Exception::class)
    fun eth_getBalance(address: String): String

    @Throws(Exception::class)
    fun eth_getStorageAt(address: String, storageIdx: String, blockId: String): String

    @Throws(Exception::class)
    fun eth_getTransactionCount(address: String, blockId: String): String

    @Throws(Exception::class)
    fun eth_getBlockTransactionCountByHash(blockHash: String): String

    @Throws(Exception::class)
    fun eth_getBlockTransactionCountByNumber(bnOrId: String): String

    @Throws(Exception::class)
    fun eth_getUncleCountByBlockHash(blockHash: String): String

    @Throws(Exception::class)
    fun eth_getUncleCountByBlockNumber(bnOrId: String): String

    @Throws(Exception::class)
    fun eth_getCode(addr: String, bnOrId: String): String

    @Throws(Exception::class)
    fun eth_sign(addr: String, data: String): String

    @Throws(Exception::class)
    fun eth_sendTransaction(transactionArgs: CallArguments): String

    // TODO: Remove, obsolete with this params
    @Throws(Exception::class)
    fun eth_sendTransaction(from: String, to: String, gas: String,
                            gasPrice: String, value: String, data: String, nonce: String): String

    @Throws(Exception::class)
    fun eth_sendRawTransaction(rawData: String): String

    @Throws(Exception::class)
    fun eth_call(args: CallArguments, bnOrId: String): String

    @Throws(Exception::class)
    fun eth_estimateGas(args: CallArguments): String

    @Throws(Exception::class)
    fun eth_getBlockByHash(blockHash: String, fullTransactionObjects: Boolean?): BlockResult

    @Throws(Exception::class)
    fun eth_getBlockByNumber(bnOrId: String, fullTransactionObjects: Boolean?): BlockResult

    @Throws(Exception::class)
    fun eth_getTransactionByHash(transactionHash: String): TransactionResultDTO

    @Throws(Exception::class)
    fun eth_getTransactionByBlockHashAndIndex(blockHash: String, index: String): TransactionResultDTO

    @Throws(Exception::class)
    fun eth_getTransactionByBlockNumberAndIndex(bnOrId: String, index: String): TransactionResultDTO

    @Throws(Exception::class)
    fun eth_getTransactionReceipt(transactionHash: String): TransactionReceiptDTO

    @Throws(Exception::class)
    fun ethj_getTransactionReceipt(transactionHash: String): TransactionReceiptDTOExt

    @Throws(Exception::class)
    fun eth_getUncleByBlockHashAndIndex(blockHash: String, uncleIdx: String): BlockResult

    @Throws(Exception::class)
    fun eth_getUncleByBlockNumberAndIndex(blockId: String, uncleIdx: String): BlockResult

    fun eth_getCompilers(): Array<String>

    fun eth_compileLLL(contract: String): CompilationResult

    @Throws(Exception::class)
    fun eth_compileSolidity(contract: String): CompilationResult

    fun eth_compileSerpent(contract: String): CompilationResult

    fun eth_resend(): String

    fun eth_pendingTransactions(): String

    @Throws(Exception::class)
    fun eth_newFilter(fr: FilterRequest): String

    fun eth_newBlockFilter(): String

    fun eth_newPendingTransactionFilter(): String

    fun eth_uninstallFilter(id: String): Boolean

    fun eth_getFilterChanges(id: String): Array<Any>

    fun eth_getFilterLogs(id: String): Array<Any>

    @Throws(Exception::class)
    fun eth_getLogs(fr: FilterRequest): Array<Any>

    fun eth_getWork(): String

    //    String eth_newFilter(String fromBlock, String toBlock, String address, String[] topics) throws Exception;

    fun eth_submitWork(): String

    fun eth_submitHashrate(): String

    fun db_putString(): String

    fun db_getString(): String

    fun db_putHex(): String

    fun db_getHex(): String

    fun shh_post(): String

    fun shh_version(): String

    fun shh_newIdentity(): String

    fun shh_hasIdentity(): String

    fun shh_newGroup(): String

    fun shh_addToGroup(): String

    fun shh_newFilter(): String

    fun shh_uninstallFilter(): String

    fun shh_getFilterChanges(): String

    fun shh_getMessages(): String

    fun admin_addPeer(s: String): Boolean

    fun admin_exportChain(): String

    fun admin_importChain(): String

    fun admin_sleepBlocks(): String

    fun admin_verbosity(): String

    fun admin_setSolc(): String

    fun admin_startRPC(): String

    fun admin_stopRPC(): String

    fun admin_setGlobalRegistrar(): String

    fun admin_setHashReg(): String

    fun admin_setUrlHint(): String

    fun admin_saveInfo(): String

    fun admin_register(): String

    fun admin_registerUrl(): String

    fun admin_startNatSpec(): String

    fun admin_stopNatSpec(): String

    fun admin_getContractInfo(): String

    fun admin_httpGet(): String

    fun admin_nodeInfo(): String

    fun admin_peers(): String

    fun admin_datadir(): String

    fun net_addPeer(): String

    fun miner_start(): Boolean

    fun miner_stop(): Boolean

    @Throws(Exception::class)
    fun miner_setEtherbase(coinBase: String): Boolean

    @Throws(Exception::class)
    fun miner_setExtra(data: String): Boolean

    fun miner_setGasPrice(newMinGasPrice: String): Boolean

    fun miner_startAutoDAG(): Boolean

    fun miner_stopAutoDAG(): Boolean

    fun miner_makeDAG(): Boolean

    fun miner_hashrate(): String

    fun debug_printBlock(): String

    fun debug_getBlockRlp(): String

    fun debug_setHead(): String

    fun debug_processBlock(): String

    fun debug_seedHash(): String

    fun debug_dumpBlock(): String

    fun debug_metrics(): String

    fun personal_newAccount(seed: String): String

    fun personal_unlockAccount(addr: String, pass: String, duration: String): Boolean

    fun personal_listAccounts(): Array<String>

    class SyncingResult {
        var startingBlock: String? = null
        var currentBlock: String? = null
        var highestBlock: String? = null

        override fun toString(): String {
            return "SyncingResult{" +
                    "startingBlock='" + startingBlock + '\'' +
                    ", currentBlock='" + currentBlock + '\'' +
                    ", highestBlock='" + highestBlock + '\'' +
                    '}'
        }
    }

    class CallArguments {
        var from: String? = null
        var to: String? = null
        var gas: String? = null
        var gasPrice: String? = null
        var value: String? = null
        var data: String? = null // compiledCode
        var nonce: String? = null

        override fun toString(): String {
            return "CallArguments{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", gasLimit='" + gas + '\'' +
                    ", gasPrice='" + gasPrice + '\'' +
                    ", value='" + value + '\'' +
                    ", data='" + data + '\'' +
                    ", nonce='" + nonce + '\'' +
                    '}'
        }
    }

    class BlockResult {
        var number: String? = null // QUANTITY - the block number. null when its pending block.
        var hash: String? = null // DATA, 32 Bytes - hash of the block. null when its pending block.
        var parentHash: String? = null // DATA, 32 Bytes - hash of the parent block.
        var nonce: String? = null // DATA, 8 Bytes - hash of the generated proof-of-work. null when its pending block.
        var sha3Uncles: String? = null // DATA, 32 Bytes - SHA3 of the uncles data in the block.
        var logsBloom: String? = null // DATA, 256 Bytes - the bloom filter for the logs of the block. null when its pending block.
        var transactionsRoot: String? = null // DATA, 32 Bytes - the root of the transaction trie of the block.
        var stateRoot: String? = null // DATA, 32 Bytes - the root of the final state trie of the block.
        var receiptsRoot: String? = null // DATA, 32 Bytes - the root of the receipts trie of the block.
        var miner: String? = null // DATA, 20 Bytes - the address of the beneficiary to whom the mining rewards were given.
        var difficulty: String? = null // QUANTITY - integer of the difficulty for this block.
        var totalDifficulty: String? = null // QUANTITY - integer of the total difficulty of the chain until this block.
        var extraData: String? = null // DATA - the "extra data" field of this block
        var size: String? = null//QUANTITY - integer the size of this block in bytes.
        var gasLimit: String? = null//: QUANTITY - the maximum gas allowed in this block.
        var gasUsed: String? = null // QUANTITY - the total used gas by all transactions in this block.
        var timestamp: String? = null //: QUANTITY - the unix timestamp for when the block was collated.
        var transactions: Array<Any>? = null //: Array - Array of transaction objects, or 32 Bytes transaction hashes depending on the last given parameter.
        var uncles: Array<String>? = null //: Array - Array of uncle hashes.

        override fun toString(): String {
            return "BlockResult{" +
                    "number='" + number + '\'' +
                    ", hash='" + hash + '\'' +
                    ", parentHash='" + parentHash + '\'' +
                    ", nonce='" + nonce + '\'' +
                    ", sha3Uncles='" + sha3Uncles + '\'' +
                    ", logsBloom='" + logsBloom + '\'' +
                    ", transactionsRoot='" + transactionsRoot + '\'' +
                    ", stateRoot='" + stateRoot + '\'' +
                    ", receiptsRoot='" + receiptsRoot + '\'' +
                    ", miner='" + miner + '\'' +
                    ", difficulty='" + difficulty + '\'' +
                    ", totalDifficulty='" + totalDifficulty + '\'' +
                    ", extraData='" + extraData + '\'' +
                    ", size='" + size + '\'' +
                    ", gasLimit='" + gasLimit + '\'' +
                    ", gasUsed='" + gasUsed + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", transactions=" + Arrays.toString(transactions) +
                    ", uncles=" + Arrays.toString(uncles) +
                    '}'
        }
    }

    class CompilationResult {
        var code: String? = null
        var info: CompilationInfo? = null

        override fun toString(): String {
            return "CompilationResult{" +
                    "code='" + code + '\'' +
                    ", info=" + info +
                    '}'
        }
    }

    class CompilationInfo {
        var source: String? = null
        var language: String? = null
        var languageVersion: String? = null
        var compilerVersion: String? = null
        var abiDefinition: Array<CallTransaction.Function>? = null
        var userDoc: String? = null
        var developerDoc: String? = null

        override fun toString(): String {
            return "CompilationInfo{" +
                    "source='" + source + '\'' +
                    ", language='" + language + '\'' +
                    ", languageVersion='" + languageVersion + '\'' +
                    ", compilerVersion='" + compilerVersion + '\'' +
                    ", abiDefinition=" + abiDefinition +
                    ", userDoc='" + userDoc + '\'' +
                    ", developerDoc='" + developerDoc + '\'' +
                    '}'
        }
    }

    class FilterRequest {
        var fromBlock: String? = null
        var toBlock: String? = null
        var address: Any? = null
        var topics: Array<Any>? = null

        override fun toString(): String {
            return "FilterRequest{" +
                    "fromBlock='" + fromBlock + '\'' +
                    ", toBlock='" + toBlock + '\'' +
                    ", address=" + address +
                    ", topics=" + Arrays.toString(topics) +
                    '}'
        }
    }

    class LogFilterElement(logInfo: LogInfo, b: Block?, txIndex: Int, tx: Transaction, logIdx: Int) {
        val logIndex: String
        val blockNumber: String?
        val blockHash: String?
        val transactionHash: String
        val transactionIndex: String?
        val address: String
        val data: String
        val topics: Array<String?>

        init {
            logIndex = toJsonHex(logIdx.toLong())
            blockNumber = if (b == null) null else toJsonHex(b.number)
            blockHash = if (b == null) null else toJsonHex(b.hash)
            transactionIndex = if (b == null) null else toJsonHex(txIndex.toLong())
            transactionHash = toJsonHex(tx.hash)
            address = toJsonHex(tx.receiveAddress)
            data = toJsonHex(logInfo.data)
            topics = arrayOfNulls<String>(logInfo.topics.size)
            for (i in topics.indices) {
                topics[i] = toJsonHex(logInfo.topics[i].data)
            }
        }

        override fun toString(): String {
            return "LogFilterElement{" +
                    "logIndex='" + logIndex + '\'' +
                    ", blockNumber='" + blockNumber + '\'' +
                    ", blockHash='" + blockHash + '\'' +
                    ", transactionHash='" + transactionHash + '\'' +
                    ", transactionIndex='" + transactionIndex + '\'' +
                    ", address='" + address + '\'' +
                    ", data='" + data + '\'' +
                    ", topics=" + Arrays.toString(topics) +
                    '}'
        }
    }
}
