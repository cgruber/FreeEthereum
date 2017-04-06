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

import java.util.List;

/**
 * Composite {@link BlockHeader} validator
 * aggregating list of simple validation rules depending on parent's block header
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class ParentBlockHeaderValidator extends DependentBlockHeaderRule {

    private final List<DependentBlockHeaderRule> rules;

    public ParentBlockHeaderValidator(final List<DependentBlockHeaderRule> rules) {
        this.rules = rules;
    }

    @Override
    public boolean validate(final BlockHeader header, final BlockHeader parent) {
        errors.clear();

        for (final DependentBlockHeaderRule rule : rules) {
            if (!rule.validate(header, parent)) {
                errors.addAll(rule.getErrors());
                return false;
            }
        }

        return true;
    }
}
