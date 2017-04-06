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

package org.ethereum.tck;

import org.ethereum.jsontestsuite.suite.JSONReader;
import org.ethereum.jsontestsuite.suite.StateTestCase;
import org.ethereum.jsontestsuite.suite.StateTestSuite;
import org.ethereum.jsontestsuite.suite.runners.StateTestRunner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RunTck {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");


    public static void main(final String[] args) throws ParseException, IOException {

        if (args.length > 0){

            if (args[0].equals("filerun")) {
                logger.info("TCK Running, file: " + args[1]);
                runTest(args[1]);
            } else if ((args[0].equals("content"))) {
                logger.debug("TCK Running, content: ");
                runContentTest(args[1].replaceAll("'", "\""));
            }

        } else {
            logger.info("No test case specified");
        }
    }

    private static void runContentTest(final String content) throws ParseException, IOException {

        final Map<String, Boolean> summary = new HashMap<>();

        final JSONParser parser = new JSONParser();
        final JSONObject testSuiteObj = (JSONObject) parser.parse(content);

        final StateTestSuite stateTestSuite = new StateTestSuite(testSuiteObj.toJSONString());
        final Map<String, StateTestCase> testCases = stateTestSuite.getTestCases();

        for (final String testName : testCases.keySet()) {

            logger.info(" Test case: {}", testName);

            final StateTestCase stateTestCase = testCases.get(testName);
            final List<String> result = StateTestRunner.run(stateTestCase);

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

        if (fails > 0)
            System.exit(1);
        else
            System.exit(0);

    }


    private static void runTest(final String name) throws ParseException, IOException {

        final String testCaseJson = JSONReader.getFromLocal(name);
        runContentTest(testCaseJson);
    }
}
