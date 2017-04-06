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

import org.ethereum.jsontestsuite.suite.*;
import org.ethereum.jsontestsuite.suite.runners.StateTestRunner;
import org.ethereum.jsontestsuite.suite.runners.TransactionTestRunner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Test file specific for tests maintained in the GitHub repository
 * by the Ethereum DEV team. <br/>
 *
 * @see <a href="https://github.com/ethereum/tests/">https://github.com/ethereum/tests/</a>
 */
class GitHubJSONTestSuite {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");


    static void runGitHubJsonVMTest(final String json, final String testName) throws ParseException {
        Assume.assumeFalse("Online test is not available", json.equals(""));

        final JSONParser parser = new JSONParser();
        final JSONObject testSuiteObj = (JSONObject) parser.parse(json);

        final TestSuite testSuite = new TestSuite(testSuiteObj);
        final Iterator<TestCase> testIterator = testSuite.iterator();

        for (final TestCase testCase : testSuite.getAllTests()) {

            String prefix = "    ";
            if (testName.equals(testCase.getName())) prefix = " => ";

            logger.info(prefix + testCase.getName());
        }

        while (testIterator.hasNext()) {

            final TestCase testCase = testIterator.next();
            if (testName.equals((testCase.getName()))) {
                final TestRunner runner = new TestRunner();
                final List<String> result = runner.runTestCase(testCase);
                Assert.assertTrue(result.isEmpty());
                return;
            }
        }
    }

    static void runGitHubJsonVMTest(final String json) throws ParseException {
        final Set<String> excluded = new HashSet<>();
        runGitHubJsonVMTest(json, excluded);
    }


    static void runGitHubJsonVMTest(final String json, final Set<String> excluded) throws ParseException {
        Assume.assumeFalse("Online test is not available", json.equals(""));

        final JSONParser parser = new JSONParser();
        final JSONObject testSuiteObj = (JSONObject) parser.parse(json);

        final TestSuite testSuite = new TestSuite(testSuiteObj);
        final Iterator<TestCase> testIterator = testSuite.iterator();

        for (final TestCase testCase : testSuite.getAllTests()) {

            String prefix = "    ";
            if (excluded.contains(testCase.getName())) prefix = "[X] ";

            logger.info(prefix + testCase.getName());
        }


        while (testIterator.hasNext()) {

            final TestCase testCase = testIterator.next();
            if (excluded.contains(testCase.getName()))
                continue;

            final TestRunner runner = new TestRunner();
            final List<String> result = runner.runTestCase(testCase);
            try {
                Assert.assertTrue(result.isEmpty());
            } catch (final AssertionError e) {
                System.out.println(String.format("Error on running testcase %s : %s", testCase.getName(), result.get(0)));
                throw e;
            }
        }
    }


    static void runGitHubJsonSingleBlockTest(final String json, final String testName) throws ParseException, IOException {

        final BlockTestSuite testSuite = new BlockTestSuite(json);
        final Set<String> testCollection = testSuite.getTestCases().keySet();

        for (final String testCase : testCollection) {
            if (testCase.equals(testName))
                logger.info(" => " + testCase);
            else
                logger.info("    " + testCase);
        }

        runSingleBlockTest(testSuite, testName);
    }


    static void runGitHubJsonBlockTest(final String json, final Set<String> excluded) throws ParseException, IOException {
        Assume.assumeFalse("Online test is not available", json.equals(""));

        final BlockTestSuite testSuite = new BlockTestSuite(json);
        final Set<String> testCases = testSuite.getTestCases().keySet();
        final Map<String, Boolean> summary = new HashMap<>();

        for (final String testCase : testCases)
            if ( excluded.contains(testCase))
                logger.info(" [X] " + testCase);
            else
                logger.info("     " + testCase);


        for (final String testName : testCases) {

            if ( excluded.contains(testName)) {
                logger.info(" Not running: " + testName);
                continue;
            }

            final List<String> result = runSingleBlockTest(testSuite, testName);

            if (!result.isEmpty())
                summary.put(testName, false);
            else
                summary.put(testName, true);
        }


        logger.info("");
        logger.info("");
        logger.info("Summary: ");
        logger.info("=========");

        int fails = 0; int pass = 0;
        for (final String key : summary.keySet()) {

            if (summary.get(key)) ++pass; else ++fails;
            final String sumTest = String.format("%-60s:^%s", key, (summary.get(key) ? "OK" : "FAIL")).
                    replace(' ', '.').
                    replace("^", " ");
            logger.info(sumTest);
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails);

        Assert.assertTrue(fails == 0);

    }

    protected static void runGitHubJsonBlockTest(final String json) throws ParseException, IOException {
        final Set<String> excluded = new HashSet<>();
        runGitHubJsonBlockTest(json, excluded);
    }

    private static List<String> runSingleBlockTest(final BlockTestSuite testSuite, final String testName) {

        final BlockTestCase blockTestCase = testSuite.getTestCases().get(testName);
        final TestRunner runner = new TestRunner();

        logger.info("\n\n ***************** Running test: {} ***************************** \n\n", testName);
        final List<String> result = runner.runTestCase(blockTestCase);

        logger.info("--------- POST Validation---------");
        if (!result.isEmpty())
            for (final String single : result)
                logger.info(single);


        return result;
    }


    public static void runStateTest(final String jsonSuite) throws IOException {
        runStateTest(jsonSuite, new HashSet<>());
    }


    public static void runStateTest(final String jsonSuite, final String testName) throws IOException {

        final StateTestSuite stateTestSuite = new StateTestSuite(jsonSuite);
        final Map<String, StateTestCase> testCases = stateTestSuite.getTestCases();

        for (final String testCase : testCases.keySet()) {
            if (testCase.equals(testName))
                logger.info("  => " + testCase);
            else
                logger.info("     " + testCase);
        }

        final StateTestCase testCase = testCases.get(testName);
        if (testCase != null){
            final String output = String.format("*  running: %s  *", testName);
            final String line = output.replaceAll(".", "*");

            logger.info(line);
            logger.info(output);
            logger.info(line);
            final List<String> fails = StateTestRunner.run(testCases.get(testName));

            Assert.assertTrue(fails.isEmpty());

        } else {
            logger.error("Sorry test case doesn't exist: {}", testName);
        }
    }

    public static void runStateTest(final String jsonSuite, final Set<String> excluded) throws IOException {

        final StateTestSuite stateTestSuite = new StateTestSuite(jsonSuite);
        final Map<String, StateTestCase> testCases = stateTestSuite.getTestCases();
        final Map<String, Boolean> summary = new HashMap<>();


        for (final String testCase : testCases.keySet()) {
            if ( excluded.contains(testCase))
                logger.info(" [X] " + testCase);
            else
                logger.info("     " + testCase);
        }

        final Set<String> testNames = stateTestSuite.getTestCases().keySet();
        for (final String testName : testNames) {

            if (excluded.contains(testName)) continue;
            final String output = String.format("*  running: %s  *", testName);
            final String line = output.replaceAll(".", "*");

            logger.info(line);
            logger.info(output);
            logger.info(line);

            final List<String> result = StateTestRunner.run(testCases.get(testName));
            if (!result.isEmpty())
                summary.put(testName, false);
            else
                summary.put(testName, true);
        }

        logger.info("Summary: ");
        logger.info("=========");

        int fails = 0; int pass = 0;
        for (final String key : summary.keySet()) {

            if (summary.get(key)) ++pass; else ++fails;
            final String sumTest = String.format("%-60s:^%s", key, (summary.get(key) ? "OK" : "FAIL")).
                    replace(' ', '.').
                    replace("^", " ");
            logger.info(sumTest);
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails);

        Assert.assertTrue(fails == 0);
    }

    public static void runGitHubJsonTransactionTest(final String json, final Set<String> excluded) throws IOException, ParseException {

        final TransactionTestSuite transactionTestSuite = new TransactionTestSuite(json);
        final Map<String, TransactionTestCase> testCases = transactionTestSuite.getTestCases();
        final Map<String, Boolean> summary = new HashMap<>();


        for (final String testCase : testCases.keySet()) {
            if ( excluded.contains(testCase))
                logger.info(" [X] " + testCase);
            else
                logger.info("     " + testCase);
        }

        final Set<String> testNames = transactionTestSuite.getTestCases().keySet();
        for (final String testName : testNames) {

            if (excluded.contains(testName)) continue;
            final String output = String.format("*  running: %s  *", testName);
            final String line = output.replaceAll(".", "*");

            logger.info(line);
            logger.info(output);
            logger.info(line);

            logger.info("==> Running test case: {}", testName);
            final List<String> result = TransactionTestRunner.run(testCases.get(testName));
            if (!result.isEmpty())
                summary.put(testName, false);
            else
                summary.put(testName, true);
        }

        logger.info("Summary: ");
        logger.info("=========");

        int fails = 0; int pass = 0;
        for (final String key : summary.keySet()) {

            if (summary.get(key)) ++pass; else ++fails;
            final String sumTest = String.format("%-60s:^%s", key, (summary.get(key) ? "OK" : "FAIL")).
                    replace(' ', '.').
                    replace("^", " ");
            logger.info(sumTest);
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails);

        Assert.assertTrue(fails == 0);
    }

}
