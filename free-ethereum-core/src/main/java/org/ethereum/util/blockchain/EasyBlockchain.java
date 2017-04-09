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

import org.ethereum.core.Blockchain;
import org.ethereum.crypto.ECKey;
import org.ethereum.solidity.compiler.CompilationResult.ContractMetadata;

import java.math.BigInteger;

/**
 * Interface for easy blockchain interaction
 *
 * Created by Anton Nashatyrev on 23.03.2016.
 */
interface EasyBlockchain {

    /**
     *  Set the current sender key which all transactions (value transfer or
     *  contract creation/invocation) will be signed with
     *  The sender should have enough balance value
     */
    void setSender(ECKey senderPrivateKey);

    /**
     * Sends the value from the current sender to the specified recipient address
     */
    void sendEther(byte[] toAddress, BigInteger weis);

    /**
     * Creates and sends the transaction with the Solidity contract creation code
     * If the soliditySrc has more than one contract the {@link #submitNewContract(String, String, Object[])}
     * method should be used. This method will generate exception in this case
     */
    SolidityContract submitNewContract(String soliditySrc, Object... constructorArgs);

    /**
     * Creates and sends the transaction with the Solidity contract creation code
     * The contract name is specified when the soliditySrc has more than one contract
     */
    SolidityContract submitNewContract(String soliditySrc, String contractName, Object... constructorArgs);

    /**
     * Creates and sends the transaction with the Solidity contract creation code from a compiled json.
     * If the soliditySrc has more than one contract the {@link #submitNewContract(String, String, Object[])}
     * method should be used. This method will generate exception in this case
     */
    SolidityContract submitNewContractFromJson(String json, Object... constructorArgs);

    /**
     * Creates and sends the transaction with the Solidity contract creation code from a compiled json.
     * The contract name is specified when the soliditySrc has more than one contract
     */
    SolidityContract submitNewContractFromJson(String json, String contractName, Object... constructorArgs);

    /**
     * Creates and sends the transaction with the Solidity contract creation code from the contractMetaData.
     */
	SolidityContract submitNewContract(ContractMetadata contractMetaData, Object... constructorArgs);

    /**
     * Creates an interface to the Solidity contract already existing on the blockchain.
     * The contract source in that case is required only as an interface
     * @param soliditySrc  Source which describes the existing contract interface
     *                     This could be an abstract contract without function implementations
     * @param contractAddress The address of the existing contract
     */
    SolidityContract createExistingContractFromSrc(String soliditySrc, byte[] contractAddress);

    /**
     * The same as the previous method with specification of the exact contract
     * in the Solidity source
     */
    SolidityContract createExistingContractFromSrc(String soliditySrc, String contractName, byte[] contractAddress);

    /**
     * Creates an interface to the Solidity contract already existing on the blockchain.
     * @param ABI  Contract JSON ABI string
     * @param contractAddress The address of the existing contract
     */
    SolidityContract createExistingContractFromABI(String ABI, byte[] contractAddress);

    /**
     * Returns underlying Blockchain instance
     */
    Blockchain getBlockchain();
}
