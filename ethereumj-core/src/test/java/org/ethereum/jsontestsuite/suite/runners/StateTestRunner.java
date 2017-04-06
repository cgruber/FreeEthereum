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

package org.ethereum.jsontestsuite.suite.runners;

import org.ethereum.core.*;
import org.ethereum.db.BlockStoreDummy;
import org.ethereum.jsontestsuite.suite.Env;
import org.ethereum.jsontestsuite.suite.StateTestCase;
import org.ethereum.jsontestsuite.suite.TestProgramInvokeFactory;
import org.ethereum.jsontestsuite.suite.builder.*;
import org.ethereum.jsontestsuite.suite.validators.LogsValidator;
import org.ethereum.jsontestsuite.suite.validators.OutputValidator;
import org.ethereum.jsontestsuite.suite.validators.RepositoryValidator;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class StateTestRunner {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");
    private final StateTestCase stateTestCase;
    protected Repository repository;
    protected ProgramInvokeFactory invokeFactory;
    protected Block block;
    private Transaction transaction;
    private BlockchainImpl blockchain;
    private Env env;

    public StateTestRunner(final StateTestCase stateTestCase) {
        this.stateTestCase = stateTestCase;
    }

    public static List<String> run(final StateTestCase stateTestCase2) {
        return new StateTestRunner(stateTestCase2).runImpl();
    }

    protected ProgramResult executeTransaction(final Transaction tx) {
        final Repository track = repository.startTracking();

        final TransactionExecutor executor =
                new TransactionExecutor(transaction, env.getCurrentCoinbase(), track, new BlockStoreDummy(),
                        invokeFactory, blockchain.getBestBlock());

        try{
            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();
        } catch (final StackOverflowError soe) {
            logger.error(" !!! StackOverflowError: update your java run command with -Xss2M !!!");
            System.exit(-1);
        }

        track.commit();
        return executor.getResult();
    }

    public List<String> runImpl() {

        logger.info("");
        repository = RepositoryBuilder.build(stateTestCase.getPre());
        logger.info("loaded repository");

        transaction = TransactionBuilder.build(stateTestCase.getTransaction());
        logger.info("transaction: {}", transaction.toString());

        blockchain = new BlockchainImpl();
        blockchain.setRepository(repository);

        env = EnvBuilder.build(stateTestCase.getEnv());
        invokeFactory = new TestProgramInvokeFactory(env);

        block = BlockBuilder.build(env);

        blockchain.setBestBlock(block);
        blockchain.setProgramInvokeFactory(invokeFactory);

        final ProgramResult programResult = executeTransaction(transaction);

        repository.commit();

        final List<LogInfo> origLogs = programResult.getLogInfoList();
        final List<LogInfo> postLogs = LogBuilder.build(stateTestCase.getLogs());

        final List<String> logsResult = LogsValidator.valid(origLogs, postLogs);

        final Repository postRepository = RepositoryBuilder.build(stateTestCase.getPost());
        final List<String> repoResults = RepositoryValidator.valid(repository, postRepository);

        logger.info("--------- POST Validation---------");
        final List<String> outputResults =
                OutputValidator.valid(Hex.toHexString(programResult.getHReturn()), stateTestCase.getOut());

        final List<String> results = new ArrayList<>();
        results.addAll(repoResults);
        results.addAll(logsResult);
        results.addAll(outputResults);

        for (final String result : results) {
            logger.error(result);
        }

        logger.info("\n\n");
        return results;
    }
}
