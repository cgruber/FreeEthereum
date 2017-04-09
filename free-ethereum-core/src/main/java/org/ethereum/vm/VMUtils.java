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

package org.ethereum.vm;

import org.ethereum.config.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.springframework.util.StringUtils.isEmpty;

public final class VMUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("VM");
    private static final int BUF_SIZE = 4096;

    private VMUtils() {
    }

    private static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }

    private static File createProgramTraceFile(final SystemProperties config, final String txHash) {
        File result = null;

        if (config.vmTrace() && !isEmpty(config.vmTraceDir())) {

            final File file = new File(new File(config.databaseDir(), config.vmTraceDir()), txHash + ".json");

            if (file.exists()) {
                if (file.isFile() && file.canWrite()) {
                    result = file;
                }
            } else {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    result = file;
                } catch (final IOException e) {
                    // ignored
                }
            }
        }

        return result;
    }

    private static void writeStringToFile(final File file, final String data) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (data != null) {
                out.write(data.getBytes("UTF-8"));
            }
        } catch (final Exception e) {
            LOGGER.error(format("Cannot write to file '%s': ", file.getAbsolutePath()), e);
        } finally {
            closeQuietly(out);
        }
    }

    public static void saveProgramTraceFile(final SystemProperties config, final String txHash, final String content) {
        final File file = createProgramTraceFile(config, txHash);
        if (file != null) {
            writeStringToFile(file, content);
        }
    }

    private static void write(final InputStream in, final OutputStream out, final int bufSize) throws IOException {
        try {
            final byte[] buf = new byte[bufSize];
            for (int count = in.read(buf); count != -1; count = in.read(buf)) {
                out.write(buf, 0, count);
            }
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private static byte[] compress(final byte[] bytes) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        final DeflaterOutputStream out = new DeflaterOutputStream(baos, new Deflater(), BUF_SIZE);

        write(in, out, BUF_SIZE);

        return baos.toByteArray();
    }

    private static byte[] compress(final String content) throws IOException {
        return compress(content.getBytes("UTF-8"));
    }

    private static byte[] decompress(final byte[] data) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);

        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        final InflaterOutputStream out = new InflaterOutputStream(baos, new Inflater(), BUF_SIZE);

        write(in, out, BUF_SIZE);

        return baos.toByteArray();
    }

    public static String zipAndEncode(final String content) {
        try {
            return encodeBase64String(compress(content));
        } catch (final Exception e) {
            LOGGER.error("Cannot zip or encode: ", e);
            return content;
        }
    }

    public static String unzipAndDecode(final String content) {
        try {
            final byte[] decoded = decodeBase64(content);
            return new String(decompress(decoded), "UTF-8");
        } catch (final Exception e) {
            LOGGER.error("Cannot unzip or decode: ", e);
            return content;
        }
    }
}
