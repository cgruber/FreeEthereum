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

package org.ethereum.jsontestsuite

import org.ethereum.jsontestsuite.suite.*
import org.ethereum.jsontestsuite.suite.runners.StateTestRunner
import org.ethereum.jsontestsuite.suite.runners.TransactionTestRunner
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.junit.Assert
import org.junit.Assume
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

/**
 * Test file specific for tests maintained in the GitHub repository
 * by the Ethereum DEV team. <br></br>

 * @see [https://github.com/ethereum/tests/](https://github.com/ethereum/tests/)
 */
internal object GitHubJSONTestSuite {

    private val logger = LoggerFactory.getLogger("TCK-Test")


    @Throws(ParseException::class)
    fun runGitHubJsonVMTest(json: String, testName: String) {
        Assume.assumeFalse("Online test is not available", json == "")

        val parser = JSONParser()
        val testSuiteObj = parser.parse(json) as JSONObject

        val testSuite = TestSuite(testSuiteObj)
        val testIterator = testSuite.iterator()

        for (testCase in testSuite.allTests) {

            var prefix = "    "
            if (testName == testCase.name) prefix = " => "

            logger.info(prefix + testCase.name)
        }

        while (testIterator.hasNext()) {

            val testCase = testIterator.next()
            if (testName == testCase.name) {
                val runner = TestRunner()
                val result = runner.runTestCase(testCase)
                Assert.assertTrue(result.isEmpty())
                return
            }
        }
    }

    @Throws(ParseException::class)
    fun runGitHubJsonVMTest(json: String) {
        val excluded = HashSet<String>()
        runGitHubJsonVMTest(json, excluded)
    }


    @Throws(ParseException::class)
    fun runGitHubJsonVMTest(json: String, excluded: Set<String>) {
        Assume.assumeFalse("Online test is not available", json == "")

        val parser = JSONParser()
        val testSuiteObj = parser.parse(json) as JSONObject

        val testSuite = TestSuite(testSuiteObj)
        val testIterator = testSuite.iterator()

        for (testCase in testSuite.allTests) {

            var prefix = "    "
            if (excluded.contains(testCase.name)) prefix = "[X] "

            logger.info(prefix + testCase.name)
        }


        while (testIterator.hasNext()) {

            val testCase = testIterator.next()
            if (excluded.contains(testCase.name))
                continue

            val runner = TestRunner()
            val result = runner.runTestCase(testCase)
            try {
                Assert.assertTrue(result.isEmpty())
            } catch (e: AssertionError) {
                println(String.format("Error on running testcase %s : %s", testCase.name, result[0]))
                throw e
            }

        }
    }


    @Throws(ParseException::class, IOException::class)
    fun runGitHubJsonSingleBlockTest(json: String, testName: String) {

        val testSuite = BlockTestSuite(json)
        val testCollection = testSuite.testCases.keys

        for (testCase in testCollection) {
            if (testCase == testName)
                logger.info(" => " + testCase)
            else
                logger.info("    " + testCase)
        }

        runSingleBlockTest(testSuite, testName)
    }


    @Throws(ParseException::class, IOException::class)
    fun runGitHubJsonBlockTest(json: String, excluded: Set<String>) {
        Assume.assumeFalse("Online test is not available", json == "")

        val testSuite = BlockTestSuite(json)
        val testCases = testSuite.testCases.keys
        val summary = HashMap<String, Boolean>()

        for (testCase in testCases)
            if (excluded.contains(testCase))
                logger.info(" [X] " + testCase)
            else
                logger.info("     " + testCase)


        for (testName in testCases) {

            if (excluded.contains(testName)) {
                logger.info(" Not running: " + testName)
                continue
            }

            val result = runSingleBlockTest(testSuite, testName)

            if (!result.isEmpty())
                summary.put(testName, false)
            else
                summary.put(testName, true)
        }


        logger.info("")
        logger.info("")
        logger.info("Summary: ")
        logger.info("=========")

        var fails = 0
        var pass = 0
        for (key in summary.keys) {

            if (summary[key]!!) ++pass else ++fails
            val sumTest = String.format("%-60s:^%s", key, if (summary[key]!!) "OK" else "FAIL").replace(' ', '.').replace("^", " ")
            logger.info(sumTest)
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails)

        Assert.assertTrue(fails == 0)

    }

    @Throws(ParseException::class, IOException::class)
    fun runGitHubJsonBlockTest(json: String) {
        val excluded = HashSet<String>()
        runGitHubJsonBlockTest(json, excluded)
    }

    private fun runSingleBlockTest(testSuite: BlockTestSuite, testName: String): List<String> {

        val blockTestCase = testSuite.testCases[testName]
        val runner = TestRunner()

        logger.info("\n\n ***************** Running test: {} ***************************** \n\n", testName)
        val result = runner.runTestCase(blockTestCase)

        logger.info("--------- POST Validation---------")
        if (!result.isEmpty())
            for (single in result)
                logger.info(single)


        return result
    }


    @Throws(IOException::class)
    fun runStateTest(jsonSuite: String, testName: String) {

        val stateTestSuite = StateTestSuite(jsonSuite)
        val testCases = stateTestSuite.testCases

        for (testCase in testCases.keys) {
            if (testCase == testName)
                logger.info("  => " + testCase)
            else
                logger.info("     " + testCase)
        }

        val testCase = testCases[testName]
        if (testCase != null) {
            val output = String.format("*  running: %s  *", testName)
            val line = output.replace(".".toRegex(), "*")

            logger.info(line)
            logger.info(output)
            logger.info(line)
            val fails = StateTestRunner.run(testCases[testName])

            Assert.assertTrue(fails.isEmpty())

        } else {
            logger.error("Sorry test case doesn't exist: {}", testName)
        }
    }

    @Throws(IOException::class)
    @JvmOverloads fun runStateTest(jsonSuite: String, excluded: Set<String> = HashSet<String>()) {

        val stateTestSuite = StateTestSuite(jsonSuite)
        val testCases = stateTestSuite.testCases
        val summary = HashMap<String, Boolean>()


        for (testCase in testCases.keys) {
            if (excluded.contains(testCase))
                logger.info(" [X] " + testCase)
            else
                logger.info("     " + testCase)
        }

        val testNames = stateTestSuite.testCases.keys
        for (testName in testNames) {

            if (excluded.contains(testName)) continue
            val output = String.format("*  running: %s  *", testName)
            val line = output.replace(".".toRegex(), "*")

            logger.info(line)
            logger.info(output)
            logger.info(line)

            val result = StateTestRunner.run(testCases[testName])
            if (!result.isEmpty())
                summary.put(testName, false)
            else
                summary.put(testName, true)
        }

        logger.info("Summary: ")
        logger.info("=========")

        var fails = 0
        var pass = 0
        for (key in summary.keys) {

            if (summary[key]!!) ++pass else ++fails
            val sumTest = String.format("%-60s:^%s", key, if (summary[key]!!) "OK" else "FAIL").replace(' ', '.').replace("^", " ")
            logger.info(sumTest)
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails)

        Assert.assertTrue(fails == 0)
    }

    @Throws(IOException::class, ParseException::class)
    fun runGitHubJsonTransactionTest(json: String, excluded: Set<String>) {

        val transactionTestSuite = TransactionTestSuite(json)
        val testCases = transactionTestSuite.testCases
        val summary = HashMap<String, Boolean>()


        for (testCase in testCases.keys) {
            if (excluded.contains(testCase))
                logger.info(" [X] " + testCase)
            else
                logger.info("     " + testCase)
        }

        val testNames = transactionTestSuite.testCases.keys
        for (testName in testNames) {

            if (excluded.contains(testName)) continue
            val output = String.format("*  running: %s  *", testName)
            val line = output.replace(".".toRegex(), "*")

            logger.info(line)
            logger.info(output)
            logger.info(line)

            logger.info("==> Running test case: {}", testName)
            val result = TransactionTestRunner.run(testCases[testName])
            if (!result.isEmpty())
                summary.put(testName, false)
            else
                summary.put(testName, true)
        }

        logger.info("Summary: ")
        logger.info("=========")

        var fails = 0
        var pass = 0
        for (key in summary.keys) {

            if (summary[key]!!) ++pass else ++fails
            val sumTest = String.format("%-60s:^%s", key, if (summary[key]!!) "OK" else "FAIL").replace(' ', '.').replace("^", " ")
            logger.info(sumTest)
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails)

        Assert.assertTrue(fails == 0)
    }

}
