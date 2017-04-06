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
import org.slf4j.Logger;

/**
 * Parent class for {@link BlockHeader} validators
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public abstract class BlockHeaderRule extends AbstractValidationRule {

    static final ValidationResult Success = new ValidationResult(true, null);

    @Override
    public Class getEntityClass() {
        return BlockHeader.class;
    }

    /**
     * Runs header validation and returns its result
     *
     * @param header block header
     */
    protected abstract ValidationResult validate(BlockHeader header);

    ValidationResult fault(final String error) {
        return new ValidationResult(false, error);
    }

    public boolean validateAndLog(final BlockHeader header, final Logger logger) {
        final ValidationResult result = validate(header);
        if (!result.success && logger.isErrorEnabled()) {
            logger.warn("{} invalid {}", getEntityClass(), result.error);
        }
        return result.success;
    }

    /**
     * Validation result is either success or fault
     */
    public static final class ValidationResult {

        public final boolean success;

        public final String error;

        public ValidationResult(final boolean success, final String error) {
            this.success = success;
            this.error = error;
        }
    }
}
