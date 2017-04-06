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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey.ECDSASignature;
import org.ethereum.crypto.jce.SpongyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class ECKeyTest {
    private static final Logger log = LoggerFactory.getLogger(ECKeyTest.class);

    private static final SecureRandom secureRandom = new SecureRandom();

    private final String privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
    private final BigInteger privateKey = new BigInteger(privString, 16);

    private final String pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
    private final String compressedPubString = "030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad";
    private final byte[] pubKey = Hex.decode(pubString);
    private final byte[] compressedPubKey = Hex.decode(compressedPubString);

    private final String exampleMessage = "This is an example of a signed message.";
    private final String sigBase64 = "HNLOSI9Nop5o8iywXKwbGbdd8XChK0rRvdRTG46RFcb7dcH+UKlejM/8u1SCoeQvu91jJBMd/nXDs7f5p8ch7Ms=";

    @Test
    public void testHashCode() {
        Assert.assertEquals(-351262686, ECKey.fromPrivate(privateKey).hashCode());
    }

    @Test
    public void testECKey() {
        final ECKey key = new ECKey();
        assertTrue(key.isPubKeyCanonical());
        assertNotNull(key.getPubKey());
        assertNotNull(key.getPrivKeyBytes());
        log.debug(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
        log.debug(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
    }

    @Test
    public void testFromPrivateKey() {
        final ECKey key = ECKey.fromPrivate(privateKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.hasPrivKey());
        assertArrayEquals(pubKey, key.getPubKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivatePublicKeyBytesNoArg() {
        new ECKey((BigInteger) null, null);
        fail("Expecting an IllegalArgumentException for using only null-parameters");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPrivateKey() throws Exception {
        new ECKey(
            Security.getProvider("SunEC"),
            KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate(),
            ECKey.fromPublicOnly(pubKey).getPubKeyPoint());
        fail("Expecting an IllegalArgumentException for using an non EC private key");
    }

    @Test
    public void testIsPubKeyOnly() {
        final ECKey key = ECKey.fromPublicOnly(pubKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.isPubKeyOnly());
        assertArrayEquals(key.getPubKey(), pubKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSignIncorrectInputSize() {
        final ECKey key = new ECKey();
        final String message = "The quick brown fox jumps over the lazy dog.";
        final ECDSASignature sig = key.doSign(message.getBytes());
        fail("Expecting an IllegalArgumentException for a non 32-byte input");
    }

    @Test(expected = ECKey.MissingPrivateKeyException.class)
    public void testSignWithPubKeyOnly() {
        final ECKey key = ECKey.fromPublicOnly(pubKey);
        final String message = "The quick brown fox jumps over the lazy dog.";
        final byte[] input = HashUtil.sha3(message.getBytes());
        final ECDSASignature sig = key.doSign(input);
        fail("Expecting an MissingPrivateKeyException for a public only ECKey");
    }

    @Test(expected = SignatureException.class)
    public void testBadBase64Sig() throws SignatureException {
        final byte[] messageHash = new byte[32];
        ECKey.signatureToKey(messageHash, "This is not valid Base64!");
        fail("Expecting a SignatureException for invalid Base64");
    }

    @Test(expected = SignatureException.class)
    public void testInvalidSignatureLength() throws SignatureException {
        final byte[] messageHash = new byte[32];
        ECKey.signatureToKey(messageHash, "abcdefg");
        fail("Expecting a SignatureException for invalid signature length");
    }

    @Test
    public void testPublicKeyFromPrivate() {
        final byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, false);
        assertArrayEquals(pubKey, pubFromPriv);
    }

    @Test
    public void testPublicKeyFromPrivateCompressed() {
        final byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, true);
        assertArrayEquals(compressedPubKey, pubFromPriv);
    }

    @Test
    public void testGetAddress() {
        final ECKey key = ECKey.fromPublicOnly(pubKey);
        final String address = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
        assertArrayEquals(Hex.decode(address), key.getAddress());
    }

    @Test
    public void testToString() {
        final ECKey key = ECKey.fromPrivate(BigInteger.TEN); // An example private key.
        assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString());
    }

    @Test
    public void testEthereumSign() throws IOException {
        final ECKey key = ECKey.fromPrivate(privateKey);
        System.out.println("Secret\t: " + Hex.toHexString(key.getPrivKeyBytes()));
        System.out.println("Pubkey\t: " + Hex.toHexString(key.getPubKey()));
        System.out.println("Data\t: " + exampleMessage);
        final byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        final ECDSASignature signature = key.sign(messageHash);
        final String output = signature.toBase64();
        System.out.println("Signtr\t: " + output + " (Base64, length: " + output.length() + ")");
        assertEquals(sigBase64, output);
    }

    /**
     * Verified via https://etherchain.org/verify/signature
     */
    @Test
    public void testEthereumSignToHex() {
        final ECKey key = ECKey.fromPrivate(privateKey);
        final byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        final ECDSASignature signature = key.sign(messageHash);
        final String output = signature.toHex();
        System.out.println("Signature\t: " + output + " (Hex, length: " + output.length() + ")");
        final String signatureHex = "d2ce488f4da29e68f22cb05cac1b19b75df170a12b4ad1bdd4531b8e9115c6fb75c1fe50a95e8ccffcbb5482a1e42fbbdd6324131dfe75c3b3b7f9a7c721eccb01";
        assertEquals(signatureHex, output);
    }

    @Test
    public void testVerifySignature1() {
        final ECKey key = ECKey.fromPublicOnly(pubKey);
        final BigInteger r = new BigInteger("28157690258821599598544026901946453245423343069728565040002908283498585537001");
        final BigInteger s = new BigInteger("30212485197630673222315826773656074299979444367665131281281249560925428307087");
        final ECDSASignature sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 28);
        key.verify(HashUtil.sha3(exampleMessage.getBytes()), sig);
    }

    @Test
    public void testVerifySignature2() {
        final BigInteger r = new BigInteger("c52c114d4f5a3ba904a9b3036e5e118fe0dbb987fe3955da20f2cd8f6c21ab9c", 16);
        final BigInteger s = new BigInteger("6ba4c2874299a55ad947dbc98a25ee895aabf6b625c26c435e84bfd70edf2f69", 16);
        final ECDSASignature sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 0x1b);
        final byte[] rawtx = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13cd947ec05abc7fe734df8dd8268609184e72a0006480");
        final byte[] rawHash = HashUtil.sha3(rawtx);
        final byte[] address = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
        try {
            final ECKey key = ECKey.signatureToKey(rawHash, sig);

            System.out.println("Signature public key\t: " + Hex.toHexString(key.getPubKey()));
            System.out.println("Sender is\t\t: " + Hex.toHexString(key.getAddress()));

            assertEquals(key, ECKey.signatureToKey(rawHash, sig.toBase64()));
            assertEquals(key, ECKey.recoverFromSignature(0, sig, rawHash));
            assertArrayEquals(key.getPubKey(), ECKey.recoverPubBytesFromSignature(0, sig, rawHash));


            assertArrayEquals(address, key.getAddress());
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig));
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig.toBase64()));
            assertArrayEquals(address, ECKey.recoverAddressFromSignature(0, sig, rawHash));

            assertTrue(key.verify(rawHash, sig));
        } catch (final SignatureException e) {
            fail();
        }
    }

    @Test
    public void testVerifySignature3() throws SignatureException {

        final byte[] rawtx = Hex.decode("f88080893635c9adc5dea000008609184e72a00094109f3535353535353535353535353535353535359479b08ad8787060333663d19704909ee7b1903e58801ba0899b92d0c76cbf18df24394996beef19c050baa9823b4a9828cd9b260c97112ea0c9e62eb4cf0a9d95ca35c8830afac567619d6b3ebee841a3c8be61d35acd8049");

        final Transaction tx = new Transaction(rawtx);
        final ECKey key = ECKey.signatureToKey(HashUtil.sha3(rawtx), tx.getSignature());

        System.out.println("Signature public key\t: " + Hex.toHexString(key.getPubKey()));
        System.out.println("Sender is\t\t: " + Hex.toHexString(key.getAddress()));

        //  sender: CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826
        // todo: add test assertion when the sign/verify part actually works.
    }


    @Test
    public void testSValue() throws Exception {
        // Check that we never generate an S value that is larger than half the curve order. This avoids a malleability
        // issue that can allow someone to change a transaction [hash] without invalidating the signature.
        final int ITERATIONS = 10;
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ITERATIONS));
        final List<ListenableFuture<ECKey.ECDSASignature>> sigFutures = Lists.newArrayList();
        final ECKey key = new ECKey();
        for (byte i = 0; i < ITERATIONS; i++) {
            final byte[] hash = HashUtil.sha3(new byte[]{i});
            sigFutures.add(executor.submit(new Callable<ECDSASignature>() {
                @Override
                public ECKey.ECDSASignature call() throws Exception {
                    return key.doSign(hash);
                }
            }));
        }
        final List<ECKey.ECDSASignature> sigs = Futures.allAsList(sigFutures).get();
        for (final ECKey.ECDSASignature signature : sigs) {
            assertTrue(signature.s.compareTo(ECKey.HALF_CURVE_ORDER) <= 0);
        }
        final ECKey.ECDSASignature duplicate = new ECKey.ECDSASignature(sigs.get(0).r, sigs.get(0).s);
        assertEquals(sigs.get(0), duplicate);
        assertEquals(sigs.get(0).hashCode(), duplicate.hashCode());
    }

    @Test
    public void testSignVerify() {
        final ECKey key = ECKey.fromPrivate(privateKey);
        final String message = "This is an example of a signed message.";
        final byte[] input = HashUtil.sha3(message.getBytes());
        final ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    private void testProviderRoundTrip(final Provider provider) throws Exception {
        final ECKey key = new ECKey(provider, secureRandom);
        final String message = "The quick brown fox jumps over the lazy dog.";
        final byte[] input = HashUtil.sha3(message.getBytes());
        final ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    @Test
    public void testSunECRoundTrip() throws Exception {
        final Provider provider = Security.getProvider("SunEC");
        if (provider != null) {
            testProviderRoundTrip(provider);
        } else {
            System.out.println("Skip test as provider doesn't exist. Must be OpenJDK 1.7 which ships without 'SunEC'");
        }
    }

    @Test
    public void testSpongyCastleRoundTrip() throws Exception {
        testProviderRoundTrip(SpongyCastleProvider.getInstance());
    }

    @Test
    public void testIsPubKeyCanonicalCorect() {
        // Test correct prefix 4, right length 65
        final byte[] canonicalPubkey1 = new byte[65];
        canonicalPubkey1[0] = 0x04;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey1));
        // Test correct prefix 2, right length 33
        final byte[] canonicalPubkey2 = new byte[33];
        canonicalPubkey2[0] = 0x02;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey2));
        // Test correct prefix 3, right length 33
        final byte[] canonicalPubkey3 = new byte[33];
        canonicalPubkey3[0] = 0x03;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey3));
    }

    @Test
    public void testIsPubKeyCanonicalWrongLength() {
        // Test correct prefix 4, but wrong length !65
        final byte[] nonCanonicalPubkey1 = new byte[64];
        nonCanonicalPubkey1[0] = 0x04;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey1));
        // Test correct prefix 2, but wrong length !33
        final byte[] nonCanonicalPubkey2 = new byte[32];
        nonCanonicalPubkey2[0] = 0x02;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey2));
        // Test correct prefix 3, but wrong length !33
        final byte[] nonCanonicalPubkey3 = new byte[32];
        nonCanonicalPubkey3[0] = 0x03;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey3));
    }

    @Test
    public void testIsPubKeyCanonicalWrongPrefix() {
        // Test wrong prefix 4, right length 65
        final byte[] nonCanonicalPubkey4 = new byte[65];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey4));
        // Test wrong prefix 2, right length 33
        final byte[] nonCanonicalPubkey5 = new byte[33];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey5));
        // Test wrong prefix 3, right length 33
        final byte[] nonCanonicalPubkey6 = new byte[33];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey6));
    }

    @Test
    public void keyRecovery() throws Exception {
        ECKey key = new ECKey();
        final String message = "Hello World!";
        final byte[] hash = HashUtil.sha256(message.getBytes());
        final ECKey.ECDSASignature sig = key.doSign(hash);
        key = ECKey.fromPublicOnly(key.getPubKeyPoint());
        boolean found = false;
        for (int i = 0; i < 4; i++) {
            final ECKey key2 = ECKey.recoverFromSignature(i, sig, hash);
            checkNotNull(key2);
            if (key.equals(key2)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testSignedMessageToKey() throws SignatureException {
        final byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        final ECKey key = ECKey.signatureToKey(messageHash, sigBase64);
        assertNotNull(key);
        assertArrayEquals(pubKey, key.getPubKey());
    }

    @Test
    public void testGetPrivKeyBytes() {
        final ECKey key = new ECKey();
        assertNotNull(key.getPrivKeyBytes());
        assertEquals(32, key.getPrivKeyBytes().length);
    }

    @Test
    public void testEqualsObject() {
        final ECKey key0 = new ECKey();
        final ECKey key1 = ECKey.fromPrivate(privateKey);
        final ECKey key2 = ECKey.fromPrivate(privateKey);

        assertFalse(key0.equals(key1));
        assertTrue(key1.equals(key1));
        assertTrue(key1.equals(key2));
    }


    @Test
    public void decryptAECSIC(){
        final ECKey key = ECKey.fromPrivate(Hex.decode("abb51256c1324a1350598653f46aa3ad693ac3cf5d05f36eba3f495a1f51590f"));
        final byte[] payload = key.decryptAES(Hex.decode("84a727bc81fa4b13947dc9728b88fd08"));
        System.out.println(Hex.toHexString(payload));
    }

    @Test
    public void testNodeId() {
        final ECKey key = ECKey.fromPublicOnly(pubKey);

        assertEquals(key, ECKey.fromNodeId(key.getNodeId()));
    }
}
