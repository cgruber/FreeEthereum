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

package org.ethereum.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.io.SegmentedStringWriter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper

import java.io.IOException

/**
 * An extended [ObjectMapper][com.fasterxml.jackson.databind.ObjectMapper] class to
 * customize ethereum state dumps.

 * @author Alon Muroch
 */
internal class EtherObjectMapper : ObjectMapper() {

    @Throws(JsonProcessingException::class)
    override fun writeValueAsString(value: Any): String {
        // alas, we have to pull the recycler directly here...
        val sw = SegmentedStringWriter(_jsonFactory._getBufferRecycler())
        try {
            val ge = _jsonFactory.createGenerator(sw)
            // set ethereum custom pretty printer
            val pp = EtherPrettyPrinter()
            ge.prettyPrinter = pp

            _configAndWriteValue(ge, value)
        } catch (e: JsonProcessingException) { // to support [JACKSON-758]
            throw e
        } catch (e: IOException) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e)
        }

        return sw.andClear
    }

    /**
     * An extended [com.fasterxml.jackson.core.util.DefaultPrettyPrinter] class to customize
     * an ethereum [Pretty Printer][com.fasterxml.jackson.core.PrettyPrinter] Generator

     * @author Alon Muroch
     */
    inner class EtherPrettyPrinter : DefaultPrettyPrinter() {

        @Throws(IOException::class)
        override fun writeObjectFieldValueSeparator(jg: JsonGenerator) {
            /**
             * Custom object separator (Default is " : ") to make it easier to compare state dumps with other
             * ethereum client implementations
             */
            jg.writeRaw(": ")
        }
    }
}
