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

import com.typesafe.config.ConfigFactory
import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.FrontierConfig
import org.ethereum.core.CallTransaction
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.facade.Ethereum
import org.ethereum.facade.EthereumFactory
import org.ethereum.facade.EthereumImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import java.math.BigInteger
import java.math.BigInteger.valueOf

class JsonRpcTest {

    @Test
    @Throws(Exception::class)
    fun complexTest() {
        println("Starting Ethereum...")
        val ethereum = EthereumFactory.createEthereum(TestConfig::class.java)
        println("Ethereum started")
        val testRunner = (ethereum as EthereumImpl).applicationContext.getBean(TestRunner::class.java)
        println("Starting test...")
        testRunner.runTests()
        println("Test complete.")
    }

    private open class TestConfig {

        private val config =
                // no need for discovery in that small network
                "peer.discovery.enabled = false \n" +
                        "peer.listen.port = 0 \n" +
                        // need to have different nodeId's for the peers
                        "peer.privateKey = 6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec \n" +
                        // our private net ID
                        "peer.networkId = 555 \n" +
                        // we have no peers to sync with
                        "sync.enabled = false \n" +
                        // genesis with a lower initial difficulty and some predefined known funded accounts
                        "genesis = genesis-light.json \n" +
                        // two peers need to have separate database dirs
                        "database.dir = sampleDB-1 \n" +
                        "keyvalue.datasource = inmem \n" +
                        // when more than 1 miner exist on the network extraData helps to identify the block creator
                        "mine.extraDataHex = cccccccccccccccccccc \n" +
                        "mine.fullDataSet = false \n" +
                        "mine.cpuMineThreads = 2"

        /**
         * Instead of supplying properties via config file for the peer
         * we are substituting the corresponding bean which returns required
         * config for this instance.
         */
        @Bean
        open fun systemProperties(): SystemProperties {
            val props = SystemProperties()
            props.overrideParams(ConfigFactory.parseString(config.replace("'".toRegex(), "\"")))
            val config = FrontierConfig(object : FrontierConfig.FrontierConstants() {
                override val minimumDifficulty: BigInteger
                    get() = BigInteger.ONE
            })
            SystemProperties.getDefault()!!.blockchainConfig = config
            props.blockchainConfig = config
            return props
        }

        @Bean
        open fun test(): TestRunner {
            return TestRunner()
        }
    }

    internal class TestRunner {
        @Autowired
        var jsonRpc: JsonRpc? = null

        @Autowired
        var ethereum: Ethereum? = null

        //        @PostConstruct
        @Throws(Exception::class)
        fun runTests() {
            val cowAcct = jsonRpc!!.personal_newAccount("cow")
            val bal0 = jsonRpc!!.eth_getBalance(cowAcct)
            println("Balance: " + bal0)
            assertTrue(TypeConverter.StringHexToBigInteger(bal0) > BigInteger.ZERO)

            val pendingTxFilterId = jsonRpc!!.eth_newPendingTransactionFilter()
            var changes = jsonRpc!!.eth_getFilterChanges(pendingTxFilterId)
            assertEquals(0, changes.size.toLong())

            val ca = JsonRpc.CallArguments()
            ca.from = cowAcct
            ca.to = "0x0000000000000000000000000000000000001234"
            ca.gas = "0x300000"
            ca.gasPrice = "0x10000000000"
            ca.value = "0x7777"
            ca.data = "0x"
            var sGas = TypeConverter.StringHexToBigInteger(jsonRpc!!.eth_estimateGas(ca)).toLong()

            val txHash1 = jsonRpc!!.eth_sendTransaction(cowAcct, "0x0000000000000000000000000000000000001234", "0x300000",
                    "0x10000000000", "0x7777", "0x", "0x00")
            println("Tx hash: " + txHash1)
            assertTrue(TypeConverter.StringHexToBigInteger(txHash1) > BigInteger.ZERO)

            var i = 0
            while (i < 50 && changes.isEmpty()) {
                changes = jsonRpc!!.eth_getFilterChanges(pendingTxFilterId)
                Thread.sleep(200)
                i++
            }
            assertEquals(1, changes.size.toLong())
            changes = jsonRpc!!.eth_getFilterChanges(pendingTxFilterId)
            assertEquals(0, changes.size.toLong())

            val blockResult = jsonRpc!!.eth_getBlockByNumber("pending", true)
            println(blockResult)
            assertEquals(txHash1, (blockResult.transactions[0] as TransactionResultDTO).hash)

            val hash1 = mineBlock()

            val blockResult1 = jsonRpc!!.eth_getBlockByHash(hash1, true)
            assertEquals(hash1, blockResult1.hash)
            assertEquals(txHash1, (blockResult1.transactions[0] as TransactionResultDTO).hash)
            val receipt1 = jsonRpc!!.eth_getTransactionReceipt(txHash1)
            assertEquals(1, receipt1.blockNumber)
            assertTrue(receipt1.gasUsed > 0)
            assertEquals(sGas, receipt1.gasUsed)

            val bal1 = jsonRpc!!.eth_getBalance(cowAcct)
            println("Balance: " + bal0)
            assertTrue(TypeConverter.StringHexToBigInteger(bal0) > TypeConverter.StringHexToBigInteger(bal1))

            val compRes = jsonRpc!!.eth_compileSolidity(
                    "contract A { " +
                            "uint public num; " +
                            "function set(uint a) {" +
                            "  num = a; " +
                            "  log1(0x1111, 0x2222);" +
                            "}}")
            assertEquals(compRes.info.abiDefinition[0].name, "num")
            assertEquals(compRes.info.abiDefinition[1].name, "set")
            assertTrue(compRes.code.length > 10)

            val callArgs = JsonRpc.CallArguments()
            callArgs.from = cowAcct
            callArgs.data = compRes.code
            callArgs.gasPrice = "0x10000000000"
            callArgs.gas = "0x1000000"
            val txHash2 = jsonRpc!!.eth_sendTransaction(callArgs)
            sGas = TypeConverter.StringHexToBigInteger(jsonRpc!!.eth_estimateGas(callArgs)).toLong()

            val hash2 = mineBlock()

            val blockResult2 = jsonRpc!!.eth_getBlockByHash(hash2, true)
            assertEquals(hash2, blockResult2.hash)
            assertEquals(txHash2, (blockResult2.transactions[0] as TransactionResultDTO).hash)
            val receipt2 = jsonRpc!!.eth_getTransactionReceipt(txHash2)
            assertTrue(receipt2.blockNumber > 1)
            assertTrue(receipt2.gasUsed > 0)
            assertEquals(sGas, receipt2.gasUsed)
            assertTrue(TypeConverter.StringHexToByteArray(receipt2.contractAddress).size == 20)

            val filterReq = JsonRpc.FilterRequest()
            filterReq.topics = arrayOf<Any>("0x2222")
            filterReq.fromBlock = "latest"
            filterReq.toBlock = "latest"
            val filterId = jsonRpc!!.eth_newFilter(filterReq)

            val function = CallTransaction.Function.fromSignature("set", "uint")
            val rawTx = ethereum!!.createTransaction(valueOf(2),
                    valueOf(50_000_000_000L),
                    valueOf(3000000),
                    TypeConverter.StringHexToByteArray(receipt2.contractAddress),
                    valueOf(0), function.encode(0x777))
            rawTx.sign(sha3("cow".toByteArray()))

            val txHash3 = jsonRpc!!.eth_sendRawTransaction(TypeConverter.toJsonHex(rawTx.encoded))

            val callArgs2 = JsonRpc.CallArguments()
            callArgs2.to = receipt2.contractAddress
            callArgs2.data = TypeConverter.toJsonHex(CallTransaction.Function.fromSignature("num").encode())

            val ret3 = jsonRpc!!.eth_call(callArgs2, "pending")
            val ret4 = jsonRpc!!.eth_call(callArgs2, "latest")

            val hash3 = mineBlock()

            val blockResult3 = jsonRpc!!.eth_getBlockByHash(hash3, true)
            assertEquals(hash3, blockResult3.hash)
            assertEquals(txHash3, (blockResult3.transactions[0] as TransactionResultDTO).hash)
            val receipt3 = jsonRpc!!.eth_getTransactionReceipt(txHash3)
            assertTrue(receipt3.blockNumber > 2)
            assertTrue(receipt3.gasUsed > 0)

            val logs = jsonRpc!!.eth_getFilterLogs(filterId)
            assertEquals(1, logs.size.toLong())
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000001111",
                    (logs[0] as JsonRpc.LogFilterElement).data)
            assertEquals(0, jsonRpc!!.eth_getFilterLogs(filterId).size.toLong())

            val ret1 = jsonRpc!!.eth_call(callArgs2, blockResult2.number)
            val ret2 = jsonRpc!!.eth_call(callArgs2, "latest")

            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000", ret1)
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000777", ret2)
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000777", ret3)
            assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000", ret4)
        }

        @Throws(InterruptedException::class)
        fun mineBlock(): String {
            val blockFilterId = jsonRpc!!.eth_newBlockFilter()
            jsonRpc!!.miner_start()
            var cnt = 0
            val hash1: String
            while (true) {
                val blocks = jsonRpc!!.eth_getFilterChanges(blockFilterId)
                cnt += blocks.size
                if (cnt > 0) {
                    hash1 = blocks[0] as String
                    break
                }
                Thread.sleep(100)
            }
            jsonRpc!!.miner_stop()
            Thread.sleep(100)
            val blocks = jsonRpc!!.eth_getFilterChanges(blockFilterId)
            cnt += blocks.size
            println(cnt.toString() + " blocks mined")
            val b = jsonRpc!!.eth_uninstallFilter(blockFilterId)
            assertTrue(b)
            return hash1
        }
    }
}
