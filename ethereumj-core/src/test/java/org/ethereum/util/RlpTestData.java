package org.ethereum.util;

import java.math.BigInteger;

class RlpTestData {

    /***********************************
     * https://github.com/ethereum/tests/blob/master/rlptest.txt
     */
    public static final int test01 = 0;
    public static final String result01 = "80";

    public static final String test02 = "";
    public static final String result02 = "80";

    public static final String test03 = "d";
    public static final String result03 = "64";

    public static final String test04 = "cat";
    public static final String result04 = "83636174";

    public static final String test05 = "dog";
    public static final String result05 = "83646f67";

    public static final String[] test06 = new String[]{"cat", "dog"};
    public static final String result06 = "c88363617483646f67";

    public static final String[] test07 = new String[]{"dog", "god", "cat"};
    public static final String result07 = "cc83646f6783676f6483636174";

    public static final int test08 = 1;
    public static final String result08 = "01";

    public static final int test09 = 10;
    public static final String result09 = "0a";

    public static final int test10 = 100;
    public static final String result10 = "64";

    public static final int test11 = 1000;
    public static final String result11 = "8203e8";

    public static final BigInteger test12 = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935");
    public static final String result12 = "a0ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    public static final BigInteger test13 = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936");
    public static final String result13 = "a1010000000000000000000000000000000000000000000000000000000000000000";

    public static final Object[] test14 = new Object[]{1, 2, new Object[]{}};
    public static final String result14 = "c30102c0";
    public static final Object[] expected14 = new Object[]{new byte[]{1}, new byte[]{2}, new Object[]{}};

    public static final Object[] test15 = new Object[]{new Object[]{new Object[]{}, new Object[]{}}, new Object[]{}};
    public static final String result15 = "c4c2c0c0c0";

    public static final Object[] test16 = new Object[]{"zw", new Object[]{4}, "wz"};
    public static final String result16 = "c8827a77c10482777a";
    public static final Object[] expected16 = new Object[]{new byte[]{122, 119}, new Object[]{new byte[]{4}}, new byte[]{119, 122}};
}
