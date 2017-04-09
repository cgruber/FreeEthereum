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

package org.ethereum.util.blockchain;

import org.ethereum.core.CallTransaction;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Anton Nashatyrev on 26.07.2016.
 */
public abstract class SolidityCallResult extends TransactionResult {
    public Object getReturnValue() {
        final Object[] returnValues = getReturnValues();
        return isIncluded() && returnValues.length > 0 ? returnValues[0] : null;
    }

    public Object[] getReturnValues() {
        if (!isIncluded()) return null;
        final byte[] executionResult = getReceipt().getExecutionResult();
        return getFunction().decodeResult(executionResult);
    }

    public abstract CallTransaction.Function getFunction();

    public boolean isSuccessful() {
        return isIncluded() && getReceipt().isSuccessful();
    }

    public abstract List<CallTransaction.Invocation> getEvents();

    @Override
    public String toString() {
        String ret = "SolidityCallResult{" +
                getFunction() + ": " +
                (isIncluded() ? "EXECUTED" : "PENDING") + ", ";
        if (isIncluded()) {
            ret += isSuccessful() ? "SUCCESS" : ("ERR (" + getReceipt().getError() + ")");
            ret += ", ";
            if (isSuccessful()) {
                ret += "Ret: " + Arrays.toString(getReturnValues()) + ", ";
                ret += "Events: " + getEvents() + ", ";
            }
        }
        return ret + "}";
    }
}
