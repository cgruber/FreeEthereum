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

/**
 * Checks diff between number of some block and number of our best block. <br>
 * The diff must be more than -1 * {@link Constants#getBestNumberDiffLimit}
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class BestNumberRule extends DependentBlockHeaderRule {

    private final int BEST_NUMBER_DIFF_LIMIT;

    public BestNumberRule(final SystemProperties config) {
        BEST_NUMBER_DIFF_LIMIT = config.getBlockchainConfig().
                getCommonConstants().getBestNumberDiffLimit();
    }

    @Override
    public boolean validate(final BlockHeader header, final BlockHeader bestHeader) {

        errors.clear();

        final long diff = header.getNumber() - bestHeader.getNumber();

        if (diff > -1 * BEST_NUMBER_DIFF_LIMIT) {
            errors.add(String.format(
                    "#%d: (header.getNumber() - bestHeader.getNumber()) <= BEST_NUMBER_DIFF_LIMIT",
                    header.getNumber()
            ));
            return false;
        }

        return true;
    }
}
