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

import org.ethereum.config.SystemProperties
import org.ethereum.crypto.ECKey
import org.ethereum.crypto.HashUtil
import org.ethereum.datasource.CountingBytesSource
import org.ethereum.datasource.JournalSource
import org.ethereum.datasource.Source
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.trie.SecureTrie
import org.ethereum.trie.TrieImpl
import org.ethereum.util.ByteUtil.intToBytes
import org.ethereum.util.FastByteComparisons
import org.ethereum.util.blockchain.EtherUtil.Unit.ETHER
import org.ethereum.util.blockchain.EtherUtil.convert
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

class PruneTest {

    @Test
    @Throws(Exception::class)
    fun testJournal1() {
        val db = HashMapDB<ByteArray>()
        val countDB = CountingBytesSource(db)
        val journalDB = JournalSource(countDB)

        put(journalDB, "11")
        put(journalDB, "22")
        put(journalDB, "33")
        journalDB.commitUpdates(intToBytes(1))
        checkKeys(db.storage, "11", "22", "33")

        put(journalDB, "22")
        delete(journalDB, "33")
        put(journalDB, "44")
        journalDB.commitUpdates(intToBytes(2))
        checkKeys(db.storage, "11", "22", "33", "44")

        journalDB.persistUpdate(intToBytes(1))
        checkKeys(db.storage, "11", "22", "33", "44")

        journalDB.revertUpdate(intToBytes(2))
        checkKeys(db.storage, "11", "22", "33")

        put(journalDB, "22")
        delete(journalDB, "33")
        put(journalDB, "44")
        journalDB.commitUpdates(intToBytes(3))
        checkKeys(db.storage, "11", "22", "33", "44")

        delete(journalDB, "22")
        put(journalDB, "33")
        delete(journalDB, "44")
        journalDB.commitUpdates(intToBytes(4))
        checkKeys(db.storage, "11", "22", "33", "44")

        journalDB.persistUpdate(intToBytes(3))
        checkKeys(db.storage, "11", "22", "33", "44")

        journalDB.persistUpdate(intToBytes(4))
        checkKeys(db.storage, "11", "22", "33")

        delete(journalDB, "22")
        journalDB.commitUpdates(intToBytes(5))
        checkKeys(db.storage, "11", "22", "33")

        journalDB.persistUpdate(intToBytes(5))
        checkKeys(db.storage, "11", "33")

    }

    @Test
    @Throws(Exception::class)
    fun simpleTest() {
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount,
                "mine.startNonce", "0")

        val bc = StandaloneBlockchain()

        val alice = ECKey.fromPrivate(BigInteger.ZERO)
        val bob = ECKey.fromPrivate(BigInteger.ONE)

        //        System.out.println("Gen root: " + Hex.toHexString(bc.getBlockchain().getBestBlock().getStateRoot()));
        bc.createBlock()
        val b0 = bc.blockchain.bestBlock
        bc.sendEther(alice.address, convert(3, ETHER))
        val b1_1 = bc.createBlock()

        bc.sendEther(alice.address, convert(3, ETHER))
        val b1_2 = bc.createForkBlock(b0)

        bc.sendEther(alice.address, convert(3, ETHER))
        val b1_3 = bc.createForkBlock(b0)

        bc.sendEther(alice.address, convert(3, ETHER))
        val b1_4 = bc.createForkBlock(b0)

        bc.sendEther(bob.address, convert(5, ETHER))
        bc.createBlock()

        bc.sendEther(alice.address, convert(3, ETHER))
        bc.createForkBlock(b1_2)

        for (i in 0..8) {
            bc.sendEther(alice.address, convert(3, ETHER))
            bc.sendEther(bob.address, convert(5, ETHER))
            bc.createBlock()
        }

        val roots = arrayOfNulls<ByteArray>(pruneCount + 1)
        for (i in 0..pruneCount + 1 - 1) {
            val bNum = bc.blockchain.bestBlock.number - i
            val b = bc.blockchain.getBlockByNumber(bNum)
            roots[i] = b.stateRoot
        }

        checkPruning(bc.stateDS, bc.pruningStateDS, *roots)

        val bestBlockNum = bc.blockchain.bestBlock.number

        Assert.assertEquals(convert(30, ETHER), bc.blockchain.repository.getBalance(alice.address))
        Assert.assertEquals(convert(50, ETHER), bc.blockchain.repository.getBalance(bob.address))

        run {
            val b1 = bc.blockchain.getBlockByNumber(bestBlockNum - 1)
            val r1 = bc.blockchain.repository.getSnapshotTo(b1.stateRoot)
            Assert.assertEquals(convert((3 * 9).toLong(), ETHER), r1.getBalance(alice.address))
            Assert.assertEquals(convert((5 * 9).toLong(), ETHER), r1.getBalance(bob.address))
        }

        run {
            val b1 = bc.blockchain.getBlockByNumber(bestBlockNum - 2)
            val r1 = bc.blockchain.repository.getSnapshotTo(b1.stateRoot)
            Assert.assertEquals(convert((3 * 8).toLong(), ETHER), r1.getBalance(alice.address))
            Assert.assertEquals(convert((5 * 8).toLong(), ETHER), r1.getBalance(bob.address))
        }

        run {
            val b1 = bc.blockchain.getBlockByNumber(bestBlockNum - 3)
            val r1 = bc.blockchain.repository.getSnapshotTo(b1.stateRoot)
            Assert.assertEquals(convert((3 * 7).toLong(), ETHER), r1.getBalance(alice.address))
            Assert.assertEquals(convert((5 * 7).toLong(), ETHER), r1.getBalance(bob.address))
        }

        run {
            // this state should be pruned already
            val b1 = bc.blockchain.getBlockByNumber(bestBlockNum - 4)
            val r1 = bc.blockchain.repository.getSnapshotTo(b1.stateRoot)
            Assert.assertEquals(BigInteger.ZERO, r1.getBalance(alice.address))
            Assert.assertEquals(BigInteger.ZERO, r1.getBalance(bob.address))
        }
    }

    @Test
    @Throws(Exception::class)
    fun contractTest() {
        // checks that pruning doesn't delete the nodes which were 're-added' later
        // e.g. when a contract variable assigned value V1 the trie acquires node with key K1
        // then if the variable reassigned value V2 the trie acquires new node with key K2
        // and the node K1 is not needed anymore and added to the prune list
        // we should avoid situations when the value V1 is back, the node K1 is also back to the trie
        // but erroneously deleted later as was in the prune list
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount)

        val bc = StandaloneBlockchain()

        val contr = bc.submitNewContract(
                "contract Simple {" +
                        "  uint public n;" +
                        "  function set(uint _n) { n = _n; } " +
                        "}")
        bc.createBlock()

        // add/remove/add in the same block
        contr.callFunction("set", 0xaaaaaaaaaaaaL)
        contr.callFunction("set", 0xbbbbbbbbbbbbL)
        contr.callFunction("set", 0xaaaaaaaaaaaaL)
        bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr.callConstFunction("n")[0])
        // force prune
        bc.createBlock()
        bc.createBlock()
        bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr.callConstFunction("n")[0])

        for (i in 1..3) {
            for (j in 0..3) {

                contr.callFunction("set", 0xbbbbbbbbbbbbL)
                for (k in 0..j - 1) {
                    bc.createBlock()
                }
                if (j > 0)
                    Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr.callConstFunction("n")[0])

                contr.callFunction("set", 0xaaaaaaaaaaaaL)

                for (k in 0..i - 1) {
                    bc.createBlock()
                }

                Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr.callConstFunction("n")[0])

            }
        }

        val roots = arrayOfNulls<ByteArray>(pruneCount + 1)
        for (i in 0..pruneCount + 1 - 1) {
            val bNum = bc.blockchain.bestBlock.number - i
            val b = bc.blockchain.getBlockByNumber(bNum)
            roots[i] = b.stateRoot
        }

        checkPruning(bc.stateDS, bc.pruningStateDS, *roots)
    }

    @Test
    @Throws(Exception::class)
    fun twoContractsTest() {
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount)

        val src = "contract Simple {" +
                "  uint public n;" +
                "  function set(uint _n) { n = _n; } " +
                "  function inc() { n++; } " +
                "}"

        val bc = StandaloneBlockchain()

        val b0 = bc.blockchain.bestBlock

        val contr1 = bc.submitNewContract(src)
        val contr2 = bc.submitNewContract(src)
        val b1 = bc.createBlock()
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b1.stateRoot, b0.stateRoot)

        // add/remove/add in the same block
        contr1.callFunction("set", 0xaaaaaaaaaaaaL)
        contr2.callFunction("set", 0xaaaaaaaaaaaaL)
        val b2 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b2.stateRoot, b1.stateRoot, b0.stateRoot)

        contr2.callFunction("set", 0xbbbbbbbbbbbbL)
        val b3 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b3.stateRoot, b2.stateRoot, b1.stateRoot, b0.stateRoot)

        // force prune
        val b4 = bc.createBlock()
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b4.stateRoot, b3.stateRoot, b2.stateRoot, b1.stateRoot)
        val b5 = bc.createBlock()
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b5.stateRoot, b4.stateRoot, b3.stateRoot, b2.stateRoot)
        val b6 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b6.stateRoot, b5.stateRoot, b4.stateRoot, b3.stateRoot)

        contr1.callFunction("set", 0xaaaaaaaaaaaaL)
        contr2.callFunction("set", 0xaaaaaaaaaaaaL)
        val b7 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b7.stateRoot, b6.stateRoot, b5.stateRoot, b4.stateRoot)

        contr1.callFunction("set", 0xbbbbbbbbbbbbL)
        val b8 = bc.createBlock()
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b8.stateRoot, b7.stateRoot, b6.stateRoot, b5.stateRoot)

        contr2.callFunction("set", 0xbbbbbbbbbbbbL)
        val b8_ = bc.createForkBlock(b7)
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b8.stateRoot, b8_.stateRoot, b7.stateRoot, b6.stateRoot, b5.stateRoot)
        val b9_ = bc.createForkBlock(b8_)
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b9_.stateRoot, b8.stateRoot, b8_.stateRoot, b7.stateRoot, b6.stateRoot)

        val b9 = bc.createForkBlock(b8)
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b9.stateRoot, b9_.stateRoot, b8.stateRoot, b8_.stateRoot, b7.stateRoot, b6.stateRoot)
        val b10 = bc.createForkBlock(b9)
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr2.callConstFunction("n")[0])
        checkPruning(bc.stateDS, bc.pruningStateDS,
                b10.stateRoot, b9.stateRoot, b9_.stateRoot, b8.stateRoot, b8_.stateRoot, b7.stateRoot)


        val b11 = bc.createForkBlock(b10)
        Assert.assertEquals(BigInteger.valueOf(0xbbbbbbbbbbbbL), contr1.callConstFunction("n")[0])
        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr2.callConstFunction("n")[0])

        checkPruning(bc.stateDS, bc.pruningStateDS,
                b11.stateRoot, b10.stateRoot, b9.stateRoot, /*b9_.getStateRoot(),*/ b8.stateRoot)
    }

    @Test
    @Throws(Exception::class)
    fun branchTest() {
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount)

        val bc = StandaloneBlockchain()

        val contr = bc.submitNewContract(
                "contract Simple {" +
                        "  uint public n;" +
                        "  function set(uint _n) { n = _n; } " +
                        "}")
        val b1 = bc.createBlock()
        contr.callFunction("set", 0xaaaaaaaaaaaaL)
        val b2 = bc.createBlock()
        contr.callFunction("set", 0xbbbbbbbbbbbbL)
        val b2_ = bc.createForkBlock(b1)
        bc.createForkBlock(b2)
        bc.createBlock()
        bc.createBlock()
        bc.createBlock()

        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr.callConstFunction("n")[0])
    }

    @Test
    @Throws(Exception::class)
    fun storagePruneTest() {
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "details.inmemory.storage.limit", "200",
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount)

        val bc = StandaloneBlockchain()
        val blockchain = bc.blockchain
        //        RepositoryImpl repository = (RepositoryImpl) blockchain.getRepository();
        //        HashMapDB storageDS = new HashMapDB();
        //        repository.getDetailsDataStore().setStorageDS(storageDS);

        val contr = bc.submitNewContract(
                "contract Simple {" +
                        "  uint public n;" +
                        "  mapping(uint => uint) largeMap;" +
                        "  function set(uint _n) { n = _n; } " +
                        "  function put(uint k, uint v) { largeMap[k] = v; }" +
                        "}")
        val b1 = bc.createBlock()

        val entriesForExtStorage = 100

        for (i in 0..entriesForExtStorage - 1) {
            contr.callFunction("put", i, i)
            if (i % 100 == 0) bc.createBlock()
        }
        bc.createBlock()
        blockchain.flush()
        contr.callFunction("put", 1000000, 1)
        bc.createBlock()
        blockchain.flush()

        for (i in 0..99) {
            contr.callFunction("set", i)
            bc.createBlock()
            blockchain.flush()
            println(bc.stateDS.storage.size.toString() + ", " + bc.stateDS.storage.size)
        }

        println("Done")
    }


    @Ignore
    @Test
    @Throws(Exception::class)
    fun rewriteSameTrieNode() {
        val pruneCount = 3
        SystemProperties.getDefault()!!.overrideParams(
                "database.prune.enabled", "true",
                "database.prune.maxDepth", "" + pruneCount)

        val bc = StandaloneBlockchain()
        val receiver = Hex.decode("0000000000000000000000000000000000000000")
        bc.sendEther(receiver, BigInteger.valueOf(0x77777777))
        bc.createBlock()

        for (i in 0..99) {
            bc.sendEther(ECKey().address, BigInteger.valueOf(i.toLong()))
        }

        val contr = bc.submitNewContract(
                "contract Stupid {" +
                        "  function wrongAddress() { " +
                        "    address addr = 0x0000000000000000000000000000000000000000; " +
                        "    addr.call();" +
                        "  } " +
                        "}")
        val b1 = bc.createBlock()
        contr.callFunction("wrongAddress")
        val b2 = bc.createBlock()
        contr.callFunction("wrongAddress")
        val b3 = bc.createBlock()

        Assert.assertEquals(BigInteger.valueOf(0xaaaaaaaaaaaaL), contr.callConstFunction("n")[0])
    }

    private fun checkPruning(stateDS: HashMapDB<ByteArray>, stateJournalDS: Source<ByteArray, ByteArray>, vararg roots: ByteArray?) {
        println("Pruned storage size: " + stateDS.storage.size)

        val allRefs = HashSet<ByteArrayWrapper>()
        for (root in roots) {

            val bRefs = getReferencedTrieNodes(stateJournalDS, true, root)
            println("#" + Hex.toHexString(root).substring(0, 8) + " refs: ")
            for (bRef in bRefs) {
                println("    " + bRef.toString().substring(0, 8))
            }
            allRefs.addAll(bRefs)
        }

        println("Trie nodes closure size: " + allRefs.size)
        if (allRefs.size != stateDS.storage.size) {
            stateDS.storage.keys
                    .filterNot { allRefs.contains(ByteArrayWrapper(it)) }
                    .forEach { println("Extra node: " + Hex.toHexString(it)) }
            //            Assert.assertEquals(allRefs.size(), stateDS.getStorage().size());
        }

        for (key in stateDS.storage.keys) {
            //            Assert.assertTrue(allRefs.contains(new ByteArrayWrapper(key)));
        }
    }

    private fun getReferencedTrieNodes(stateDS: Source<ByteArray, ByteArray>, includeAccounts: Boolean,
                                       vararg roots: ByteArray?): Set<ByteArrayWrapper> {
        val ret = HashSet<ByteArrayWrapper>()
        roots
                .map { SecureTrie(stateDS, it) }
                .forEach {
                    it.scanTree(object : TrieImpl.ScanAction {
                        override fun doOnNode(hash: ByteArray, node: TrieImpl.Node) {
                            ret.add(ByteArrayWrapper(hash))
                        }

                        override fun doOnValue(nodeHash: ByteArray, node: TrieImpl.Node, key: ByteArray, value: ByteArray) {
                            if (includeAccounts) {
                                val accountState = AccountState(value)
                                if (!FastByteComparisons.equal(accountState.codeHash, HashUtil.EMPTY_DATA_HASH)) {
                                    ret.add(ByteArrayWrapper(accountState.codeHash))
                                }
                                if (!FastByteComparisons.equal(accountState.stateRoot, HashUtil.EMPTY_TRIE_HASH)) {
                                    ret.addAll(getReferencedTrieNodes(stateDS, false, accountState.stateRoot))
                                }
                            }
                        }
                    })
                }
        return ret
    }

    private fun dumpState(stateDS: Source<ByteArray, ByteArray>, includeAccounts: Boolean,
                          root: ByteArray): String {
        val ret = StringBuilder()
        val trie = SecureTrie(stateDS, root)
        trie.scanTree(object : TrieImpl.ScanAction {
            override fun doOnNode(hash: ByteArray, node: TrieImpl.Node) {}

            override fun doOnValue(nodeHash: ByteArray, node: TrieImpl.Node, key: ByteArray, value: ByteArray) {
                if (includeAccounts) {
                    val accountState = AccountState(value)
                    ret.append(Hex.toHexString(nodeHash)).append(": Account: ").append(Hex.toHexString(key)).append(", Nonce: ").append(accountState.nonce).append(", Balance: ").append(accountState.balance).append("\n")
                    if (!FastByteComparisons.equal(accountState.codeHash, HashUtil.EMPTY_DATA_HASH)) {
                        ret.append("    CodeHash: ").append(Hex.toHexString(accountState.codeHash)).append("\n")
                    }
                    if (!FastByteComparisons.equal(accountState.stateRoot, HashUtil.EMPTY_TRIE_HASH)) {
                        ret.append(dumpState(stateDS, false, accountState.stateRoot))
                    }
                } else {
                    ret.append("    ").append(Hex.toHexString(nodeHash)).append(": ").append(Hex.toHexString(key)).append(" = ").append(Hex.toHexString(value)).append("\n")
                }
            }
        })
        return ret.toString()
    }

    companion object {

        private val stateDS: HashMapDB<ByteArray>? = null

        @AfterClass
        fun cleanup() {
            SystemProperties.resetToDefault()
        }

        private fun put(db: Source<ByteArray, ByteArray>, key: String) {
            db.put(Hex.decode(key), Hex.decode(key))
        }

        private fun delete(db: Source<ByteArray, ByteArray>, key: String) {
            db.delete(Hex.decode(key))
        }

        private fun checkKeys(map: Map<ByteArray, ByteArray>, vararg keys: String) {
            Assert.assertEquals(keys.size.toLong(), map.size.toLong())
            for (key in keys) {
                assertTrue(map.containsKey(Hex.decode(key)))
            }
        }

        internal fun getCount(hash: String): String {
            val bytes = stateDS!!.get(Hex.decode(hash))
            return if (bytes == null) "0" else "" + bytes[3]
        }
    }
}
