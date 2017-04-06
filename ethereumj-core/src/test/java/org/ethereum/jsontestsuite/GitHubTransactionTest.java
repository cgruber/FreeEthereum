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

package org.ethereum.jsontestsuite;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.*;
import org.ethereum.config.net.BaseNetConfig;
import org.ethereum.config.net.MainNetConfig;
import org.ethereum.jsontestsuite.suite.JSONReader;
import org.ethereum.jsontestsuite.suite.TransactionTestSuite;
import org.ethereum.jsontestsuite.suite.runners.TransactionTestRunner;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitHubTransactionTest {

    //SHACOMMIT of tested commit, ethereum/tests.git
    private final String shacommit = "289b3e4524786618c7ec253b516bc8e76350f947";

    @Before
    public void setup() {
        SystemProperties.getDefault().setBlockchainConfig(new MainNetConfig());
    }

    @After
    public void recover() {
        SystemProperties.resetToDefault();
    }

    @Test
    public void testEIP155TransactionTestFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        SystemProperties.getDefault().setBlockchainConfig(new BaseNetConfig() {{
            add(0, new FrontierConfig());
            add(1_150_000, new HomesteadConfig());
            add(2_457_000, new Eip150HFConfig(new DaoHFConfig()));
            add(2_675_000, new Eip160HFConfig(new DaoHFConfig()){
                @Override
                public Integer getChainId() {
                    return null;
                }
            });
        }});
        final String json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTest.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json, excluded);
    }

    @Ignore
    @Test
    public void runsingleTest() throws Exception {
        final String json = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTest.json", shacommit);
        final TransactionTestSuite testSuite = new TransactionTestSuite(json);
        final List<String> res = TransactionTestRunner.run(testSuite.getTestCases().get("V_overflow64bitPlus28"));
        System.out.println(res);
    }

    @Test
    public void testHomesteadTestsFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        final String json1 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/tt10mbDataField.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json1, excluded);

        final String json2 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTest.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json2, excluded);

        final String json3 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttTransactionTestEip155VitaliksTests.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json3, excluded);

        final String json4 = JSONReader.loadJSONFromCommit("TransactionTests/Homestead/ttWrongRLPTransaction.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json4, excluded);
    }

    @Test
    public void testRandomTestFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        // pre-EIP155 wrong chain id (negative)
        final String json = JSONReader.loadJSONFromCommit("TransactionTests/RandomTests/tr201506052141PYTHON.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json, excluded);
    }

    @Test
    public void testGeneralTestsFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        final String json1 = JSONReader.loadJSONFromCommit("TransactionTests/tt10mbDataField.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json1, excluded);

        final String json2 = JSONReader.loadJSONFromCommit("TransactionTests/ttTransactionTest.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json2, excluded);
    }

    @Ignore // Few tests fails, RLPWrongByteEncoding and RLPLength preceding 0s errors left
    @Test
    public void testWrongRLPTestsFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();

        final String json = JSONReader.loadJSONFromCommit("TransactionTests/ttWrongRLPTransaction.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json, excluded);
    }

    @Test
    public void testEip155VitaliksTestFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        final String json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTestEip155VitaliksTests.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json, excluded);
    }

    @Test
    public void testEip155VRuleTestFromGitHub() throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        final String json = JSONReader.loadJSONFromCommit("TransactionTests/EIP155/ttTransactionTestVRule.json", shacommit);
        GitHubJSONTestSuite.INSTANCE.runGitHubJsonTransactionTest(json, excluded);
    }
}
