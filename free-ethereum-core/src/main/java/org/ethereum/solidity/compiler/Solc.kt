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

package org.ethereum.solidity.compiler

import org.ethereum.config.SystemProperties
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal class Solc(config: SystemProperties) {

    var executable: File? = null
        private set

    init {
        try {
            init(config)
        } catch (e: IOException) {
            throw RuntimeException("Can't init solc compiler: ", e)
        }

    }

    @Throws(IOException::class)
    private fun init(config: SystemProperties?) {
        if (config != null && config.customSolcPath() != null) {
            executable = File(config.customSolcPath())
            if (!executable!!.canExecute()) {
                throw RuntimeException(String.format(
                        "Solidity compiler from config solc.path: %s is not a valid executable",
                        config.customSolcPath()
                ))
            }
        } else {
            initBundled()
        }
    }

    @Throws(IOException::class)
    private fun initBundled() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "solc")
        tmpDir.mkdirs()

        val `is` = javaClass.getResourceAsStream("/native/$os/solc/file.list")
        val scanner = Scanner(`is`)
        while (scanner.hasNext()) {
            val s = scanner.next()
            val targetFile = File(tmpDir, s)
            val fis = javaClass.getResourceAsStream("/native/$os/solc/$s")
            Files.copy(fis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (executable == null) {
                // first file in the list denotes executable
                executable = targetFile
                executable!!.setExecutable(true)
            }
            targetFile.deleteOnExit()
        }
    }

    private val os: String
        get() {
            val osName = System.getProperty("os.name").toLowerCase()
            if (osName.contains("win")) {
                return "win"
            } else if (osName.contains("linux")) {
                return "linux"
            } else if (osName.contains("mac")) {
                return "mac"
            } else {
                throw RuntimeException("Can't find solc compiler: unrecognized OS: " + osName)
            }
        }
}
