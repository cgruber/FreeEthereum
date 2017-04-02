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

import com.fasterxml.jackson.databind.node.ObjectNode
import org.ethereum.config.SystemProperties
import org.ethereum.core.AccountState
import org.ethereum.core.Block
import org.ethereum.core.Repository
import org.ethereum.db.ByteArrayWrapper
import org.ethereum.db.ContractDetails
import org.ethereum.util.ByteUtil
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.util.*

/**
 * JSON Helper class to format data into ObjectNodes
 * to match PyEthereum blockstate output

 * Dump format:
 * {
 * "address":
 * {
 * "nonce": "n1",
 * "balance": "b1",
 * "stateRoot": "s1",
 * "codeHash": "c1",
 * "code": "c2",
 * "storage":
 * {
 * "key1": "value1",
 * "key2": "value2"
 * }
 * }
 * }

 * @author Roman Mandeleil
 * *
 * @since 26.06.2014
 */
internal object JSONHelper {

    private fun dumpState(statesNode: ObjectNode, address: String, state: AccountState?, details: ContractDetails) {
        var state = state

        val storageKeys = ArrayList(details.storage.keys)
        Collections.sort(storageKeys)

        val account = statesNode.objectNode()
        val storage = statesNode.objectNode()

        for (key in storageKeys) {
            storage.put("0x" + Hex.toHexString(key.data),
                    "0x" + Hex.toHexString(details.storage[key]?.noLeadZeroesData))
        }

        if (state == null)
            state = AccountState(SystemProperties.getDefault()!!.blockchainConfig.commonConstants.initialNonce,
                    BigInteger.ZERO)

        account.put("balance", if (state.balance == null) "0" else state.balance.toString())
        //        account.put("codeHash", details.getCodeHash() == null ? "0x" : "0x" + Hex.toHexString(details.getCodeHash()));
        account.put("code", if (details.code == null) "0x" else "0x" + Hex.toHexString(details.code))
        account.put("nonce", if (state.nonce == null) "0" else state.nonce.toString())
        account.set("storage", storage)
        account.put("storage_root", if (state.stateRoot == null) "" else Hex.toHexString(state.stateRoot))

        statesNode.set(address, account)
    }

    fun dumpBlock(blockNode: ObjectNode, block: Block,
                  gasUsed: Long, state: ByteArray, keys: List<ByteArrayWrapper>,
                  repository: Repository) {

        blockNode.put("coinbase", Hex.toHexString(block.coinbase))
        blockNode.put("difficulty", BigInteger(1, block.difficulty).toString())
        blockNode.put("extra_data", "0x")
        blockNode.put("gas_used", gasUsed.toString())
        blockNode.put("nonce", "0x" + Hex.toHexString(block.nonce))
        blockNode.put("number", block.number.toString())
        blockNode.put("prevhash", "0x" + Hex.toHexString(block.parentHash))

        val statesNode = blockNode.objectNode()
        for (key in keys) {
            val keyBytes = key.data
            val accountState = repository.getAccountState(keyBytes)
            val details = repository.getContractDetails(keyBytes)
            dumpState(statesNode, Hex.toHexString(keyBytes), accountState, details)
        }
        blockNode.set("state", statesNode)

        blockNode.put("state_root", Hex.toHexString(state))
        blockNode.put("timestamp", block.timestamp.toString())

        val transactionsNode = blockNode.arrayNode()
        blockNode.set("transactions", transactionsNode)

        blockNode.put("tx_list_root", ByteUtil.toHexString(block.txTrieRoot))
        blockNode.put("uncles_hash", "0x" + Hex.toHexString(block.unclesHash))

        //      JSONHelper.dumpTransactions(blockNode,
        //              stateRoot, codeHash, code, storage);
    }

}
