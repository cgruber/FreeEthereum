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

import org.ethereum.config.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

class Solc {

    private File solc = null;

    Solc(final SystemProperties config) {
        try {
            init(config);
        } catch (final IOException e) {
            throw new RuntimeException("Can't init solc compiler: ", e);
        }
    }

    private void init(final SystemProperties config) throws IOException {
        if (config != null && config.customSolcPath() != null) {
            solc = new File(config.customSolcPath());
            if (!solc.canExecute()) {
                throw new RuntimeException(String.format(
                        "Solidity compiler from config solc.path: %s is not a valid executable",
                        config.customSolcPath()
                ));
            }
        } else {
            initBundled();
        }
    }

    private void initBundled() throws IOException {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "solc");
        tmpDir.mkdirs();

        final InputStream is = getClass().getResourceAsStream("/native/" + getOS() + "/solc/file.list");
        final Scanner scanner = new Scanner(is);
        while (scanner.hasNext()) {
            final String s = scanner.next();
            final File targetFile = new File(tmpDir, s);
            final InputStream fis = getClass().getResourceAsStream("/native/" + getOS() + "/solc/" + s);
            Files.copy(fis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (solc == null) {
                // first file in the list denotes executable
                solc = targetFile;
                solc.setExecutable(true);
            }
            targetFile.deleteOnExit();
        }
    }

    private String getOS() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac")) {
            return "mac";
        } else {
            throw new RuntimeException("Can't find solc compiler: unrecognized OS: " + osName);
        }
    }

    public File getExecutable() {
        return solc;
    }
}
