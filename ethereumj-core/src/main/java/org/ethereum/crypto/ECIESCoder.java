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

import com.google.common.base.Throwables;
import org.ethereum.ConcatKDFBytesGenerator;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.*;
import org.spongycastle.crypto.parsers.ECIESPublicKeyParser;
import org.spongycastle.math.ec.ECPoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static org.ethereum.crypto.ECKey.CURVE;

public class ECIESCoder {


    private static final int KEY_SIZE = 128;

    public static byte[] decrypt(final BigInteger privKey, final byte[] cipher) throws IOException, InvalidCipherTextException {
        return decrypt(privKey, cipher, null);
    }

    public static byte[] decrypt(final BigInteger privKey, final byte[] cipher, final byte[] macData) throws IOException, InvalidCipherTextException {

        final byte[] plaintext;

        final ByteArrayInputStream is = new ByteArrayInputStream(cipher);
        final byte[] ephemBytes = new byte[2 * ((CURVE.getCurve().getFieldSize() + 7) / 8) + 1];

        is.read(ephemBytes);
        final ECPoint ephem = CURVE.getCurve().decodePoint(ephemBytes);
        final byte[] IV = new byte[KEY_SIZE / 8];
        is.read(IV);
        final byte[] cipherBody = new byte[is.available()];
        is.read(cipherBody);

        plaintext = decrypt(ephem, privKey, IV, cipherBody, macData);

        return plaintext;
    }

    private static byte[] decrypt(final ECPoint ephem, final BigInteger prv, final byte[] IV, final byte[] cipher, final byte[] macData) throws InvalidCipherTextException {
        final AESFastEngine aesFastEngine = new AESFastEngine();

        final EthereumIESEngine iesEngine = new EthereumIESEngine(
                new ECDHBasicAgreement(),
                new ConcatKDFBytesGenerator(new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new SHA256Digest(),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));


        final byte[] d = new byte[]{};
        final byte[] e = new byte[]{};

        final IESParameters p = new IESWithCipherParameters(d, e, KEY_SIZE, KEY_SIZE);
        final ParametersWithIV parametersWithIV =
                new ParametersWithIV(p, IV);

        iesEngine.init(false, new ECPrivateKeyParameters(prv, CURVE), new ECPublicKeyParameters(ephem, CURVE), parametersWithIV);

        return iesEngine.processBlock(cipher, 0, cipher.length, macData);
    }

    /**
     *  Encryption equivalent to the Crypto++ default ECIES<ECP> settings:
     *
     *  DL_KeyAgreementAlgorithm:        DL_KeyAgreementAlgorithm_DH<struct ECPPoint,struct EnumToType<enum CofactorMultiplicationOption,0> >
     *  DL_KeyDerivationAlgorithm:       DL_KeyDerivationAlgorithm_P1363<struct ECPPoint,0,class P1363_KDF2<class SHA1> >
     *  DL_SymmetricEncryptionAlgorithm: DL_EncryptionAlgorithm_Xor<class HMAC<class SHA1>,0>
     *  DL_PrivateKey:                   DL_Key<ECPPoint>
     *  DL_PrivateKey_EC<class ECP>
     *
     *  Used for Whisper V3
     */
    public static byte[] decryptSimple(final BigInteger privKey, final byte[] cipher) throws IOException, InvalidCipherTextException {
        final EthereumIESEngine iesEngine = new EthereumIESEngine(
                new ECDHBasicAgreement(),
                new MGF1BytesGeneratorExt(new SHA1Digest(), 1),
                new HMac(new SHA1Digest()),
                new SHA1Digest(),
                null);

        final IESParameters p = new IESParameters(null, null, KEY_SIZE);
        final ParametersWithIV parametersWithIV = new ParametersWithIV(p, new byte[0]);

        iesEngine.setHashMacKey(false);

        iesEngine.init(new ECPrivateKeyParameters(privKey, CURVE), parametersWithIV,
                new ECIESPublicKeyParser(ECKey.CURVE));

        return iesEngine.processBlock(cipher, 0, cipher.length);
    }

    public static byte[] encrypt(final ECPoint toPub, final byte[] plaintext) {
        return encrypt(toPub, plaintext, null);
    }

    public static byte[] encrypt(final ECPoint toPub, final byte[] plaintext, final byte[] macData) {

        final ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        final SecureRandom random = new SecureRandom();
        final KeyGenerationParameters gParam = new ECKeyGenerationParameters(CURVE, random);

        eGen.init(gParam);

        final byte[] IV = new byte[KEY_SIZE / 8];
        new SecureRandom().nextBytes(IV);

        final AsymmetricCipherKeyPair ephemPair = eGen.generateKeyPair();
        final BigInteger prv = ((ECPrivateKeyParameters) ephemPair.getPrivate()).getD();
        final ECPoint pub = ((ECPublicKeyParameters) ephemPair.getPublic()).getQ();
        final EthereumIESEngine iesEngine = makeIESEngine(true, toPub, prv, IV);


        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(CURVE, random);
        final ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keygenParams);

        final ECKeyPairGenerator gen = new ECKeyPairGenerator();
        gen.init(new ECKeyGenerationParameters(ECKey.CURVE, random));

        final byte[] cipher;
        try {
            cipher = iesEngine.processBlock(plaintext, 0, plaintext.length, macData);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(pub.getEncoded(false));
            bos.write(IV);
            bos.write(cipher);
            return bos.toByteArray();
        } catch (InvalidCipherTextException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     *  Encryption equivalent to the Crypto++ default ECIES<ECP> settings:
     *
     *  DL_KeyAgreementAlgorithm:        DL_KeyAgreementAlgorithm_DH<struct ECPPoint,struct EnumToType<enum CofactorMultiplicationOption,0> >
     *  DL_KeyDerivationAlgorithm:       DL_KeyDerivationAlgorithm_P1363<struct ECPPoint,0,class P1363_KDF2<class SHA1> >
     *  DL_SymmetricEncryptionAlgorithm: DL_EncryptionAlgorithm_Xor<class HMAC<class SHA1>,0>
     *  DL_PrivateKey:                   DL_Key<ECPPoint>
     *  DL_PrivateKey_EC<class ECP>
     *
     *  Used for Whisper V3
     */
    public static byte[] encryptSimple(final ECPoint pub, final byte[] plaintext) throws IOException, InvalidCipherTextException {
        final EthereumIESEngine iesEngine = new EthereumIESEngine(
                new ECDHBasicAgreement(),
                new MGF1BytesGeneratorExt(new SHA1Digest(), 1),
                new HMac(new SHA1Digest()),
                new SHA1Digest(),
                null);

        final IESParameters p = new IESParameters(null, null, KEY_SIZE);
        final ParametersWithIV parametersWithIV = new ParametersWithIV(p, new byte[0]);

        iesEngine.setHashMacKey(false);

        final ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        final SecureRandom random = new SecureRandom();
        final KeyGenerationParameters gParam = new ECKeyGenerationParameters(CURVE, random);
        eGen.init(gParam);

//        AsymmetricCipherKeyPairGenerator testGen = new AsymmetricCipherKeyPairGenerator() {
//            ECKey priv = ECKey.fromPrivate(Hex.decode("d0b043b4c5d657670778242d82d68a29d25d7d711127d17b8e299f156dad361a"));
//
//            @Override
//            public void init(KeyGenerationParameters keyGenerationParameters) {
//            }
//
//            @Override
//            public AsymmetricCipherKeyPair generateKeyPair() {
//                return new AsymmetricCipherKeyPair(new ECPublicKeyParameters(priv.getPubKeyPoint(), CURVE),
//                        new ECPrivateKeyParameters(priv.getPrivKey(), CURVE));
//            }
//        };

        final EphemeralKeyPairGenerator ephemeralKeyPairGenerator =
                new EphemeralKeyPairGenerator(/*testGen*/eGen, new ECIESPublicKeyEncoder());

        iesEngine.init(new ECPublicKeyParameters(pub, CURVE), parametersWithIV, ephemeralKeyPairGenerator);

        return iesEngine.processBlock(plaintext, 0, plaintext.length);
    }


    private static EthereumIESEngine makeIESEngine(final boolean isEncrypt, final ECPoint pub, final BigInteger prv, final byte[] IV) {
        final AESFastEngine aesFastEngine = new AESFastEngine();

        final EthereumIESEngine iesEngine = new EthereumIESEngine(
                new ECDHBasicAgreement(),
                new ConcatKDFBytesGenerator(new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new SHA256Digest(),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));


        final byte[] d = new byte[]{};
        final byte[] e = new byte[]{};

        final IESParameters p = new IESWithCipherParameters(d, e, KEY_SIZE, KEY_SIZE);
        final ParametersWithIV parametersWithIV = new ParametersWithIV(p, IV);

        iesEngine.init(isEncrypt, new ECPrivateKeyParameters(prv, CURVE), new ECPublicKeyParameters(pub, CURVE), parametersWithIV);
        return iesEngine;
    }

    public static int getOverhead() {
        // 256 bit EC public key, IV, 256 bit MAC
        return 65 + KEY_SIZE/8 + 32;
    }
}
