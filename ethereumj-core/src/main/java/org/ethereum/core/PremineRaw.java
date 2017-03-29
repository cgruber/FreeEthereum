package org.ethereum.core;

import java.math.BigInteger;

class PremineRaw {

    private final byte[] addr;
    private final BigInteger value;
    private final Denomination denomination;

    public PremineRaw(byte[] addr, BigInteger value, Denomination denomination) {
        this.addr = addr;
        this.value = value;
        this.denomination = denomination;
    }

    public byte[] getAddr() {
        return addr;
    }

    public BigInteger getValue() {
        return value;
    }

    public Denomination getDenomination() {
        return denomination;
    }
}
