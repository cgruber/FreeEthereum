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

package org.ethereum.jsontestsuite.suite.runners

import org.ethereum.core.*
import org.ethereum.db.BlockStoreDummy
import org.ethereum.jsontestsuite.suite.Env
import org.ethereum.jsontestsuite.suite.StateTestCase
import org.ethereum.jsontestsuite.suite.TestProgramInvokeFactory
import org.ethereum.jsontestsuite.suite.builder.*
import org.ethereum.jsontestsuite.suite.validators.LogsValidator
import org.ethereum.jsontestsuite.suite.validators.OutputValidator
import org.ethereum.jsontestsuite.suite.validators.RepositoryValidator
import org.ethereum.vm.program.ProgramResult
import org.ethereum.vm.program.invoke.ProgramInvokeFactory
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.util.*

open class StateTestRunner(private val stateTestCase: StateTestCase) {
    protected var repository: Repository? = null
    protected var invokeFactory: ProgramInvokeFactory? = null
    protected var block: Block? = null
    private var transaction: Transaction? = null
    private var blockchain: BlockchainImpl? = null
    private var env: Env? = null

    protected open fun executeTransaction(tx: Transaction): ProgramResult {
        val track = repository?.startTracking()

        val executor = TransactionExecutor(transaction, env!!.currentCoinbase, track, BlockStoreDummy(),
                invokeFactory, blockchain!!.bestBlock)

        try {
            executor.init()
            executor.execute()
            executor.go()
            executor.finalization()
        } catch (soe: StackOverflowError) {
            logger.error(" !!! StackOverflowError: update your java run command with -Xss2M !!!")
            System.exit(-1)
        }

        track?.commit()
        return executor.result
    }

    fun runImpl(): List<String> {

        logger.info("")
        repository = RepositoryBuilder.build(stateTestCase.pre)
        logger.info("loaded repository")

        transaction = TransactionBuilder.build(stateTestCase.transaction)
        logger.info("transaction: {}", transaction!!.toString())

        blockchain = BlockchainImpl()
        blockchain!!.repository = repository

        env = EnvBuilder.build(stateTestCase.env)
        invokeFactory = TestProgramInvokeFactory(env)

        block = BlockBuilder.build(env)

        blockchain!!.bestBlock = block
        blockchain!!.programInvokeFactory = invokeFactory

        val programResult = executeTransaction(transaction!!)

        repository?.commit()

        val origLogs = programResult.logInfoList
        val postLogs = LogBuilder.build(stateTestCase.logs)

        val logsResult = LogsValidator.valid(origLogs, postLogs)

        val postRepository = RepositoryBuilder.build(stateTestCase.post)
        val repoResults = RepositoryValidator.valid(repository, postRepository)

        logger.info("--------- POST Validation---------")
        val outputResults = OutputValidator.valid(Hex.toHexString(programResult.hReturn), stateTestCase.out)

        val results = ArrayList<String>()
        results.addAll(repoResults)
        results.addAll(logsResult)
        results.addAll(outputResults)

        for (result in results) {
            logger.error(result)
        }

        logger.info("\n\n")
        return results
    }

    companion object {

        private val logger = LoggerFactory.getLogger("TCK-Test")

        fun run(stateTestCase2: StateTestCase): List<String> {
            return StateTestRunner(stateTestCase2).runImpl()
        }
    }
}
