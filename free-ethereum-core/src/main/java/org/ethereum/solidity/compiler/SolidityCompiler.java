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

package org.ethereum.solidity.compiler;

import com.google.common.base.Joiner;
import org.ethereum.config.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SolidityCompiler {

    private static SolidityCompiler INSTANCE;
    private final Solc solc;

    @Autowired
    public SolidityCompiler(final SystemProperties config) {
        solc = new Solc(config);
    }

    public static Result compile(final File sourceDirectory, final boolean combinedJson, final Options... options) throws IOException {
        return getInstance().compileSrc(sourceDirectory, false, combinedJson, options);
    }

    public static Result compile(final byte[] source, final boolean combinedJson, final Options... options) throws IOException {
        return getInstance().compileSrc(source, false, combinedJson, options);
    }

    public static String runGetVersionOutput() throws IOException {
        final List<String> commandParts = new ArrayList<>();
        commandParts.add(getInstance().solc.getExecutable().getCanonicalPath());
        commandParts.add("--version");

        final ProcessBuilder processBuilder = new ProcessBuilder(commandParts)
                .directory(getInstance().solc.getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH",
                getInstance().solc.getExecutable().getParentFile().getCanonicalPath());

        final Process process = processBuilder.start();

        final ParallelReader error = new ParallelReader(process.getErrorStream());
        final ParallelReader output = new ParallelReader(process.getInputStream());
        error.start();
        output.start();

        try {
            process.waitFor();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (process.exitValue() == 0) {
            return output.getContent();
        }

        throw new RuntimeException("Problem getting solc version: " + error.getContent());
    }

    private static SolidityCompiler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SolidityCompiler(SystemProperties.getDefault());
        }
        return INSTANCE;
    }

    private Result compileSrc(final File source, final boolean optimize, final boolean combinedJson, final Options... options) throws IOException {
        final List<String> commandParts = prepareCommandOptions(optimize, combinedJson, options);

        commandParts.add(source.getAbsolutePath());

        final ProcessBuilder processBuilder = new ProcessBuilder(commandParts)
                .directory(solc.getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH",
                solc.getExecutable().getParentFile().getCanonicalPath());

        final Process process = processBuilder.start();

        final ParallelReader error = new ParallelReader(process.getErrorStream());
        final ParallelReader output = new ParallelReader(process.getInputStream());
        error.start();
        output.start();

        try {
            process.waitFor();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        final boolean success = process.exitValue() == 0;

        return new Result(error.getContent(), output.getContent(), success);
    }

    private List<String> prepareCommandOptions(final boolean optimize, final boolean combinedJson, final Options[] options) throws IOException {
        final List<String> commandParts = new ArrayList<>();
        commandParts.add(solc.getExecutable().getCanonicalPath());
        if (optimize) {
            commandParts.add("--optimize");
        }
        if (combinedJson) {
            commandParts.add("--combined-json");
            commandParts.add(Joiner.on(',').join(options));
        } else {
            for (final Options option : options) {
                commandParts.add("--" + option.getName());
            }
        }
        return commandParts;
    }

    public Result compileSrc(final byte[] source, final boolean optimize, final boolean combinedJson, final Options... options) throws IOException {
        final List<String> commandParts = prepareCommandOptions(optimize, combinedJson, options);

        final ProcessBuilder processBuilder = new ProcessBuilder(commandParts)
                .directory(solc.getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH",
                solc.getExecutable().getParentFile().getCanonicalPath());

        final Process process = processBuilder.start();

        try (BufferedOutputStream stream = new BufferedOutputStream(process.getOutputStream())) {
            stream.write(source);
        }

        final ParallelReader error = new ParallelReader(process.getErrorStream());
        final ParallelReader output = new ParallelReader(process.getInputStream());
        error.start();
        output.start();

        try {
            process.waitFor();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        final boolean success = process.exitValue() == 0;

        return new Result(error.getContent(), output.getContent(), success);
    }

    public enum Options {
        AST("ast"),
        BIN("bin"),
        INTERFACE("interface"),
        ABI("abi"),
        METADATA("metadata");

        private final String name;

        Options(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Result {
        public final String errors;
        public final String output;
        private boolean success = false;

        public Result(final String errors, final String output, final boolean success) {
            this.errors = errors;
            this.output = output;
            this.success = success;
        }

        public boolean isFailed() {
            return !success;
        }
    }

    private static class ParallelReader extends Thread {

        private final StringBuilder content = new StringBuilder();
        private InputStream stream;

        ParallelReader(final InputStream stream) {
            this.stream = stream;
        }

        public String getContent() {
            return getContent(true);
        }

        public synchronized String getContent(final boolean waitForComplete) {
            if (waitForComplete) {
                while (stream != null) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return content.toString();
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            } finally {
                synchronized (this) {
                    stream = null;
                    notifyAll();
                }
            }
        }
    }
}
