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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import java.io.IOException;
import java.util.*;

/**
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class DifficultyTestSuite {

    private final List<DifficultyTestCase> testCases = new ArrayList<>();

    public DifficultyTestSuite(final String json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JavaType type = mapper.getTypeFactory().
                constructMapType(HashMap.class, String.class, DifficultyTestCase.class);

        final Map<String, DifficultyTestCase> caseMap = new ObjectMapper().readValue(json, type);

        for (final Map.Entry<String, DifficultyTestCase> e : caseMap.entrySet()) {
            e.getValue().setName(e.getKey());
            testCases.add(e.getValue());
        }

        testCases.sort(Comparator.comparing(DifficultyTestCase::getName));
    }

    public List<DifficultyTestCase> getTestCases() {
        return testCases;
    }

    @Override
    public String toString() {
        return "DifficultyTestSuite{" +
                "testCases=" + testCases +
                '}';
    }
}
