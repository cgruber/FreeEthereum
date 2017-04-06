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

import org.ethereum.util.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.KeyEncoder;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.engines.IESEngine;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.spongycastle.crypto.generators.KDF2BytesGenerator;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.modes.SICBlockCipher;
import org.spongycastle.crypto.params.*;
import org.spongycastle.crypto.parsers.ECIESPublicKeyParser;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.ethereum.crypto.HashUtil.sha3;
import static org.junit.Assert.assertEquals;

public class CryptoTest {

    private static final Logger log = LoggerFactory.getLogger("test");


    @Test
    public void test1() {

        byte[] result = HashUtil.sha3("horse".getBytes());

        assertEquals("c87f65ff3f271bf5dc8643484f66b200109caffe4bf98c4cb393dc35740b28c0",
                Hex.toHexString(result));

        result = HashUtil.sha3("cow".getBytes());

        assertEquals("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4",
                Hex.toHexString(result));
    }

    @Test
    public void test3() {
        final BigInteger privKey = new BigInteger("cd244b3015703ddf545595da06ada5516628c5feadbf49dc66049c4b370cc5d8", 16);
        final byte[] addr = ECKey.fromPrivate(privKey).getAddress();
        assertEquals("89b44e4d3c81ede05d0f5de8d1a68f754d73d997", Hex.toHexString(addr));
    }


    @Test
    public void test4() {
        final byte[] cowBytes = HashUtil.sha3("cow".getBytes());
        final byte[] addr = ECKey.fromPrivate(cowBytes).getAddress();
        assertEquals("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826", Hex.toHexString(addr).toUpperCase());
    }

    @Test
    public void test5() {
        final byte[] horseBytes = HashUtil.sha3("horse".getBytes());
        final byte[] addr = ECKey.fromPrivate(horseBytes).getAddress();
        assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F", Hex.toHexString(addr).toUpperCase());
    }

    @Test   /* performance test */
    public void test6() {

        final long firstTime = System.currentTimeMillis();
        System.out.println(firstTime);
        for (int i = 0; i < 1000; ++i) {

            final byte[] horseBytes = HashUtil.sha3("horse".getBytes());
            final byte[] addr = ECKey.fromPrivate(horseBytes).getAddress();
            assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F", Hex.toHexString(addr).toUpperCase());
        }
        final long secondTime = System.currentTimeMillis();
        System.out.println(secondTime);
        System.out.println(secondTime - firstTime + " millisec");
        // 1) result: ~52 address calculation every second
    }

    @Test /* real tx hash calc */
    public void test7() {

        final String txRaw = "F89D80809400000000000000000000000000000000000000008609184E72A000822710B3606956330C0D630000003359366000530A0D630000003359602060005301356000533557604060005301600054630000000C5884336069571CA07F6EB94576346488C6253197BDE6A7E59DDC36F2773672C849402AA9C402C3C4A06D254E662BF7450DD8D835160CBB053463FED0B53F2CDD7F3EA8731919C8E8CC";
        final byte[] txHashB = HashUtil.sha3(Hex.decode(txRaw));
        final String txHash = Hex.toHexString(txHashB);
        assertEquals("4b7d9670a92bf120d5b43400543b69304a14d767cf836a7f6abff4edde092895", txHash);
    }

    @Test /* real block hash calc */
    public void test8() {

        final String blockRaw = "F885F8818080A01DCC4DE8DEC75D7AAB85B567B6CCD41AD312451B948A7413F0A142FD40D49347940000000000000000000000000000000000000000A0BCDDD284BF396739C224DBA0411566C891C32115FEB998A3E2B4E61F3F35582AA01DCC4DE8DEC75D7AAB85B567B6CCD41AD312451B948A7413F0A142FD40D4934783800000808080C0C0";

        final byte[] blockHashB = HashUtil.sha3(Hex.decode(blockRaw));
        final String blockHash = Hex.toHexString(blockHashB);
        System.out.println(blockHash);
    }

    @Test
    public void test9() {
        // TODO: https://tools.ietf.org/html/rfc6979#section-2.2
        // TODO: https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/crypto/signers/ECDSASigner.java

        System.out.println(new BigInteger(Hex.decode("3913517ebd3c0c65000000")));
        System.out.println(Utils.getValueShortString(new BigInteger("69000000000000000000000000")));
    }

    @Test
    public void test10() {
        final BigInteger privKey = new BigInteger("74ef8a796480dda87b4bc550b94c408ad386af0f65926a392136286784d63858", 16);
        final byte[] addr = ECKey.fromPrivate(privKey).getAddress();
        assertEquals("ba73facb4f8291f09f27f90fe1213537b910065e", Hex.toHexString(addr));
    }


    @Test  // basic encryption/decryption
    public void test11() throws Throwable {

        final byte[] keyBytes = sha3("...".getBytes());
        log.info("key: {}", Hex.toHexString(keyBytes));
        final byte[] ivBytes = new byte[16];
        final byte[] payload = Hex.decode("22400891000000000000000000000000");

        final KeyParameter key = new KeyParameter(keyBytes);
        final ParametersWithIV params = new ParametersWithIV(key, new byte[16]);

        final AESFastEngine engine = new AESFastEngine();
        final SICBlockCipher ctrEngine = new SICBlockCipher(engine);

        ctrEngine.init(true, params);

        final byte[] cipher = new byte[16];
        ctrEngine.processBlock(payload, 0, cipher, 0);

        log.info("cipher: {}", Hex.toHexString(cipher));


        final byte[] output = new byte[cipher.length];
        ctrEngine.init(false, params);
        ctrEngine.processBlock(cipher, 0, output, 0);

        assertEquals(Hex.toHexString(output), Hex.toHexString(payload));
        log.info("original: {}", Hex.toHexString(payload));
    }

    @Test  // big packet encryption
    public void test12() throws Throwable {

        final AESFastEngine engine = new AESFastEngine();
        final SICBlockCipher ctrEngine = new SICBlockCipher(engine);

        final byte[] keyBytes = Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1");
        final byte[] ivBytes = new byte[16];
        final byte[] payload = Hex.decode("0109efc76519b683d543db9d0991bcde99cc9a3d14b1d0ecb8e9f1f66f31558593d746eaa112891b04ef7126e1dce17c9ac92ebf39e010f0028b8ec699f56f5d0c0d00");
        final byte[] cipherText = Hex.decode("f9fab4e9dd9fc3e5d0d0d16da254a2ac24df81c076e3214e2c57da80a46e6ae4752f4b547889fa692b0997d74f36bb7c047100ba71045cb72cfafcc7f9a251762cdf8f");

        final KeyParameter key = new KeyParameter(keyBytes);
        final ParametersWithIV params = new ParametersWithIV(key, ivBytes);

        ctrEngine.init(true, params);

        final byte[] in = payload;
        final byte[] out = new byte[in.length];

        int i = 0;

        while(i < in.length){
            ctrEngine.processBlock(in, i, out, i);
            i += engine.getBlockSize();
            if (in.length - i  < engine.getBlockSize())
                break;
        }

        // process left bytes
        if (in.length - i > 0){
            final byte[] tmpBlock = new byte[16];
            System.arraycopy(in, i, tmpBlock, 0, in.length - i);
            ctrEngine.processBlock(tmpBlock, 0, tmpBlock, 0);
            System.arraycopy(tmpBlock, 0, out, i, in.length - i);
        }

        log.info("cipher: {}", Hex.toHexString(out));

        assertEquals(Hex.toHexString(cipherText), Hex.toHexString(out));
    }

    @Test  // cpp keys demystified
    public void test13() throws Throwable {

//        us.secret() a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1
//        us.public() caa3d5086b31529bb00207eabf244a0a6c54d807d2ac0ec1f3b1bdde0dbf8130c115b1eaf62ce0f8062bcf70c0fefbc97cec79e7faffcc844a149a17fcd7bada
//        us.address() 47d8cb63a7965d98b547b9f0333a654b60ffa190


        final ECKey key = ECKey.fromPrivate(Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1"));

        final String address = Hex.toHexString(key.getAddress());
        final String pubkey = Hex.toHexString(key.getPubKeyPoint().getEncoded(/* uncompressed form */ false));

        log.info("address: " + address);
        log.info("pubkey: " + pubkey);

        assertEquals("47d8cb63a7965d98b547b9f0333a654b60ffa190", address);
        assertEquals("04caa3d5086b31529bb00207eabf244a0a6c54d807d2ac0ec1f3b1bdde0dbf8130c115b1eaf62ce0f8062bcf70c0fefbc97cec79e7faffcc844a149a17fcd7bada", pubkey);
    }



    @Test  // ECIES_AES128_SHA256 + No Ephemeral Key + IV(all zeroes)
    public void test14() throws Throwable{

        final AESFastEngine aesFastEngine = new AESFastEngine();

        final IESEngine iesEngine = new IESEngine(
                new ECDHBasicAgreement(),
                new KDF2BytesGenerator(new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));


        final byte[] d = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final byte[] e = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

        final IESParameters p = new IESWithCipherParameters(d, e, 64, 128);
        final ParametersWithIV parametersWithIV = new ParametersWithIV(p, new byte[16]);

        final ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        final KeyGenerationParameters gParam = new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom());

        eGen.init(gParam);


        final AsymmetricCipherKeyPair p1 = eGen.generateKeyPair();
        final AsymmetricCipherKeyPair p2 = eGen.generateKeyPair();


        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom());
        final ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keygenParams);

        final ECKeyPairGenerator gen = new ECKeyPairGenerator();
        gen.init(new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom()));

        iesEngine.init(true, p1.getPrivate(), p2.getPublic(), parametersWithIV);

        final byte[] message = Hex.decode("010101");
        log.info("payload: {}", Hex.toHexString(message));


        final byte[] cipher = iesEngine.processBlock(message, 0, message.length);
        log.info("cipher: {}", Hex.toHexString(cipher));


        final IESEngine decryptorIES_Engine = new IESEngine(
                new ECDHBasicAgreement(),
                new KDF2BytesGenerator (new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));

        decryptorIES_Engine.init(false, p2.getPrivate(), p1.getPublic(), parametersWithIV);

        final byte[] orig = decryptorIES_Engine.processBlock(cipher, 0, cipher.length);

        log.info("orig: " + Hex.toHexString(orig));
    }


    @Test  // ECIES_AES128_SHA256 + Ephemeral Key + IV(all zeroes)
    public void test15() throws Throwable{


        final byte[] privKey = Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1");

        final ECKey ecKey = ECKey.fromPrivate(privKey);

        final ECPrivateKeyParameters ecPrivKey = new ECPrivateKeyParameters(ecKey.getPrivKey(), ECKey.CURVE);
        final ECPublicKeyParameters ecPubKey = new ECPublicKeyParameters(ecKey.getPubKeyPoint(), ECKey.CURVE);

        final AsymmetricCipherKeyPair myKey = new AsymmetricCipherKeyPair(ecPubKey, ecPrivKey);


        final AESFastEngine aesFastEngine = new AESFastEngine();

        final IESEngine iesEngine = new IESEngine(
                new ECDHBasicAgreement(),
                new KDF2BytesGenerator(new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));


        final byte[] d = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        final byte[] e = new byte[]{8, 7, 6, 5, 4, 3, 2, 1};

        final IESParameters p = new IESWithCipherParameters(d, e, 64, 128);
        final ParametersWithIV parametersWithIV = new ParametersWithIV(p, new byte[16]);

        final ECKeyPairGenerator eGen = new ECKeyPairGenerator();
        final KeyGenerationParameters gParam = new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom());

        eGen.init(gParam);

        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom());
        final ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keygenParams);

        final EphemeralKeyPairGenerator kGen = new EphemeralKeyPairGenerator(generator, new KeyEncoder()
        {
            public byte[] getEncoded(final AsymmetricKeyParameter keyParameter)
            {
                return ((ECPublicKeyParameters)keyParameter).getQ().getEncoded();
            }
        });


        final ECKeyPairGenerator gen = new ECKeyPairGenerator();
        gen.init(new ECKeyGenerationParameters(ECKey.CURVE, new SecureRandom()));

        iesEngine.init(myKey.getPublic(), parametersWithIV, kGen);

        final byte[] message = Hex.decode("010101");
        log.info("payload: {}", Hex.toHexString(message));


        final byte[] cipher = iesEngine.processBlock(message, 0, message.length);
        log.info("cipher: {}", Hex.toHexString(cipher));


        final IESEngine decryptorIES_Engine = new IESEngine(
                new ECDHBasicAgreement(),
                new KDF2BytesGenerator (new SHA256Digest()),
                new HMac(new SHA256Digest()),
                new BufferedBlockCipher(new SICBlockCipher(aesFastEngine)));

        decryptorIES_Engine.init(myKey.getPrivate(), parametersWithIV, new ECIESPublicKeyParser(ECKey.CURVE));

        final byte[] orig = decryptorIES_Engine.processBlock(cipher, 0, cipher.length);

        log.info("orig: " + Hex.toHexString(orig));
    }

}
