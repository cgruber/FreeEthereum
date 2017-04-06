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

package org.ethereum.vm.program;

import org.ethereum.util.ByteArraySet;
import org.ethereum.vm.CallCreate;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * @author Roman Mandeleil
 * @since 07.06.2014
 */
public class ProgramResult {

    private final ByteArraySet touchedAccounts = new ByteArraySet();
    private long gasUsed;
    private byte[] hReturn = EMPTY_BYTE_ARRAY;
    private RuntimeException exception;
    private Set<DataWord> deleteAccounts;
    private List<InternalTransaction> internalTransactions;
    private List<LogInfo> logInfoList;
    private long futureRefund = 0;

    /*
     * for testing runs ,
     * call/create is not executed
     * but dummy recorded
     */
    private List<CallCreate> callCreateList;

    public static ProgramResult empty() {
        final ProgramResult result = new ProgramResult();
        result.setHReturn(EMPTY_BYTE_ARRAY);
        return result;
    }

    public void spendGas(final long gas) {
        gasUsed += gas;
    }

    public void refundGas(final long gas) {
        gasUsed -= gas;
    }

    public byte[] getHReturn() {
        return hReturn;
    }

    public void setHReturn(final byte[] hReturn) {
        this.hReturn = hReturn;

    }

    public RuntimeException getException() {
        return exception;
    }

    public void setException(final RuntimeException exception) {
        this.exception = exception;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public Set<DataWord> getDeleteAccounts() {
        if (deleteAccounts == null) {
            deleteAccounts = new HashSet<>();
        }
        return deleteAccounts;
    }

    public void addDeleteAccount(final DataWord address) {
        getDeleteAccounts().add(address);
    }

    private void addDeleteAccounts(final Set<DataWord> accounts) {
        if (!isEmpty(accounts)) {
            getDeleteAccounts().addAll(accounts);
        }
    }

    public void addTouchAccount(final byte[] addr) {
        touchedAccounts.add(addr);
    }

    public Set<byte[]> getTouchedAccounts() {
        return touchedAccounts;
    }

    private void addTouchAccounts(final Set<byte[]> accounts) {
        if (!isEmpty(accounts)) {
            getTouchedAccounts().addAll(accounts);
        }
    }

    public List<LogInfo> getLogInfoList() {
        if (logInfoList == null) {
            logInfoList = new ArrayList<>();
        }
        return logInfoList;
    }

    public void addLogInfo(final LogInfo logInfo) {
        getLogInfoList().add(logInfo);
    }

    private void addLogInfos(final List<LogInfo> logInfos) {
        if (!isEmpty(logInfos)) {
            getLogInfoList().addAll(logInfos);
        }
    }

    public List<CallCreate> getCallCreateList() {
        if (callCreateList == null) {
            callCreateList = new ArrayList<>();
        }
        return callCreateList;
    }

    public void addCallCreate(final byte[] data, final byte[] destination, final byte[] gasLimit, final byte[] value) {
        getCallCreateList().add(new CallCreate(data, destination, gasLimit, value));
    }

    public List<InternalTransaction> getInternalTransactions() {
        if (internalTransactions == null) {
            internalTransactions = new ArrayList<>();
        }
        return internalTransactions;
    }

    public InternalTransaction addInternalTransaction(final byte[] parentHash, final int deep, final byte[] nonce, final DataWord gasPrice, final DataWord gasLimit,
                                                      final byte[] senderAddress, final byte[] receiveAddress, final byte[] value, final byte[] data, final String note) {
        final InternalTransaction transaction = new InternalTransaction(parentHash, deep, size(internalTransactions), nonce, gasPrice, gasLimit, senderAddress, receiveAddress, value, data, note);
        getInternalTransactions().add(transaction);
        return transaction;
    }

    private void addInternalTransactions(final List<InternalTransaction> internalTransactions) {
        getInternalTransactions().addAll(internalTransactions);
    }

    public void rejectInternalTransactions() {
        for (final InternalTransaction internalTx : getInternalTransactions()) {
            internalTx.reject();
        }
    }

    public void addFutureRefund(final long gasValue) {
        futureRefund += gasValue;
    }

    public long getFutureRefund() {
        return futureRefund;
    }

    public void resetFutureRefund() {
        futureRefund = 0;
    }

    public void merge(final ProgramResult another) {
        addInternalTransactions(another.getInternalTransactions());
        if (another.getException() == null) {
            addDeleteAccounts(another.getDeleteAccounts());
            addLogInfos(another.getLogInfoList());
            addFutureRefund(another.getFutureRefund());
            addTouchAccounts(another.getTouchedAccounts());
        }
    }
}
