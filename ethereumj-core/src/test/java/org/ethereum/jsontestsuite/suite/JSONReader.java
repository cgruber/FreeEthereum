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

import org.apache.commons.codec.binary.Base64;
import org.ethereum.config.SystemProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JSONReader {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");

    public static String loadJSON(final String filename) {
        String json = "";
        if (!SystemProperties.getDefault().vmTestLoadLocal())
            json = getFromUrl("https://raw.githubusercontent.com/ethereum/tests/develop/" + filename);
        return json.isEmpty() ? getFromLocal(filename) : json;
    }

    public static String loadJSONFromCommit(final String filename, final String shacommit) {
        String json = "";
        if (!SystemProperties.getDefault().vmTestLoadLocal())
            json = getFromUrl("https://raw.githubusercontent.com/ethereum/tests/" + shacommit + "/" + filename);
        if (!json.isEmpty()) json = json.replaceAll("//", "data");
        return json.isEmpty() ? getFromLocal(filename) : json;
    }

    public static String getFromLocal(final String filename) {
        System.out.println("Loading local file: " + filename);
        try {
            final File vmTestFile = new File(filename);
            if (!vmTestFile.exists()){
                System.out.println(" Error: no file: " +filename);
                System.exit(1);
            }
            return new String(Files.readAllBytes(vmTestFile.toPath()));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFromUrl(final String urlToRead) {
        final URL url;
        final HttpURLConnection conn;
        final BufferedReader rd;
        final StringBuilder result = new StringBuilder();
        String line;
        try {
            url = new URL(urlToRead);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();
            final InputStream in = conn.getInputStream();
            rd = new BufferedReader(new InputStreamReader(in), 819200);

            logger.info("Loading remote file: " + urlToRead);
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static String getTestBlobForTreeSha(final String shacommit, final String testcase) {

        final String result = getFromUrl("https://api.github.com/repos/ethereum/tests/git/trees/" + shacommit);

        final JSONParser parser = new JSONParser();
        JSONObject testSuiteObj = null;

        final List<String> fileNames = new ArrayList<>();
        try {
            testSuiteObj = (JSONObject) parser.parse(result);
            final JSONArray tree = (JSONArray) testSuiteObj.get("tree");

            for (final Object oEntry : tree) {
                final JSONObject entry = (JSONObject) oEntry;
                final String testName = (String) entry.get("path");
                if ( testName.equals(testcase) ) {
                    final String blobresult = getFromUrl((String) entry.get("url"));

                    testSuiteObj = (JSONObject) parser.parse(blobresult);
                    final String blob = (String) testSuiteObj.get("content");
                    final byte[] valueDecoded = Base64.decodeBase64(blob.getBytes());
                    //System.out.println("Decoded value is " + new String(valueDecoded));
                    return new String(valueDecoded);
                }
            }
        } catch (final ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static List<String> getFileNamesForTreeSha(final String sha) {

        final String result = getFromUrl("https://api.github.com/repos/ethereum/tests/git/trees/" + sha + "?recursive=1");

        final JSONParser parser = new JSONParser();
        JSONObject testSuiteObj = null;

        final List<String> fileNames = new ArrayList<>();
        try {
            testSuiteObj = (JSONObject) parser.parse(result);
            final JSONArray tree = (JSONArray) testSuiteObj.get("tree");

            for (final Object oEntry : tree) {
                final JSONObject entry = (JSONObject) oEntry;
                final String testName = (String) entry.get("path");
                fileNames.add(testName);
            }
        } catch (final ParseException e) {
            e.printStackTrace();
        }

        return fileNames;
    }
}
