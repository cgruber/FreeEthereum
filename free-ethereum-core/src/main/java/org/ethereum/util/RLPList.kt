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

package org.ethereum.util

import java.util.*

/**
 * @author Roman Mandeleil
 * *
 * @since 21.04.14
 */
class RLPList : ArrayList<RLPElement>(), RLPElement {

    private var rlpData: ByteArray? = null

    override fun getRLPData(): ByteArray {
        return rlpData!!
    }

    fun setRLPData(rlpData: ByteArray) {
        this.rlpData = rlpData
    }

    companion object {

        fun recursivePrint(element: RLPElement?) {

            if (element == null)
                throw RuntimeException("RLPElement object can't be null")
            if (element is RLPList) {

                print("[")
                for (singleElement in element)
                    recursivePrint(singleElement)
                print("]")
            } else {
                val hex = ByteUtil.toHexString(element.rlpData)
                print(hex + ", ")
            }
        }
    }
}
