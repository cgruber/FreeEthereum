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

package org.ethereum.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.ethereum.vm.DataWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

final class Serializers {

    private static final Logger LOGGER = LoggerFactory.getLogger("vmtrace");

    public static String serializeFieldsOnly(final Object value, final boolean pretty) {
        try {
            final ObjectMapper mapper = createMapper(pretty);
            mapper.setVisibilityChecker(fieldsOnlyVisibilityChecker(mapper));

            return mapper.writeValueAsString(value);
        } catch (final Exception e) {
            LOGGER.error("JSON serialization error: ", e);
            return "{}";
        }
    }

    private static VisibilityChecker<?> fieldsOnlyVisibilityChecker(final ObjectMapper mapper) {
        return mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
    }

    private static ObjectMapper createMapper(final boolean pretty) {
        final ObjectMapper mapper = new ObjectMapper();
        if (pretty) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }

    private static class DataWordSerializer extends JsonSerializer<DataWord> {

        @Override
        public void serialize(final DataWord gas, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
            jgen.writeString(gas.value().toString());
        }
    }

    private static class ByteArraySerializer extends JsonSerializer<byte[]> {

        @Override
        public void serialize(final byte[] memory, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
            jgen.writeString(Hex.toHexString(memory));
        }
    }

    private static class OpCodeSerializer extends JsonSerializer<Byte> {

        @Override
        public void serialize(final Byte op, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
            jgen.writeString(org.ethereum.vm.OpCode.code(op).name());
        }
    }
}
