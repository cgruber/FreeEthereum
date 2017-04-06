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

package org.ethereum.core

import org.ethereum.config.CommonConfig
import org.ethereum.config.SystemProperties
import org.ethereum.core.genesis.GenesisLoader
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil
import org.ethereum.datasource.NoDeleteSource
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.db.IndexedBlockStore
import org.ethereum.db.RepositoryRoot
import org.ethereum.listener.EthereumListenerAdapter
import org.ethereum.mine.Ethash
import org.ethereum.util.ByteUtil
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.ethereum.validator.DependentBlockHeaderRuleAdapter
import org.ethereum.vm.LogInfo
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl
import org.junit.*
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.util.*

/**
 * Created by Anton Nashatyrev on 29.12.2015.
 */
class ImportLightTestBroken {

    @Test
    fun simpleFork() {
        val sb = StandaloneBlockchain()
        val b1 = sb.createBlock()
        val b2_ = sb.createBlock()
        val b3_ = sb.createForkBlock(b2_)
        val b2 = sb.createForkBlock(b1)
        val b3 = sb.createForkBlock(b2)
        val b4 = sb.createForkBlock(b3)
        val b5 = sb.createForkBlock(b4)
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun importBlocks() {
        val logger = LoggerFactory.getLogger("VM")
        logger.info("#######################################")
        val blockchain = createBlockchain(GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/frontier.json")))
        val scanner = Scanner(FileInputStream("D:\\ws\\ethereumj\\work\\blocks-rec.dmp"))
        while (scanner.hasNext()) {
            val blockHex = scanner.next()
            val block = Block(Hex.decode(blockHex))
            val result = blockchain.tryToConnect(block)
            if (result !== ImportResult.EXIST && result !== ImportResult.IMPORTED_BEST) {
                throw RuntimeException(result.toString() + ": " + block + "")
            }
            println("Imported " + block.shortDescr)
        }
    }

    @Ignore // periodically get different roots ?
    @Test
    fun putZeroValue() {
        val sb = StandaloneBlockchain()
        val a = sb.submitNewContract("contract A { uint public a; function set() { a = 0;}}")
        a.callFunction("set")
        val block = sb.createBlock()
        println(Hex.toHexString(block.stateRoot))
        Assert.assertEquals("cad42169cafc7855c25b8889df83faf38e493fb6e95b2c9c8e155dbc340160d6", Hex.toHexString(block.stateRoot))
    }

    @Test
    fun simpleRebranch() {
        val sb = StandaloneBlockchain()
        val b0 = sb.blockchain.bestBlock

        val addr1 = ECKey.fromPrivate(HashUtil.sha3("1".toByteArray()))
        val bal2 = sb.blockchain.repository.getBalance(sb.sender.address)

        sb.sendEther(addr1.address, BigInteger.valueOf(100))
        val b1 = sb.createBlock()
        sb.sendEther(addr1.address, BigInteger.valueOf(100))
        val b2 = sb.createBlock()
        sb.sendEther(addr1.address, BigInteger.valueOf(100))
        val b3 = sb.createBlock()

        val bal1 = sb.blockchain.repository.getBalance(addr1.address)
        Assert.assertEquals(BigInteger.valueOf(300), bal1)

        sb.sendEther(addr1.address, BigInteger.valueOf(200))
        val b1_ = sb.createForkBlock(b0)
        sb.sendEther(addr1.address, BigInteger.valueOf(200))
        val b2_ = sb.createForkBlock(b1_)
        sb.sendEther(addr1.address, BigInteger.valueOf(200))
        val b3_ = sb.createForkBlock(b2_)
        sb.sendEther(addr1.address, BigInteger.valueOf(200))
        val b4_ = sb.createForkBlock(b3_)

        val bal1_ = sb.blockchain.repository.getBalance(addr1.address)
        Assert.assertEquals(BigInteger.valueOf(800), bal1_)
        //        BigInteger bal2_ = sb.getBlockchain().getRepository().getBalance(sb.getSender().getAddress());
        //        Assert.assertEquals(bal2, bal2_);
    }

    @Test
    @Throws(Exception::class)
    fun createFork() {
        // importing forked chain
        val blockchain = createBlockchain(GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/genesis-light.json")))
        blockchain.minerCoinbase = Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f")
        val parent = blockchain.bestBlock

        println("Mining #1 ...")
        val b1 = blockchain.createNewBlock(parent, Collections.emptyList(), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b1.number).mineLight(b1).get()
        var importResult = blockchain.tryToConnect(b1)
        println("Best: " + blockchain.bestBlock.shortDescr)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)

        println("Mining #2 ...")
        val b2 = blockchain.createNewBlock(b1, Collections.emptyList(), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b2.number).mineLight(b2).get()
        importResult = blockchain.tryToConnect(b2)
        println("Best: " + blockchain.bestBlock.shortDescr)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)

        println("Mining #3 ...")
        val b3 = blockchain.createNewBlock(b2, Collections.emptyList(), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b3.number).mineLight(b3).get()
        importResult = blockchain.tryToConnect(b3)
        println("Best: " + blockchain.bestBlock.shortDescr)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)

        println("Mining #2' ...")
        val b2_ = blockchain.createNewBlock(b1, Collections.emptyList(), Collections.emptyList())
        b2_.extraData = byteArrayOf(77, 77) // setting extra data to differ from block #2
        Ethash.getForBlock(SystemProperties.getDefault(), b2_.number).mineLight(b2_).get()
        importResult = blockchain.tryToConnect(b2_)
        println("Best: " + blockchain.bestBlock.shortDescr)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_NOT_BEST)

        println("Mining #3' ...")
        val b3_ = blockchain.createNewBlock(b2_, Collections.emptyList(), listOf(b2.header))
        Ethash.getForBlock(SystemProperties.getDefault(), b3_.number).mineLight(b3_).get()
        importResult = blockchain.tryToConnect(b3_)
        println("Best: " + blockchain.bestBlock.shortDescr)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_NOT_BEST)
    }

    @Test
    @Throws(Exception::class)
    fun invalidBlockTest() {
        // testing that bad block import effort doesn't affect the repository state

        val blockchain = createBlockchain(GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/genesis-light.json")))
        blockchain.minerCoinbase = Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f")
        val parent = blockchain.bestBlock

        val senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"))
        val receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c")

        println("Mining #1 ...")

        val tx = Transaction(ByteUtil.intToBytesNoLeadZeroes(0),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(77), ByteArray(0))
        tx.sign(senderKey.privKeyBytes)

        val b1bad = blockchain.createNewBlock(parent, listOf(tx), Collections.emptyList())
        // making the block bad
        b1bad.stateRoot[0] = 0
        b1bad.stateRoot = b1bad.stateRoot // invalidate block

        Ethash.getForBlock(SystemProperties.getDefault(), b1bad.number).mineLight(b1bad).get()
        var importResult = blockchain.tryToConnect(b1bad)
        Assert.assertTrue(importResult === ImportResult.INVALID_BLOCK)
        val b1 = blockchain.createNewBlock(parent, listOf(tx), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b1.number).mineLight(b1).get()
        importResult = blockchain.tryToConnect(b1)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)
    }

    @Test
    @Throws(Exception::class)
    fun doubleTransactionTest() {
        // Testing that blocks containing tx with invalid nonce are rejected

        val blockchain = createBlockchain(GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/genesis-light.json")))
        blockchain.minerCoinbase = Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f")
        val parent = blockchain.bestBlock

        val senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"))
        val receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c")

        println("Mining #1 ...")

        val tx = Transaction(ByteUtil.intToBytesNoLeadZeroes(0),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(77), ByteArray(0))
        tx.sign(senderKey)

        val b1 = blockchain.createNewBlock(parent, listOf(tx), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b1.number).mineLight(b1).get()
        var importResult = blockchain.tryToConnect(b1)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)

        println("Mining #2 (bad) ...")
        var b2 = blockchain.createNewBlock(b1, listOf(tx), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b2.number).mineLight(b2).get()
        importResult = blockchain.tryToConnect(b2)
        Assert.assertTrue(importResult === ImportResult.INVALID_BLOCK)

        println("Mining #2 (bad) ...")
        var tx1 = Transaction(ByteUtil.intToBytesNoLeadZeroes(1),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(77), ByteArray(0))
        tx1.sign(senderKey)
        b2 = blockchain.createNewBlock(b1, Arrays.asList(tx1, tx1), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b2.number).mineLight(b2).get()
        importResult = blockchain.tryToConnect(b2)
        Assert.assertTrue(importResult === ImportResult.INVALID_BLOCK)

        println("Mining #2 ...")
        var tx2 = Transaction(ByteUtil.intToBytesNoLeadZeroes(2),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(77), ByteArray(0))
        tx2.sign(senderKey)
        b2 = blockchain.createNewBlock(b1, Arrays.asList(tx1, tx2), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b2.number).mineLight(b2).get()
        importResult = blockchain.tryToConnect(b2)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)

        println("Mining #2 (fork) ...")
        tx1 = Transaction(ByteUtil.intToBytesNoLeadZeroes(1),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(88), ByteArray(0))
        tx1.sign(senderKey)
        val b2f = blockchain.createNewBlock(b1, listOf(tx1), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b2f.number).mineLight(b2f).get()
        importResult = blockchain.tryToConnect(b2f)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_NOT_BEST)

        println("Mining #3 ...")
        tx1 = Transaction(ByteUtil.intToBytesNoLeadZeroes(3),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(88), ByteArray(0))
        tx1.sign(senderKey)
        tx2 = Transaction(ByteUtil.intToBytesNoLeadZeroes(4),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(88), ByteArray(0))
        tx2.sign(senderKey)
        val tx3 = Transaction(ByteUtil.intToBytesNoLeadZeroes(5),
                ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, byteArrayOf(88), ByteArray(0))
        tx3.sign(senderKey)
        val b3 = blockchain.createNewBlock(b2, Arrays.asList(tx1, tx2, tx3), Collections.emptyList())
        Ethash.getForBlock(SystemProperties.getDefault(), b3.number).mineLight(b3).get()
        importResult = blockchain.tryToConnect(b3)
        Assert.assertTrue(importResult === ImportResult.IMPORTED_BEST)
    }

    @Test
    @Throws(Exception::class)
    fun invalidBlockTotalDiff() {
        // Check that importing invalid block doesn't affect totalDifficulty

        val blockchain = createBlockchain(GenesisLoader.loadGenesis(
                javaClass.getResourceAsStream("/genesis/genesis-light.json")))
        blockchain.minerCoinbase = Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f")
        val parent = blockchain.bestBlock

        println("Mining #1 ...")

        val totalDifficulty = blockchain.totalDifficulty

        val b1 = blockchain.createNewBlock(parent, Collections.emptyList(), Collections.emptyList())
        b1.stateRoot = ByteArray(32)
        Ethash.getForBlock(SystemProperties.getDefault(), b1.number).mineLight(b1).get()
        val importResult = blockchain.tryToConnect(b1)
        Assert.assertTrue(importResult === ImportResult.INVALID_BLOCK)
        Assert.assertEquals(totalDifficulty, blockchain.totalDifficulty)

    }

    @Test
    fun simpleDbTest() {
        val bc = StandaloneBlockchain()
        val parent = bc.submitNewContract("contract A {" +
                "  uint public a;" +
                "  function set(uint a_) { a = a_;}" +
                "}")
        bc.createBlock()
        parent.callFunction("set", 123)
        bc.createBlock()
        val ret = parent.callConstFunction("a")[0]
        println("Ret = " + ret)
    }

    @Test
    @Throws(Exception::class)
    fun createContractFork() {
        //  #1 (Parent) --> #2 --> #3 (Child) ----------------------> #4 (call Child)
        //    \-------------------------------> #2' (forked Child)
        //
        // Testing the situation when the Child contract is created by the Parent contract
        // first on the main chain with one parameter (#3) and then on the fork with another parameter (#2')
        // so their storages are different. Check that original Child storage is not broken
        // on the main chain (#4)

        val contractSrc = "contract Child {" +
                "  int a;" +
                "  int b;" +
                "  int public c;" +
                "  function Child(int i) {" +
                "    a = 333 + i;" +
                "    b = 444 + i;" +
                "  }" +
                "  function sum() {" +
                "    c = a + b;" +
                "  }" +
                "}" +
                "contract Parent {" +
                "  address public child;" +
                "  function createChild(int a) returns (address) {" +
                "    child = new Child(a);" +
                "    return child;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val parent = bc.submitNewContract(contractSrc, "Parent")
        val b1 = bc.createBlock()
        val b2 = bc.createBlock()
        parent.callFunction("createChild", 100)
        val b3 = bc.createBlock()
        val childAddress = parent.callConstFunction("child")[0] as ByteArray
        parent.callFunction("createChild", 200)
        val b2_ = bc.createForkBlock(b1)
        val child = bc.createExistingContractFromSrc(contractSrc, "Child", childAddress)
        child.callFunction("sum")
        val b4 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf((100 + 333 + 100 + 444).toLong()), child.callConstFunction("c")[0])
    }

    @Test
    @Throws(Exception::class)
    fun createContractFork1() {
        // Test creation of the contract on forked branch with different storage
        val contractSrc = "contract A {" +
                "  int public a;" +
                "  function A() {" +
                "    a = 333;" +
                "  }" +
                "}" +
                "contract B {" +
                "  int public a;" +
                "  function B() {" +
                "    a = 111;" +
                "  }" +
                "}"

        run {
            val bc = StandaloneBlockchain()
            val b1 = bc.createBlock()
            val b2 = bc.createBlock()
            val a = bc.submitNewContract(contractSrc, "A")
            val b3 = bc.createBlock()
            val b = bc.submitNewContract(contractSrc, "B")
            val b2_ = bc.createForkBlock(b1)
            Assert.assertEquals(BigInteger.valueOf(333), a.callConstFunction("a")[0])
            Assert.assertEquals(BigInteger.valueOf(111), b.callConstFunction(b2_, "a")[0])
            val b3_ = bc.createForkBlock(b2_)
            val b4_ = bc.createForkBlock(b3_)
            Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0])
            Assert.assertEquals(BigInteger.valueOf(333), a.callConstFunction(b3, "a")[0])

        }
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun createValueTest() {
        // checks that correct msg.value is passed when contract internally created with value
        val contract = "pragma solidity ^0.4.3;\n" +
                "contract B {\n" +
                "      uint public valReceived;\n" +
                "      \n" +
                "      function B() payable {\n" +
                "        valReceived = msg.value;\n" +
                "      }\n" +
                "}\n" +
                "contract A {\n" +
                "    function () payable { }\n" +
                "    address public child;\n" +
                "    function create() payable {\n" +
                "        child = (new B).value(20)();\n" +
                "    }\n" +
                "}"
        val bc = StandaloneBlockchain().withAutoblock(true)
        val a = bc.submitNewContract(contract, "A")
        bc.sendEther(a.address, BigInteger.valueOf(10000))
        a.callFunction(10, "create")
        val childAddress = a.callConstFunction("child")[0] as ByteArray
        val b = bc.createExistingContractFromSrc(contract, "B", childAddress)
        val `val` = b.callConstFunction("valReceived")[0] as BigInteger
        Assert.assertEquals(20, `val`.toLong())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun contractCodeForkTest() {
        val contractA = "contract A {" +
                "  function call() returns (uint) {" +
                "    return 111;" +
                "  }" +
                "}"

        val contractB = "contract B {" +
                "  function call() returns (uint) {" +
                "    return 222222;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val b1 = bc.createBlock()
        val a = bc.submitNewContract(contractA)
        val b2 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("call")[0])
        val b = bc.submitNewContract(contractB)
        val b2_ = bc.createForkBlock(b1)
        val b3 = bc.createForkBlock(b2)
        Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("call")[0])
        Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction(b2, "call")[0])
        Assert.assertEquals(BigInteger.valueOf(222222), b.callConstFunction(b2_, "call")[0])
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun operateNotExistingContractTest() {
        // checking that addr.balance doesn't cause the account to be created
        // and the subsequent call to that non-existent address costs 25K gas
        val addr = Hex.decode("0101010101010101010101010101010101010101")
        val contractA = "pragma solidity ^0.4.3;" +
                "contract B { function dummy() {}}" +
                "contract A {" +
                "  function callBalance() returns (uint) {" +
                "    address addr = 0x" + Hex.toHexString(addr) + ";" +
                "    uint bal = addr.balance;" +
                "  }" +
                "  function callMethod() returns (uint) {" +
                "    address addr = 0x" + Hex.toHexString(addr) + ";" +
                "    B b = B(addr);" +
                "    b.dummy();" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
                .withGasPrice(1)
                .withGasLimit(5_000_000L)
        val a = bc.submitNewContract(contractA, "A")
        bc.createBlock()

        run {
            val balance1 = getSenderBalance(bc)
            a.callFunction("callBalance")
            bc.createBlock()
            val balance2 = getSenderBalance(bc)
            val spent = balance1.subtract(balance2).toLong()

            // checking balance of not existed address should take
            // less that gas limit
            Assert.assertEquals(21508, spent)
        }

        run {
            val balance1 = getSenderBalance(bc)
            a.callFunction("callMethod")
            bc.createBlock()
            val balance2 = getSenderBalance(bc)
            val spent = balance1.subtract(balance2).toLong()

            // invalid jump error occurred
            // all gas wasted
            // (for history: it is worked fine in ^0.3.1)
            Assert.assertEquals(5_000_000L, spent)
        }
    }

    private fun getSenderBalance(bc: StandaloneBlockchain): BigInteger {
        return bc.blockchain.repository.getBalance(bc.sender.address)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun spendGasSimpleTest() {
        // check the caller spend value for tx
        val bc = StandaloneBlockchain().withGasPrice(1)
        val balance1 = bc.blockchain.repository.getBalance(bc.sender.address)
        bc.sendEther(ByteArray(20), BigInteger.ZERO)
        bc.createBlock()
        val balance2 = bc.blockchain.repository.getBalance(bc.sender.address)
        val spent = balance1.subtract(balance2).toLong()
        Assert.assertNotEquals(0, spent)
    }

    @Test
    @Throws(Exception::class)
    fun deepRecursionTest() {
        val contractA = "contract A {" +
                "  function recursive(){" +
                "    this.recursive();" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain().withGasLimit(5000000)
        val a = bc.submitNewContract(contractA, "A")
        bc.createBlock()
        a.callFunction("recursive")
        bc.createBlock()

        // no StackOverflowException
    }

    @Test
    @Throws(Exception::class)
    fun prevBlockHashOnFork() {
        val contractA = "contract A {" +
                "  bytes32 public blockHash;" +
                "  function a(){" +
                "    blockHash = block.blockhash(block.number - 1);" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val a = bc.submitNewContract(contractA)
        val b1 = bc.createBlock()
        val b2 = bc.createBlock()
        val b3 = bc.createBlock()
        val b4 = bc.createBlock()
        val b5 = bc.createBlock()
        val b6 = bc.createBlock()
        val b2_ = bc.createForkBlock(b1)
        a.callFunction("a")
        val b3_ = bc.createForkBlock(b2_)
        val hash = a.callConstFunction(b3_, "blockHash")[0]

        Assert.assertArrayEquals(hash as ByteArray, b2_.hash)

        // no StackOverflowException
    }

    @Test
    @Throws(Exception::class)
    fun rollbackInternalTx() {
        val contractA = "contract A {" +
                "  uint public a;" +
                "  uint public b;" +
                "  function f() {" +
                "    b = 1;" +
                "    this.call(bytes4(sha3('exception()')));" +
                "    a = 2;" +
                "  }" +

                "  function exception() {" +
                "    b = 2;" +
                "    throw;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val a = bc.submitNewContract(contractA)
        bc.createBlock()
        a.callFunction("f")
        bc.createBlock()
        val av = a.callConstFunction("a")[0]
        val bv = a.callConstFunction("b")[0]

        assert(BigInteger.valueOf(2) == av)
        assert(BigInteger.valueOf(1) == bv)
    }

    @Test
    @Throws(Exception::class)
    fun selfdestructAttack() {
        val contractSrc = "" +
                "pragma solidity ^0.4.3;" +
                "contract B {" +
                "  function suicide(address benefic) {" +
                "    selfdestruct(benefic);" +
                "  }" +
                "}" +
                "contract A {" +
                "  uint public a;" +
                "  function f() {" +
                "    B b = new B();" +
                "    for (uint i = 0; i < 3500; i++) {" +
                "      b.suicide(address(i));" +
                "    }" +
                "    a = 2;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
                .withGasLimit(1_000_000_000L)
                .withDbDelay(0)
        val a = bc.submitNewContract(contractSrc, "A")
        bc.createBlock()
        a.callFunction("f")
        bc.createBlock()
        val stateRoot = Hex.toHexString(bc.blockchain.repository.root)
        //        Assert.assertEquals("82d5bdb6531e26011521da5601481c9dbef326aa18385f2945fd77bee288ca31", stateRoot);
        val av = a.callConstFunction("a")[0]
        assert(BigInteger.valueOf(2) == av)
        assert(bc.totalDbHits < 8300) // reduce this assertion if you make further optimizations
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun threadRacePendingTest() {
        val contractA = "contract A {" +
                "  uint[32] public somedata1;" +
                "  uint[32] public somedata2;" +
                "  function set1(uint idx, uint val){" +
                "    somedata1[idx] = val;" +
                "  }" +
                "  function set2(uint idx, uint val){" +
                "    somedata2[idx] = val;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val a = bc.submitNewContract(contractA) as StandaloneBlockchain.SolidityContractImpl
        bc.createBlock()

        var b: Block? = null
        var cnt = 1

        val function = a.contract.getByName("set")
        Thread(Runnable {
            var cnt = 1
            while (cnt++ > 0) {
                try {
                    bc.generatePendingTransactions()
                    //                    byte[] encode = function.encode(cnt % 32, cnt);
                    //                    Transaction callTx1 = bc.createTransaction(new ECKey(), 0, a.getAddress(), BigInteger.ZERO, encode);
                    //                    bc.getPendingState().addPendingTransaction(callTx1);
                    //                    Transaction callTx2 = bc.createTransaction(, 0, a.getAddress(), BigInteger.ZERO, encode);
                    //                    bc.getPendingState().addPendingTransaction(callTx);
                    Thread.sleep(10)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }).start()

        var b_1: Block? = null
        while (cnt++ > 0) {
            val s = System.nanoTime()

            a.callFunction("set1", cnt % 32, cnt)
            a.callFunction("set2", cnt % 32, cnt)
            bc.sendEther(ByteArray(32), BigInteger.ONE)
            a.callFunction("set1", (cnt + 1) % 32, cnt + 1)
            a.callFunction("set2", (cnt + 1) % 32, cnt + 1)
            bc.sendEther(ByteArray(32), BigInteger.ONE)

            val prev = b
            if (cnt % 5 == 0) {
                b = bc.createForkBlock(b_1)
            } else {
                b = bc.createBlock()
            }
            b_1 = prev

            if (cnt % 3 == 0) {
                bc.blockchain.flush()
            }
            val t = System.nanoTime() - s


            println("" + String.format(Locale.US, "%1$.2f", t / 1_000_000.0) + ", " + b!!.difficultyBI + ", " + b.shortDescr)
        }


        //        SolidityContract a = bc.submitNewContract(contractA);
        //        Block b1 = bc.createBlock();
        //        Block b2 = bc.createBlock();
        //        Block b3 = bc.createBlock();
        //        Block b4 = bc.createBlock();
        //        Block b5 = bc.createBlock();
        //        Block b6 = bc.createBlock();
        //        Block b2_ = bc.createForkBlock(b1);
        //        a.callFunction("a");
        //        Block b3_ = bc.createForkBlock(b2_);
        //        Object hash = a.callConstFunction(b3_, "blockHash")[0];
        //
        //        System.out.println(Hex.toHexString((byte[]) hash));
        //        System.out.println(Hex.toHexString(b2_.getHash()));

        // no StackOverflowException
    }

    @Test
    @Throws(Exception::class)
    fun suicideInFailedCall() {
        // check that if a contract is suicide in call which is failed (thus suicide is reverted)
        // the refund for this suicide is not added
        val contractA = "contract B {" +
                "  function f(){" +
                "    suicide(msg.sender);" +
                "  }" +
                "}" +
                "contract A {" +
                "  function f(){" +
                "    this.call(bytes4(sha3('bad()')));" +
                "  }" +
                "  function bad() {" +
                "    B b = new B();" +
                "    b.call(bytes4(sha3('f()')));" +
                "    throw;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain().withGasLimit(5000000)
        val a = bc.submitNewContract(contractA, "A")
        bc.createBlock()
        val refund = arrayOfNulls<BigInteger>(1)
        bc.addEthereumListener(object : EthereumListenerAdapter() {
            override fun onTransactionExecuted(summary: TransactionExecutionSummary) {
                refund[0] = summary.gasRefund
            }
        })
        a.callFunction("f")
        bc.createBlock()

        Assert.assertEquals(BigInteger.ZERO, refund[0])

        // no StackOverflowException
    }

    @Test
    @Throws(Exception::class)
    fun logInFailedCall() {
        // check that if a contract is suicide in call which is failed (thus suicide is reverted)
        // the refund for this suicide is not added
        val contractA = "contract A {" +
                "  function f(){" +
                "    this.call(bytes4(sha3('bad()')));" +
                "  }" +
                "  function bad() {" +
                "    log0(1234);" +
                "    throw;" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain().withGasLimit(5000000)
        val a = bc.submitNewContract(contractA, "A")
        bc.createBlock()
        val logs = ArrayList<LogInfo>()
        bc.addEthereumListener(object : EthereumListenerAdapter() {
            override fun onTransactionExecuted(summary: TransactionExecutionSummary) {
                logs.addAll(summary.logs)
            }
        })
        a.callFunction("f")
        bc.createBlock()

        Assert.assertEquals(0, logs.size.toLong())

        // no StackOverflowException
    }

    @Test
    @Throws(Exception::class)
    fun ecRecoverTest() {
        // checks that ecrecover precompile contract rejects v > 255
        val contractA = "contract A {" +
                "  function f (bytes32 hash, bytes32 v, bytes32 r, bytes32 s) returns (address) {" +
                "    assembly {" +
                "      mstore(0x100, hash)" +
                "      mstore(0x120, v)" +
                "      mstore(0x140, r)" +
                "      mstore(0x160, s)" +
                "      callcode(0x50000, 0x01, 0x0, 0x100, 0x80, 0x200, 0x220)" + // call ecrecover

                "      return(0x200, 0x20)" +
                "    }" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain().withGasLimit(5000000)
        val a = bc.submitNewContract(contractA, "A")
        bc.createBlock()

        val key = ECKey.fromPrivate(BigInteger.ONE)
        val hash = ByteArray(32)
        val signature = key.sign(hash)

        var ret = a.callConstFunction("f", hash,
                ByteUtil.merge(ByteArray(31), byteArrayOf(signature.v)),
                ByteUtil.bigIntegerToBytes(signature.r, 32),
                ByteUtil.bigIntegerToBytes(signature.s, 32))

        Assert.assertArrayEquals(key.address, ret[0] as ByteArray)

        ret = a.callConstFunction("f", hash,
                ByteUtil.merge(byteArrayOf(1), ByteArray(30), byteArrayOf(signature.v)),
                ByteUtil.bigIntegerToBytes(signature.r, 32),
                ByteUtil.bigIntegerToBytes(signature.s, 32))

        Assert.assertArrayEquals(ByteArray(20), ret[0] as ByteArray)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun functionTypeTest() {
        val contractA = "contract A {" +
                "  int public res;" +
                "  function calc(int b, function (int a) external returns (int) f) external returns (int) {" +
                "    return f(b);" +
                "  }" +
                "  function fInc(int a) external returns (int) { return a + 1;}" +
                "  function fDec(int a) external returns (int) { return a - 1;}" +
                "  function test() {" +
                "    res = this.calc(111, this.fInc);" +
                "  }" +
                "}"

        val bc = StandaloneBlockchain()
        val a = bc.submitNewContract(contractA)
        bc.createBlock()
        a.callFunction("test")
        bc.createBlock()
        Assert.assertEquals(a.callConstFunction("res")[0], BigInteger.valueOf(112))

        val r1 = a.callConstFunction("calc", 222, a.getFunction("fInc"))[0] as BigInteger
        Assert.assertEquals(223, r1.toInt().toLong())
        val r2 = a.callConstFunction("calc", 222, a.getFunction("fDec"))[0] as BigInteger
        Assert.assertEquals(221, r2.toInt().toLong())
    }

    companion object {

        @BeforeClass
        fun setup() {
            SystemProperties.getDefault()!!.blockchainConfig = StandaloneBlockchain.getEasyMiningConfig()
        }

        @AfterClass
        fun cleanup() {
            SystemProperties.resetToDefault()
        }

        fun createBlockchain(genesis: Genesis): BlockchainImpl {
            val blockStore = IndexedBlockStore()
            blockStore.init(HashMapDB<ByteArray>(), HashMapDB<ByteArray>())

            val repository = RepositoryRoot(NoDeleteSource(HashMapDB<ByteArray>()))

            val programInvokeFactory = ProgramInvokeFactoryImpl()
            val listener = EthereumListenerAdapter()

            val blockchain = BlockchainImpl(blockStore, repository)
                    .withParentBlockHeaderValidator(CommonConfig().parentHeaderValidator())
            blockchain.setParentHeaderValidator(DependentBlockHeaderRuleAdapter())
            blockchain.programInvokeFactory = programInvokeFactory

            blockchain.byTest = true

            val pendingState = PendingStateImpl(listener, blockchain)

            pendingState.setBlockchain(blockchain)
            blockchain.pendingState = pendingState

            val track = repository.startTracking()
            Genesis.populateRepository(track, genesis)

            track.commit()
            repository.commit()

            blockStore.saveBlock(genesis, genesis.cumulativeDifficulty, true)

            blockchain.bestBlock = genesis
            blockchain.totalDifficulty = genesis.cumulativeDifficulty

            return blockchain
        }
    }
}
