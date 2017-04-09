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

package org.ethereum.util

import com.cedarsoftware.util.DeepEquals
import org.ethereum.crypto.HashUtil
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

/**
 * Class to encapsulate an object and provide utilities for conversion
 */
class Value {

    private var value: Any? = null
    private var rlp: ByteArray? = null
    private var sha3: ByteArray? = null

    private var decoded = false

    private constructor()

    constructor(obj: Any?) {

        this.decoded = true
        if (obj == null) return

        if (obj is Value) {
            this.value = obj.asObj()
        } else {
            this.value = obj
        }
    }

    private fun init(rlp: ByteArray) {
        this.rlp = rlp
    }

    fun withHash(hash: ByteArray): Value {
        sha3 = hash
        return this
    }

    /* *****************
     *      Convert
     * *****************/

    fun asObj(): Any {
        decode()
        return value!!
    }

    fun asList(): List<Any> {
        decode()
        val valueArray = value as Array<Any>?
        return Arrays.asList(*valueArray!!)
    }

    fun asInt(): Int {
        decode()
        if (isInt) {
            return (value as Int?)!!
        } else if (isBytes) {
            return BigInteger(1, asBytes()).toInt()
        }
        return 0
    }

    fun asLong(): Long {
        decode()
        if (isLong) {
            return (value as Long?)!!
        } else if (isBytes) {
            return BigInteger(1, asBytes()).toLong()
        }
        return 0
    }

    fun asBigInt(): BigInteger {
        decode()
        return value as BigInteger
    }

    fun asString(): String {
        decode()
        if (isBytes) {
            return String((value as ByteArray?)!!)
        } else if (isString) {
            return value as String
        }
        return ""
    }

    fun asBytes(): ByteArray {
        decode()
        if (isBytes) {
            return value as ByteArray
        } else if (isString) {
            return asString().toByteArray()
        }
        return ByteUtil.EMPTY_BYTE_ARRAY
    }

    val hex: String
        get() = Hex.toHexString(this.encode())

    val data: ByteArray
        get() = this.encode()


    fun asSlice(): IntArray {
        return value as IntArray
    }

    operator fun get(index: Int): Value {
        if (isList) {
            // Guard for OutOfBounds
            if (asList().size <= index) {
                return Value(null)
            }
            if (index < 0) {
                throw RuntimeException("Negative index not allowed")
            }
            return Value(asList()[index])
        }
        // If this wasn't a slice you probably shouldn't be using this function
        return Value(null)
    }

    /* *****************
     *      Utility
     * *****************/

    private fun decode() {
        if (!this.decoded) {
            this.value = RLP.decode(rlp, 0).decoded
            this.decoded = true
        }
    }

    fun encode(): ByteArray {
        if (rlp == null)
            rlp = RLP.encode(value)
        return rlp!!
    }

    fun hash(): ByteArray {
        if (sha3 == null)
            sha3 = HashUtil.sha3(encode())
        return sha3!!
    }

    fun cmp(o: Value): Boolean {
        return DeepEquals.deepEquals(this, o)
    }

    /* *****************
     *      Checks
     * *****************/

    val isList: Boolean
        get() {
            decode()
            return value != null && value!!.javaClass.isArray && !value!!.javaClass.componentType.isPrimitive
        }

    private val isString: Boolean
        get() {
            decode()
            return value is String
        }

    private val isInt: Boolean
        get() {
            decode()
            return value is Int
        }

    private val isLong: Boolean
        get() {
            decode()
            return value is Long
        }

    val isBigInt: Boolean
        get() {
            decode()
            return value is BigInteger
        }

    private val isBytes: Boolean
        get() {
            decode()
            return value is ByteArray
        }

    // it's only if the isBytes() = true;
    private val isReadableString: Boolean
        get() {

            decode()
            val data = value as ByteArray?

            if (data!!.size == 1 && data[0] > 31 && data[0] < 126) {
                return true
            }

            val readableChars = data.count { it > 32 && it < 126 }

            return readableChars.toDouble() / data.size.toDouble() > 0.55
        }

    // it's only if the isBytes() = true;
    val isHexString: Boolean
        get() {

            decode()
            val data = value as ByteArray?

            val hexChars = data!!.count { it >= 48 && it <= 57 || it >= 97 && it <= 102 }

            return hexChars.toDouble() / data.size.toDouble() > 0.9
        }

    val isHashCode: Boolean
        get() {
            decode()
            return this.asBytes().size == 32
        }

    private val isNull: Boolean
        get() {
            decode()
            return value == null
        }

    private val isEmpty: Boolean
        get() {
            decode()
            if (isNull) return true
            if (isBytes && asBytes().isEmpty()) return true
            if (isList && asList().isEmpty()) return true
            return isString && asString() == ""

        }

    fun length(): Int {
        decode()
        if (isList) {
            return asList().size
        } else if (isBytes) {
            return asBytes().size
        } else if (isString) {
            return asString().length
        }
        return 0
    }

    override fun toString(): String {

        decode()
        val stringBuilder = StringBuilder()

        if (isList) {

            val list = value as Array<Any>?

            // special case - key/value node
            if (list!!.size == 2) {

                stringBuilder.append("[ ")

                val key = Value(list[0])

                val keyNibbles = CompactEncoder.binToNibblesNoTerminator(key.asBytes())
                val keyString = ByteUtil.nibblesToPrettyString(keyNibbles)
                stringBuilder.append(keyString)

                stringBuilder.append(",")

                val `val` = Value(list[1])
                stringBuilder.append(`val`.toString())

                stringBuilder.append(" ]")
                return stringBuilder.toString()
            }
            stringBuilder.append(" [")

            for (i in list.indices) {
                val `val` = Value(list[i])
                if (`val`.isString || `val`.isEmpty) {
                    stringBuilder.append("'").append(`val`.toString()).append("'")
                } else {
                    stringBuilder.append(`val`.toString())
                }
                if (i < list.size - 1)
                    stringBuilder.append(", ")
            }
            stringBuilder.append("] ")

            return stringBuilder.toString()
        } else if (isEmpty) {
            return ""
        } else if (isBytes) {

            val output = StringBuilder()
            if (isHashCode) {
                output.append(Hex.toHexString(asBytes()))
            } else if (isReadableString) {
                output.append("'")
                for (oneByte in asBytes()) {
                    if (oneByte < 16) {
                        output.append("\\x").append(ByteUtil.oneByteToHexString(oneByte))
                    } else {
                        output.append(Character.valueOf(oneByte.toChar()))
                    }
                }
                output.append("'")
                return output.toString()
            }
            return Hex.toHexString(this.asBytes())
        } else if (isString) {
            return asString()
        }
        return "Unexpected type"
    }

    private fun countBranchNodes(): Int {
        decode()
        if (this.isList) {
            val objList = this.asList()
            val i = objList.sumBy { Value(it).countBranchNodes() }
            return i
        } else if (this.isBytes) {
            this.asBytes()
        }
        return 0
    }

    companion object {

        fun fromRlpEncoded(data: ByteArray?): Value? {

            if (data != null && data.isNotEmpty()) {
                val v = Value()
                v.init(data)
                return v
            }
            return null
        }
    }
}
