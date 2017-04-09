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

package org.ethereum.crypto;

import org.spongycastle.crypto.*;
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.spongycastle.crypto.params.*;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.Pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Support class for constructing integrated encryption cipher
 * for doing basic message exchanges on top of key agreement ciphers.
 * Follows the description given in IEEE Std 1363a with a couple of changes
 * specific to Ethereum:
 * - Hash the MAC key before use
 * - Include the encryption IV in the MAC computation
 */
class EthereumIESEngine
{
    private final Digest hash;
    private final BasicAgreement agree;
    private final DerivationFunction kdf;
    private final Mac mac;
    private final BufferedBlockCipher cipher;

    private boolean forEncryption;
    private CipherParameters privParam;
    private CipherParameters pubParam;
    private IESParameters param;

    private byte[] V;
    private EphemeralKeyPairGenerator keyPairGenerator;
    private KeyParser keyParser;
    private byte[] IV;
    private boolean hashK2 = true;

    /**
     * set up for use with stream mode, where the key derivation function
     * is used to provide a stream of bytes to xor with the message.
     *  @param agree the key agreement used as the basis for the encryption
     * @param kdf    the key derivation function used for byte generation
     * @param mac    the message authentication code generator for the message
     * @param hash   hash ing function
     * @param cipher the actual cipher
     */
    public EthereumIESEngine(
            final BasicAgreement agree,
            final DerivationFunction kdf,
            final Mac mac, final Digest hash, final BufferedBlockCipher cipher)
    {
        this.agree = agree;
        this.kdf = kdf;
        this.mac = mac;
        this.hash = hash;
        byte[] macBuf = new byte[mac.getMacSize()];
        this.cipher = cipher;
    }


    public void setHashMacKey(final boolean hashK2) {
        this.hashK2 = hashK2;
    }

    /**
     * Initialise the encryptor.
     *
     * @param forEncryption whether or not this is encryption/decryption.
     * @param privParam     our private key parameters
     * @param pubParam      the recipient's/sender's public key parameters
     * @param params        encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     */
    public void init(
            final boolean forEncryption,
            final CipherParameters privParam,
            final CipherParameters pubParam,
            final CipherParameters params)
    {
        this.forEncryption = forEncryption;
        this.privParam = privParam;
        this.pubParam = pubParam;
        this.V = new byte[0];

        extractParams(params);
    }


    /**
     * Initialise the encryptor.
     *
     * @param publicKey      the recipient's/sender's public key parameters
     * @param params         encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     * @param ephemeralKeyPairGenerator             the ephemeral key pair generator to use.
     */
    public void init(final AsymmetricKeyParameter publicKey, final CipherParameters params, final EphemeralKeyPairGenerator ephemeralKeyPairGenerator)
    {
        this.forEncryption = true;
        this.pubParam = publicKey;
        this.keyPairGenerator = ephemeralKeyPairGenerator;

        extractParams(params);
    }

    /**
     * Initialise the encryptor.
     *
     * @param privateKey      the recipient's private key.
     * @param params          encoding and derivation parameters, may be wrapped to include an IV for an underlying block cipher.
     * @param publicKeyParser the parser for reading the ephemeral public key.
     */
    public void init(final AsymmetricKeyParameter privateKey, final CipherParameters params, final KeyParser publicKeyParser)
    {
        this.forEncryption = false;
        this.privParam = privateKey;
        this.keyParser = publicKeyParser;

        extractParams(params);
    }

    private void extractParams(final CipherParameters params)
    {
        if (params instanceof ParametersWithIV)
        {
            this.IV = ((ParametersWithIV)params).getIV();
            this.param = (IESParameters)((ParametersWithIV)params).getParameters();
        }
        else
        {
            this.IV = null;
            this.param = (IESParameters)params;
        }
    }

    public BufferedBlockCipher getCipher()
    {
        return cipher;
    }

    public Mac getMac()
    {
        return mac;
    }

    private byte[] encryptBlock(
            final byte[] in,
            final int inOff,
            final int inLen,
            final byte[] macData)
        throws InvalidCipherTextException
    {
        byte[] C = null, K = null, K1 = null, K2 = null;
        int len;

        if (cipher == null)
        {
            // Streaming mode.
            K1 = new byte[inLen];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);

//            if (V.length != 0)
//            {
//                System.arraycopy(K, 0, K2, 0, K2.length);
//                System.arraycopy(K, K2.length, K1, 0, K1.length);
//            }
//            else
            {
                System.arraycopy(K, 0, K1, 0, K1.length);
                System.arraycopy(K, inLen, K2, 0, K2.length);
            }

            C = new byte[inLen];

            for (int i = 0; i != inLen; i++)
            {
                C[i] = (byte)(in[inOff + i] ^ K1[i]);
            }
            len = inLen;
        }
        else
        {
            // Block cipher mode.
            K1 = new byte[((IESWithCipherParameters)param).getCipherKeySize() / 8];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);
            System.arraycopy(K, 0, K1, 0, K1.length);
            System.arraycopy(K, K1.length, K2, 0, K2.length);

            // If iv provided use it to initialise the cipher
            if (IV != null)
            {
                cipher.init(true, new ParametersWithIV(new KeyParameter(K1), IV));
            }
            else
            {
                cipher.init(true, new KeyParameter(K1));
            }

            C = new byte[cipher.getOutputSize(inLen)];
            len = cipher.processBytes(in, inOff, inLen, C, 0);
            len += cipher.doFinal(C, len);
        }


        // Convert the length of the encoding vector into a byte array.
        final byte[] P2 = param.getEncodingV();

        // Apply the MAC.
        final byte[] T = new byte[mac.getMacSize()];

        final byte[] K2a;
        if (hashK2) {
            K2a = new byte[hash.getDigestSize()];
            hash.reset();
            hash.update(K2, 0, K2.length);
            hash.doFinal(K2a, 0);
        } else {
            K2a = K2;
        }
        mac.init(new KeyParameter(K2a));
        mac.update(IV, 0, IV.length);
        mac.update(C, 0, C.length);
        if (P2 != null)
        {
            mac.update(P2, 0, P2.length);
        }
        if (V.length != 0 && P2 != null) {
            final byte[] L2 = new byte[4];
            Pack.intToBigEndian(P2.length * 8, L2, 0);
            mac.update(L2, 0, L2.length);
        }

        if (macData != null) {
            mac.update(macData, 0, macData.length);
        }

        mac.doFinal(T, 0);

        // Output the triple (V,C,T).
        final byte[] Output = new byte[V.length + len + T.length];
        System.arraycopy(V, 0, Output, 0, V.length);
        System.arraycopy(C, 0, Output, V.length, len);
        System.arraycopy(T, 0, Output, V.length + len, T.length);
        return Output;
    }

    private byte[] decryptBlock(
            final byte[] in_enc,
            final int inOff,
            final int inLen,
            final byte[] macData)
        throws InvalidCipherTextException
    {
        byte[] M = null, K = null, K1 = null, K2 = null;
        int len;

        // Ensure that the length of the input is greater than the MAC in bytes
        if (inLen <= (param.getMacKeySize() / 8))
        {
            throw new InvalidCipherTextException("Length of input must be greater than the MAC");
        }

        if (cipher == null)
        {
            // Streaming mode.
            K1 = new byte[inLen - V.length - mac.getMacSize()];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);

//            if (V.length != 0)
//            {
//                System.arraycopy(K, 0, K2, 0, K2.length);
//                System.arraycopy(K, K2.length, K1, 0, K1.length);
//            }
//            else
            {
                System.arraycopy(K, 0, K1, 0, K1.length);
                System.arraycopy(K, K1.length, K2, 0, K2.length);
            }

            M = new byte[K1.length];

            for (int i = 0; i != K1.length; i++)
            {
                M[i] = (byte)(in_enc[inOff + V.length + i] ^ K1[i]);
            }

            len = K1.length;
        }
        else
        {
            // Block cipher mode.
            K1 = new byte[((IESWithCipherParameters)param).getCipherKeySize() / 8];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);
            System.arraycopy(K, 0, K1, 0, K1.length);
            System.arraycopy(K, K1.length, K2, 0, K2.length);

            // If IV provide use it to initialize the cipher
            if (IV != null)
            {
                cipher.init(false, new ParametersWithIV(new KeyParameter(K1), IV));
            }
            else
            {
                cipher.init(false, new KeyParameter(K1));
            }

            M = new byte[cipher.getOutputSize(inLen - V.length - mac.getMacSize())];
            len = cipher.processBytes(in_enc, inOff + V.length, inLen - V.length - mac.getMacSize(), M, 0);
            len += cipher.doFinal(M, len);
        }


        // Convert the length of the encoding vector into a byte array.
        final byte[] P2 = param.getEncodingV();

        // Verify the MAC.
        final int end = inOff + inLen;
        final byte[] T1 = Arrays.copyOfRange(in_enc, end - mac.getMacSize(), end);

        final byte[] T2 = new byte[T1.length];
        final byte[] K2a;
        if (hashK2) {
            K2a = new byte[hash.getDigestSize()];
            hash.reset();
            hash.update(K2, 0, K2.length);
            hash.doFinal(K2a, 0);
        } else {
            K2a = K2;
        }
        mac.init(new KeyParameter(K2a));
        mac.update(IV, 0, IV.length);
        mac.update(in_enc, inOff + V.length, inLen - V.length - T2.length);

        if (P2 != null)
        {
            mac.update(P2, 0, P2.length);
        }

        if (V.length != 0 && P2 != null) {
            final byte[] L2 = new byte[4];
            Pack.intToBigEndian(P2.length * 8, L2, 0);
            mac.update(L2, 0, L2.length);
        }

        if (macData != null) {
            mac.update(macData, 0, macData.length);
        }

        mac.doFinal(T2, 0);

        if (!Arrays.constantTimeAreEqual(T1, T2))
        {
            throw new InvalidCipherTextException("Invalid MAC.");
        }


        // Output the message.
        return Arrays.copyOfRange(M, 0, len);
    }

    public byte[] processBlock(final byte[] in, final int inOff, final int inLen) throws InvalidCipherTextException {
        return processBlock(in, inOff, inLen, null);
    }

    public byte[] processBlock(
            final byte[] in,
            final int inOff,
            final int inLen,
            final byte[] macData)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            if (keyPairGenerator != null)
            {
                final EphemeralKeyPair ephKeyPair = keyPairGenerator.generate();

                this.privParam = ephKeyPair.getKeyPair().getPrivate();
                this.V = ephKeyPair.getEncodedPublicKey();
            }
        }
        else
        {
            if (keyParser != null)
            {
                final ByteArrayInputStream bIn = new ByteArrayInputStream(in, inOff, inLen);

                try
                {
                    this.pubParam = keyParser.readKey(bIn);
                } catch (final IOException e)
                {
                    throw new InvalidCipherTextException("unable to recover ephemeral public key: " + e.getMessage(), e);
                }

                final int encLength = (inLen - bIn.available());
                this.V = Arrays.copyOfRange(in, inOff, inOff + encLength);
            }
        }

        // Compute the common value and convert to byte array.
        agree.init(privParam);
        final BigInteger z = agree.calculateAgreement(pubParam);
        final byte[] Z = BigIntegers.asUnsignedByteArray(agree.getFieldSize(), z);

        // Create input to KDF.
        final byte[] VZ;
//        if (V.length != 0)
//        {
//            VZ = new byte[V.length + Z.length];
//            System.arraycopy(V, 0, VZ, 0, V.length);
//            System.arraycopy(Z, 0, VZ, V.length, Z.length);
//        }
//        else
        {
            VZ = Z;
        }

        // Initialise the KDF.
        final DerivationParameters kdfParam;
        if (kdf instanceof MGF1BytesGeneratorExt) {
            kdfParam = new MGFParameters(VZ);
        } else {
            kdfParam = new KDFParameters(VZ, param.getDerivationV());
        }
        kdf.init(kdfParam);

        return forEncryption
            ? encryptBlock(in, inOff, inLen, macData)
            : decryptBlock(in, inOff, inLen, macData);
    }
}
