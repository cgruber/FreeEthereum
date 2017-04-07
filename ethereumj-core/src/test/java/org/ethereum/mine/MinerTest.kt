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

package org.ethereum.mine

import com.typesafe.config.ConfigFactory
import org.apache.commons.lang3.tuple.Pair
import org.ethereum.config.NoAutoscan
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.BlockHeader
import org.ethereum.core.Transaction
import org.ethereum.core.TransactionReceipt
import org.ethereum.crypto.ECKey
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.facade.EthereumFactory
import org.ethereum.listener.EthereumListener
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.net.eth.handler.Eth62
import org.ethereum.net.eth.message.NewBlockMessage
import org.ethereum.util.ByteUtil
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Long running test

 * Creates an instance

 * Created by Anton Nashatyrev on 13.10.2015.
 */
@Ignore
class MinerTest {

    private val submittedTxs = Collections.synchronizedMap(
            HashMap<ByteArrayWrapper, Pair<Transaction, Long>>())

    @Test
    @Throws(Exception::class)
    fun startMiningConsumer() {
        SysPropConfig2.props.overrideParams(ConfigFactory.parseString(
                "peer.listen.port = 30336 \n" +
                        "peer.privateKey = 3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c \n" +
                        "peer.networkId = 555 \n" +
                        "peer.active = [" +
                        "{ url = \"enode://b23b3b9f38f1d314b27e63c63f3e45a6ea5f82c83f282e2d38f2f01c22165e897656fe2e5f9f18616b81f41cbcf2e9100fc9f8dad099574f3d84cf9623de2fc9@localhost:20301\" }," +
                        "{ url = \"enode://26ba1aadaf59d7607ad7f437146927d79e80312f026cfa635c6b2ccf2c5d3521f5812ca2beb3b295b14f97110e6448c1c7ff68f14c5328d43a3c62b44143e9b1@localhost:30335\" }" +
                        "] \n" +
                        "sync.enabled = true \n" +
                        "genesis = genesis-harder.json \n" +
                        //                        "genesis = frontier.json \n" +
                        "database.dir = testDB-1 \n"))

        val ethereum2 = EthereumFactory.createEthereum(SysPropConfig2.props, SysPropConfig2::class.java)

        val semaphore = CountDownLatch(1)
        ethereum2.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                System.err.println("=== New block: " + blockInfo(block))
                System.err.println(block)

                for (tx in block.transactionsList) {
                    //                    Pair<Transaction, Long> remove = submittedTxs.remove(new ByteArrayWrapper(tx.getHash()));
                    //                    if (remove == null) {
                    //                        System.err.println("===== !!! Unknown Tx: " + tx);
                    //                    } else {
                    //                        System.out.println("===== Tx included in " + (System.currentTimeMillis() - remove.getRight()) / 1000
                    //                                + " sec: " + tx);
                    //                    }

                }

                //                for (Pair<Transaction, Long> pair : submittedTxs.values()) {
                //                    if (System.currentTimeMillis() - pair.getRight() > 60 * 1000) {
                //                        System.err.println("==== !!! Lost Tx: " + (System.currentTimeMillis() - pair.getRight()) / 1000
                //                                + " sec: " + pair.getLeft());
                //                    }
                //                }
            }

            override fun onPendingTransactionsReceived(transactions: List<Transaction>) {
                System.err.println("=== Tx: " + transactions)
            }

            override fun onSyncDone(state: EthereumListener.SyncState) {
                semaphore.countDown()
                System.err.println("=== Sync Done!")
            }
        })

        println("Waiting for sync...")
        semaphore.await()

        //        ECKey senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        //        byte[] receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c");
        val senderKey = ECKey.fromPrivate(Hex.decode("6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec"))
        val receiverAddr = Hex.decode("5db10750e8caff27f906b41c71b3471057dd2004")

        var i = ethereum2.repository.getNonce(senderKey.address).toInt()
        var j = 0
        while (j < 200) {
            run {
                val tx = Transaction(ByteUtil.intToBytesNoLeadZeroes(i),
                        ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L), ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                        receiverAddr, byteArrayOf(77), ByteArray(0))
                tx.sign(senderKey)
                println("=== Submitting tx: " + tx)
                ethereum2.submitTransaction(tx)

                submittedTxs.put(ByteArrayWrapper(tx.hash), Pair.of(tx, System.currentTimeMillis()))
            }
            Thread.sleep(7000)
            i++
            j++
        }

        Thread.sleep(100000000L)
    }

    @Test
    @Throws(FileNotFoundException::class, InterruptedException::class)
    fun startMiningTest() {
        SysPropConfig1.props.overrideParams(ConfigFactory.parseString(
                "peer.listen.port = 30335 \n" +
                        "peer.privateKey = 6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec \n" +
                        "peer.networkId = 555 \n" +
                        "peer.active = [{ url = \"enode://b23b3b9f38f1d314b27e63c63f3e45a6ea5f82c83f282e2d38f2f01c22165e897656fe2e5f9f18616b81f41cbcf2e9100fc9f8dad099574f3d84cf9623de2fc9@localhost:20301\" }] \n" +
                        "sync.enabled = true \n" +
                        "genesis = genesis-harder.json \n" +
                        //                        "genesis = frontier.json \n" +
                        "database.dir = testDB-2 \n" +
                        "mine.extraDataHex = cccccccccccccccccccc \n" +
                        "mine.cpuMineThreads = 2"))

        //        SysPropConfig2.props.overrideParams(ConfigFactory.parseString(
        //                "peer.listen.port = 30336 \n" +
        //                        "peer.privateKey = 3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c \n" +
        //                        "peer.networkId = 555 \n" +
        //                        "peer.active = [{ url = \"enode://b23b3b9f38f1d314b27e63c63f3e45a6ea5f82c83f282e2d38f2f01c22165e897656fe2e5f9f18616b81f41cbcf2e9100fc9f8dad099574f3d84cf9623de2fc9@localhost:20301\" }] \n" +
        //                        "sync.enabled = true \n" +
        //                        "genesis = genesis-light.json \n" +
        ////                        "genesis = frontier.json \n" +
        //                        "database.dir = testDB-1 \n"));

        val ethereum1 = EthereumFactory.createEthereum(SysPropConfig1.props, SysPropConfig1::class.java)
        //        Ethereum ethereum2 = EthereumFactory.createEthereum(SysPropConfig2.props, SysPropConfig2.class);

        val semaphore = CountDownLatch(1)
        ethereum1.addListener(object : EthereumListenerAdapter() {
            override fun onBlock(block: Block, receipts: List<TransactionReceipt>) {
                println("=== New block: " + blockInfo(block))
            }

            override fun onSyncDone(state: EthereumListener.SyncState) {
                semaphore.countDown()
            }
        })

        //        ethereum2.addListener(new EthereumListenerAdapter() {
        //            @Override
        //            public void onBlock(Block block, List<TransactionReceipt> receipts) {
        //                System.err.println("=== New block: " + block);
        //            }
        //
        //            @Override
        //            public void onPendingStateChanged(List<Transaction> transactions) {
        //                System.err.println("=== Tx: " + transactions);
        //            }
        //        });

        println("=== Waiting for sync ...")
        semaphore.await(600, TimeUnit.SECONDS)
        println("=== Sync done.")


        val blockMiner = ethereum1.blockMiner
        blockMiner.addListener(object : MinerListener {
            override fun miningStarted() {
                println("=== MinerTest.miningStarted")
            }

            override fun miningStopped() {
                println("=== MinerTest.miningStopped")
            }

            override fun blockMiningStarted(block: Block) {
                println("=== MinerTest.blockMiningStarted " + blockInfo(block))
            }

            override fun blockMined(block: Block) {
                //                boolean validate = Ethash.getForBlock(block.getNumber()).validate(block.getHeader());
                println("=== MinerTest.blockMined " + blockInfo(block))
                //                System.out.println("=== MinerTest.blockMined: " + validate);
            }

            override fun blockMiningCanceled(block: Block) {
                println("=== MinerTest.blockMiningCanceled " + blockInfo(block))
            }
        })
        Ethash.fileCacheEnabled = true
        blockMiner.setFullMining(true)
        blockMiner.startMining()

        //        System.out.println("======= Waiting for block #4");
        //        semaphore.await(60, TimeUnit.SECONDS);
        //        if(semaphore.getCount() > 0) {
        //            throw new RuntimeException("4 blocks were not imported.");
        //        }
        //
        //        System.out.println("======= Sending forked block without parent...");


        val senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"))
        val receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c")

        var i = ethereum1.repository.getNonce(senderKey.address).toInt()
        var j = 0
        while (j < 20000) {
            run {
                val tx = Transaction(ByteUtil.intToBytesNoLeadZeroes(i),
                        ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L), ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                        receiverAddr, byteArrayOf(77), ByteArray(0))
                tx.sign(senderKey)
                println("=== Submitting tx: " + tx)
                ethereum1.submitTransaction(tx)

                submittedTxs.put(ByteArrayWrapper(tx.hash), Pair.of(tx, System.currentTimeMillis()))
            }
            Thread.sleep(5000)
            i++
            j++
        }

        Thread.sleep(1000000000)

        ethereum1.close()

        println("Passed.")

    }

    @Test
    fun blockTest() {
        val rlp = "f90498f90490f90217a0887405e8cf98cfdbbf5ab4521d45a0803e397af61852d94dc46ca077787088bfa0d6d234f05ac90931822b7b6f244cef81" +
                "83e5dd3080483273d6c2fcc02399216294ee0250c19ad59305b2bdb61f34b45b72fe37154fa0caa558a47a66b975426c6b963c46bb83d969787cfedc09fd2cab8ab83155568da07c970ab3f2004e2aa0" +
                "7d7b3dda348a3a4f8f4ab98b3504c46c1ffae09ef5bd23a077007d3a4c7c88823e9f80b1ba48ec74f88f40e9ec9c5036235fc320fe8a29ffb90100000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008311fc6782" +
                "02868508cc8cac48825208845682c3479ad983010302844765746887676f312e352e318777696e646f7773a020269a7f434e365b012659d1b190aa1586c22d7bbf0ed7fad73ca7d2b60f623c8841c814" +
                "3ae845fb3df867f8656e850ba43b7400830fffff9431e2e1ed11951c7091dfba62cd4b7145e947219c4d801ba0ad82686ee482723f88d9760b773e7b8234f126271e53369b928af0cbab302baea04fe3" +
                "dd2e0dbcc5d53123fe49f3515f0844c7e4c6dd3752f0cf916f4bb5cbe80bf9020af90207a08752c2c47c96537cf695bdecc9696a8ea35b6bfdc1389a134b47ad63fea38c2ca01dcc4de8dec75d7aab85" +
                "b567b6ccd41ad312451b948a7413f0a142fd40d493479450b8f981ce93fd5b81b844409169148428400bf3a05bff1dc620da4d3a123f8e08536434071281d64fc106105fb3bc94b6b1b8913ba0b59542" +
                "42bb4483396ae474b02651b40d4a9d61ab99a7455e57ef31f2688bdf81a03068c58706501d3059f40a5366debf4bf1cad48439d19f00154076c5d96c26d6b90100000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "008311f7ea8202848508d329637f825208845682c3438acccccccccccccccccccca0216ef6369d46fe0ad70d2b7f9aa0785f33cbb8894733859136b5143c0ed8b09f88bc45a9ac8c1cea67842b0113c6"
        val msg = NewBlockMessage(Hex.decode(rlp))
        val b = msg.block
        println(b)
        println(msg.difficultyAsBigInt.toString(16))
    }

    @Test
    fun blockTest1() {
        val rlp = "f90214a0a9c93d6bcd5dbcc94e0f467c88c59851b0951990c1c340c7b20aa967758ecd87a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d4934794ee0250c19ad59305b2bdb61f34b45b72fe37154fa0ed230be6c9531d27a387fa5ca2cb2e70848d5de33636ae2a28d5e9e623a3089da056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000831116b881d9850ec4a4290b8084567406d39ad983010302844765746887676f312e352e318777696e646f7773a09a518a25af220fb8afe23bcafd71e4a0dba0da38972e962b07ed89dab34ac2748872311081e40c488a | f90214a01e479ea9dc53e675780469952ea87d531eb3d47808e2b57339055bdc6e61ae57a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d4934794ee0250c19ad59305b2bdb61f34b45b72fe37154fa0ea92e8c9e36ffe81be59f06af1a3b4b18b778838d4fac19f4dfeb08a5a1046dfa056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000831118da81da850ec0f300028084567406dd9ad983010302844765746887676f312e352e318777696e646f7773a056bacad6b399e5e39f5168080941d54807f25984544f6bc885bbf1a0ffd0a0298856ceeb676b74d420 | " + "f90214a0b7992b18db1b3514b90376fe96235bc73db9eba3cb21ecb190d34e9c680c914fa06ab8465069b6d6a758c73894d6fbd2ad98f9c551a7a99672aedba3b12d1e76f594ee0250c19ad59305b2bdb61f34b45b72fe37154fa0499179489d7c04a781c3fd8b8b0f0a04030fd2057a11e86e6a12e4baa07dfdd6a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b901000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000083111afd81db850ebd42c3438084567406e19ad983010302844765746887676f312e352e318777696e646f7773a04d78ab8e21ea8630f52a60ed60d945c7bbb8267777d28a98612b77a673663430886b676858a8d6b99a | f90204a08680005ea64540a769286d281cb931a97e7abed2611f00f2c6f47a7aaad5faa8a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d493479450b8f981ce93fd5b81b844409169148428400bf3a0bf55a6e82564fb532316e694838dae38d21fa80bc8af1867e418cb26bcdf0e61a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000831118da81dc850ebd42c3438084567406f58acccccccccccccccccccca04c2135ea0bb1f148303b201141df207898fa0897e3fb48fe661cda3ba2880da388b4ce6cb5535077af | f90204a05897e0e01ed54cf189751c6dd7b0107b2b3b1c841cb7d9bdb6f2aca6ff770c17a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d493479450b8f981ce93fd5b81b844409169148428400bf3a01ad754d90de6789c4fa5708992d9f089165e5047d52e09abc22cf49428af23cda056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000831116b781dd850ebd42c3438084567407048acccccccccccccccccccca0b314fab93d91a0adea632fd58027cb39857a0ad188c473f41a640052a6a0141d88d64656f7bb5f1066"
        for (s in rlp.split("\\|".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()) {
            val blockHeader = BlockHeader(Hex.decode(s))
            println(Hex.toHexString(blockHeader.hash).substring(0, 6))
            println(blockHeader)
        }
    }

    @Configuration
    @NoAutoscan
    class SysPropConfig1 {

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        companion object {
            internal val props = SystemProperties()
            internal var testHandler: Eth62? = null
        }
    }

    @Configuration
    @NoAutoscan
    class SysPropConfig2 {

        @Bean
        fun systemProperties(): SystemProperties {
            return props
        }

        companion object {
            internal val props = SystemProperties()
            internal var testHandler: Eth62? = null
        }
    }

    companion object {

        @BeforeClass
        fun setup() {
            //        Constants.MINIMUM_DIFFICULTY = BigInteger.valueOf(1);
        }

        private fun blockInfo(b: Block): String {
            val ours = Hex.toHexString(b.extraData).startsWith("cccccccccc")
            var txs = StringBuilder("Tx[")
            for (tx in b.transactionsList) {
                txs.append(ByteUtil.byteArrayToLong(tx.nonce)).append(", ")
            }
            txs = StringBuilder(txs.substring(0, txs.length - 2) + "]")
            return (if (ours) "##" else "  ") + b.shortDescr + " " + txs
        }

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            val k = ECKey.fromPrivate(Hex.decode("6ef8da380c27cea8fdf7448340ea99e8e2268fc2950d79ed47cbf6f85dc977ec"))
            println(Hex.toHexString(k.privKeyBytes!!))
            println(Hex.toHexString(k.address))
            println(Hex.toHexString(k.nodeId))
        }
    }


}
