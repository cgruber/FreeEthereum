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

package org.ethereum.net.rlpx;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.ethereum.crypto.ECIESCoder;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.math.ec.ECPoint;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static org.ethereum.crypto.HashUtil.sha3;

public class EncryptionHandshake {
    private static final int NONCE_SIZE = 32;
    private static final int MAC_SIZE = 256;
    private static final int SECRET_SIZE = 32;
    private final SecureRandom random = new SecureRandom();
    private final boolean isInitiator;
    private final ECKey ephemeralKey;
    private ECPoint remotePublicKey;
    private ECPoint remoteEphemeralKey;
    private byte[] initiatorNonce;
    private byte[] responderNonce;
    private Secrets secrets;

    public EncryptionHandshake(final ECPoint remotePublicKey) {
        this.remotePublicKey = remotePublicKey;
        ephemeralKey = new ECKey(random);
        initiatorNonce = new byte[NONCE_SIZE];
        random.nextBytes(initiatorNonce);
        isInitiator = true;
    }

    EncryptionHandshake(final ECPoint remotePublicKey, final ECKey ephemeralKey, final byte[] initiatorNonce, final byte[] responderNonce, final boolean isInitiator) {
        this.remotePublicKey = remotePublicKey;
        this.ephemeralKey = ephemeralKey;
        this.initiatorNonce = initiatorNonce;
        this.responderNonce = responderNonce;
        this.isInitiator = isInitiator;
    }

    public EncryptionHandshake() {
        ephemeralKey = new ECKey(random);
        responderNonce = new byte[NONCE_SIZE];
        random.nextBytes(responderNonce);
        isInitiator = false;
    }

    private static byte[] xor(final byte[] b1, final byte[] b2) {
        Preconditions.checkArgument(b1.length == b2.length);
        final byte[] out = new byte[b1.length];
        for (int i = 0; i < b1.length; i++) {
            out[i] = (byte) (b1[i] ^ b2[i]);
        }
        return out;
    }

    static public byte recIdFromSignatureV(int v) {
        if (v >= 31) {
            // compressed
            v -= 4;
        }
        return (byte) (v - 27);
    }

    /**
     * Create a handshake auth message defined by EIP-8
     *
     * @param key our private key
     */
    public AuthInitiateMessageV4 createAuthInitiateV4(final ECKey key) {
        final AuthInitiateMessageV4 message = new AuthInitiateMessageV4();

        final BigInteger secretScalar = key.keyAgreement(remotePublicKey);
        final byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);

        final byte[] nonce = initiatorNonce;
        final byte[] signed = xor(token, nonce);
        message.signature = ephemeralKey.sign(signed);
        message.publicKey = key.getPubKeyPoint();
        message.nonce = initiatorNonce;
        return message;
    }

    public byte[] encryptAuthInitiateV4(final AuthInitiateMessageV4 message) {

        final byte[] msg = message.encode();
        final byte[] padded = padEip8(msg);

        return encryptAuthEIP8(padded);
    }

    public AuthInitiateMessageV4 decryptAuthInitiateV4(final byte[] in, final ECKey myKey) throws InvalidCipherTextException {
        try {

            final byte[] prefix = new byte[2];
            System.arraycopy(in, 0, prefix, 0, 2);
            final short size = ByteUtil.bigEndianToShort(prefix, 0);
            final byte[] ciphertext = new byte[size];
            System.arraycopy(in, 2, ciphertext, 0, size);

            final byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext, prefix);

            return AuthInitiateMessageV4.decode(plaintext);
        } catch (final InvalidCipherTextException e) {
            throw e;
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public byte[] encryptAuthResponseV4(final AuthResponseMessageV4 message) {

        final byte[] msg = message.encode();
        final byte[] padded = padEip8(msg);

        return encryptAuthEIP8(padded);
    }

    public AuthResponseMessageV4 decryptAuthResponseV4(final byte[] in, final ECKey myKey) {
        try {

            final byte[] prefix = new byte[2];
            System.arraycopy(in, 0, prefix, 0, 2);
            final short size = ByteUtil.bigEndianToShort(prefix, 0);
            final byte[] ciphertext = new byte[size];
            System.arraycopy(in, 2, ciphertext, 0, size);

            final byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext, prefix);

            return AuthResponseMessageV4.decode(plaintext);
        } catch (IOException | InvalidCipherTextException e) {
            throw Throwables.propagate(e);
        }
    }

    AuthResponseMessageV4 makeAuthInitiateV4(final AuthInitiateMessageV4 initiate, final ECKey key) {
        initiatorNonce = initiate.nonce;
        remotePublicKey = initiate.publicKey;

        final BigInteger secretScalar = key.keyAgreement(remotePublicKey);

        final byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        final byte[] signed = xor(token, initiatorNonce);

        final ECKey ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiate.signature.v),
                initiate.signature, signed);
        if (ephemeral == null) {
            throw new RuntimeException("failed to recover signatue from message");
        }
        remoteEphemeralKey = ephemeral.getPubKeyPoint();
        final AuthResponseMessageV4 response = new AuthResponseMessageV4();
        response.ephemeralPublicKey = ephemeralKey.getPubKeyPoint();
        response.nonce = responderNonce;
        return response;
    }

    public AuthResponseMessageV4 handleAuthResponseV4(final ECKey myKey, final byte[] initiatePacket, final byte[] responsePacket) {
        final AuthResponseMessageV4 response = decryptAuthResponseV4(responsePacket, myKey);
        remoteEphemeralKey = response.ephemeralPublicKey;
        responderNonce = response.nonce;
        agreeSecret(initiatePacket, responsePacket);
        return response;
    }

    private byte[] encryptAuthEIP8(final byte[] msg) {

        final short size = (short) (msg.length + ECIESCoder.getOverhead());
        final byte[] prefix = ByteUtil.shortToBytes(size);
        final byte[] encrypted = ECIESCoder.encrypt(remotePublicKey, msg, prefix);

        final byte[] out = new byte[prefix.length + encrypted.length];
        int offset = 0;
        System.arraycopy(prefix, 0, out, offset, prefix.length);
        offset += prefix.length;
        System.arraycopy(encrypted, 0, out, offset, encrypted.length);

        return out;
    }

    /**
     * Create a handshake auth message
     *
     * @param token previous token if we had a previous session
     * @param key our private key
     */
    public AuthInitiateMessage createAuthInitiate(@Nullable byte[] token, final ECKey key) {
        final AuthInitiateMessage message = new AuthInitiateMessage();
        final boolean isToken;
        if (token == null) {
            isToken = false;
            final BigInteger secretScalar = key.keyAgreement(remotePublicKey);
            token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        } else {
            isToken = true;
        }

        final byte[] nonce = initiatorNonce;
        final byte[] signed = xor(token, nonce);
        message.signature = ephemeralKey.sign(signed);
        message.isTokenUsed = isToken;
        message.ephemeralPublicHash = sha3(ephemeralKey.getPubKey(), 1, 64);
        message.publicKey = key.getPubKeyPoint();
        message.nonce = initiatorNonce;
        return message;
    }

    public byte[] encryptAuthMessage(final AuthInitiateMessage message) {
        return ECIESCoder.encrypt(remotePublicKey, message.encode());
    }

    public byte[] encryptAuthResponse(final AuthResponseMessage message) {
        return ECIESCoder.encrypt(remotePublicKey, message.encode());
    }

    private AuthResponseMessage decryptAuthResponse(final byte[] ciphertext, final ECKey myKey) {
        try {
            final byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext);
            return AuthResponseMessage.decode(plaintext);
        } catch (IOException | InvalidCipherTextException e) {
            throw Throwables.propagate(e);
        }
    }

    public AuthInitiateMessage decryptAuthInitiate(final byte[] ciphertext, final ECKey myKey) throws InvalidCipherTextException {
        try {
            final byte[] plaintext = ECIESCoder.decrypt(myKey.getPrivKey(), ciphertext);
            return AuthInitiateMessage.decode(plaintext);
        } catch (final InvalidCipherTextException e) {
            throw e;
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public AuthResponseMessage handleAuthResponse(final ECKey myKey, final byte[] initiatePacket, final byte[] responsePacket) {
        final AuthResponseMessage response = decryptAuthResponse(responsePacket, myKey);
        remoteEphemeralKey = response.ephemeralPublicKey;
        responderNonce = response.nonce;
        agreeSecret(initiatePacket, responsePacket);
        return response;
    }

    void agreeSecret(final byte[] initiatePacket, final byte[] responsePacket) {
        final BigInteger secretScalar = ephemeralKey.keyAgreement(remoteEphemeralKey);
        final byte[] agreedSecret = ByteUtil.bigIntegerToBytes(secretScalar, SECRET_SIZE);
        final byte[] sharedSecret = sha3(agreedSecret, sha3(responderNonce, initiatorNonce));
        final byte[] aesSecret = sha3(agreedSecret, sharedSecret);
        secrets = new Secrets();
        secrets.aes = aesSecret;
        secrets.mac = sha3(agreedSecret, aesSecret);
        secrets.token = sha3(sharedSecret);
//        System.out.println("mac " + Hex.toHexString(secrets.mac));
//        System.out.println("aes " + Hex.toHexString(secrets.aes));
//        System.out.println("shared " + Hex.toHexString(sharedSecret));
//        System.out.println("ecdhe " + Hex.toHexString(agreedSecret));

        final KeccakDigest mac1 = new KeccakDigest(MAC_SIZE);
        mac1.update(xor(secrets.mac, responderNonce), 0, secrets.mac.length);
        final byte[] buf = new byte[32];
        new KeccakDigest(mac1).doFinal(buf, 0);
        mac1.update(initiatePacket, 0, initiatePacket.length);
        new KeccakDigest(mac1).doFinal(buf, 0);
        final KeccakDigest mac2 = new KeccakDigest(MAC_SIZE);
        mac2.update(xor(secrets.mac, initiatorNonce), 0, secrets.mac.length);
        new KeccakDigest(mac2).doFinal(buf, 0);
        mac2.update(responsePacket, 0, responsePacket.length);
        new KeccakDigest(mac2).doFinal(buf, 0);
        if (isInitiator) {
            secrets.egressMac = mac1;
            secrets.ingressMac = mac2;
        } else {
            secrets.egressMac = mac2;
            secrets.ingressMac = mac1;
        }
    }

    public byte[] handleAuthInitiate(final byte[] initiatePacket, final ECKey key) throws InvalidCipherTextException {
        final AuthResponseMessage response = makeAuthInitiate(initiatePacket, key);
        final byte[] responsePacket = encryptAuthResponse(response);
        agreeSecret(initiatePacket, responsePacket);
        return responsePacket;
    }

    private AuthResponseMessage makeAuthInitiate(final byte[] initiatePacket, final ECKey key) throws InvalidCipherTextException {
        final AuthInitiateMessage initiate = decryptAuthInitiate(initiatePacket, key);
        return makeAuthInitiate(initiate, key);
    }

    AuthResponseMessage makeAuthInitiate(final AuthInitiateMessage initiate, final ECKey key) {
        initiatorNonce = initiate.nonce;
        remotePublicKey = initiate.publicKey;
        final BigInteger secretScalar = key.keyAgreement(remotePublicKey);
        final byte[] token = ByteUtil.bigIntegerToBytes(secretScalar, NONCE_SIZE);
        final byte[] signed = xor(token, initiatorNonce);

        final ECKey ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiate.signature.v),
                initiate.signature, signed);
        if (ephemeral == null) {
            throw new RuntimeException("failed to recover signatue from message");
        }
        remoteEphemeralKey = ephemeral.getPubKeyPoint();
        final AuthResponseMessage response = new AuthResponseMessage();
        response.isTokenUsed = initiate.isTokenUsed;
        response.ephemeralPublicKey = ephemeralKey.getPubKeyPoint();
        response.nonce = responderNonce;
        return response;
    }

    /**
     * Pads messages with junk data,
     * pad data length is random value satisfying 100 < len < 300.
     * It's necessary to make messages described by EIP-8 distinguishable from pre-EIP-8 msgs
     *
     * @param msg message to pad
     * @return padded message
     */
    private byte[] padEip8(final byte[] msg) {

        final byte[] paddedMessage = new byte[msg.length + random.nextInt(200) + 100];
        random.nextBytes(paddedMessage);
        System.arraycopy(msg, 0, paddedMessage, 0, msg.length);

        return paddedMessage;
    }

    public Secrets getSecrets() {
        return secrets;
    }

    public ECPoint getRemotePublicKey() {
        return remotePublicKey;
    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public static class Secrets {
        byte[] aes;
        byte[] mac;
        byte[] token;
        KeccakDigest egressMac;
        KeccakDigest ingressMac;

        public byte[] getAes() {
            return aes;
        }

        public byte[] getMac() {
            return mac;
        }

        public byte[] getToken() {
            return token;
        }

        public KeccakDigest getIngressMac() {
            return ingressMac;
        }

        public KeccakDigest getEgressMac() {
            return egressMac;
        }
    }
}
