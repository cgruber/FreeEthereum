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

import org.codehaus.jackson.map.ObjectMapper
import org.ethereum.jsontestsuite.suite.CryptoTestCase
import org.ethereum.jsontestsuite.suite.JSONReader
import org.json.simple.parser.ParseException
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.*


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GitHubCryptoTest {


    @Test
    @Throws(ParseException::class, IOException::class)
    fun testAllInCryptoSute() {

        val json = JSONReader.loadJSON("BasicTests/crypto.json")

        val mapper = ObjectMapper()
        val type = mapper.typeFactory.constructMapType(HashMap::class.java, String::class.java, CryptoTestCase::class.java)


        val testSuite = mapper.readValue<HashMap<String, CryptoTestCase>>(json, type)

        for (key in testSuite.keys) {

            println("executing: " + key)
            testSuite[key]?.execute()

        }
    }


}
