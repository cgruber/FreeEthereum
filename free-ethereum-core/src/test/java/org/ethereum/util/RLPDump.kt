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

import org.ethereum.crypto.ECKey
import org.junit.Test
import org.spongycastle.util.encoders.Hex

/**
 * Created by Anton Nashatyrev on 29.09.2015.
 */
class RLPDump {
    private fun dump(el: RLPElement, indent: Int): String {
        var ret = StringBuilder()
        if (el is RLPList) {
            ret = StringBuilder(Utils.repeat("  ", indent) + "[\n")
            for (element in el) {
                ret.append(dump(element, indent + 1))
            }
            ret.append(Utils.repeat("  ", indent)).append("]\n")
        } else {
            ret.append(Utils.repeat("  ", indent)).append(if (el.rlpData == null) "<null>" else Hex.toHexString(el.rlpData)).append("\n")
        }
        return ret.toString()
    }

    @Test
    fun dumpTest() {
        println(Hex.toHexString(ECKey().pubKey))
        var hexRlp = "f872f870845609a1ba64c0b8660480136e573eb81ac4a664f8f76e4887ba927f791a053ec5ff580b1037a8633320ca70f8ec0cdea59167acaa1debc07bc0a0b3a5b41bdf0cb4346c18ddbbd2cf222f54fed795dde94417d2e57f85a580d87238efc75394ca4a92cfe6eb9debcc3583c26fee8580"
        println(dump(RLP.decode2(Hex.decode(hexRlp)), 0))
        hexRlp = "f8d1f8cf845605846c3cc58479a94c49b8c0800b0b2d39d7c59778edb5166bfd0415c5e02417955ef4ef7f7d8c1dfc7f59a0141d97dd798bde6b972090390758b67457e93c2acb11ed4941d4443f87cedbc09c1b0476ca17f4f04da3d69cfb6470969f73d401ee7692293a00a2ff2d7f3fac87d43d85aed19c9e6ecbfe7e5f8268209477ffda58c7a481eec5c50abd313d10b6554e6e04a04fd93b9bf781d600f4ceb3060002ce1eddbbd51a9a902a970d9b41a9627141c0c52742b1179d83e17f1a273adf0a4a1d0346c68686a51428dd9a01"
        println(dump(RLP.decode2(Hex.decode(hexRlp)), 0))
        hexRlp = "dedd84560586f03cc58479a94c498e0c48656c6c6f205768697370657281bc"
        println(dump(RLP.decode2(Hex.decode(hexRlp)), 0))
        hexRlp = "dedd84560586f03cc58479a94c498e0c48656c6c6f205768697370657281bc"
        println(dump(RLP.decode2(Hex.decode(hexRlp)), 0))
    }
}
