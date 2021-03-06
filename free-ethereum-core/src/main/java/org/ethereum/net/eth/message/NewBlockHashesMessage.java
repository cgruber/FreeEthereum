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

package org.ethereum.net.eth.message;

import org.ethereum.core.BlockIdentifier;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum NewBlockHashes message on the network<br>
 *
 * @see EthMessageCodes#NEW_BLOCK_HASHES
 *
 * @author Mikhail Kalinin
 * @since 05.09.2015
 */
public class NewBlockHashesMessage extends EthMessage {

    /**
     * List of identifiers holding hash and number of the blocks
     */
    private List<BlockIdentifier> blockIdentifiers;

    public NewBlockHashesMessage(final byte[] payload) {
        super(payload);
    }

    public NewBlockHashesMessage(final List<BlockIdentifier> blockIdentifiers) {
        this.blockIdentifiers = blockIdentifiers;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockIdentifiers = new ArrayList<>();
        for (final RLPElement aParamsList : paramsList) {
            final RLPList rlpData = ((RLPList) aParamsList);
            blockIdentifiers.add(new BlockIdentifier(rlpData));
        }
        parsed = true;
    }

    private void encode() {
        final List<byte[]> encodedElements = new ArrayList<>();
        for (final BlockIdentifier identifier : blockIdentifiers)
            encodedElements.add(identifier.getEncoded());
        final byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public List<BlockIdentifier> getBlockIdentifiers() {
        parse();
        return blockIdentifiers;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.NEW_BLOCK_HASHES;
    }

    @Override
    public String toString() {
        parse();

        return "[" + this.getCommand().name() + "] (" + blockIdentifiers.size() + ")";
    }

}
