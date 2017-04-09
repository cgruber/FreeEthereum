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

package org.ethereum.core.genesis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.AccountState;
import org.ethereum.core.Genesis;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.trie.SecureTrie;
import org.ethereum.trie.Trie;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.ethereum.core.BlockHeader.NONCE_LENGTH;
import static org.ethereum.core.Genesis.PremineAccount;
import static org.ethereum.core.Genesis.ZERO_HASH_2048;
import static org.ethereum.crypto.HashUtil.EMPTY_LIST_HASH;
import static org.ethereum.util.ByteUtil.*;

public class GenesisLoader {

    /**
     * Load genesis from passed location or from classpath `genesis` directory
     */
    public static GenesisJson loadGenesisJson(final SystemProperties config, final ClassLoader classLoader) throws RuntimeException {
        final String genesisFile = config.getProperty("genesisFile", null);
        final String genesisResource = config.genesisInfo();

        // #1 try to find genesis at passed location
        if (genesisFile != null) {
            try (InputStream is = new FileInputStream(new File(genesisFile))) {
                return loadGenesisJson(is);
            } catch (final Exception e) {
                showLoadError("Problem loading genesis file from " + genesisFile, genesisFile, genesisResource);
            }
        }

        // #2 fall back to old genesis location at `src/main/resources/genesis` directory
        final InputStream is = classLoader.getResourceAsStream("genesis/" + genesisResource);
        if (is != null) {
            try {
                return loadGenesisJson(is);
            } catch (final Exception e) {
                showLoadError("Problem loading genesis file from resource directory", genesisFile, genesisResource);
            }
        } else {
            showLoadError("Genesis file was not found in resource directory", genesisFile, genesisResource);
        }

        return null;
    }

    private static void showLoadError(final String message, final String genesisFile, final String genesisResource) {
        Utils.showErrorAndExit(
            message,
            "Config option 'genesisFile': " + genesisFile,
            "Config option 'genesis': " + genesisResource);
    }

    public static Genesis parseGenesis(final BlockchainNetConfig blockchainNetConfig, final GenesisJson genesisJson) throws RuntimeException {
        try {
            final Genesis genesis = createBlockForJson(genesisJson);

            genesis.setPremine(generatePreMine(blockchainNetConfig, genesisJson.getAlloc()));

            final byte[] rootHash = generateRootHash(genesis.getPremine());
            genesis.setStateRoot(rootHash);

            return genesis;
        } catch (final Exception e) {
            e.printStackTrace();
            Utils.showErrorAndExit("Problem parsing genesis", e.getMessage());
        }
        return null;
    }

    /**
     * Method used much in tests.
     */
    public static Genesis loadGenesis(final InputStream resourceAsStream) {
        final GenesisJson genesisJson = loadGenesisJson(resourceAsStream);
        return parseGenesis(SystemProperties.getDefault().getBlockchainConfig(), genesisJson);
    }

    public static GenesisJson loadGenesisJson(final InputStream genesisJsonIS) throws RuntimeException {
        String json = null;
        try {
            json = new String(ByteStreams.toByteArray(genesisJsonIS));

            final ObjectMapper mapper = new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

            final GenesisJson genesisJson = mapper.readValue(json, GenesisJson.class);
            return genesisJson;
        } catch (final Exception e) {

            Utils.showErrorAndExit("Problem parsing genesis: "+ e.getMessage(), json);

            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private static Genesis createBlockForJson(final GenesisJson genesisJson) {

        final byte[] nonce = prepareNonce(ByteUtil.hexStringToBytes(genesisJson.nonce));
        final byte[] difficulty = hexStringToBytesValidate(genesisJson.difficulty, 32, true);
        final byte[] mixHash = hexStringToBytesValidate(genesisJson.mixhash, 32, false);
        final byte[] coinbase = hexStringToBytesValidate(genesisJson.coinbase, 20, false);

        final byte[] timestampBytes = hexStringToBytesValidate(genesisJson.timestamp, 8, true);
        final long timestamp = ByteUtil.byteArrayToLong(timestampBytes);

        final byte[] parentHash = hexStringToBytesValidate(genesisJson.parentHash, 32, false);
        final byte[] extraData = hexStringToBytesValidate(genesisJson.extraData, 32, true);

        final byte[] gasLimitBytes = hexStringToBytesValidate(genesisJson.gasLimit, 8, true);
        final long gasLimit = ByteUtil.byteArrayToLong(gasLimitBytes);

        return new Genesis(parentHash, EMPTY_LIST_HASH, coinbase, ZERO_HASH_2048,
                            difficulty, 0, gasLimit, 0, timestamp, extraData,
                            mixHash, nonce);
    }

    private static byte[] hexStringToBytesValidate(final String hex, final int bytes, final boolean notGreater) {
        final byte[] ret = ByteUtil.hexStringToBytes(hex);
        if (notGreater) {
            if (ret.length > bytes) {
                throw new RuntimeException("Wrong value length: " + hex + ", expected length < " + bytes + " bytes");
            }
        } else {
            if (ret.length != bytes) {
                throw new RuntimeException("Wrong value length: " + hex + ", expected length " + bytes + " bytes");
            }
        }
        return ret;
    }

    /**
     * Prepares nonce to be correct length
     * @param nonceUnchecked    unchecked, user-provided nonce
     * @return  correct nonce
     * @throws RuntimeException when nonce is too long
     */
    private static byte[] prepareNonce(final byte[] nonceUnchecked) {
        if (nonceUnchecked.length > 8) {
            throw new RuntimeException(String.format("Invalid nonce, should be %s length", NONCE_LENGTH));
        } else if (nonceUnchecked.length == 8) {
            return nonceUnchecked;
        }
        final byte[] nonce = new byte[NONCE_LENGTH];
        final int diff = NONCE_LENGTH - nonceUnchecked.length;
        System.arraycopy(nonceUnchecked, 0, nonce, diff, NONCE_LENGTH - diff);
        return nonce;
    }


    private static Map<ByteArrayWrapper, PremineAccount> generatePreMine(final BlockchainNetConfig blockchainNetConfig, final Map<String, GenesisJson.AllocatedAccount> allocs) {

        final Map<ByteArrayWrapper, PremineAccount> premine = new HashMap<>();

        for (final String key : allocs.keySet()) {

            final byte[] address = hexStringToBytes(key);
            final GenesisJson.AllocatedAccount alloc = allocs.get(key);
            final PremineAccount state = new PremineAccount();
            AccountState accountState = new AccountState(
                    blockchainNetConfig.getCommonConstants().getInitialNonce(), parseHexOrDec(alloc.balance));

            if (alloc.nonce != null) {
                accountState = accountState.withNonce(parseHexOrDec(alloc.nonce));
            }

            if (alloc.code != null) {
                final byte[] codeBytes = hexStringToBytes(alloc.code);
                accountState = accountState.withCodeHash(HashUtil.sha3(codeBytes));
                state.code = codeBytes;
            }

            state.accountState = accountState;
            premine.put(wrap(address), state);
        }

        return premine;
    }

    /**
     * @param rawValue either hex started with 0x or dec
     * return BigInteger
     */
    private static BigInteger parseHexOrDec(final String rawValue) {
        if (rawValue != null) {
            return rawValue.startsWith("0x") ? bytesToBigInteger(hexStringToBytes(rawValue)) : new BigInteger(rawValue);
        } else {
            return BigInteger.ZERO;
        }
    }

    public static byte[] generateRootHash(final Map<ByteArrayWrapper, PremineAccount> premine) {

        final Trie<byte[]> state = new SecureTrie((byte[]) null);

        for (final ByteArrayWrapper key : premine.keySet()) {
            state.put(key.getData(), premine.get(key).accountState.getEncoded());
        }

        return state.getRootHash();
    }
}
