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

package org.ethereum.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {

    private static final Logger logger = LoggerFactory.getLogger("general");

    public static String buildHash;
    public static String buildBranch;
    private static String buildTime;

    static {
        try {
            final Properties props = new Properties();
            final InputStream is = BuildInfo.class.getResourceAsStream("/build-info.properties");

            if (is != null) {
                props.load(is);

                buildHash = props.getProperty("build.hash");
                buildTime = props.getProperty("build.time");
                buildBranch = props.getProperty("build.branch");
            } else {
                logger.warn("File not found `build-info.properties`. Run `gradle build` to generate it");
            }
        } catch (final IOException e) {
            logger.error("Error reading /build-info.properties", e);
        }
    }

    public static void printInfo(){
        logger.info("git.hash: [{}]", buildHash);
        logger.info("build.time: {}", buildTime);
    }
}
