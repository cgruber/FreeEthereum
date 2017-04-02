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

package org.ethereum.config

import org.apache.commons.lang3.tuple.Pair
import org.ethereum.core.Block
import org.ethereum.core.BlockHeader
import org.ethereum.core.Repository
import org.ethereum.core.Transaction
import org.ethereum.db.BlockStore
import org.ethereum.mine.MinerIfc
import org.ethereum.validator.BlockHeaderValidator
import org.ethereum.vm.DataWord
import org.ethereum.vm.GasCost
import org.ethereum.vm.OpCode
import org.ethereum.vm.program.Program
import java.math.BigInteger

/**
 * Describes constants and algorithms used for a specific blockchain at specific stage

 * Created by Anton Nashatyrev on 25.02.2016.
 */
interface BlockchainConfig {

    /**
     * Get blockchain constants
     */
    val constants: Constants

    /**
     * Returns the mining algorithm
     */
    fun getMineAlgorithm(config: SystemProperties): MinerIfc

    /**
     * Calculates the difficulty for the block depending on the parent
     */
    fun calcDifficulty(curBlock: BlockHeader, parent: BlockHeader): BigInteger

    /**
     * Calculates transaction gas fee
     */
    fun getTransactionCost(tx: Transaction): Long

    /**
     * Validates Tx signature (introduced in Homestead)
     */
    fun acceptTransactionSignature(tx: Transaction): Boolean

    /**
     * Validates transaction by the changes made by it in the repository
     * @param blockStore
     * *
     * @param curBlock The block being imported
     * *
     * @param repositoryTrack The repository track changed by transaction
     * *
     * @return null if all is fine or String validation error
     */
    fun validateTransactionChanges(blockStore: BlockStore, curBlock: Block, tx: Transaction,
                                   repositoryTrack: Repository): String


    /**
     * Prior to block processing performs some repository manipulations according
     * to HardFork rules.
     * This method is normally executes the logic on a specific hardfork block only
     * for other blocks it just does nothing
     */
    fun hardForkTransfers(block: Block, repo: Repository)

    /**
     * DAO hard fork marker
     */
    fun getExtraData(minerExtraData: ByteArray, blockNumber: Long): ByteArray

    /**
     * Fork related validators. Ensure that connected peer operates on the same fork with us
     * For example: DAO config will have validator that checks presence of extra data in specific block
     */
    fun headerValidators(): List<Pair<Long, BlockHeaderValidator>>

    /**
     * EVM operations costs
     */
    val gasCost: GasCost

    /**
     * Calculates available gas to be passed for callee
     * Since EIP150
     * @param op  Opcode
     * *
     * @param requestedGas amount of gas requested by the program
     * *
     * @param availableGas available gas
     * *
     * @throws Program.OutOfGasException If passed args doesn't conform to limitations
     */
    @Throws(Program.OutOfGasException::class)
    fun getCallGas(op: OpCode, requestedGas: DataWord, availableGas: DataWord): DataWord

    /**
     * Calculates available gas to be passed for contract constructor
     * Since EIP150
     */
    fun getCreateGas(availableGas: DataWord): DataWord

    /**
     * EIP161: https://github.com/ethereum/EIPs/issues/161
     */
    fun eip161(): Boolean

    /**
     * EIP155: https://github.com/ethereum/EIPs/issues/155
     */
    val chainId: Int?
}
