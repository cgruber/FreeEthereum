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

import org.ethereum.config.BlockchainConfig;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.ContractDetails;
import org.ethereum.util.ByteArraySet;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.Utils;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.MessageCall;
import org.ethereum.vm.MessageCall.MsgType;
import org.ethereum.vm.OpCode;
import org.ethereum.vm.PrecompiledContracts.PrecompiledContract;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.ethereum.vm.program.listener.CompositeProgramListener;
import org.ethereum.vm.program.listener.ProgramListenerAware;
import org.ethereum.vm.program.listener.ProgramStorageChangeListener;
import org.ethereum.vm.trace.ProgramTrace;
import org.ethereum.vm.trace.ProgramTraceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;

import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang3.ArrayUtils.*;
import static org.ethereum.util.BIUtil.*;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
public class Program {

    private static final Logger logger = LoggerFactory.getLogger("VM");

    /**
     * This attribute defines the number of recursive calls allowed in the EVM
     * Note: For the JVM to reach this level without a StackOverflow exception,
     * ethereumj may need to be started with a JVM argument to increase
     * the stack size. For example: -Xss10m
     */
    private static final int MAX_DEPTH = 1024;

    //Max size for stack checks
    private static final int MAX_STACKSIZE = 1024;
    private final SystemProperties config;
    private final BlockchainConfig blockchainConfig;
    private final Transaction transaction;
    private final ProgramInvoke invoke;
    private final ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    private final ProgramTraceListener traceListener;
    private final ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();
    private final CompositeProgramListener programListener = new CompositeProgramListener();
    private final Stack stack;
    private final Memory memory;
    private final Storage storage;
    private final ProgramResult result = new ProgramResult();
    private final byte[] codeHash;
    private final byte[] ops;
    private CommonConfig commonConfig = CommonConfig.getDefault();
    private ProgramOutListener listener;
    private ProgramTrace trace = new ProgramTrace();
    private int pc;
    private byte lastOp;
    private byte previouslyExecutedOp;
    private boolean stopped;
    private ByteArraySet touchedAccounts = new ByteArraySet();
    private ProgramPrecompile programPrecompile;

    public Program(final byte[] ops, final ProgramInvoke programInvoke) {
        this(ops, programInvoke, null);
    }

    private Program(final byte[] ops, final ProgramInvoke programInvoke, final Transaction transaction) {
        this(ops, programInvoke, transaction, SystemProperties.getDefault());
    }

    public Program(final byte[] ops, final ProgramInvoke programInvoke, final Transaction transaction, final SystemProperties config) {
        this(null, ops, programInvoke, transaction, config);
    }

    public Program(final byte[] codeHash, final byte[] ops, final ProgramInvoke programInvoke, final Transaction transaction, final SystemProperties config) {
        this.config = config;
        this.invoke = programInvoke;
        this.transaction = transaction;

        this.codeHash = codeHash == null || FastByteComparisons.equal(HashUtil.EMPTY_DATA_HASH, codeHash) ? null : codeHash;
        this.ops = nullToEmpty(ops);

        traceListener = new ProgramTraceListener(config.vmTrace());
        this.memory = setupProgramListener(new Memory());
        this.stack = setupProgramListener(new Stack());
        this.storage = setupProgramListener(new Storage(programInvoke));
        this.trace = new ProgramTrace(config, programInvoke);
        this.blockchainConfig = config.getBlockchainConfig().getConfigForBlock(programInvoke.getNumber().longValue());
    }

    private static String formatBinData(final byte[] binData, final int startPC) {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < binData.length; i += 16) {
            ret.append(Utils.align("" + Integer.toHexString(startPC + (i)) + ":", ' ', 8, false));
            ret.append(Hex.toHexString(binData, i, min(16, binData.length - i))).append('\n');
        }
        return ret.toString();
    }

    public static String stringifyMultiline(final byte[] code) {
        int index = 0;
        final StringBuilder sb = new StringBuilder();
        final BitSet mask = buildReachableBytecodesMask(code);
        ByteArrayOutputStream binData = new ByteArrayOutputStream();
        int binDataStartPC = -1;

        while (index < code.length) {
            final byte opCode = code[index];
            final OpCode op = OpCode.code(opCode);

            if (!mask.get(index)) {
                if (binDataStartPC == -1) {
                    binDataStartPC = index;
                }
                binData.write(code[index]);
                index++;
                if (index < code.length) continue;
            }

            if (binDataStartPC != -1) {
                sb.append(formatBinData(binData.toByteArray(), binDataStartPC));
                binDataStartPC = -1;
                binData = new ByteArrayOutputStream();
                if (index == code.length) continue;
            }

            sb.append(Utils.align("" + Integer.toHexString(index) + ":", ' ', 8, false));

            if (op == null) {
                sb.append("<UNKNOWN>: ").append(0xFF & opCode).append("\n");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                final int nPush = op.val() - OpCode.PUSH1.val() + 1;
                final byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                final BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16));
                if (bi.bitLength() <= 32) {
                    sb.append(" (").append(new BigInteger(1, data).toString()).append(") ");
                }

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    private static BitSet buildReachableBytecodesMask(final byte[] code) {
        final NavigableSet<Integer> gotos = new TreeSet<>();
        final ByteCodeIterator it = new ByteCodeIterator(code);
        final BitSet ret = new BitSet(code.length);
        int lastPush = 0;
        int lastPushPC = 0;
        do {
            ret.set(it.getPC()); // reachable bytecode
            if (it.isPush()) {
                lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
                lastPushPC = it.getPC();
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
                if (it.getPC() != lastPushPC + 1) {
                    // some PC arithmetic we totally can't deal with
                    // assuming all bytecodes are reachable as a fallback
                    ret.set(0, code.length);
                    return ret;
                }
                final int jumpPC = lastPush;
                if (!ret.get(jumpPC)) {
                    // code was not explored yet
                    gotos.add(jumpPC);
                }
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN ||
                    it.getCurOpcode() == OpCode.STOP) {
                if (gotos.isEmpty()) break;
                it.setPC(gotos.pollFirst());
            }
        } while (it.next());
        return ret;
    }

    public static String stringify(final byte[] code) {
        int index = 0;
        final StringBuilder sb = new StringBuilder();
        final BitSet mask = buildReachableBytecodesMask(code);
        final String binData = "";

        while (index < code.length) {
            final byte opCode = code[index];
            final OpCode op = OpCode.code(opCode);

            if (op == null) {
                sb.append(" <UNKNOWN>: ").append(0xFF & opCode).append(" ");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                final int nPush = op.val() - OpCode.PUSH1.val() + 1;
                final byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                final BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16)).append(" ");

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
        }

        return sb.toString();
    }

    private ProgramPrecompile getProgramPrecompile() {
        if (programPrecompile == null) {
            if (codeHash != null && commonConfig.precompileSource() != null) {
                programPrecompile = commonConfig.precompileSource().get(codeHash);
            }
            if (programPrecompile == null) {
                programPrecompile = ProgramPrecompile.compile(ops);

                if (codeHash != null && commonConfig.precompileSource() != null) {
                    commonConfig.precompileSource().put(codeHash, programPrecompile);
                }
            }
        }
        return programPrecompile;
    }

    public Program withCommonConfig(final CommonConfig commonConfig) {
        this.commonConfig = commonConfig;
        return this;
    }

    public int getCallDeep() {
        return invoke.getCallDeep();
    }

    private InternalTransaction addInternalTx(final byte[] nonce, final DataWord gasLimit, final byte[] senderAddress, final byte[] receiveAddress,
                                              final BigInteger value, final byte[] data, final String note) {

        InternalTransaction result = null;
        if (transaction != null) {
            final byte[] senderNonce = isEmpty(nonce) ? getStorage().getNonce(senderAddress).toByteArray() : nonce;

            result = getResult().addInternalTransaction(transaction.getHash(), getCallDeep(), senderNonce,
                    getGasPrice(), gasLimit, senderAddress, receiveAddress, value.toByteArray(), data, note);
        }

        return result;
    }

    private <T extends ProgramListenerAware> T setupProgramListener(final T programListenerAware) {
        if (programListener.isEmpty()) {
            programListener.addListener(traceListener);
            programListener.addListener(storageDiffListener);
        }

        programListenerAware.setProgramListener(programListener);

        return programListenerAware;
    }

    private Map<DataWord, DataWord> getStorageDiff() {
        return storageDiffListener.getDiff();
    }

    public byte getOp(final int pc) {
        return (getLength(ops) <= pc) ? 0 : ops[pc];
    }

    public byte getCurrentOp() {
        return isEmpty(ops) ? 0 : ops[pc];
    }

    /**
     * Last Op can only be set publicly (no getLastOp method), is used for logging.
     */
    public void setLastOp(final byte op) {
        this.lastOp = op;
    }

    /**
     * Returns the last fully executed OP.
     */
    public byte getPreviouslyExecutedOp() {
        return this.previouslyExecutedOp;
    }

    /**
     * Should be set only after the OP is fully executed.
     */
    public void setPreviouslyExecutedOp(final byte op) {
        this.previouslyExecutedOp = op;
    }

    public void stackPush(final byte[] data) {
        stackPush(new DataWord(data));
    }

    private void stackPushZero() {
        stackPush(new DataWord(0));
    }

    private void stackPushOne() {
        final DataWord stackWord = new DataWord(1);
        stackPush(stackWord);
    }

    public void stackPush(final DataWord stackWord) {
        verifyStackOverflow(0, 1); //Sanity Check
        stack.push(stackWord);
    }

    public Stack getStack() {
        return this.stack;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(final DataWord pc) {
        this.setPC(pc.intValue());
    }

    public void setPC(final int pc) {
        this.pc = pc;

        if (this.pc >= ops.length) {
            stop();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }

    public void setHReturn(final byte[] buff) {
        getResult().setHReturn(buff);
    }

    public void step() {
        setPC(pc + 1);
    }

    public byte[] sweep(final int n) {

        if (pc + n > ops.length)
            stop();

        final byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
        pc += n;
        if (pc >= ops.length) stop();

        return data;
    }

    public DataWord stackPop() {
        return stack.pop();
    }

    /**
     * Verifies that the stack is at least <code>stackSize</code>
     *
     * @param stackSize int
     * @throws StackTooSmallException If the stack is
     *                                smaller than <code>stackSize</code>
     */
    public void verifyStackSize(final int stackSize) {
        if (stack.size() < stackSize) {
            throw Program.Exception.tooSmallStack(stackSize, stack.size());
        }
    }

    public void verifyStackOverflow(final int argsReqs, final int returnReqs) {
        if ((stack.size() - argsReqs + returnReqs) > MAX_STACKSIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
        }
    }

    public int getMemSize() {
        return memory.size();
    }

    public void memorySave(final DataWord addrB, final DataWord value) {
        memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
    }

    private void memorySaveLimited(final int addr, final byte[] data, final int dataSize) {
        memory.write(addr, data, dataSize, true);
    }

    public void memorySave(final int addr, final byte[] value) {
        memory.write(addr, value, value.length, false);
    }

    public void memoryExpand(final DataWord outDataOffs, final DataWord outDataSize) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue());
        }
    }

    /**
     * Allocates a piece of memory and stores value at given offset address
     *
     * @param addr      is the offset address
     * @param allocSize size of memory needed to write
     * @param value     the data to write to memory
     */
    public void memorySave(final int addr, final int allocSize, final byte[] value) {
        memory.extendAndWrite(addr, allocSize, value);
    }

    public DataWord memoryLoad(final DataWord addr) {
        return memory.readWord(addr.intValue());
    }

    public DataWord memoryLoad(final int address) {
        return memory.readWord(address);
    }

    public byte[] memoryChunk(final int offset, final int size) {
        return memory.read(offset, size);
    }

    /**
     * Allocates extra memory in the program for
     * a specified size, calculated from a given offset
     *
     * @param offset the memory address offset
     * @param size   the number of bytes to allocate
     */
    public void allocateMemory(final int offset, final int size) {
        memory.extend(offset, size);
    }

    public void suicide(final DataWord obtainerAddress) {

        final byte[] owner = getOwnerAddress().getLast20Bytes();
        final byte[] obtainer = obtainerAddress.getLast20Bytes();
        final BigInteger balance = getStorage().getBalance(owner);

        if (logger.isInfoEnabled())
            logger.info("Transfer to: [{}] heritage: [{}]",
                    Hex.toHexString(obtainer),
                    balance);

        addInternalTx(null, null, owner, obtainer, balance, null, "suicide");

        if (FastByteComparisons.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
            // if owner == obtainer just zeroing account according to Yellow Paper
            getStorage().addBalance(owner, balance.negate());
        } else {
            transfer(getStorage(), owner, obtainer, balance);
        }

        getResult().addDeleteAccount(this.getOwnerAddress());
    }

    public Repository getStorage() {
        return this.storage;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void createContract(final DataWord value, final DataWord memStart, final DataWord memSize) {

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }

        final byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        final BigInteger endowment = value.value();
        if (isNotCovers(getStorage().getBalance(senderAddress), endowment)) {
            stackPushZero();
            return;
        }

        // [1] FETCH THE CODE FROM THE MEMORY
        final byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

        if (logger.isInfoEnabled())
            logger.info("creating a new contract inside contract run: [{}]", Hex.toHexString(senderAddress));

        //  actual gas subtract
        final DataWord gasLimit = config.getBlockchainConfig().getConfigForBlock(getNumber().longValue()).
                getCreateGas(getGas());
        spendGas(gasLimit.longValue(), "internal call");

        // [2] CREATE THE CONTRACT ADDRESS
        final byte[] nonce = getStorage().getNonce(senderAddress).toByteArray();
        final byte[] newAddress = HashUtil.calcNewAddr(getOwnerAddress().getLast20Bytes(), nonce);

        if (byTestingSuite()) {
            // This keeps track of the contracts created for a test
            getResult().addCallCreate(programCode, EMPTY_BYTE_ARRAY,
                    gasLimit.getNoLeadZeroesData(),
                    value.getNoLeadZeroesData());
        }

        // [3] UPDATE THE NONCE
        // (THIS STAGE IS NOT REVERTED BY ANY EXCEPTION)
        if (!byTestingSuite()) {
            getStorage().increaseNonce(senderAddress);
        }

        final Repository track = getStorage().startTracking();

        //In case of hashing collisions, check for any balance before createAccount()
        final BigInteger oldBalance = track.getBalance(newAddress);
        track.createAccount(newAddress);
        if (blockchainConfig.eip161()) {
            track.increaseNonce(newAddress);
        }
        track.addBalance(newAddress, oldBalance);

        // [4] TRANSFER THE BALANCE
        BigInteger newBalance = ZERO;
        if (!byTestingSuite()) {
            track.addBalance(senderAddress, endowment.negate());
            newBalance = track.addBalance(newAddress, endowment);
        }


        // [5] COOK THE INVOKE AND EXECUTE
        final InternalTransaction internalTx = addInternalTx(nonce, getGasLimit(), senderAddress, null, endowment, programCode, "create");
        final ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                this, new DataWord(newAddress), getOwnerAddress(), value, gasLimit,
                newBalance, null, track, this.invoke.getBlockStore(), byTestingSuite());

        ProgramResult result = ProgramResult.empty();
        if (isNotEmpty(programCode)) {

            final VM vm = new VM(config);
            final Program program = new Program(programCode, programInvoke, internalTx, config).withCommonConfig(commonConfig);
            vm.play(program);
            result = program.getResult();

            getResult().merge(result);
        }

        // 4. CREATE THE CONTRACT OUT OF RETURN
        final byte[] code = result.getHReturn();

        final long storageCost = getLength(code) * getBlockchainConfig().getGasCost().getCREATE_DATA();
        final long afterSpend = programInvoke.getGas().longValue() - storageCost - result.getGasUsed();
        if (afterSpend < 0) {
            if (!config.getBlockchainConfig().getConfigForBlock(getNumber().longValue()).getConstants().createEmptyContractOnOOG()) {
                result.setException(Program.Exception.notEnoughSpendingGas("No gas to return just created contract",
                        storageCost, this));
            } else {
                track.saveCode(newAddress, EMPTY_BYTE_ARRAY);
            }
        } else if (getLength(code) > blockchainConfig.getConstants().getMaxContractSize()) {
            result.setException(Program.Exception.notEnoughSpendingGas("Contract size too large: " + getLength(result.getHReturn()),
                    storageCost, this));
        } else {
            result.spendGas(storageCost);
            track.saveCode(newAddress, code);
        }

        if (result.getException() != null) {
            logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                    Hex.toHexString(newAddress),
                    result.getException());

            internalTx.reject();
            result.rejectInternalTransactions();

            track.rollback();
            stackPushZero();
            return;
        }

        if (!byTestingSuite())
            track.commit();

        // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
        stackPush(new DataWord(newAddress));

        // 5. REFUND THE REMAIN GAS
        final long refundGas = gasLimit.longValue() - result.getGasUsed();
        if (refundGas > 0) {
            refundGas(refundGas, "remain gas from the internal call");
            if (logger.isInfoEnabled()) {
                logger.info("The remaining gas is refunded, account: [{}], gas: [{}] ",
                        Hex.toHexString(getOwnerAddress().getLast20Bytes()),
                        refundGas);
            }
        }
    }

    /**
     * That method is for internal code invocations
     * <p/>
     * - Normal calls invoke a specified contract which updates itself
     * - Stateless calls invoke code from another contract, within the context of the caller
     *
     * @param msg is the message call object
     */
    public void callToAddress(final MessageCall msg) {

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        final byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

        // FETCH THE SAVED STORAGE
        final byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        final byte[] senderAddress = getOwnerAddress().getLast20Bytes();
        final byte[] contextAddress = msg.getType().isStateless() ? senderAddress : codeAddress;

        if (logger.isInfoEnabled())
            logger.info(msg.getType().name() + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
                    Hex.toHexString(contextAddress), msg.getOutDataOffs().longValue(), msg.getOutDataSize().longValue());

        final Repository track = getStorage().startTracking();

        // 2.1 PERFORM THE VALUE (endowment) PART
        final BigInteger endowment = msg.getEndowment().value();
        final BigInteger senderBalance = track.getBalance(senderAddress);
        if (isNotCovers(senderBalance, endowment)) {
            stackPushZero();
            refundGas(msg.getGas().longValue(), "refund gas from message call");
            return;
        }


        // FETCH THE CODE
        final byte[] programCode = getStorage().isExist(codeAddress) ? getStorage().getCode(codeAddress) : EMPTY_BYTE_ARRAY;


        BigInteger contextBalance = ZERO;
        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            getResult().addCallCreate(data, contextAddress,
                    msg.getGas().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());
        } else {
            track.addBalance(senderAddress, endowment.negate());
            contextBalance = track.addBalance(contextAddress, endowment);
        }

        // CREATE CALL INTERNAL TRANSACTION
        final InternalTransaction internalTx = addInternalTx(null, getGasLimit(), senderAddress, contextAddress, endowment, data, "call");

        ProgramResult result = null;
        if (isNotEmpty(programCode)) {
            final ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                    this, new DataWord(contextAddress),
                    msg.getType() == MsgType.DELEGATECALL ? getCallerAddress() : getOwnerAddress(),
                    msg.getType() == MsgType.DELEGATECALL ? getCallValue() : msg.getEndowment(),
                    msg.getGas(), contextBalance, data, track, this.invoke.getBlockStore(), byTestingSuite());

            final VM vm = new VM(config);
            final Program program = new Program(getStorage().getCodeHash(codeAddress), programCode, programInvoke, internalTx, config).withCommonConfig(commonConfig);
            vm.play(program);
            result = program.getResult();

            getTrace().merge(program.getTrace());
            getResult().merge(result);

            if (result.getException() != null) {
                logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                        Hex.toHexString(contextAddress),
                        result.getException());

                internalTx.reject();
                result.rejectInternalTransactions();

                track.rollback();
                stackPushZero();
                return;
            } else if (byTestingSuite()) {
                logger.info("Testing run, skipping storage diff listener");
            } else if (Arrays.equals(transaction.getReceiveAddress(), internalTx.getReceiveAddress())) {
                storageDiffListener.merge(program.getStorageDiff());
            }
        }

        // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
        if (result != null) {
            final byte[] buffer = result.getHReturn();
            final int offset = msg.getOutDataOffs().intValue();
            final int size = msg.getOutDataSize().intValue();

            memorySaveLimited(offset, buffer, size);
        }

        // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
        track.commit();
        stackPushOne();

        // 5. REFUND THE REMAIN GAS
        if (result != null) {
            final BigInteger refundGas = msg.getGas().value().subtract(toBI(result.getGasUsed()));
            if (isPositive(refundGas)) {
                refundGas(refundGas.longValue(), "remaining gas from the internal call");
                if (logger.isInfoEnabled())
                    logger.info("The remaining gas refunded, account: [{}], gas: [{}] ",
                            Hex.toHexString(senderAddress),
                            refundGas.toString());
            }
        } else {
            refundGas(msg.getGas().longValue(), "remaining gas from the internal call");
        }
    }

    public void spendGas(final long gasValue, final String cause) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Spent for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
        }

        if (getGasLong() < gasValue) {
            throw Program.Exception.notEnoughSpendingGas(cause, gasValue, this);
        }
        getResult().spendGas(gasValue);
    }

    public void spendAllGas() {
        spendGas(getGas().longValue(), "Spending all remaining");
    }

    private void refundGas(final long gasValue, final String cause) {
        logger.info("[{}] Refund for cause: [{}], gas: [{}]", invoke.hashCode(), cause, gasValue);
        getResult().refundGas(gasValue);
    }

    public void futureRefundGas(final long gasValue) {
        logger.info("Future refund added: [{}]", gasValue);
        getResult().addFutureRefund(gasValue);
    }

    public void resetFutureRefund() {
        getResult().resetFutureRefund();
    }

    public void storageSave(final DataWord word1, final DataWord word2) {
        storageSave(word1.getData(), word2.getData());
    }

    private void storageSave(final byte[] key, final byte[] val) {
        final DataWord keyWord = new DataWord(key);
        final DataWord valWord = new DataWord(val);
        getStorage().addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord);
    }

    public byte[] getCode() {
        return ops;
    }

    public byte[] getCodeAt(final DataWord address) {
        final byte[] code = invoke.getRepository().getCode(address.getLast20Bytes());
        return nullToEmpty(code);
    }

    public DataWord getOwnerAddress() {
        return invoke.getOwnerAddress().clone();
    }

    public DataWord getBlockHash(final int index) {
        return index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().intValue()) - 256 ?
                new DataWord(this.invoke.getBlockStore().getBlockHashByNumber(index, getPrevHash().getData())).clone() :
                DataWord.ZERO.clone();
    }

    public DataWord getBalance(final DataWord address) {
        final BigInteger balance = getStorage().getBalance(address.getLast20Bytes());
        return new DataWord(balance.toByteArray());
    }

    public DataWord getOriginAddress() {
        return invoke.getOriginAddress().clone();
    }

    public DataWord getCallerAddress() {
        return invoke.getCallerAddress().clone();
    }

    public DataWord getGasPrice() {
        return invoke.getMinGasPrice().clone();
    }

    public long getGasLong() {
        return invoke.getGasLong() - getResult().getGasUsed();
    }

    public DataWord getGas() {
        return new DataWord(invoke.getGasLong() - getResult().getGasUsed());
    }

    public DataWord getCallValue() {
        return invoke.getCallValue().clone();
    }

    public DataWord getDataSize() {
        return invoke.getDataSize().clone();
    }

    public DataWord getDataValue(final DataWord index) {
        return invoke.getDataValue(index);
    }

    public byte[] getDataCopy(final DataWord offset, final DataWord length) {
        return invoke.getDataCopy(offset, length);
    }

    public DataWord storageLoad(final DataWord key) {
        final DataWord ret = getStorage().getStorageValue(getOwnerAddress().getLast20Bytes(), key.clone());
        return ret == null ? null : ret.clone();
    }

    public DataWord getPrevHash() {
        return invoke.getPrevHash().clone();
    }

    public DataWord getCoinbase() {
        return invoke.getCoinbase().clone();
    }

    public DataWord getTimestamp() {
        return invoke.getTimestamp().clone();
    }

    public DataWord getNumber() {
        return invoke.getNumber().clone();
    }

    public BlockchainConfig getBlockchainConfig() {
        return blockchainConfig;
    }

    public DataWord getDifficulty() {
        return invoke.getDifficulty().clone();
    }

    public DataWord getGasLimit() {
        return invoke.getGaslimit().clone();
    }

    public ProgramResult getResult() {
        return result;
    }

    public void setRuntimeFailure(final RuntimeException e) {
        getResult().setException(e);
    }

    public String memoryToString() {
        return memory.toString();
    }

    public void fullTrace() {

        if (logger.isTraceEnabled() || listener != null) {

            final StringBuilder stackData = new StringBuilder();
            for (int i = 0; i < stack.size(); ++i) {
                stackData.append(" ").append(stack.get(i));
                if (i < stack.size() - 1) stackData.append("\n");
            }

            if (stackData.length() > 0) stackData.insert(0, "\n");

            final ContractDetails contractDetails = getStorage().
                    getContractDetails(getOwnerAddress().getLast20Bytes());
            final StringBuilder storageData = new StringBuilder();
            if (contractDetails != null) {
                final List<DataWord> storageKeys = new ArrayList<>(contractDetails.getStorage().keySet());
                Collections.sort(storageKeys);
                for (final DataWord key : storageKeys) {
                    storageData.append(" ").append(key).append(" -> ").
                            append(contractDetails.getStorage().get(key)).append("\n");
                }
                if (storageData.length() > 0) storageData.insert(0, "\n");
            }

            final StringBuilder memoryData = new StringBuilder();
            final StringBuilder oneLine = new StringBuilder();
            if (memory.size() > 320)
                memoryData.append("... Memory Folded.... ")
                        .append("(")
                        .append(memory.size())
                        .append(") bytes");
            else
                for (int i = 0; i < memory.size(); ++i) {

                    final byte value = memory.readByte(i);
                    oneLine.append(ByteUtil.oneByteToHexString(value)).append(" ");

                    if ((i + 1) % 16 == 0) {
                        final String tmp = format("[%4s]-[%4s]", Integer.toString(i - 15, 16),
                                Integer.toString(i, 16)).replace(" ", "0");
                        memoryData.append("").append(tmp).append(" ");
                        memoryData.append(oneLine);
                        if (i < memory.size()) memoryData.append("\n");
                        oneLine.setLength(0);
                    }
                }
            if (memoryData.length() > 0) memoryData.insert(0, "\n");

            final StringBuilder opsString = new StringBuilder();
            for (int i = 0; i < ops.length; ++i) {

                String tmpString = Integer.toString(ops[i] & 0xFF, 16);
                tmpString = tmpString.length() == 1 ? "0" + tmpString : tmpString;

                if (i != pc)
                    opsString.append(tmpString);
                else
                    opsString.append(" >>").append(tmpString).append("");

            }
            if (pc >= ops.length) opsString.append(" >>");
            if (opsString.length() > 0) opsString.insert(0, "\n ");

            logger.trace(" -- OPS --     {}", opsString);
            logger.trace(" -- STACK --   {}", stackData);
            logger.trace(" -- MEMORY --  {}", memoryData);
            logger.trace(" -- STORAGE -- {}\n", storageData);
            logger.trace("\n  Spent Gas: [{}]/[{}]\n  Left Gas:  [{}]\n",
                    getResult().getGasUsed(),
                    invoke.getGas().longValue(),
                    getGas().longValue());

            final StringBuilder globalOutput = new StringBuilder("\n");
            if (stackData.length() > 0) stackData.append("\n");

            if (pc != 0)
                globalOutput.append("[Op: ").append(OpCode.code(lastOp).name()).append("]\n");

            globalOutput.append(" -- OPS --     ").append(opsString).append("\n");
            globalOutput.append(" -- STACK --   ").append(stackData).append("\n");
            globalOutput.append(" -- MEMORY --  ").append(memoryData).append("\n");
            globalOutput.append(" -- STORAGE -- ").append(storageData).append("\n");

            if (getResult().getHReturn() != null)
                globalOutput.append("\n  HReturn: ").append(
                        Hex.toHexString(getResult().getHReturn()));

            // sophisticated assumption that msg.data != codedata
            // means we are calling the contract not creating it
            final byte[] txData = invoke.getDataCopy(DataWord.ZERO, getDataSize());
            if (!Arrays.equals(txData, ops))
                globalOutput.append("\n  msg.data: ").append(Hex.toHexString(txData));
            globalOutput.append("\n\n  Spent Gas: ").append(getResult().getGasUsed());

            if (listener != null)
                listener.output(globalOutput.toString());
        }
    }

    public void saveOpTrace() {
        if (this.pc < ops.length) {
            trace.addOp(ops[pc], pc, getCallDeep(), getGas(), traceListener.resetActions());
        }
    }

    public ProgramTrace getTrace() {
        return trace;
    }

    public void addListener(final ProgramOutListener listener) {
        this.listener = listener;
    }

    public int verifyJumpDest(final DataWord nextPC) {
        if (nextPC.bytesOccupied() > 4) {
            throw Program.Exception.badJumpDestination(-1);
        }
        final int ret = nextPC.intValue();
        if (!getProgramPrecompile().hasJumpDest(ret)) {
            throw Program.Exception.badJumpDestination(ret);
        }
        return ret;
    }

    public void callToPrecompiledAddress(final MessageCall msg, final PrecompiledContract contract) {

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            this.refundGas(msg.getGas().longValue(), " call deep limit reach");
            return;
        }

        final Repository track = getStorage().startTracking();

        final byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        final byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        final byte[] contextAddress = msg.getType().isStateless() ? senderAddress : codeAddress;


        final BigInteger endowment = msg.getEndowment().value();
        final BigInteger senderBalance = track.getBalance(senderAddress);
        if (senderBalance.compareTo(endowment) < 0) {
            stackPushZero();
            this.refundGas(msg.getGas().longValue(), "refund gas from message call");
            return;
        }

        final byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
                msg.getInDataSize().intValue());

        // Charge for endowment - is not reversible by rollback
        transfer(track, senderAddress, contextAddress, msg.getEndowment().value());

        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            this.getResult().addCallCreate(data,
                    msg.getCodeAddress().getLast20Bytes(),
                    msg.getGas().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());

            stackPushOne();
            return;
        }


        final long requiredGas = contract.getGasForData(data);
        if (requiredGas > msg.getGas().longValue()) {

            this.refundGas(0, "call pre-compiled"); //matches cpp logic
            this.stackPushZero();
            track.rollback();
        } else {

            this.refundGas(msg.getGas().longValue() - requiredGas, "call pre-compiled");
            final byte[] out = contract.execute(data);

            this.memorySave(msg.getOutDataOffs().intValue(), out);
            this.stackPushOne();
            track.commit();
        }
    }

    public boolean byTestingSuite() {
        return invoke.byTestingSuite();
    }

    /**
     * used mostly for testing reasons
     */
    public byte[] getMemory() {
        return memory.read(0, memory.size());
    }

    /**
     * used mostly for testing reasons
     */
    public void initMem(final byte[] data) {
        this.memory.write(0, data, data.length, false);
    }

    public interface ProgramOutListener {
        void output(String out);
    }

    static class ByteCodeIterator {
        final byte[] code;
        int pc;

        public ByteCodeIterator(final byte[] code) {
            this.code = code;
        }

        public int getPC() {
            return pc;
        }

        public void setPC(final int pc) {
            this.pc = pc;
        }

        public OpCode getCurOpcode() {
            return pc < code.length ? OpCode.code(code[pc]) : null;
        }

        public boolean isPush() {
            return getCurOpcode() != null && getCurOpcode().name().startsWith("PUSH");
        }

        public byte[] getCurOpcodeArg() {
            if (isPush()) {
                final int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
                final byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
                return data;
            } else {
                return new byte[0];
            }
        }

        public boolean next() {
            pc += 1 + getCurOpcodeArg().length;
            return pc < code.length;
        }
    }

    /**
     * Denotes problem when executing Ethereum bytecode.
     * From blockchain and peer perspective this is quite normal situation
     * and doesn't mean exceptional situation in terms of the program execution
     */
    @SuppressWarnings("serial")
    public static class BytecodeExecutionException extends RuntimeException {
        public BytecodeExecutionException(final String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    public static class OutOfGasException extends BytecodeExecutionException {

        public OutOfGasException(final String message, final Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class IllegalOperationException extends BytecodeExecutionException {

        public IllegalOperationException(final String message, final Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class BadJumpDestinationException extends BytecodeExecutionException {

        public BadJumpDestinationException(final String message, final Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class StackTooSmallException extends BytecodeExecutionException {

        public StackTooSmallException(final String message, final Object... args) {
            super(format(message, args));
        }
    }

    public static class Exception {

        public static OutOfGasException notEnoughOpGas(final OpCode op, final long opGas, final long programGas) {
            return new OutOfGasException("Not enough gas for '%s' operation executing: opGas[%d], programGas[%d];", op, opGas, programGas);
        }

        public static OutOfGasException notEnoughOpGas(final OpCode op, final DataWord opGas, final DataWord programGas) {
            return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
        }

        public static OutOfGasException notEnoughOpGas(final OpCode op, final BigInteger opGas, final BigInteger programGas) {
            return notEnoughOpGas(op, opGas.longValue(), programGas.longValue());
        }

        public static OutOfGasException notEnoughSpendingGas(final String cause, final long gasValue, final Program program) {
            return new OutOfGasException("Not enough gas for '%s' cause spending: invokeGas[%d], gas[%d], usedGas[%d];",
                    cause, program.invoke.getGas().longValue(), gasValue, program.getResult().getGasUsed());
        }

        public static OutOfGasException gasOverflow(final BigInteger actualGas, final BigInteger gasLimit) {
            return new OutOfGasException("Gas value overflow: actualGas[%d], gasLimit[%d];", actualGas.longValue(), gasLimit.longValue());
        }

        public static IllegalOperationException invalidOpCode(final byte... opCode) {
            return new IllegalOperationException("Invalid operation code: opCode[%s];", Hex.toHexString(opCode, 0, 1));
        }

        public static BadJumpDestinationException badJumpDestination(final int pc) {
            return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
        }

        public static StackTooSmallException tooSmallStack(final int expectedSize, final int actualSize) {
            return new StackTooSmallException("Expected stack size %d but actual %d;", expectedSize, actualSize);
        }
    }

    @SuppressWarnings("serial")
    public class StackTooLargeException extends BytecodeExecutionException {
        public StackTooLargeException(final String message) {
            super(message);
        }
    }


}
