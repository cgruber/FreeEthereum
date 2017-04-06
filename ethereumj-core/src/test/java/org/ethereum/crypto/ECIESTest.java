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

import org.ethereum.ConcatKDFBytesGenerator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.*;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

public class ECIESTest {
    private static final int KEY_SIZE = 128;
    private static final String CIPHERTEXT1 = "042a851331790adacf6e64fcb19d0872fcdf1285a899a12cdc897da941816b0ea6485402aaf6c2e0a5d98ae3af1b05c68b307d1e0eb7a426a46f1617ba5b94f90b606eee3b5e9d2b527a9ee52cfa377bcd118b9390ed27ffe7d48e8155004375cae209012c3e057bb13a478a64a201d79ad4ae83";
    private static final X9ECParameters IES_CURVE_PARAM = SECNamedCurves.getByName("secp256r1");
    private static final BigInteger PRIVATE_KEY1 = new BigInteger("51134539186617376248226283012294527978458758538121566045626095875284492680246");
    static Logger log = LoggerFactory.getLogger("test");
    private static ECDomainParameters curve;

    private static ECPoint pub(final BigInteger d) {
        return curve.getG().multiply(d);
    }

    @BeforeClass
    public static void beforeAll() {
        curve = new ECDomainParameters(IES_CURVE_PARAM.getCurve(), IES_CURVE_PARAM.getG(), IES_CURVE_PARAM.getN(), IES_CURVE_PARAM.getH());
    }

    private static byte[] decrypt(final BigInteger prv, final byte[] cipher) throws InvalidCipherTextException, IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream(cipher);
        final byte[] ephemBytes = new byte[2 * ((curve.getCurve().getFieldSize() + 7) / 8) + 1];
        is.read(ephemBytes);
        final ECPoint ephem = curve.getCurve().decodePoint(ephemBytes);
        final byte[] IV = new byte[KEY_SIZE / 8];
        is.read(IV);
        final byte[] cipherBody = new byte[is.available()];
        is.read(cipherBody);

        final EthereumIESEngine iesEngine = makeIESEngine(false, ephem, prv, IV);

        final byte[] message = iesEngine.processBlock(cipherBody, 0, cipherBody.length);
        return message;
    }

    private static byte[] encrypt(final ECPoint toPub, final byte[] plaintext) throws InvalidCipherTextException, IOException {

        final ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        final SecureRandom random = new SecureRandom();
        final KeyGenerationParameters gParam = new ECKeyGenerationParameters(curve, random);

        eGen.init(gParam);

        final byte[] IV = new byte[KEY_SIZE / 8];
        new SecureRandom().nextBytes(IV);

        final AsymmetricCipherKeyPair ephemPair = eGen.generateKeyPair();
        final BigInteger prv = ((ECPrivateKeyParameters) ephemPair.getPrivate()).getD();
        final ECPoint pub = ((ECPublicKeyParameters) ephemPair.getPublic()).getQ();
        final EthereumIESEngine iesEngine = makeIESEngine(true, toPub, prv, IV);


        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(curve, random);
        final ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keygenParams);

        final ECKeyPairGenerator gen = new ECKeyPairGenerator();
        gen.init(new ECKeyGenerationParameters(ECKey.CURVE, random));

        final byte[] cipher = iesEngine.processBlock(plaintext, 0, plaintext.length);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(pub.getEncoded(false));
        bos.write(IV);
        bos.write(cipher);
        return bos.toByteArray();
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

        iesEngine.init(isEncrypt, new ECPrivateKeyParameters(prv, curve), new ECPublicKeyParameters(pub, curve), parametersWithIV);
        return iesEngine;
    }

    @Test
    public void testKDF() {
        final ConcatKDFBytesGenerator kdf = new ConcatKDFBytesGenerator(new SHA256Digest());
        kdf.init(new KDFParameters("Hello".getBytes(), new byte[0]));
        final byte[] bytes = new byte[2];
        kdf.generateBytes(bytes, 0, bytes.length);
        assertArrayEquals(new byte[]{-66, -89}, bytes);
    }

    @Test
    public void testDecryptTestVector() throws IOException, InvalidCipherTextException {
        final ECPoint pub1 = pub(PRIVATE_KEY1);
        final byte[] ciphertext = Hex.decode(CIPHERTEXT1);
        final byte[] plaintext = decrypt(PRIVATE_KEY1, ciphertext);
        assertArrayEquals(new byte[]{1, 1, 1}, plaintext);
    }

    @Test
    public void testRoundTrip() throws InvalidCipherTextException, IOException {
        final ECPoint pub1 = pub(PRIVATE_KEY1);
        final byte[] plaintext = "Hello world".getBytes();
        final byte[] ciphertext = encrypt(pub1, plaintext);
        final byte[] plaintext1 = decrypt(PRIVATE_KEY1, ciphertext);
        assertArrayEquals(plaintext, plaintext1);
    }

}
