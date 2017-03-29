package org.ethereum.core;

import java.math.BigInteger;

public enum Denomination {

    WEI(newBigInt(0)),
    SZABO(newBigInt(12)),
    FINNEY(newBigInt(15)),
    ETHER(newBigInt(18));

    private final BigInteger amount;

    Denomination(BigInteger value) {
        this.amount = value;
    }

    private static BigInteger newBigInt(int value) {
        return BigInteger.valueOf(10).pow(value);
    }

    public static String toFriendlyString(BigInteger value) {
        if (value.compareTo(ETHER.value()) == 1 || value.compareTo(ETHER.value()) == 0) {
            return Float.toString(value.divide(ETHER.value()).floatValue()) +  " ETHER";
        }
        else if(value.compareTo(FINNEY.value()) == 1 || value.compareTo(FINNEY.value()) == 0) {
            return Float.toString(value.divide(FINNEY.value()).floatValue()) +  " FINNEY";
        }
        else if(value.compareTo(SZABO.value()) == 1 || value.compareTo(SZABO.value()) == 0) {
            return Float.toString(value.divide(SZABO.value()).floatValue()) +  " SZABO";
        }
        else
            return Float.toString(value.divide(WEI.value()).floatValue()) +  " WEI";
    }

    public BigInteger value() {
        return amount;
    }

    public long longValue() {
        return value().longValue();
    }
}
