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

package org.ethereum.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.crypto.HashUtil;
import org.ethereum.solidity.SolidityType;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.vm.LogInfo;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.ethereum.solidity.SolidityType.IntType;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * Creates a contract function call transaction.
 * Serializes arguments according to the function ABI .
 *
 * Created by Anton Nashatyrev on 25.08.2015.
 */
public class CallTransaction {

    private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    public static Transaction createRawTransaction(final long nonce, final long gasPrice, final long gasLimit, final String toAddress,
                                                   final long value, final byte[] data) {
        final Transaction tx = new Transaction(longToBytesNoLeadZeroes(nonce),
                longToBytesNoLeadZeroes(gasPrice),
                longToBytesNoLeadZeroes(gasLimit),
                toAddress == null ? null : Hex.decode(toAddress),
                longToBytesNoLeadZeroes(value),
                data,
                null);
        return tx;
    }


    public static Transaction createCallTransaction(final long nonce, final long gasPrice, final long gasLimit, final String toAddress,
                                                    final long value, final Function callFunc, final Object... funcArgs) {

        final byte[] callData = callFunc.encode(funcArgs);
        return createRawTransaction(nonce, gasPrice, gasLimit, toAddress, value, callData);
    }


    public enum FunctionType {
        constructor,
        function,
        event,
        fallback
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Param {
        public Boolean indexed;
        public String name;
        public SolidityType type;

        @JsonGetter("type")
        public String getType() {
            return type.getName();
        }
    }

    public static class Function {
        public boolean anonymous;
        public boolean constant;
        public boolean payable;
        public String name = "";
        public Param[] inputs = new Param[0];
        public Param[] outputs = new Param[0];
        public FunctionType type;

        private Function() {}

        public static Function fromJsonInterface(final String json) {
            try {
                return DEFAULT_MAPPER.readValue(json, Function.class);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static Function fromSignature(final String funcName, final String... paramTypes) {
            return fromSignature(funcName, paramTypes, new String[0]);
        }

        public static Function fromSignature(final String funcName, final String[] paramTypes, final String[] resultTypes) {
            final Function ret = new Function();
            ret.name = funcName;
            ret.constant = false;
            ret.type = FunctionType.function;
            ret.inputs = new Param[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                ret.inputs[i] = new Param();
                ret.inputs[i].name = "param" + i;
                ret.inputs[i].type = SolidityType.getType(paramTypes[i]);
            }
            ret.outputs = new Param[resultTypes.length];
            for (int i = 0; i < resultTypes.length; i++) {
                ret.outputs[i] = new Param();
                ret.outputs[i].name = "res" + i;
                ret.outputs[i].type = SolidityType.getType(resultTypes[i]);
            }
            return ret;
        }

        public byte[] encode(final Object... args) {
            return ByteUtil.merge(encodeSignature(), encodeArguments(args));
        }

        public byte[] encodeArguments(final Object... args) {
            if (args.length > inputs.length) throw new RuntimeException("Too many arguments: " + args.length + " > " + inputs.length);

            int staticSize = 0;
            int dynamicCnt = 0;
            // calculating static size and number of dynamic params
            for (int i = 0; i < args.length; i++) {
                final Param param = inputs[i];
                if (param.type.isDynamicType()) {
                    dynamicCnt++;
                }
                staticSize += param.type.getFixedSize();
            }

            final byte[][] bb = new byte[args.length + dynamicCnt][];

            int curDynamicPtr = staticSize;
            int curDynamicCnt = 0;
            for (int i = 0; i < args.length; i++) {
                if (inputs[i].type.isDynamicType()) {
                    final byte[] dynBB = inputs[i].type.encode(args[i]);
                    bb[i] = SolidityType.IntType.encodeInt(curDynamicPtr);
                    bb[args.length + curDynamicCnt] = dynBB;
                    curDynamicCnt++;
                    curDynamicPtr += dynBB.length;
                } else {
                    bb[i] = inputs[i].type.encode(args[i]);
                }
            }
            return ByteUtil.merge(bb);
        }

        private Object[] decode(final byte[] encoded, final Param[] params) {
            final Object[] ret = new Object[params.length];

            int off = 0;
            for (int i = 0; i < params.length; i++) {
                if (params[i].type.isDynamicType()) {
                    ret[i] = params[i].type.decode(encoded, IntType.decodeInt(encoded, off).intValue());
                } else {
                    ret[i] = params[i].type.decode(encoded, off);
                }
                off += params[i].type.getFixedSize();
            }
            return ret;
        }

        public Object[] decode(final byte[] encoded) {
            return decode(subarray(encoded, 4, encoded.length), inputs);
        }

        public Object[] decodeResult(final byte[] encodedRet) {
            return decode(encodedRet, outputs);
        }

        public String formatSignature() {
            final StringBuilder paramsTypes = new StringBuilder();
            for (final Param param : inputs) {
                paramsTypes.append(param.type.getCanonicalName()).append(",");
            }

            return format("%s(%s)", name, stripEnd(paramsTypes.toString(), ","));
        }

        public byte[] encodeSignatureLong() {
            final String signature = formatSignature();
            final byte[] sha3Fingerprint = HashUtil.INSTANCE.sha3(signature.getBytes());
            return sha3Fingerprint;
        }

        public byte[] encodeSignature() {
            return Arrays.copyOfRange(encodeSignatureLong(), 0, 4);
        }

        @Override
        public String toString() {
            return formatSignature();
        }


    }

    public static class Contract {
        public Function[] functions;

        public Contract(final String jsonInterface) {
            try {
                functions = new ObjectMapper().readValue(jsonInterface, Function[].class);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Function getByName(final String name) {
            for (final Function function : functions) {
                if (name.equals(function.name)) {
                    return function;
                }
            }
            return null;
        }

        public Function getConstructor() {
            for (final Function function : functions) {
                if (function.type == FunctionType.constructor) {
                    return function;
                }
            }
            return null;
        }

        private Function getBySignatureHash(final byte[] hash) {
            if (hash.length == 4 ) {
                for (final Function function : functions) {
                    if (FastByteComparisons.equal(function.encodeSignature(), hash)) {
                        return function;
                    }
                }
            } else if (hash.length == 32 ) {
                for (final Function function : functions) {
                    if (FastByteComparisons.equal(function.encodeSignatureLong(), hash)) {
                        return function;
                    }
                }
            } else {
                throw new RuntimeException("Function signature hash should be 4 or 32 bytes length");
            }
            return null;
        }

        /**
         * Parses function and its arguments from transaction invocation binary data
         */
        public Invocation parseInvocation(final byte[] data) {
            if (data.length < 4) throw new RuntimeException("Invalid data length: " + data.length);
            final Function function = getBySignatureHash(Arrays.copyOfRange(data, 0, 4));
            if (function == null) throw new RuntimeException("Can't find function/event by it signature");
            final Object[] args = function.decode(data);
            return new Invocation(this, function, args);
        }

        /**
         * Parses Solidity Event and its data members from transaction receipt LogInfo
         */
        public Invocation parseEvent(final LogInfo eventLog) {
            final CallTransaction.Function event = getBySignatureHash(eventLog.getTopics().get(0).getData());
            int indexedArg = 1;
            if (event == null) return null;
            final List<Object> indexedArgs = new ArrayList<>();
            final List<Param> unindexed = new ArrayList<>();
            for (final Param input : event.inputs) {
                if (input.indexed) {
                    indexedArgs.add(input.type.decode(eventLog.getTopics().get(indexedArg++).getData()));
                    continue;
                }
                unindexed.add(input);
            }

            final Object[] unindexedArgs = event.decode(eventLog.getData(), unindexed.toArray(new Param[unindexed.size()]));
            final Object[] args = new Object[event.inputs.length];
            int unindexedIndex = 0;
            int indexedIndex = 0;
            for (int i = 0; i < args.length; i++) {
                if (event.inputs[i].indexed) {
                    args[i] = indexedArgs.get(indexedIndex++);
                    continue;
                }
                args[i] = unindexedArgs[unindexedIndex++];
            }
            return new Invocation(this, event, args);
        }
    }

    /**
     * Represents either function invocation with its arguments
     * or Event instance with its data members
     */
    public static class Invocation {
        public final Contract contract;
        public final Function function;
        public final Object[] args;

        public Invocation(final Contract contract, final Function function, final Object[] args) {
            this.contract = contract;
            this.function = function;
            this.args = args;
        }

        @Override
        public String toString() {
            return "[" + "contract=" + contract +
                    (function.type == FunctionType.event ? ", event=" : ", function=")
                    + function + ", args=" + Arrays.toString(args) + ']';
        }
    }
}
