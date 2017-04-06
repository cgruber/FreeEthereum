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

package org.ethereum.tck

import org.ethereum.jsontestsuite.suite.JSONReader
import org.ethereum.jsontestsuite.suite.StateTestSuite
import org.ethereum.jsontestsuite.suite.runners.StateTestRunner
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

internal object RunTck {

    private val logger = LoggerFactory.getLogger("TCK-Test")


    @Throws(ParseException::class, IOException::class)
    @JvmStatic fun main(args: Array<String>) {

        if (args.isNotEmpty()) {

            if (args[0] == "filerun") {
                logger.info("TCK Running, file: " + args[1])
                runTest(args[1])
            } else if (args[0] == "content") {
                logger.debug("TCK Running, content: ")
                runContentTest(args[1].replace("'".toRegex(), "\""))
            }

        } else {
            logger.info("No test case specified")
        }
    }

    @Throws(ParseException::class, IOException::class)
    private fun runContentTest(content: String) {

        val summary = HashMap<String, Boolean>()

        val parser = JSONParser()
        val testSuiteObj = parser.parse(content) as JSONObject

        val stateTestSuite = StateTestSuite(testSuiteObj.toJSONString())
        val testCases = stateTestSuite.testCases

        for (testName in testCases.keys) {

            logger.info(" Test case: {}", testName)

            val stateTestCase = testCases[testName]
            val result = StateTestRunner.run(stateTestCase)

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

        if (fails > 0)
            System.exit(1)
        else
            System.exit(0)

    }


    @Throws(ParseException::class, IOException::class)
    private fun runTest(name: String) {

        val testCaseJson = JSONReader.getFromLocal(name)
        runContentTest(testCaseJson)
    }
}
