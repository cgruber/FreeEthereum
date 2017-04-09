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

package org.ethereum.solidity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.ethereum.util.ByteUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static java.lang.String.format;
import static org.apache.commons.collections4.ListUtils.select;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.ethereum.crypto.HashUtil.sha3;
import static org.ethereum.solidity.SolidityType.IntType.decodeInt;
import static org.ethereum.solidity.SolidityType.IntType.encodeInt;

public class Abi extends ArrayList<Abi.Entry> {
    private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    public static Abi fromJson(final String json) {
        try {
            return DEFAULT_MAPPER.readValue(json, Abi.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Abi.Entry> T find(final Class<T> resultClass, final Abi.Entry.Type type, final Predicate<T> searchPredicate) {
        return (T) CollectionUtils.find(this, entry -> entry.type == type && searchPredicate.evaluate((T) entry));
    }

    public Function findFunction(final Predicate<Function> searchPredicate) {
        return find(Function.class, Abi.Entry.Type.function, searchPredicate);
    }

    public Event findEvent(final Predicate<Event> searchPredicate) {
        return find(Event.class, Abi.Entry.Type.event, searchPredicate);
    }

    public Abi.Constructor findConstructor() {
        return find(Constructor.class, Entry.Type.constructor, object -> true);
    }

    @Override
    public String toString() {
        return toJson();
    }


    @JsonInclude(Include.NON_NULL)
    public static abstract class Entry {

        public final Boolean anonymous;
        public final Boolean constant;
        public final Boolean payable;
        public final String name;
        public final List<Param> inputs;
        public final List<Param> outputs;
        public final Type type;

        public Entry(final Boolean anonymous, final Boolean constant, final String name, final List<Param> inputs, final List<Param> outputs, final Type type, final Boolean payable) {
            this.anonymous = anonymous;
            this.constant = constant;
            this.payable = payable;
            this.name = name;
            this.inputs = inputs;
            this.outputs = outputs;
            this.type = type;
        }

        @JsonCreator
        public static Entry create(@JsonProperty("anonymous") final boolean anonymous,
                                   @JsonProperty("constant") final boolean constant,
                                   @JsonProperty("name") final String name,
                                   @JsonProperty("inputs") final List<Param> inputs,
                                   @JsonProperty("outputs") final List<Param> outputs,
                                   @JsonProperty("type") final Type type,
                                   @JsonProperty(value = "payable", required = false, defaultValue = "false") final Boolean payable) {
            Entry result = null;
            switch (type) {
                case constructor:
                    result = new Constructor(inputs, outputs);
                    break;
                case function:
                    result = new Function(constant, name, inputs, outputs, payable);
                    break;
                case event:
                    result = new Event(anonymous, name, inputs, outputs);
                    break;
            }

            return result;
        }

        public String formatSignature() {
            final StringBuilder paramsTypes = new StringBuilder();
            for (final Entry.Param param : inputs) {
                paramsTypes.append(param.type.getCanonicalName()).append(",");
            }

            return format("%s(%s)", name, stripEnd(paramsTypes.toString(), ","));
        }

        public byte[] fingerprintSignature() {
            return sha3(formatSignature().getBytes());
        }

        public byte[] encodeSignature() {
            return fingerprintSignature();
        }

        public enum Type {
            constructor,
            function,
            event,
            fallback
        }

        @JsonInclude(Include.NON_NULL)
        public static class Param {
            public Boolean indexed;
            public String name;
            public SolidityType type;

            public static List<?> decodeList(final List<Param> params, final byte[] encoded) {
                final List<Object> result = new ArrayList<>(params.size());

                int offset = 0;
                for (final Param param : params) {
                    final Object decoded = param.type.isDynamicType()
                            ? param.type.decode(encoded, decodeInt(encoded, offset).intValue())
                            : param.type.decode(encoded, offset);
                    result.add(decoded);

                    offset += param.type.getFixedSize();
                }

                return result;
            }

            @Override
            public String toString() {
                return format("%s%s%s", type.getCanonicalName(), (indexed != null && indexed) ? " indexed " : " ", name);
            }
        }
    }

    public static class Constructor extends Entry {

        public Constructor(final List<Param> inputs, final List<Param> outputs) {
            super(null, null, "", inputs, outputs, Type.constructor, false);
        }

        public List<?> decode(final byte[] encoded) {
            return Param.decodeList(inputs, encoded);
        }

        public String formatSignature(final String contractName) {
            return format("function %s(%s)", contractName, join(inputs, ", "));
        }
    }

    public static class Function extends Entry {

        private static final int ENCODED_SIGN_LENGTH = 4;

        public Function(final boolean constant, final String name, final List<Param> inputs, final List<Param> outputs, final Boolean payable) {
            super(null, constant, name, inputs, outputs, Type.function, payable);
        }

        public static byte[] extractSignature(final byte[] data) {
            return subarray(data, 0, ENCODED_SIGN_LENGTH);
        }

        public byte[] encode(final Object... args) {
            return ByteUtil.merge(encodeSignature(), encodeArguments(args));
        }

        private byte[] encodeArguments(final Object... args) {
            if (args.length > inputs.size())
                throw new RuntimeException("Too many arguments: " + args.length + " > " + inputs.size());

            int staticSize = 0;
            int dynamicCnt = 0;
            // calculating static size and number of dynamic params
            for (int i = 0; i < args.length; i++) {
                final SolidityType type = inputs.get(i).type;
                if (type.isDynamicType()) {
                    dynamicCnt++;
                }
                staticSize += type.getFixedSize();
            }

            final byte[][] bb = new byte[args.length + dynamicCnt][];
            for (int curDynamicPtr = staticSize, curDynamicCnt = 0, i = 0; i < args.length; i++) {
                final SolidityType type = inputs.get(i).type;
                if (type.isDynamicType()) {
                    final byte[] dynBB = type.encode(args[i]);
                    bb[i] = encodeInt(curDynamicPtr);
                    bb[args.length + curDynamicCnt] = dynBB;
                    curDynamicCnt++;
                    curDynamicPtr += dynBB.length;
                } else {
                    bb[i] = type.encode(args[i]);
                }
            }

            return ByteUtil.merge(bb);
        }

        public List<?> decode(final byte[] encoded) {
            return Param.decodeList(inputs, subarray(encoded, ENCODED_SIGN_LENGTH, encoded.length));
        }

        public List<?> decodeResult(final byte[] encoded) {
            return Param.decodeList(outputs, encoded);
        }

        @Override
        public byte[] encodeSignature() {
            return extractSignature(super.encodeSignature());
        }

        @Override
        public String toString() {
            String returnTail = "";
            if (constant) {
                returnTail += " constant";
            }
            if (!outputs.isEmpty()) {
                final List<String> types = new ArrayList<>();
                for (final Param output : outputs) {
                    types.add(output.type.getCanonicalName());
                }
                returnTail += format(" returns(%s)", join(types, ", "));
            }

            return format("function %s(%s)%s;", name, join(inputs, ", "), returnTail);
        }
    }

    public static class Event extends Entry {

        public Event(final boolean anonymous, final String name, final List<Param> inputs, final List<Param> outputs) {
            super(anonymous, null, name, inputs, outputs, Type.event, false);
        }

        public List<?> decode(final byte[] data, final byte[][] topics) {
            final List<Object> result = new ArrayList<>(inputs.size());

            final byte[][] argTopics = anonymous ? topics : subarray(topics, 1, topics.length);
            final List<?> indexed = Param.decodeList(filteredInputs(true), ByteUtil.merge(argTopics));
            final List<?> notIndexed = Param.decodeList(filteredInputs(false), data);

            for (final Param input : inputs) {
                result.add(input.indexed ? indexed.remove(0) : notIndexed.remove(0));
            }

            return result;
        }

        private List<Param> filteredInputs(final boolean indexed) {
            return select(inputs, param -> param.indexed == indexed);
        }

        @Override
        public String toString() {
            return format("event %s(%s);", name, join(inputs, ", "));
        }
    }
}
