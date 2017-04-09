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

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.Lists
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.ethereum.core.Transaction
import org.ethereum.crypto.ECKey.ECDSASignature
import org.ethereum.crypto.jce.SpongyCastleProvider
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ECKeyTest {

    private val privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4"
    private val privateKey = BigInteger(privString, 16)

    private val pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b"
    private val compressedPubString = "030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"
    private val pubKey = Hex.decode(pubString)
    private val compressedPubKey = Hex.decode(compressedPubString)

    private val exampleMessage = "This is an example of a signed message."
    private val sigBase64 = "HNLOSI9Nop5o8iywXKwbGbdd8XChK0rRvdRTG46RFcb7dcH+UKlejM/8u1SCoeQvu91jJBMd/nXDs7f5p8ch7Ms="

    @Test
    fun testHashCode() {
        Assert.assertEquals(-351262686, ECKey.fromPrivate(privateKey).hashCode().toLong())
    }

    @Test
    fun testECKey() {
        val key = ECKey()
        assertTrue(key.isPubKeyCanonical)
        assertNotNull(key.pubKey)
        assertNotNull(key.privKeyBytes)
        log.debug(Hex.toHexString(key.privKeyBytes!!) + " :Generated privkey")
        log.debug(Hex.toHexString(key.pubKey) + " :Generated pubkey")
    }

    @Test
    fun testFromPrivateKey() {
        val key = ECKey.fromPrivate(privateKey)
        assertTrue(key.isPubKeyCanonical)
        assertTrue(key.hasPrivKey())
        assertArrayEquals(pubKey, key.pubKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPrivatePublicKeyBytesNoArg() {
        ECKey(null as BigInteger?, null)
        fail("Expecting an IllegalArgumentException for using only null-parameters")
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testInvalidPrivateKey() {
        ECKey(
                Security.getProvider("SunEC"),
                KeyPairGenerator.getInstance("RSA").generateKeyPair().private,
                ECKey.fromPublicOnly(pubKey).pubKeyPoint)
        fail("Expecting an IllegalArgumentException for using an non EC private key")
    }

    @Test
    fun testIsPubKeyOnly() {
        val key = ECKey.fromPublicOnly(pubKey)
        assertTrue(key.isPubKeyCanonical)
        assertTrue(key.isPubKeyOnly)
        assertArrayEquals(key.pubKey, pubKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSignIncorrectInputSize() {
        val key = ECKey()
        val message = "The quick brown fox jumps over the lazy dog."
        val sig = key.doSign(message.toByteArray())
        fail("Expecting an IllegalArgumentException for a non 32-byte input")
    }

    @Test(expected = ECKey.MissingPrivateKeyException::class)
    fun testSignWithPubKeyOnly() {
        val key = ECKey.fromPublicOnly(pubKey)
        val message = "The quick brown fox jumps over the lazy dog."
        val input = HashUtil.sha3(message.toByteArray())
        val sig = key.doSign(input)
        fail("Expecting an MissingPrivateKeyException for a public only ECKey")
    }

    @Test(expected = SignatureException::class)
    @Throws(SignatureException::class)
    fun testBadBase64Sig() {
        val messageHash = ByteArray(32)
        ECKey.signatureToKey(messageHash, "This is not valid Base64!")
        fail("Expecting a SignatureException for invalid Base64")
    }

    @Test(expected = SignatureException::class)
    @Throws(SignatureException::class)
    fun testInvalidSignatureLength() {
        val messageHash = ByteArray(32)
        ECKey.signatureToKey(messageHash, "abcdefg")
        fail("Expecting a SignatureException for invalid signature length")
    }

    @Test
    fun testPublicKeyFromPrivate() {
        val pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, false)
        assertArrayEquals(pubKey, pubFromPriv)
    }

    @Test
    fun testPublicKeyFromPrivateCompressed() {
        val pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, true)
        assertArrayEquals(compressedPubKey, pubFromPriv)
    }

    @Test
    fun testGetAddress() {
        val key = ECKey.fromPublicOnly(pubKey)
        val address = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826"
        assertArrayEquals(Hex.decode(address), key.address)
    }

    @Test
    fun testToString() {
        val key = ECKey.fromPrivate(BigInteger.TEN) // An example private key.
        assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString())
    }

    @Test
    @Throws(IOException::class)
    fun testEthereumSign() {
        val key = ECKey.fromPrivate(privateKey)
        println("Secret\t: " + Hex.toHexString(key.privKeyBytes!!))
        println("Pubkey\t: " + Hex.toHexString(key.pubKey))
        println("Data\t: $exampleMessage")
        val messageHash = HashUtil.sha3(exampleMessage.toByteArray())
        val signature = key.sign(messageHash)
        val output = signature.toBase64()
        println("Signtr\t: " + output + " (Base64, length: " + output.length + ")")
        assertEquals(sigBase64, output)
    }

    /**
     * Verified via https://etherchain.org/verify/signature
     */
    @Test
    fun testEthereumSignToHex() {
        val key = ECKey.fromPrivate(privateKey)
        val messageHash = HashUtil.sha3(exampleMessage.toByteArray())
        val signature = key.sign(messageHash)
        val output = signature.toHex()
        println("Signature\t: " + output + " (Hex, length: " + output.length + ")")
        val signatureHex = "d2ce488f4da29e68f22cb05cac1b19b75df170a12b4ad1bdd4531b8e9115c6fb75c1fe50a95e8ccffcbb5482a1e42fbbdd6324131dfe75c3b3b7f9a7c721eccb01"
        assertEquals(signatureHex, output)
    }

    @Test
    fun testVerifySignature1() {
        val key = ECKey.fromPublicOnly(pubKey)
        val r = BigInteger("28157690258821599598544026901946453245423343069728565040002908283498585537001")
        val s = BigInteger("30212485197630673222315826773656074299979444367665131281281249560925428307087")
        val sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), 28.toByte())
        key.verify(HashUtil.sha3(exampleMessage.toByteArray()), sig)
    }

    @Test
    fun testVerifySignature2() {
        val r = BigInteger("c52c114d4f5a3ba904a9b3036e5e118fe0dbb987fe3955da20f2cd8f6c21ab9c", 16)
        val s = BigInteger("6ba4c2874299a55ad947dbc98a25ee895aabf6b625c26c435e84bfd70edf2f69", 16)
        val sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), 0x1b.toByte())
        val rawtx = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13cd947ec05abc7fe734df8dd8268609184e72a0006480")
        val rawHash = HashUtil.sha3(rawtx)
        val address = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826")
        try {
            val key = ECKey.signatureToKey(rawHash, sig)

            println("Signature public key\t: " + Hex.toHexString(key.pubKey))
            println("Sender is\t\t: " + Hex.toHexString(key.address))

            assertEquals(key, ECKey.signatureToKey(rawHash, sig.toBase64()))
            assertEquals(key, ECKey.recoverFromSignature(0, sig, rawHash))
            assertArrayEquals(key.pubKey, ECKey.recoverPubBytesFromSignature(0, sig, rawHash))


            assertArrayEquals(address, key.address)
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig))
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig.toBase64()))
            assertArrayEquals(address, ECKey.recoverAddressFromSignature(0, sig, rawHash))

            assertTrue(key.verify(rawHash, sig))
        } catch (e: SignatureException) {
            fail()
        }

    }

    @Test
    @Throws(SignatureException::class)
    fun testVerifySignature3() {

        val rawtx = Hex.decode("f88080893635c9adc5dea000008609184e72a00094109f3535353535353535353535353535353535359479b08ad8787060333663d19704909ee7b1903e58801ba0899b92d0c76cbf18df24394996beef19c050baa9823b4a9828cd9b260c97112ea0c9e62eb4cf0a9d95ca35c8830afac567619d6b3ebee841a3c8be61d35acd8049")

        val tx = Transaction(rawtx)
        val key = ECKey.signatureToKey(HashUtil.sha3(rawtx), tx.signature)

        println("Signature public key\t: " + Hex.toHexString(key.pubKey))
        println("Sender is\t\t: " + Hex.toHexString(key.address))

        //  sender: CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826
        // todo: add test assertion when the sign/verify part actually works.
    }


    @Test
    @Throws(Exception::class)
    fun testSValue() {
        // Check that we never generate an S value that is larger than half the curve order. This avoids a malleability
        // issue that can allow someone to change a transaction [hash] without invalidating the signature.
        val ITERATIONS = 10
        val executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ITERATIONS))
        val sigFutures = Lists.newArrayList<ListenableFuture<ECKey.ECDSASignature>>()
        val key = ECKey()
        (0..ITERATIONS - 1)
                .asSequence()
                .map { HashUtil.sha3(byteArrayOf(it.toByte())) }
                .mapTo(sigFutures) { executor.submit(Callable { key.doSign(it) }) }
        val sigs = Futures.allAsList(sigFutures).get()
        for (signature in sigs) {
            assertTrue(signature.s.compareTo(ECKey.HALF_CURVE_ORDER) <= 0)
        }
        val duplicate = ECKey.ECDSASignature(sigs[0].r, sigs[0].s)
        assertEquals(sigs[0], duplicate)
        assertEquals(sigs[0].hashCode().toLong(), duplicate.hashCode().toLong())
    }

    @Test
    fun testSignVerify() {
        val key = ECKey.fromPrivate(privateKey)
        val message = "This is an example of a signed message."
        val input = HashUtil.sha3(message.toByteArray())
        val sig = key.sign(input)
        assertTrue(sig.validateComponents())
        assertTrue(key.verify(input, sig))
    }

    @Throws(Exception::class)
    private fun testProviderRoundTrip(provider: Provider) {
        val key = ECKey(provider, secureRandom)
        val message = "The quick brown fox jumps over the lazy dog."
        val input = HashUtil.sha3(message.toByteArray())
        val sig = key.sign(input)
        assertTrue(sig.validateComponents())
        assertTrue(key.verify(input, sig))
    }

    @Test
    @Throws(Exception::class)
    fun testSunECRoundTrip() {
        val provider = Security.getProvider("SunEC")
        if (provider != null) {
            testProviderRoundTrip(provider)
        } else {
            println("Skip test as provider doesn't exist. Must be OpenJDK 1.7 which ships without 'SunEC'")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSpongyCastleRoundTrip() {
        testProviderRoundTrip(SpongyCastleProvider.getInstance())
    }

    @Test
    fun testIsPubKeyCanonicalCorect() {
        // Test correct prefix 4, right length 65
        val canonicalPubkey1 = ByteArray(65)
        canonicalPubkey1[0] = 0x04
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey1))
        // Test correct prefix 2, right length 33
        val canonicalPubkey2 = ByteArray(33)
        canonicalPubkey2[0] = 0x02
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey2))
        // Test correct prefix 3, right length 33
        val canonicalPubkey3 = ByteArray(33)
        canonicalPubkey3[0] = 0x03
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey3))
    }

    @Test
    fun testIsPubKeyCanonicalWrongLength() {
        // Test correct prefix 4, but wrong length !65
        val nonCanonicalPubkey1 = ByteArray(64)
        nonCanonicalPubkey1[0] = 0x04
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey1))
        // Test correct prefix 2, but wrong length !33
        val nonCanonicalPubkey2 = ByteArray(32)
        nonCanonicalPubkey2[0] = 0x02
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey2))
        // Test correct prefix 3, but wrong length !33
        val nonCanonicalPubkey3 = ByteArray(32)
        nonCanonicalPubkey3[0] = 0x03
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey3))
    }

    @Test
    fun testIsPubKeyCanonicalWrongPrefix() {
        // Test wrong prefix 4, right length 65
        val nonCanonicalPubkey4 = ByteArray(65)
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey4))
        // Test wrong prefix 2, right length 33
        val nonCanonicalPubkey5 = ByteArray(33)
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey5))
        // Test wrong prefix 3, right length 33
        val nonCanonicalPubkey6 = ByteArray(33)
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey6))
    }

    @Test
    @Throws(Exception::class)
    fun keyRecovery() {
        var key = ECKey()
        val message = "Hello World!"
        val hash = HashUtil.sha256(message.toByteArray())
        val sig = key.doSign(hash)
        key = ECKey.fromPublicOnly(key.pubKeyPoint)
        var found = false
        for (i in 0..3) {
            val key2 = ECKey.recoverFromSignature(i, sig, hash)
            checkNotNull<ECKey>(key2)
            if (key == key2) {
                found = true
                break
            }
        }
        assertTrue(found)
    }

    @Test
    @Throws(SignatureException::class)
    fun testSignedMessageToKey() {
        val messageHash = HashUtil.sha3(exampleMessage.toByteArray())
        val key = ECKey.signatureToKey(messageHash, sigBase64)
        assertNotNull(key)
        assertArrayEquals(pubKey, key.pubKey)
    }

    @Test
    fun testGetPrivKeyBytes() {
        val key = ECKey()
        assertNotNull(key.privKeyBytes)
        assertEquals(32, key.privKeyBytes!!.size.toLong())
    }

    @Test
    fun testEqualsObject() {
        val key0 = ECKey()
        val key1 = ECKey.fromPrivate(privateKey)
        val key2 = ECKey.fromPrivate(privateKey)

        assertFalse(key0 == key1)
        assertTrue(key1 == key1)
        assertTrue(key1 == key2)
    }


    @Test
    fun decryptAECSIC() {
        val key = ECKey.fromPrivate(Hex.decode("abb51256c1324a1350598653f46aa3ad693ac3cf5d05f36eba3f495a1f51590f"))
        val payload = key.decryptAES(Hex.decode("84a727bc81fa4b13947dc9728b88fd08"))
        println(Hex.toHexString(payload))
    }

    @Test
    fun testNodeId() {
        val key = ECKey.fromPublicOnly(pubKey)

        assertEquals(key, ECKey.fromNodeId(key.nodeId))
    }

    companion object {
        private val log = LoggerFactory.getLogger(ECKeyTest::class.java)

        private val secureRandom = SecureRandom()
    }
}
