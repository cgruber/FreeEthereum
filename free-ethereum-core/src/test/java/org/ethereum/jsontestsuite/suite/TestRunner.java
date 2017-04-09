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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.*;
import org.ethereum.jsontestsuite.suite.builder.BlockBuilder;
import org.ethereum.jsontestsuite.suite.builder.RepositoryBuilder;
import org.ethereum.jsontestsuite.suite.model.BlockTck;
import org.ethereum.jsontestsuite.suite.validators.BlockHeaderValidator;
import org.ethereum.jsontestsuite.suite.validators.RepositoryValidator;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.ByteUtil;
import org.ethereum.validator.DependentBlockHeaderRuleAdapter;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.ethereum.vm.program.invoke.ProgramInvokeImpl;
import org.ethereum.vm.trace.ProgramTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

import static org.ethereum.crypto.HashUtil.shortHash;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.vm.VMUtils.saveProgramTraceFile;

/**
 * @author Roman Mandeleil
 * @since 02.07.2014
 */
public class TestRunner {

    private final Logger logger = LoggerFactory.getLogger("TCK-Test");
    private ProgramTrace trace = null;
    private String bestStateRoot;

    public List<String> runTestSuite(final TestSuite testSuite) {

        final Iterator<TestCase> testIterator = testSuite.iterator();
        final List<String> resultCollector = new ArrayList<>();

        while (testIterator.hasNext()) {

            final TestCase testCase = testIterator.next();

            final TestRunner runner = new TestRunner();
            final List<String> result = runner.runTestCase(testCase);
            resultCollector.addAll(result);
        }

        return resultCollector;
    }


    public List<String> runTestCase(final BlockTestCase testCase) {


        /* 1 */ // Create genesis + init pre state
        final Block genesis = BlockBuilder.INSTANCE.build(testCase.getGenesisBlockHeader(), null, null);
        Repository repository = RepositoryBuilder.INSTANCE.build(testCase.getPre());

        final IndexedBlockStore blockStore = new IndexedBlockStore();
        blockStore.init(new HashMapDB<>(), new HashMapDB<>());
        blockStore.saveBlock(genesis, genesis.getCumulativeDifficulty(), true);

        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();

        final BlockchainImpl blockchain = new BlockchainImpl(blockStore, repository)
                .withParentBlockHeaderValidator(CommonConfig.getDefault().parentHeaderValidator());
        blockchain.byTest = true;

        final PendingStateImpl pendingState = new PendingStateImpl(new EthereumListenerAdapter(), blockchain);

        blockchain.setBestBlock(genesis);
        blockchain.setTotalDifficulty(genesis.getCumulativeDifficulty());
        blockchain.setParentHeaderValidator(new DependentBlockHeaderRuleAdapter());
        blockchain.setProgramInvokeFactory(programInvokeFactory);

        blockchain.setPendingState(pendingState);
        pendingState.setBlockchain(blockchain);

        /* 2 */ // Create block traffic list
        final List<Block> blockTraffic = new ArrayList<>();
        for (final BlockTck blockTck : testCase.getBlocks()) {
            final Block block = BlockBuilder.INSTANCE.build(blockTck.getBlockHeader(),
                    blockTck.getTransactions(),
                    blockTck.getUncleHeaders());

            final boolean setNewStateRoot = !((blockTck.getTransactions() == null)
                    && (blockTck.getUncleHeaders() == null)
                    && (blockTck.getBlockHeader() == null));

            Block tBlock = null;
            try {
                final byte[] rlp = Utils.parseData(blockTck.getRlp());
                tBlock = new Block(rlp);

                final ArrayList<String> outputSummary =
                        BlockHeaderValidator.valid(tBlock.getHeader(), block.getHeader());

                if (!outputSummary.isEmpty()){
                    for (final String output : outputSummary)
                        logger.error("{}", output);
                }

                blockTraffic.add(tBlock);
            } catch (final Exception e) {
                System.out.println("*** Exception");
            }
        }

        /* 3 */ // Inject blocks to the blockchain execution
        for (final Block block : blockTraffic) {

            final ImportResult importResult = blockchain.tryToConnect(block);
            logger.debug("{} ~ {} difficulty: {} ::: {}", block.getShortHash(), shortHash(block.getParentHash()),
                    block.getCumulativeDifficulty(), importResult.toString());
        }

        repository = blockchain.getRepository();

        //Check state root matches last valid block
        final List<String> results = new ArrayList<>();
        final String currRoot = Hex.toHexString(repository.getRoot());

        final byte[] bestHash = Hex.decode(testCase.getLastblockhash());
        final String finalRoot = Hex.toHexString(blockStore.getBlockByHash(bestHash).getStateRoot());

        if (!finalRoot.equals(currRoot)){
            final String formattedString = String.format("Root hash doesn't match best: expected: %s current: %s",
                    finalRoot, currRoot);
            results.add(formattedString);
        }

        final Repository postRepository = RepositoryBuilder.INSTANCE.build(testCase.getPostState());
        final List<String> repoResults = RepositoryValidator.valid(repository, postRepository);
        results.addAll(repoResults);

        return results;
    }


    public List<String> runTestCase(final TestCase testCase) {

        logger.info("\n***");
        logger.info(" Running test case: [" + testCase.getName() + "]");
        logger.info("***\n");
        final List<String> results = new ArrayList<>();


        logger.info("--------- PRE ---------");
        final IterableTestRepository testRepository = new IterableTestRepository(new RepositoryRoot(new HashMapDB<>()));
        testRepository.environmental = true;
        final Repository repository = loadRepository(testRepository, testCase.getPre());


            /* 2. Create ProgramInvoke - Env/Exec */
        final Env env = testCase.getEnv();
        final Exec exec = testCase.getExec();
        final Logs logs = testCase.getLogs();

        final byte[] address = exec.getAddress();
        final byte[] origin = exec.getOrigin();
        final byte[] caller = exec.getCaller();
        final byte[] balance = ByteUtil.bigIntegerToBytes(repository.getBalance(exec.getAddress()));
        final byte[] gasPrice = exec.getGasPrice();
        final byte[] gas = exec.getGas();
        final byte[] callValue = exec.getValue();
        final byte[] msgData = exec.getData();
        final byte[] lastHash = env.getPreviousHash();
        final byte[] coinbase = env.getCurrentCoinbase();
        final long timestamp = ByteUtil.byteArrayToLong(env.getCurrentTimestamp());
        final long number = ByteUtil.byteArrayToLong(env.getCurrentNumber());
        final byte[] difficulty = env.getCurrentDifficulty();
        final byte[] gaslimit = env.getCurrentGasLimit();

        // Origin and caller need to exist in order to be able to execute
        if (repository.getAccountState(origin) == null)
            repository.createAccount(origin);
        if (repository.getAccountState(caller) == null)
            repository.createAccount(caller);

        final ProgramInvoke programInvoke = new ProgramInvokeImpl(address, origin, caller, balance,
                gasPrice, gas, callValue, msgData, lastHash, coinbase,
                timestamp, number, difficulty, gaslimit, repository, new BlockStoreDummy(), true);

            /* 3. Create Program - exec.code */
            /* 4. run VM */
        final VM vm = new VM();
        final Program program = new Program(exec.getCode(), programInvoke);
        boolean vmDidThrowAnEception = false;
        RuntimeException e = null;
        try {
            while (!program.isStopped())
                vm.step(program);
        } catch (final RuntimeException ex) {
            vmDidThrowAnEception = true;
            e = ex;
        }
        final String content = program.getTrace().asJsonString(true);
        saveProgramTraceFile(SystemProperties.getDefault(), testCase.getName(), content);

        if (testCase.getPost() == null) {
            if (!vmDidThrowAnEception) {
                final String output =
                        "VM was expected to throw an exception";
                logger.info(output);
                results.add(output);
            } else
                logger.info("VM did throw an exception: " + e.toString());
        } else {
            if (vmDidThrowAnEception) {
                final String output =
                        "VM threw an unexpected exception: " + e.toString();
                logger.info(output, e);
                results.add(output);
                return results;
            }

            this.trace = program.getTrace();

            logger.info("--------- POST --------");

            /* 5. Assert Post values */
            if (testCase.getPost() != null) {
                for (final ByteArrayWrapper key : testCase.getPost().keySet()) {

                    final AccountState accountState = testCase.getPost().get(key);

                    final long expectedNonce = accountState.getNonceLong();
                    final BigInteger expectedBalance = accountState.getBigIntegerBalance();
                    final byte[] expectedCode = accountState.getCode();

                    final boolean accountExist = (null != repository.getAccountState(key.getData()));
                    if (!accountExist) {

                        final String output =
                                String.format("The expected account does not exist. key: [ %s ]",
                                        Hex.toHexString(key.getData()));
                        logger.info(output);
                        results.add(output);
                        continue;
                    }

                    final long actualNonce = repository.getNonce(key.getData()).longValue();
                    final BigInteger actualBalance = repository.getBalance(key.getData());
                    byte[] actualCode = repository.getCode(key.getData());
                    if (actualCode == null) actualCode = "".getBytes();

                    if (expectedNonce != actualNonce) {

                        final String output =
                                String.format("The nonce result is different. key: [ %s ],  expectedNonce: [ %d ] is actualNonce: [ %d ] ",
                                        Hex.toHexString(key.getData()), expectedNonce, actualNonce);
                        logger.info(output);
                        results.add(output);
                    }

                    if (!expectedBalance.equals(actualBalance)) {

                        final String output =
                                String.format("The balance result is different. key: [ %s ],  expectedBalance: [ %s ] is actualBalance: [ %s ] ",
                                        Hex.toHexString(key.getData()), expectedBalance.toString(), actualBalance.toString());
                        logger.info(output);
                        results.add(output);
                    }

                    if (!Arrays.equals(expectedCode, actualCode)) {

                        final String output =
                                String.format("The code result is different. account: [ %s ],  expectedCode: [ %s ] is actualCode: [ %s ] ",
                                        Hex.toHexString(key.getData()),
                                        Hex.toHexString(expectedCode),
                                        Hex.toHexString(actualCode));
                        logger.info(output);
                        results.add(output);
                    }

                    // assert storage
                    final Map<DataWord, DataWord> storage = accountState.getStorage();
                    for (final DataWord storageKey : storage.keySet()) {

                        final byte[] expectedStValue = storage.get(storageKey).getData();

                        final ContractDetails contractDetails =
                                program.getStorage().getContractDetails(accountState.getAddress());

                        if (contractDetails == null) {

                            final String output =
                                    String.format("Storage raw doesn't exist: key [ %s ], expectedValue: [ %s ]",
                                            Hex.toHexString(storageKey.getData()),
                                            Hex.toHexString(expectedStValue)
                                    );
                            logger.info(output);
                            results.add(output);
                            continue;
                        }

                        final Map<DataWord, DataWord> testStorage = contractDetails.getStorage();
                        final DataWord actualValue = testStorage.get(new DataWord(storageKey.getData()));

                        if (actualValue == null ||
                                !Arrays.equals(expectedStValue, actualValue.getData())) {

                            final String output =
                                    String.format("Storage value different: key [ %s ], expectedValue: [ %s ], actualValue: [ %s ]",
                                            Hex.toHexString(storageKey.getData()),
                                            Hex.toHexString(expectedStValue),
                                            actualValue == null ? "" : Hex.toHexString(actualValue.getNoLeadZeroesData()));
                            logger.info(output);
                            results.add(output);
                        }
                    }

                /* asset logs */
                    final List<LogInfo> logResult = program.getResult().getLogInfoList();

                    final Iterator<LogInfo> postLogs = logs.getIterator();
                    int i = 0;
                    while (postLogs.hasNext()) {

                        final LogInfo expectedLogInfo = postLogs.next();

                        LogInfo foundLogInfo = null;
                        if (logResult.size() > i)
                            foundLogInfo = logResult.get(i);

                        if (foundLogInfo == null) {
                            final String output =
                                    String.format("Expected log [ %s ]", expectedLogInfo.toString());
                            logger.info(output);
                            results.add(output);
                        } else {
                            if (!Arrays.equals(expectedLogInfo.getAddress(), foundLogInfo.getAddress())) {
                                final String output =
                                        String.format("Expected address [ %s ], found [ %s ]", Hex.toHexString(expectedLogInfo.getAddress()), Hex.toHexString(foundLogInfo.getAddress()));
                                logger.info(output);
                                results.add(output);
                            }

                            if (!Arrays.equals(expectedLogInfo.getData(), foundLogInfo.getData())) {
                                final String output =
                                        String.format("Expected data [ %s ], found [ %s ]", Hex.toHexString(expectedLogInfo.getData()), Hex.toHexString(foundLogInfo.getData()));
                                logger.info(output);
                                results.add(output);
                            }

                            if (!expectedLogInfo.getBloom().equals(foundLogInfo.getBloom())) {
                                final String output =
                                        String.format("Expected bloom [ %s ], found [ %s ]",
                                                Hex.toHexString(expectedLogInfo.getBloom().getData()),
                                                Hex.toHexString(foundLogInfo.getBloom().getData()));
                                logger.info(output);
                                results.add(output);
                            }

                            if (expectedLogInfo.getTopics().size() != foundLogInfo.getTopics().size()) {
                                final String output =
                                        String.format("Expected number of topics [ %d ], found [ %d ]",
                                                expectedLogInfo.getTopics().size(), foundLogInfo.getTopics().size());
                                logger.info(output);
                                results.add(output);
                            } else {
                                int j = 0;
                                for (final DataWord topic : expectedLogInfo.getTopics()) {
                                    final byte[] foundTopic = foundLogInfo.getTopics().get(j).getData();

                                    if (!Arrays.equals(topic.getData(), foundTopic)) {
                                        final String output =
                                                String.format("Expected topic [ %s ], found [ %s ]", Hex.toHexString(topic.getData()), Hex.toHexString(foundTopic));
                                        logger.info(output);
                                        results.add(output);
                                    }

                                    ++j;
                                }
                            }
                        }

                        ++i;
                    }
                }
            }

            // TODO: assert that you have no extra accounts in the repository
            // TODO:  -> basically the deleted by suicide should be deleted
            // TODO:  -> and no unexpected created

            final List<org.ethereum.vm.CallCreate> resultCallCreates =
                    program.getResult().getCallCreateList();

            // assert call creates
            for (int i = 0; i < testCase.getCallCreateList().size(); ++i) {

                org.ethereum.vm.CallCreate resultCallCreate = null;
                if (resultCallCreates != null && resultCallCreates.size() > i) {
                    resultCallCreate = resultCallCreates.get(i);
                }

                final CallCreate expectedCallCreate = testCase.getCallCreateList().get(i);

                if (resultCallCreate == null && expectedCallCreate != null) {

                    final String output =
                            String.format("Missing call/create invoke: to: [ %s ], data: [ %s ], gas: [ %s ], value: [ %s ]",
                                    Hex.toHexString(expectedCallCreate.getDestination()),
                                    Hex.toHexString(expectedCallCreate.getData()),
                                    Hex.toHexString(expectedCallCreate.getGasLimit()),
                                    Hex.toHexString(expectedCallCreate.getValue()));
                    logger.info(output);
                    results.add(output);

                    continue;
                }

                final boolean assertDestination = Arrays.equals(
                        expectedCallCreate.getDestination(),
                        resultCallCreate.getDestination());
                if (!assertDestination) {

                    final String output =
                            String.format("Call/Create destination is different. Expected: [ %s ], result: [ %s ]",
                                    Hex.toHexString(expectedCallCreate.getDestination()),
                                    Hex.toHexString(resultCallCreate.getDestination()));
                    logger.info(output);
                    results.add(output);
                }

                final boolean assertData = Arrays.equals(
                        expectedCallCreate.getData(),
                        resultCallCreate.getData());
                if (!assertData) {

                    final String output =
                            String.format("Call/Create data is different. Expected: [ %s ], result: [ %s ]",
                                    Hex.toHexString(expectedCallCreate.getData()),
                                    Hex.toHexString(resultCallCreate.getData()));
                    logger.info(output);
                    results.add(output);
                }

                final boolean assertGasLimit = Arrays.equals(
                        expectedCallCreate.getGasLimit(),
                        resultCallCreate.getGasLimit());
                if (!assertGasLimit) {
                    final String output =
                            String.format("Call/Create gasLimit is different. Expected: [ %s ], result: [ %s ]",
                                    Hex.toHexString(expectedCallCreate.getGasLimit()),
                                    Hex.toHexString(resultCallCreate.getGasLimit()));
                    logger.info(output);
                    results.add(output);
                }

                final boolean assertValue = Arrays.equals(
                        expectedCallCreate.getValue(),
                        resultCallCreate.getValue());
                if (!assertValue) {
                    final String output =
                            String.format("Call/Create value is different. Expected: [ %s ], result: [ %s ]",
                                    Hex.toHexString(expectedCallCreate.getValue()),
                                    Hex.toHexString(resultCallCreate.getValue()));
                    logger.info(output);
                    results.add(output);
                }
            }

            // assert out
            final byte[] expectedHReturn = testCase.getOut();
            byte[] actualHReturn = EMPTY_BYTE_ARRAY;
            if (program.getResult().getHReturn() != null) {
                actualHReturn = program.getResult().getHReturn();
            }

            if (!Arrays.equals(expectedHReturn, actualHReturn)) {

                final String output =
                        String.format("HReturn is different. Expected hReturn: [ %s ], actual hReturn: [ %s ]",
                                Hex.toHexString(expectedHReturn),
                                Hex.toHexString(actualHReturn));
                logger.info(output);
                results.add(output);
            }

            // assert gas
            final BigInteger expectedGas = new BigInteger(1, testCase.getGas());
            final BigInteger actualGas = new BigInteger(1, gas).subtract(BigInteger.valueOf(program.getResult().getGasUsed()));

            if (!expectedGas.equals(actualGas)) {

                final String output =
                        String.format("Gas remaining is different. Expected gas remaining: [ %s ], actual gas remaining: [ %s ]",
                                expectedGas.toString(),
                                actualGas.toString());
                logger.info(output);
                results.add(output);
            }
            /*
             * end of if(testCase.getPost().size() == 0)
             */
        }

        return results;
    }

    public org.ethereum.core.Transaction createTransaction(final Transaction tx) {

        final byte[] nonceBytes = ByteUtil.longToBytes(tx.nonce);
        final byte[] gasPriceBytes = ByteUtil.longToBytes(tx.gasPrice);
        final byte[] gasBytes = tx.gasLimit;
        final byte[] valueBytes = ByteUtil.longToBytes(tx.value);
        final byte[] toAddr = tx.getTo();
        final byte[] data = tx.getData();

        final org.ethereum.core.Transaction transaction = new org.ethereum.core.Transaction(
                nonceBytes, gasPriceBytes, gasBytes,
                toAddr, valueBytes, data);

        return transaction;
    }

    private Repository loadRepository(final Repository track, final Map<ByteArrayWrapper, AccountState> pre) {


            /* 1. Store pre-exist accounts - Pre */
        for (final ByteArrayWrapper key : pre.keySet()) {

            final AccountState accountState = pre.get(key);
            final byte[] addr = key.getData();

            track.addBalance(addr, new BigInteger(1, accountState.getBalance()));
            track.setNonce(key.getData(), new BigInteger(1, accountState.getNonce()));

            track.saveCode(addr, accountState.getCode());

            for (final DataWord storageKey : accountState.getStorage().keySet()) {
                track.addStorageRow(addr, storageKey, accountState.getStorage().get(storageKey));
            }
        }

        return track;
    }


    public ProgramTrace getTrace() {
        return trace;
    }
}
