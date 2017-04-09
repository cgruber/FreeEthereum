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

import org.ethereum.core.Block;

/**
 * Interface to Ethereum contract compiled with Solidity with
 * respect to language function signatures encoding and
 * storage layout
 *
 * Below is Java <=> Solidity types mapping:
 *
 *  Input arguments Java -> Solidity mapping is the following:
 *    Number, BigInteger, String (hex) -> any integer type
 *    byte[], String (hex) -> bytesN, byte[]
 *    String -> string
 *    Java array of the above types -> Solidity dynamic array of the corresponding type
 *
 *  Output arguments Solidity -> Java mapping:
 *    any integer type -> BigInteger
 *    string -> String
 *    bytesN, byte[] -> byte[]
 *    Solidity dynamic array -> Java array
 *
 * Created by Anton Nashatyrev on 23.03.2016.
 */
public interface SolidityContract extends Contract {

    /**
     * Submits the transaction which invokes the specified contract function
     * with corresponding arguments
     *
     * TODO: either return pending transaction execution result
     * or return Future which is available upon block including trnasaction
     * or combine both approaches
     */
    SolidityCallResult callFunction(String functionName, Object ... args);

    /**
     * Submits the transaction which invokes the specified contract function
     * with corresponding arguments and sends the specified value to the contract
     */
    SolidityCallResult callFunction(long value, String functionName, Object ... args);

    /**
     * Call the function without submitting a transaction and without
     * modifying the contract state.
     * Synchronously returns function execution result
     * (see output argument mapping in class doc)
     */
    Object[] callConstFunction(String functionName, Object ... args);

    /**
     * Call the function without submitting a transaction and without
     * modifying the contract state. The function is executed with the
     * contract state actual after including the specified block.
     *
     * Synchronously returns function execution result
     * (see output argument mapping in class doc)
     */
    Object[] callConstFunction(Block callBlock, String functionName, Object... args);

    /**
     * Gets the contract function. This object can be passed as a call argument for another
     * function with a function type parameter
     */
    SolidityFunction getFunction(String name);

    /**
     * Returns the Solidity JSON ABI (Application Binary Interface)
     */
    String getABI();
}
