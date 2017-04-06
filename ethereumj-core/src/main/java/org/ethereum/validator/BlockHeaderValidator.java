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

import org.ethereum.core.BlockHeader;

import java.util.Arrays;
import java.util.List;

/**
 * Composite {@link BlockHeader} validator
 * aggregating list of simple validation rules
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class BlockHeaderValidator extends BlockHeaderRule {

    private final List<BlockHeaderRule> rules;

    public BlockHeaderValidator(final List<BlockHeaderRule> rules) {
        this.rules = rules;
    }

    public BlockHeaderValidator(final BlockHeaderRule... rules) {
        this.rules = Arrays.asList(rules);
    }

    @Override
    public ValidationResult validate(final BlockHeader header) {
        for (final BlockHeaderRule rule : rules) {
            final ValidationResult result = rule.validate(header);
            if (!result.success) {
                return result;
            }
        }
        return Success;
    }
}
