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

package org.ethereum.validator

import org.ethereum.core.BlockHeader
import org.ethereum.util.ByteUtil
import org.ethereum.util.FastByteComparisons
import org.spongycastle.util.encoders.Hex

/**
 * Created by Stan Reshetnyk on 26.12.16.
 */
class ExtraDataPresenceRule(private val data: ByteArray, private val required: Boolean) : BlockHeaderRule() {

    public override fun validate(header: BlockHeader): BlockHeaderRule.ValidationResult {
        val extraData = if (header.extraData != null) header.extraData else ByteUtil.EMPTY_BYTE_ARRAY
        val extraDataMatches = FastByteComparisons.equal(extraData, data)

        if (required && !extraDataMatches) {
            return fault("Block " + header.number + " is no-fork. Expected presence of: " +
                    Hex.toHexString(data) + ", in extra data: " + Hex.toHexString(extraData))
        } else if (!required && extraDataMatches) {
            return fault("Block " + header.number + " is pro-fork. Expected no: " +
                    Hex.toHexString(data) + ", in extra data: " + Hex.toHexString(extraData))
        }
        return BlockHeaderRule.Success
    }
}
