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

package org.ethereum.crypto

import org.spongycastle.crypto.*
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator
import org.spongycastle.crypto.params.*
import org.spongycastle.util.Arrays
import org.spongycastle.util.BigIntegers
import org.spongycastle.util.Pack
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.experimental.xor

/**
 * Support class for constructing integrated encryption cipher
 * for doing basic message exchanges on top of key agreement ciphers.
 * Follows the description given in IEEE Std 1363a with a couple of changes
 * specific to Ethereum:
 * - Hash the MAC key before use
 * - Include the encryption IV in the MAC computation
 */
internal class EthereumIESEngine
/**
 * set up for use with stream mode, where the key derivation function
 * is used to provide a stream of bytes to xor with the message.
 * @param agree the key agreement used as the basis for the encryption
 * *
 * @param kdf    the key derivation function used for byte generation
 * *
 * @param mac    the message authentication code generator for the message
 * *
 * @param hash   hash ing function
 * *
 * @param cipher the actual cipher
 */
(
        private val agree: BasicAgreement,
        private val kdf: DerivationFunction,
        val mac: Mac, private val hash: Digest, val cipher: BufferedBlockCipher?) {

    private var forEncryption: Boolean = false
    private var privParam: CipherParameters? = null
    private var pubParam: CipherParameters? = null
    private var param: IESParameters? = null

    private var V: ByteArray? = null
    private var keyPairGenerator: EphemeralKeyPairGenerator? = null
    private var keyParser: KeyParser? = null
    private var IV: ByteArray? = null
    private var hashK2 = true

    init {
        val macBuf = ByteArray(mac.macSize)
    }


    fun setHashMacKey(hashK2: Boolean) {
        this.hashK2 = hashK2
    }

    /**
     * Initialise the encryptor.

     * @param forEncryption whether or not this is encryption/decryption.
     * *
     * @param privParam     our private key parameters
     * *
     * @param pubParam      the recipient's/sender's public key parameters
     * *
     * @param params        encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     */
    fun init(
            forEncryption: Boolean,
            privParam: CipherParameters,
            pubParam: CipherParameters,
            params: CipherParameters) {
        this.forEncryption = forEncryption
        this.privParam = privParam
        this.pubParam = pubParam
        this.V = ByteArray(0)

        extractParams(params)
    }


    /**
     * Initialise the encryptor.

     * @param publicKey      the recipient's/sender's public key parameters
     * *
     * @param params         encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     * *
     * @param ephemeralKeyPairGenerator             the ephemeral key pair generator to use.
     */
    fun init(publicKey: AsymmetricKeyParameter, params: CipherParameters, ephemeralKeyPairGenerator: EphemeralKeyPairGenerator) {
        this.forEncryption = true
        this.pubParam = publicKey
        this.keyPairGenerator = ephemeralKeyPairGenerator

        extractParams(params)
    }

    /**
     * Initialise the encryptor.

     * @param privateKey      the recipient's private key.
     * *
     * @param params          encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     * *
     * @param publicKeyParser the parser for reading the ephemeral public key.
     */
    fun init(privateKey: AsymmetricKeyParameter, params: CipherParameters, publicKeyParser: KeyParser) {
        this.forEncryption = false
        this.privParam = privateKey
        this.keyParser = publicKeyParser

        extractParams(params)
    }

    private fun extractParams(params: CipherParameters) {
        if (params is ParametersWithIV) {
            this.IV = params.iv
            this.param = params.parameters as IESParameters
        } else {
            this.IV = null
            this.param = params as IESParameters
        }
    }

    @Throws(InvalidCipherTextException::class)
    private fun encryptBlock(
            `in`: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray?): ByteArray {
        var C: ByteArray? = null
        var K: ByteArray? = null
        var K1: ByteArray? = null
        var K2: ByteArray? = null
        var len: Int

        if (cipher == null) {
            // Streaming mode.
            K1 = ByteArray(inLen)
            K2 = ByteArray(param!!.macKeySize / 8)
            K = ByteArray(K1.size + K2.size)

            kdf.generateBytes(K, 0, K.size)

            //            if (V.length != 0)
            //            {
            //                System.arraycopy(K, 0, K2, 0, K2.length);
            //                System.arraycopy(K, K2.length, K1, 0, K1.length);
            //            }
            //            else
            run {
                System.arraycopy(K!!, 0, K1!!, 0, K1!!.size)
                System.arraycopy(K!!, inLen, K2!!, 0, K2!!.size)
            }

            C = ByteArray(inLen)

            for (i in 0..inLen - 1) {
                C[i] = (`in`[inOff + i] xor K1[i])
            }
            len = inLen
        } else {
            // Block cipher mode.
            K1 = ByteArray((param as IESWithCipherParameters).cipherKeySize / 8)
            K2 = ByteArray(param!!.macKeySize / 8)
            K = ByteArray(K1.size + K2.size)

            kdf.generateBytes(K, 0, K.size)
            System.arraycopy(K, 0, K1, 0, K1.size)
            System.arraycopy(K, K1.size, K2, 0, K2.size)

            // If iv provided use it to initialise the cipher
            if (IV != null) {
                cipher.init(true, ParametersWithIV(KeyParameter(K1), IV!!))
            } else {
                cipher.init(true, KeyParameter(K1))
            }

            C = ByteArray(cipher.getOutputSize(inLen))
            len = cipher.processBytes(`in`, inOff, inLen, C, 0)
            len += cipher.doFinal(C, len)
        }


        // Convert the length of the encoding vector into a byte array.
        val P2 = param!!.encodingV

        // Apply the MAC.
        val T = ByteArray(mac.macSize)

        val K2a: ByteArray
        if (hashK2) {
            K2a = ByteArray(hash.digestSize)
            hash.reset()
            hash.update(K2, 0, K2.size)
            hash.doFinal(K2a, 0)
        } else {
            K2a = K2
        }
        mac.init(KeyParameter(K2a))
        mac.update(IV, 0, IV!!.size)
        mac.update(C, 0, C.size)
        if (P2 != null) {
            mac.update(P2, 0, P2.size)
        }
        if (V!!.isNotEmpty() && P2 != null) {
            val L2 = ByteArray(4)
            Pack.intToBigEndian(P2.size * 8, L2, 0)
            mac.update(L2, 0, L2.size)
        }

        if (macData != null) {
            mac.update(macData, 0, macData.size)
        }

        mac.doFinal(T, 0)

        // Output the triple (V,C,T).
        val Output = ByteArray(V!!.size + len + T.size)
        System.arraycopy(V!!, 0, Output, 0, V!!.size)
        System.arraycopy(C, 0, Output, V!!.size, len)
        System.arraycopy(T, 0, Output, V!!.size + len, T.size)
        return Output
    }

    @Throws(InvalidCipherTextException::class)
    private fun decryptBlock(
            in_enc: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray?): ByteArray {
        var M: ByteArray? = null
        var K: ByteArray? = null
        var K1: ByteArray? = null
        var K2: ByteArray? = null
        var len: Int

        // Ensure that the length of the input is greater than the MAC in bytes
        if (inLen <= param!!.macKeySize / 8) {
            throw InvalidCipherTextException("Length of input must be greater than the MAC")
        }

        if (cipher == null) {
            // Streaming mode.
            K1 = ByteArray(inLen - V!!.size - mac.macSize)
            K2 = ByteArray(param!!.macKeySize / 8)
            K = ByteArray(K1.size + K2.size)

            kdf.generateBytes(K, 0, K.size)

            //            if (V.length != 0)
            //            {
            //                System.arraycopy(K, 0, K2, 0, K2.length);
            //                System.arraycopy(K, K2.length, K1, 0, K1.length);
            //            }
            //            else
            run {
                System.arraycopy(K!!, 0, K1!!, 0, K1!!.size)
                System.arraycopy(K!!, K1!!.size, K2!!, 0, K2!!.size)
            }

            M = ByteArray(K1.size)

            for (i in K1.indices) {
                M[i] = (in_enc[inOff + V!!.size + i] xor K1[i])
            }

            len = K1.size
        } else {
            // Block cipher mode.
            K1 = ByteArray((param as IESWithCipherParameters).cipherKeySize / 8)
            K2 = ByteArray(param!!.macKeySize / 8)
            K = ByteArray(K1.size + K2.size)

            kdf.generateBytes(K, 0, K.size)
            System.arraycopy(K, 0, K1, 0, K1.size)
            System.arraycopy(K, K1.size, K2, 0, K2.size)

            // If IV provide use it to initialize the cipher
            if (IV != null) {
                cipher.init(false, ParametersWithIV(KeyParameter(K1), IV!!))
            } else {
                cipher.init(false, KeyParameter(K1))
            }

            M = ByteArray(cipher.getOutputSize(inLen - V!!.size - mac.macSize))
            len = cipher.processBytes(in_enc, inOff + V!!.size, inLen - V!!.size - mac.macSize, M, 0)
            len += cipher.doFinal(M, len)
        }


        // Convert the length of the encoding vector into a byte array.
        val P2 = param!!.encodingV

        // Verify the MAC.
        val end = inOff + inLen
        val T1 = Arrays.copyOfRange(in_enc, end - mac.macSize, end)

        val T2 = ByteArray(T1.size)
        val K2a: ByteArray
        if (hashK2) {
            K2a = ByteArray(hash.digestSize)
            hash.reset()
            hash.update(K2, 0, K2.size)
            hash.doFinal(K2a, 0)
        } else {
            K2a = K2
        }
        mac.init(KeyParameter(K2a))
        mac.update(IV, 0, IV!!.size)
        mac.update(in_enc, inOff + V!!.size, inLen - V!!.size - T2.size)

        if (P2 != null) {
            mac.update(P2, 0, P2.size)
        }

        if (V!!.isNotEmpty() && P2 != null) {
            val L2 = ByteArray(4)
            Pack.intToBigEndian(P2.size * 8, L2, 0)
            mac.update(L2, 0, L2.size)
        }

        if (macData != null) {
            mac.update(macData, 0, macData.size)
        }

        mac.doFinal(T2, 0)

        if (!Arrays.constantTimeAreEqual(T1, T2)) {
            throw InvalidCipherTextException("Invalid MAC.")
        }


        // Output the message.
        return Arrays.copyOfRange(M, 0, len)
    }

    @Throws(InvalidCipherTextException::class)
    @JvmOverloads fun processBlock(
            `in`: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray? = null): ByteArray {
        if (forEncryption) {
            if (keyPairGenerator != null) {
                val ephKeyPair = keyPairGenerator!!.generate()

                this.privParam = ephKeyPair.keyPair.private
                this.V = ephKeyPair.encodedPublicKey
            }
        } else {
            if (keyParser != null) {
                val bIn = ByteArrayInputStream(`in`, inOff, inLen)

                try {
                    this.pubParam = keyParser!!.readKey(bIn)
                } catch (e: IOException) {
                    throw InvalidCipherTextException("unable to recover ephemeral public key: " + e.message, e)
                }

                val encLength = inLen - bIn.available()
                this.V = Arrays.copyOfRange(`in`, inOff, inOff + encLength)
            }
        }

        // Compute the common value and convert to byte array.
        agree.init(privParam)
        val z = agree.calculateAgreement(pubParam)
        val Z = BigIntegers.asUnsignedByteArray(agree.fieldSize, z)

        // Create input to KDF.
        var VZ: ByteArray = byteArrayOf()
        //        if (V.length != 0)
        //        {
        //            VZ = new byte[V.length + Z.length];
        //            System.arraycopy(V, 0, VZ, 0, V.length);
        //            System.arraycopy(Z, 0, VZ, V.length, Z.length);
        //        }
        //        else
        run { VZ = Z }

        // Initialise the KDF.
        val kdfParam: DerivationParameters
        if (kdf is MGF1BytesGeneratorExt) {
            kdfParam = MGFParameters(VZ)
        } else {
            kdfParam = KDFParameters(VZ, param!!.derivationV)
        }
        kdf.init(kdfParam)

        return if (forEncryption)
            encryptBlock(`in`, inOff, inLen, macData)
        else
            decryptBlock(`in`, inOff, inLen, macData)
    }
}
