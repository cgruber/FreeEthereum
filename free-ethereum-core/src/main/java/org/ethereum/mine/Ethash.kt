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

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.commons.lang3.tuple.Pair
import org.ethereum.config.SystemProperties
import org.ethereum.core.Block
import org.ethereum.core.BlockHeader
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.mine.MinerIfc.MiningResult
import org.ethereum.util.ByteUtil
import org.ethereum.util.ByteUtil.longToBytes
import org.ethereum.util.FastByteComparisons
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * More high level validator/miner class which keeps a cache for the last requested block epoch

 * Created by Anton Nashatyrev on 04.12.2015.
 */
class Ethash(private val config: SystemProperties, private val blockNumber: Long) {
    private val ethashAlgo = EthashAlgo(ethashParams)
    private var cacheLight: IntArray? = null
    private var fullData: IntArray? = null
    private var startNonce: Long = -1

    init {
        if (config.config.hasPath("mine.startNonce")) {
            startNonce = config.config.getLong("mine.startNonce")
        }
    }

    @Synchronized private fun getCacheLight(): IntArray {
        if (cacheLight == null) {
            val file = File(config.ethashDir(), "mine-dag-light.dat")
            if (fileCacheEnabled && file.canRead()) {
                try {
                    ObjectInputStream(FileInputStream(file)).use { ois ->
                        logger.info("Loading light dataset from " + file.absolutePath)
                        val bNum = ois.readLong()
                        if (bNum == blockNumber) {
                            cacheLight = ois.readObject() as IntArray
                            logger.info("Dataset loaded.")
                        } else {
                            logger.info("Dataset block number miss: $bNum != $blockNumber")
                        }
                    }
                } catch (e: IOException) {
                    throw RuntimeException(e)
                } catch (e: ClassNotFoundException) {
                    throw RuntimeException(e)
                }

            }

            if (cacheLight == null) {
                logger.info("Calculating light dataset...")
                cacheLight = ethashAlgo.makeCache(ethashAlgo.params.getCacheSize(blockNumber),
                        ethashAlgo.getSeedHash(blockNumber))
                logger.info("Light dataset calculated.")

                if (fileCacheEnabled) {
                    file.parentFile.mkdirs()
                    try {
                        ObjectOutputStream(FileOutputStream(file)).use { oos ->
                            logger.info("Writing light dataset to " + file.absolutePath)
                            oos.writeLong(blockNumber)
                            oos.writeObject(cacheLight)
                        }
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }

                }
            }
        }
        return cacheLight!!
    }

    val fullDataset: IntArray
        @Synchronized get() {
            if (fullData == null) {
                val file = File(config.ethashDir(), "mine-dag.dat")
                if (fileCacheEnabled && file.canRead()) {
                    try {
                        ObjectInputStream(FileInputStream(file)).use { ois ->
                            logger.info("Loading dataset from " + file.absolutePath)
                            val bNum = ois.readLong()
                            if (bNum == blockNumber) {
                                fullData = ois.readObject() as IntArray
                                logger.info("Dataset loaded.")
                            } else {
                                logger.info("Dataset block number miss: $bNum != $blockNumber")
                            }
                        }
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    } catch (e: ClassNotFoundException) {
                        throw RuntimeException(e)
                    }

                }

                if (fullData == null) {

                    logger.info("Calculating full dataset...")
                    fullData = ethashAlgo.calcDataset(fullSize, getCacheLight())
                    logger.info("Full dataset calculated.")

                    if (fileCacheEnabled) {
                        file.parentFile.mkdirs()
                        try {
                            ObjectOutputStream(FileOutputStream(file)).use { oos ->
                                logger.info("Writing dataset to " + file.absolutePath)
                                oos.writeLong(blockNumber)
                                oos.writeObject(fullData)
                            }
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }

                    }
                }
            }
            return fullData!!
        }

    private val fullSize: Long
        get() = ethashAlgo.params.getFullSize(blockNumber)

    /**
     * See [EthashAlgo.hashimotoLight]
     */
    private fun hashimotoLight(header: BlockHeader, nonce: Long): Pair<ByteArray, ByteArray> {
        return hashimotoLight(header, longToBytes(nonce))
    }

    private fun hashimotoLight(header: BlockHeader, nonce: ByteArray): Pair<ByteArray, ByteArray> {
        return ethashAlgo.hashimotoLight(fullSize, getCacheLight(),
                sha3(header.encodedWithoutNonce), nonce)
    }

    /**
     * See [EthashAlgo.hashimotoFull]
     */
    fun hashimotoFull(header: BlockHeader, nonce: Long): Pair<ByteArray, ByteArray> {
        return ethashAlgo.hashimotoFull(fullSize, fullDataset, sha3(header.encodedWithoutNonce),
                longToBytes(nonce))
    }

    /**
     * Mines the nonce for the specified Block with difficulty BlockHeader.getDifficulty()
     * When mined the Block 'nonce' and 'mixHash' fields are updated
     * Uses the full dataset i.e. it faster but takes > 1Gb of memory and may
     * take up to 10 mins for starting up (depending on whether the dataset was cached)

     * @param block The block to mine. The difficulty is taken from the block header
     * *               This block is updated when mined
     * *
     * @param nThreads CPU threads to mine on
     * *
     * @return the task which may be cancelled. On success returns nonce
     */
    @JvmOverloads fun mine(block: Block, nThreads: Int = 1): ListenableFuture<MiningResult> {
        return MineTask(block, nThreads, object : Callable<MiningResult> {
            internal val taskStartNonce = AtomicLong(if (startNonce >= 0) startNonce else Random().nextLong())
            @Throws(Exception::class)
            override fun call(): MiningResult {
                val threadStartNonce = taskStartNonce.getAndAdd(0x100000000L)
                val nonce = ethashAlgo.mine(fullSize, fullDataset,
                        sha3(block.header.encodedWithoutNonce),
                        ByteUtil.byteArrayToLong(block.header.difficulty), threadStartNonce)
                val pair = hashimotoLight(block.header, nonce)
                return MiningResult(nonce, pair.left, block)
            }
        }).submit()
    }

    /**
     * Mines the nonce for the specified Block with difficulty BlockHeader.getDifficulty()
     * When mined the Block 'nonce' and 'mixHash' fields are updated
     * Uses the light cache i.e. it slower but takes only ~16Mb of memory and takes less
     * time to start up

     * @param block The block to mine. The difficulty is taken from the block header
     * *               This block is updated when mined
     * *
     * @param nThreads CPU threads to mine on
     * *
     * @return the task which may be cancelled. On success returns nonce
     */
    @JvmOverloads fun mineLight(block: Block, nThreads: Int = 1): ListenableFuture<MiningResult> {
        return MineTask(block, nThreads, object : Callable<MiningResult> {
            internal val taskStartNonce = AtomicLong(if (startNonce >= 0) startNonce else Random().nextLong())
            @Throws(Exception::class)
            override fun call(): MiningResult {
                val threadStartNonce = taskStartNonce.getAndAdd(0x100000000L)
                val nonce = ethashAlgo.mineLight(fullSize, getCacheLight(),
                        sha3(block.header.encodedWithoutNonce),
                        ByteUtil.byteArrayToLong(block.header.difficulty), threadStartNonce)
                val pair = hashimotoLight(block.header, nonce)
                return MiningResult(nonce, pair.left, block)
            }
        }).submit()
    }

    /**
     * Validates the BlockHeader against its getDifficulty() and getNonce()
     */
    fun validate(header: BlockHeader): Boolean {
        val boundary = header.powBoundary
        val hash = hashimotoLight(header, header.nonce).right

        return FastByteComparisons.compareTo(hash, 0, 32, boundary, 0, 32) < 0
    }

    internal inner class MineTask(val block: Block, val nThreads: Int, val miner: Callable<MiningResult>) : AnyFuture<MiningResult>() {

        fun submit(): MineTask {
            for (i in 0..nThreads - 1) {
                val f = executor.submit(miner)
                add(f)
            }
            return this
        }

        override fun postProcess(result: MiningResult) {
            val pair = hashimotoLight(block.header, result.nonce)
            block.nonce = longToBytes(result.nonce)
            block.mixHash = pair.left
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("mine")
        private val ethashParams = EthashParams()
        //    private static ExecutorService executor = Executors.newSingleThreadExecutor();
        private val executor = MoreExecutors.listeningDecorator(
                ThreadPoolExecutor(8, 8, 0L, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(),
                        ThreadFactoryBuilder().setNameFormat("ethash-pool-%d").build()))
        var fileCacheEnabled = true
        private var cachedInstance: Ethash? = null
        private var cachedBlockEpoch: Long = 0

        /**
         * Returns instance for the specified block number either from cache or calculates a new one
         */
        fun getForBlock(config: SystemProperties, blockNumber: Long): Ethash {
            val epoch = blockNumber / ethashParams.epocH_LENGTH
            if (cachedInstance == null || epoch != cachedBlockEpoch) {
                cachedInstance = Ethash(config, epoch * ethashParams.epocH_LENGTH)
                cachedBlockEpoch = epoch
            }
            return cachedInstance!!
        }
    }
}
