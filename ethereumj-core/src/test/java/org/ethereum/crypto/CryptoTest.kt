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

import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.AsymmetricCipherKeyPair
import org.spongycastle.crypto.BufferedBlockCipher
import org.spongycastle.crypto.KeyEncoder
import org.spongycastle.crypto.agreement.ECDHBasicAgreement
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESFastEngine
import org.spongycastle.crypto.engines.IESEngine
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator
import org.spongycastle.crypto.generators.KDF2BytesGenerator
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.*
import org.spongycastle.crypto.parsers.ECIESPublicKeyParser
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.SecureRandom

class CryptoTest {


    @Test
    fun test1() {

        var result = HashUtil.sha3("horse".toByteArray())

        assertEquals("c87f65ff3f271bf5dc8643484f66b200109caffe4bf98c4cb393dc35740b28c0",
                Hex.toHexString(result))

        result = HashUtil.sha3("cow".toByteArray())

        assertEquals("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4",
                Hex.toHexString(result))
    }

    @Test
    fun test3() {
        val privKey = BigInteger("cd244b3015703ddf545595da06ada5516628c5feadbf49dc66049c4b370cc5d8", 16)
        val addr = ECKey.fromPrivate(privKey).address
        assertEquals("89b44e4d3c81ede05d0f5de8d1a68f754d73d997", Hex.toHexString(addr))
    }


    @Test
    fun test4() {
        val cowBytes = HashUtil.sha3("cow".toByteArray())
        val addr = ECKey.fromPrivate(cowBytes).address
        assertEquals("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826", Hex.toHexString(addr).toUpperCase())
    }

    @Test
    fun test5() {
        val horseBytes = HashUtil.sha3("horse".toByteArray())
        val addr = ECKey.fromPrivate(horseBytes).address
        assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F", Hex.toHexString(addr).toUpperCase())
    }

    @Test /* performance test */
    fun test6() {

        val firstTime = System.currentTimeMillis()
        println(firstTime)
        for (i in 0..999) {

            val horseBytes = HashUtil.sha3("horse".toByteArray())
            val addr = ECKey.fromPrivate(horseBytes).address
            assertEquals("13978AEE95F38490E9769C39B2773ED763D9CD5F", Hex.toHexString(addr).toUpperCase())
        }
        val secondTime = System.currentTimeMillis()
        println(secondTime)
        println((secondTime - firstTime).toString() + " millisec")
        // 1) result: ~52 address calculation every second
    }

    @Test /* real tx hash calc */
    fun test7() {

        val txRaw = "F89D80809400000000000000000000000000000000000000008609184E72A000822710B3606956330C0D630000003359366000530A0D630000003359602060005301356000533557604060005301600054630000000C5884336069571CA07F6EB94576346488C6253197BDE6A7E59DDC36F2773672C849402AA9C402C3C4A06D254E662BF7450DD8D835160CBB053463FED0B53F2CDD7F3EA8731919C8E8CC"
        val txHashB = HashUtil.sha3(Hex.decode(txRaw))
        val txHash = Hex.toHexString(txHashB)
        assertEquals("4b7d9670a92bf120d5b43400543b69304a14d767cf836a7f6abff4edde092895", txHash)
    }

    @Test /* real block hash calc */
    fun test8() {

        val blockRaw = "F885F8818080A01DCC4DE8DEC75D7AAB85B567B6CCD41AD312451B948A7413F0A142FD40D49347940000000000000000000000000000000000000000A0BCDDD284BF396739C224DBA0411566C891C32115FEB998A3E2B4E61F3F35582AA01DCC4DE8DEC75D7AAB85B567B6CCD41AD312451B948A7413F0A142FD40D4934783800000808080C0C0"

        val blockHashB = HashUtil.sha3(Hex.decode(blockRaw))
        val blockHash = Hex.toHexString(blockHashB)
        println(blockHash)
    }

    @Test
    fun test9() {
        // TODO: https://tools.ietf.org/html/rfc6979#section-2.2
        // TODO: https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/crypto/signers/ECDSASigner.java

        println(BigInteger(Hex.decode("3913517ebd3c0c65000000")))
        println(Utils.getValueShortString(BigInteger("69000000000000000000000000")))
    }

    @Test
    fun test10() {
        val privKey = BigInteger("74ef8a796480dda87b4bc550b94c408ad386af0f65926a392136286784d63858", 16)
        val addr = ECKey.fromPrivate(privKey).address
        assertEquals("ba73facb4f8291f09f27f90fe1213537b910065e", Hex.toHexString(addr))
    }


    @Test  // basic encryption/decryption
    @Throws(Throwable::class)
    fun test11() {

        val keyBytes = sha3("...".toByteArray())
        log.info("key: {}", Hex.toHexString(keyBytes))
        val ivBytes = ByteArray(16)
        val payload = Hex.decode("22400891000000000000000000000000")

        val key = KeyParameter(keyBytes)
        val params = ParametersWithIV(key, ByteArray(16))

        val engine = AESFastEngine()
        val ctrEngine = SICBlockCipher(engine)

        ctrEngine.init(true, params)

        val cipher = ByteArray(16)
        ctrEngine.processBlock(payload, 0, cipher, 0)

        log.info("cipher: {}", Hex.toHexString(cipher))


        val output = ByteArray(cipher.size)
        ctrEngine.init(false, params)
        ctrEngine.processBlock(cipher, 0, output, 0)

        assertEquals(Hex.toHexString(output), Hex.toHexString(payload))
        log.info("original: {}", Hex.toHexString(payload))
    }

    @Test  // big packet encryption
    @Throws(Throwable::class)
    fun test12() {

        val engine = AESFastEngine()
        val ctrEngine = SICBlockCipher(engine)

        val keyBytes = Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1")
        val ivBytes = ByteArray(16)
        val payload = Hex.decode("0109efc76519b683d543db9d0991bcde99cc9a3d14b1d0ecb8e9f1f66f31558593d746eaa112891b04ef7126e1dce17c9ac92ebf39e010f0028b8ec699f56f5d0c0d00")
        val cipherText = Hex.decode("f9fab4e9dd9fc3e5d0d0d16da254a2ac24df81c076e3214e2c57da80a46e6ae4752f4b547889fa692b0997d74f36bb7c047100ba71045cb72cfafcc7f9a251762cdf8f")

        val key = KeyParameter(keyBytes)
        val params = ParametersWithIV(key, ivBytes)

        ctrEngine.init(true, params)

        val `in` = payload
        val out = ByteArray(`in`.size)

        var i = 0

        while (i < `in`.size) {
            ctrEngine.processBlock(`in`, i, out, i)
            i += engine.blockSize
            if (`in`.size - i < engine.blockSize)
                break
        }

        // process left bytes
        if (`in`.size - i > 0) {
            val tmpBlock = ByteArray(16)
            System.arraycopy(`in`, i, tmpBlock, 0, `in`.size - i)
            ctrEngine.processBlock(tmpBlock, 0, tmpBlock, 0)
            System.arraycopy(tmpBlock, 0, out, i, `in`.size - i)
        }

        log.info("cipher: {}", Hex.toHexString(out))

        assertEquals(Hex.toHexString(cipherText), Hex.toHexString(out))
    }

    @Test  // cpp keys demystified
    @Throws(Throwable::class)
    fun test13() {

        //        us.secret() a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1
        //        us.public() caa3d5086b31529bb00207eabf244a0a6c54d807d2ac0ec1f3b1bdde0dbf8130c115b1eaf62ce0f8062bcf70c0fefbc97cec79e7faffcc844a149a17fcd7bada
        //        us.address() 47d8cb63a7965d98b547b9f0333a654b60ffa190


        val key = ECKey.fromPrivate(Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1"))

        val address = Hex.toHexString(key.address)
        val pubkey = Hex.toHexString(key.pubKeyPoint.getEncoded(/* uncompressed form */false))

        log.info("address: " + address)
        log.info("pubkey: " + pubkey)

        assertEquals("47d8cb63a7965d98b547b9f0333a654b60ffa190", address)
        assertEquals("04caa3d5086b31529bb00207eabf244a0a6c54d807d2ac0ec1f3b1bdde0dbf8130c115b1eaf62ce0f8062bcf70c0fefbc97cec79e7faffcc844a149a17fcd7bada", pubkey)
    }


    @Test  // ECIES_AES128_SHA256 + No Ephemeral Key + IV(all zeroes)
    @Throws(Throwable::class)
    fun test14() {

        val aesFastEngine = AESFastEngine()

        val iesEngine = IESEngine(
                ECDHBasicAgreement(),
                KDF2BytesGenerator(SHA256Digest()),
                HMac(SHA256Digest()),
                BufferedBlockCipher(SICBlockCipher(aesFastEngine)))


        val d = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val e = byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1)

        val p = IESWithCipherParameters(d, e, 64, 128)
        val parametersWithIV = ParametersWithIV(p, ByteArray(16))

        val eGen = ECKeyPairGenerator()
        val gParam = ECKeyGenerationParameters(ECKey.CURVE, SecureRandom())

        eGen.init(gParam)


        val p1 = eGen.generateKeyPair()
        val p2 = eGen.generateKeyPair()


        val keygenParams = ECKeyGenerationParameters(ECKey.CURVE, SecureRandom())
        val generator = ECKeyPairGenerator()
        generator.init(keygenParams)

        val gen = ECKeyPairGenerator()
        gen.init(ECKeyGenerationParameters(ECKey.CURVE, SecureRandom()))

        iesEngine.init(true, p1.private, p2.public, parametersWithIV)

        val message = Hex.decode("010101")
        log.info("payload: {}", Hex.toHexString(message))


        val cipher = iesEngine.processBlock(message, 0, message.size)
        log.info("cipher: {}", Hex.toHexString(cipher))


        val decryptorIES_Engine = IESEngine(
                ECDHBasicAgreement(),
                KDF2BytesGenerator(SHA256Digest()),
                HMac(SHA256Digest()),
                BufferedBlockCipher(SICBlockCipher(aesFastEngine)))

        decryptorIES_Engine.init(false, p2.private, p1.public, parametersWithIV)

        val orig = decryptorIES_Engine.processBlock(cipher, 0, cipher.size)

        log.info("orig: " + Hex.toHexString(orig))
    }


    @Test  // ECIES_AES128_SHA256 + Ephemeral Key + IV(all zeroes)
    @Throws(Throwable::class)
    fun test15() {


        val privKey = Hex.decode("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1")

        val ecKey = ECKey.fromPrivate(privKey)

        val ecPrivKey = ECPrivateKeyParameters(ecKey.privKey, ECKey.CURVE)
        val ecPubKey = ECPublicKeyParameters(ecKey.pubKeyPoint, ECKey.CURVE)

        val myKey = AsymmetricCipherKeyPair(ecPubKey, ecPrivKey)


        val aesFastEngine = AESFastEngine()

        val iesEngine = IESEngine(
                ECDHBasicAgreement(),
                KDF2BytesGenerator(SHA256Digest()),
                HMac(SHA256Digest()),
                BufferedBlockCipher(SICBlockCipher(aesFastEngine)))


        val d = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val e = byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1)

        val p = IESWithCipherParameters(d, e, 64, 128)
        val parametersWithIV = ParametersWithIV(p, ByteArray(16))

        val eGen = ECKeyPairGenerator()
        val gParam = ECKeyGenerationParameters(ECKey.CURVE, SecureRandom())

        eGen.init(gParam)

        val keygenParams = ECKeyGenerationParameters(ECKey.CURVE, SecureRandom())
        val generator = ECKeyPairGenerator()
        generator.init(keygenParams)

        val kGen = EphemeralKeyPairGenerator(generator, KeyEncoder { keyParameter -> (keyParameter as ECPublicKeyParameters).q.encoded })


        val gen = ECKeyPairGenerator()
        gen.init(ECKeyGenerationParameters(ECKey.CURVE, SecureRandom()))

        iesEngine.init(myKey.public, parametersWithIV, kGen)

        val message = Hex.decode("010101")
        log.info("payload: {}", Hex.toHexString(message))


        val cipher = iesEngine.processBlock(message, 0, message.size)
        log.info("cipher: {}", Hex.toHexString(cipher))


        val decryptorIES_Engine = IESEngine(
                ECDHBasicAgreement(),
                KDF2BytesGenerator(SHA256Digest()),
                HMac(SHA256Digest()),
                BufferedBlockCipher(SICBlockCipher(aesFastEngine)))

        decryptorIES_Engine.init(myKey.private, parametersWithIV, ECIESPublicKeyParser(ECKey.CURVE))

        val orig = decryptorIES_Engine.processBlock(cipher, 0, cipher.size)

        log.info("orig: " + Hex.toHexString(orig))
    }

    companion object {

        private val log = LoggerFactory.getLogger("test")
    }

}
