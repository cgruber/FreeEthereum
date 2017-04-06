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

package org.ethereum.validator;

import org.ethereum.config.Constants;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.BlockHeader;

import java.math.BigInteger;

/**
 * Checks {@link BlockHeader#gasLimit} against {@link Constants#getMinGasLimit}. <br>
 *
 * This check is NOT run in Frontier
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class GasLimitRule extends BlockHeaderRule {

    private final int MIN_GAS_LIMIT;

    public GasLimitRule(final SystemProperties config) {
        MIN_GAS_LIMIT = config.getBlockchainConfig().
                getCommonConstants().getMinGasLimit();
    }

    @Override
    public ValidationResult validate(final BlockHeader header) {

        if (new BigInteger(1, header.getGasLimit()).compareTo(BigInteger.valueOf(MIN_GAS_LIMIT)) < 0) {
            return fault("header.getGasLimit() < MIN_GAS_LIMIT");
        }

        return Success;
    }
}
