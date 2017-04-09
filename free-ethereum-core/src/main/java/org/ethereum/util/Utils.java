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

package org.ethereum.util;

import org.ethereum.datasource.DbSource;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.vm.DataWord;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import javax.swing.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {
    private static final DataWord DIVISOR = new DataWord(64);
    private static final BigInteger _1000_ = new BigInteger("1000");
    private static final SecureRandom random = new SecureRandom();
    public static double JAVA_VERSION = getJavaVersion();

    /**
     * @param number should be in form '0x34fabd34....'
     * @return String
     */
    public static BigInteger unifiedNumericToBigInteger(String number) {

        final boolean match = Pattern.matches("0[xX][0-9a-fA-F]+", number);
        if (!match)
            return (new BigInteger(number));
        else{
            number = number.substring(2);
            number = number.length() % 2 != 0 ? "0".concat(number) : number;
            final byte[] numberBytes = Hex.decode(number);
            return (new BigInteger(1, numberBytes));
        }
    }

    /**
     * Return formatted Date String: yyyy.MM.dd HH:mm:ss
     * Based on Unix's time() input in seconds
     *
     * @param timestamp seconds since start of Unix-time
     * @return String formatted as - yyyy.MM.dd HH:mm:ss
     */
    public static String longToDateTime(final long timestamp) {
        final Date date = new Date(timestamp * 1000);
        final DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        return formatter.format(date);
    }

    public static String longToTimePeriod(final long msec) {
        if (msec < 1000) return msec + "ms";
        if (msec < 3000) return String.format("%.2f", msec / 1000d);
        if (msec < 60 * 1000) return (msec / 1000) + "s";
        final long sec = msec / 1000;
        if (sec < 5 * 60) return (sec / 60) +  "m" + (sec % 60) + "s";
        final long min = sec / 60;
        if (min < 60) return min + "m";
        final long hour = min / 60;
        if (min < 24 * 60) return hour + "h" + (min % 60) + "m";
        final long day = hour / 24;
        return day + "d" + (day % 24) + "h";
    }

    public static ImageIcon getImageIcon(final String resource) {
        final URL imageURL = ClassLoader.getSystemResource(resource);
        final ImageIcon image = new ImageIcon(imageURL);
        return image;
    }

    public static String getValueShortString(final BigInteger number) {
        BigInteger result = number;
        int pow = 0;
        while (result.compareTo(_1000_) == 1 || result.compareTo(_1000_) == 0) {
            result = result.divide(_1000_);
            pow += 3;
        }
        return result.toString() + "\u00b7(" + "10^" + pow + ")";
    }

    /**
     * Decodes a hex string to address bytes and checks validity
     *
     * @param hex - a hex string of the address, e.g., 6c386a4b26f73c802f34673f7248bb118f97424a
     * @return - decode and validated address byte[]
     */
    public static byte[] addressStringToBytes(final String hex) {
        final byte[] addr;
        try {
            addr = Hex.decode(hex);
        } catch (final DecoderException addressIsNotValid) {
            return null;
        }

        if (isValidAddress(addr))
            return addr;
        return null;
    }

    private static boolean isValidAddress(final byte[] addr) {
        return addr != null && addr.length == 20;
    }

    /**
     * @param addr length should be 20
     * @return short string represent 1f21c...
     */
    public static String getAddressShortString(final byte[] addr) {

        if (!isValidAddress(addr)) throw new Error("not an address");

        final String addrShort = Hex.toHexString(addr, 0, 3);

        final String sb = addrShort +
                "...";

        return sb;
    }

    public static SecureRandom getRandom() {
        return random;
    }

    private static double getJavaVersion() {
        final String version = System.getProperty("java.version");

        // on android this property equals to 0
        if (version.equals("0")) return 0;

        int pos = 0, count = 0;
        for (; pos < version.length() && count < 2; pos++) {
            if (version.charAt(pos) == '.') count++;
        }
        return Double.parseDouble(version.substring(0, pos - 1));
    }

    public static String getHashListShort(final List<byte[]> blockHashes) {
        if (blockHashes.isEmpty()) return "[]";

        final StringBuilder sb = new StringBuilder();
        final String firstHash = Hex.toHexString(blockHashes.get(0));
        final String lastHash = Hex.toHexString(blockHashes.get(blockHashes.size() - 1));
        return sb.append(" ").append(firstHash).append("...").append(lastHash).toString();
    }

    public static String getNodeIdShort(final String nodeId) {
        return nodeId == null ? "<null>" : nodeId.substring(0, 8);
    }

    public static long toUnixTime(final long javaTime) {
        return javaTime / 1000;
    }

    public static long fromUnixTime(final long unixTime) {
        return unixTime * 1000;
    }

    @SafeVarargs
    public static <T> T[] mergeArrays(final T[]... arr) {
        int size = 0;
        for (final T[] ts : arr) {
            size += ts.length;
        }
        final T[] ret = (T[]) Array.newInstance(arr[0].getClass().getComponentType(), size);
        int off = 0;
        for (final T[] ts : arr) {
            System.arraycopy(ts, 0, ret, off, ts.length);
            off += ts.length;
        }
        return ret;
    }

    public static String align(final String s, final char fillChar, final int targetLen, final boolean alignRight) {
        if (targetLen <= s.length()) return s;
        final String alignString = repeat("" + fillChar, targetLen - s.length());
        return alignRight ? alignString + s : s + alignString;

    }

    public static String repeat(final String s, final int n) {
        if (s.length() == 1) {
            final byte[] bb = new byte[n];
            Arrays.fill(bb, s.getBytes()[0]);
            return new String(bb);
        } else {
            final StringBuilder ret = new StringBuilder();
            for (int i = 0; i < n; i++) ret.append(s);
            return ret.toString();
        }
    }

    public static List<ByteArrayWrapper> dumpKeys(final DbSource<byte[]> ds) {

        final ArrayList<ByteArrayWrapper> keys = new ArrayList<>();

        for (final byte[] key : ds.keys()) {
            keys.add(ByteUtil.wrap(key));
        }
        Collections.sort(keys);
        return keys;
    }

    public static DataWord allButOne64th(final DataWord dw) {
        final DataWord ret = dw.clone();
        final DataWord d = dw.clone();
        d.div(DIVISOR);
        ret.sub(d);
        return ret;
    }

    /**
     * Show std err messages in red and throw RuntimeException to stop execution.
     */
    public static void showErrorAndExit(final String message, final String... messages) {
        LoggerFactory.getLogger("general").error(message);
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_RESET = "\u001B[0m";

        System.err.println(ANSI_RED);
        System.err.println("");
        System.err.println("        " + message);
        for (final String msg : messages) {
            System.err.println("        " + msg);
        }
        System.err.println("");
        System.err.println(ANSI_RESET);

        throw new RuntimeException(message);
    }

    public static String sizeToStr(final long size) {
        if (size < 2 * (1L << 10)) return size + "b";
        if (size < 2 * (1L << 20)) return String.format("%dKb", size / (1L << 10));
        if (size < 2 * (1L << 30)) return String.format("%dMb", size / (1L << 20));
        return String.format("%dGb", size / (1L << 30));
    }

    public static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}