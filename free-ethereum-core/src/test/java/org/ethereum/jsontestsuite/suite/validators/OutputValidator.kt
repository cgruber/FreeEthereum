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

package org.ethereum.jsontestsuite.suite.validators

import org.spongycastle.util.encoders.Hex

import java.util.ArrayList

import org.ethereum.jsontestsuite.suite.Utils.parseData

object OutputValidator {

    fun valid(origOutput: String, postOutput: String): List<String> {

        val results = ArrayList<String>()

        if (postOutput.startsWith("#")) {
            val postLen = Integer.parseInt(postOutput.substring(1))
            if (postLen != origOutput.length / 2) {
                results.add("Expected output length: " + postLen + ", actual: " + origOutput.length / 2)
            }
        } else {
            val postOutputFormated = Hex.toHexString(parseData(postOutput))

            if (origOutput != postOutputFormated) {
                val formattedString = String.format("HReturn: wrong expected: %s, current: %s",
                        postOutputFormated, origOutput)
                results.add(formattedString)
            }
        }

        return results
    }

}
