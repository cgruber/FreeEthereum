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

package org.ethereum.facade

import org.ethereum.core.Block
import org.ethereum.core.CallTransaction
import org.ethereum.core.Transaction
import org.ethereum.core.TransactionReceipt
import org.ethereum.crypto.ECKey
import org.ethereum.listener.EthereumListener
import org.ethereum.manager.AdminInfo
import org.ethereum.manager.BlockLoader
import org.ethereum.mine.BlockMiner
import org.ethereum.net.client.PeerClient
import org.ethereum.net.rlpx.Node
import org.ethereum.net.server.ChannelManager
import org.ethereum.net.shh.Whisper
import org.ethereum.vm.program.ProgramResult

import java.math.BigInteger
import java.net.InetAddress
import java.util.concurrent.Future

/**
 * @author Roman Mandeleil
 * *
 * @since 27.07.2014
 */
interface Ethereum {

    fun startPeerDiscovery()

    fun stopPeerDiscovery()

    fun connect(addr: InetAddress, port: Int, remoteId: String)

    fun connect(ip: String, port: Int, remoteId: String)

    fun connect(node: Node)

    val blockchain: Blockchain

    fun addListener(listener: EthereumListener)

    val defaultPeer: PeerClient

    val isConnected: Boolean

    fun close()

    /**
     * Gets the current sync state
     */
    val syncStatus: SyncStatus

    /**
     * Factory for general transaction


     * @param nonce - account nonce, based on number of transaction submited by
     * *                this account
     * *
     * @param gasPrice - gas price bid by miner , the user ask can be based on
     * *                   lastr submited block
     * *
     * @param gas - the quantity of gas requested for the transaction
     * *
     * @param receiveAddress - the target address of the transaction
     * *
     * @param value - the ether value of the transaction
     * *
     * @param data - can be init procedure for creational transaction,
     * *               also msg data for invoke transaction for only value
     * *               transactions this one is empty.
     * *
     * @return newly created transaction
     */
    fun createTransaction(nonce: BigInteger,
                          gasPrice: BigInteger,
                          gas: BigInteger,
                          receiveAddress: ByteArray,
                          value: BigInteger, data: ByteArray): Transaction


    /**
     * @param transaction submit transaction to the net, return option to wait for net
     * *                    return this transaction as approved
     */
    fun submitTransaction(transaction: Transaction): Future<Transaction>


    /**
     * Executes the transaction based on the specified block but doesn't change the blockchain state
     * and doesn't send the transaction to the network
     * @param tx     The transaction to execute. No need to sign the transaction and specify the correct nonce
     * *
     * @param block  Transaction is executed the same way as if it was executed after all transactions existing
     * *               in that block. I.e. the root state is the same as this block's root state and this block
     * *               is assumed to be the current block
     * *
     * @return       receipt of the executed transaction
     */
    fun callConstant(tx: Transaction, block: Block): TransactionReceipt

    /**
     * Call a contract function locally without sending transaction to the network
     * and without changing contract storage.
     * @param receiveAddress hex encoded contract address
     * *
     * @param function  contract function
     * *
     * @param funcArgs  function arguments
     * *
     * @return function result. The return value can be fetched via [ProgramResult.getHReturn]
     * * and decoded with [org.ethereum.core.CallTransaction.Function.decodeResult].
     */
    fun callConstantFunction(receiveAddress: String, function: CallTransaction.Function,
                             vararg funcArgs: Any): ProgramResult


    /**
     * Call a contract function locally without sending transaction to the network
     * and without changing contract storage.
     * @param receiveAddress hex encoded contract address
     * *
     * @param senderPrivateKey  Normally the constant call doesn't require a sender though
     * *                          in some cases it may affect the result (e.g. if function refers to msg.sender)
     * *
     * @param function  contract function
     * *
     * @param funcArgs  function arguments
     * *
     * @return function result. The return value can be fetched via [ProgramResult.getHReturn]
     * * and decoded with [org.ethereum.core.CallTransaction.Function.decodeResult].
     */
    fun callConstantFunction(receiveAddress: String, senderPrivateKey: ECKey,
                             function: CallTransaction.Function, vararg funcArgs: Any): ProgramResult

    /**
     * Returns the Repository instance which always refers to the latest (best block) state
     * It is always better using [.getLastRepositorySnapshot] to work on immutable
     * state as this instance can change its state between calls (when a new block is imported)

     * @return - repository for all state data.
     */
    val repository: Repository

    /**
     * Returns the latest (best block) Repository snapshot
     */
    val lastRepositorySnapshot: Repository

    /**
     * @return - pending state repository
     */
    val pendingState: Repository

    //  2.   // is blockchain still loading - if buffer is not empty

    fun getSnapshotTo(root: ByteArray): Repository

    val adminInfo: AdminInfo

    val channelManager: ChannelManager

    /**
     * @return - currently pending transactions received from the net
     */
    val wireTransactions: List<Transaction>

    /**
     * @return - currently pending transactions sent to the net
     */
    val pendingStateTransactions: List<Transaction>

    val blockLoader: BlockLoader

    /**
     * @return Whisper implementation if the protocol is available
     */
    val whisper: Whisper

    /**
     * Gets the Miner component
     */
    val blockMiner: BlockMiner

    /**
     * Initiates blockchain syncing process
     */
    fun initSyncing()

    /**
     * Calculates a 'reasonable' Gas price based on statistics of the latest transaction's Gas prices
     * Normally the price returned should be sufficient to execute a transaction since ~25% of the latest
     * transactions were executed at this or lower price.
     * If the transaction is wanted to be executed promptly with higher chances the returned price might
     * be increased at some ratio (e.g. * 1.2)
     */
    val gasPrice: Long

    /**
     * Chain id for next block.
     * Introduced in EIP-155
     * @return chain id or null
     */
    val chainIdForNextBlock: Int?

    fun exitOn(number: Long)
}
