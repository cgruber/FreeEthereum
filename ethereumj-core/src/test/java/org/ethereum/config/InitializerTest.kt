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

package org.ethereum.config

import com.google.common.io.Files
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.ethereum.config.Initializer.DatabaseVersionHandler.Behavior
import org.ethereum.config.Initializer.DatabaseVersionHandler.Behavior.*
import org.ethereum.util.FileUtil
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Created by Stan Reshetnyk on 11.09.16.
 */
class InitializerTest {

    private val resetHelper = Initializer.DatabaseVersionHandler()

    private var tempFile: File? = null
    private var databaseDir: String? = null
    private var versionFile: File? = null

    @Before
    fun before() {
        tempFile = Files.createTempDir()
        databaseDir = tempFile!!.absolutePath + "/database"
        versionFile = File(databaseDir!! + "/version.properties")
    }

    @After
    fun after() {
        FileUtil.recursiveDelete(tempFile!!.absolutePath)
    }

    @Test
    fun helper_shouldAllowCleanWorkspace() {
        val props = withConfig(2, null)

        resetHelper.process(props)
        assertEquals(2, resetHelper.getDatabaseVersion(versionFile))
        resetHelper.process(props)
    }

    @Test
    fun helper_shouldCreateVersionFile() {
        val props = withConfig(1, null)

        // state without database
        assertEquals(-1, resetHelper.getDatabaseVersion(versionFile))
        assertTrue(!resetHelper.isDatabaseDirectoryExists(props))

        // create database version file
        resetHelper.process(props)

        // state with just created database
        assertEquals(1, resetHelper.getDatabaseVersion(versionFile))
        assertTrue(resetHelper.isDatabaseDirectoryExists(props))

        // running process for a second time should change nothing
        resetHelper.process(props)
        assertEquals(1, resetHelper.getDatabaseVersion(versionFile))
        assertTrue(resetHelper.isDatabaseDirectoryExists(props))
    }

    @Test
    fun helper_shouldCreateVersionFile_whenOldVersion() {
        // create database without version
        val props1 = withConfig(1, null)
        resetHelper.process(props1)
        versionFile!!.renameTo(File(versionFile!!.absoluteFile.toString() + ".renamed"))

        val props2 = withConfig(2, IGNORE)
        resetHelper.process(props2)

        assertEquals(1, resetHelper.getDatabaseVersion(versionFile))
        assertTrue(resetHelper.isDatabaseDirectoryExists(props2))
    }

    @Test(expected = RuntimeException::class)
    @Throws(IOException::class)
    fun helper_shouldStop_whenNoVersionFileAndNotFirstVersion() {
        val props = withConfig(2, EXIT)
        resetHelper.process(props)

        // database is assumed to exist if dir is not empty
        versionFile!!.renameTo(File(versionFile!!.absoluteFile.toString() + ".renamed"))

        resetHelper.process(props)
    }

    @Test
    fun helper_shouldReset_whenDifferentVersionAndFlag() {
        val props1 = withConfig(1, null)
        resetHelper.process(props1)

        val testFile = createFile()
        val props2 = withConfig(2, RESET)
        resetHelper.process(props2)
        assertFalse(testFile.exists())
        assertEquals(2, resetHelper.getDatabaseVersion(versionFile))
    }

    @Test(expected = RuntimeException::class)
    fun helper_shouldExit_whenDifferentVersionAndFlag() {
        val props1 = withConfig(1, null)
        resetHelper.process(props1)

        val props2 = withConfig(2, EXIT)
        resetHelper.process(props2)
    }

    @Test(expected = RuntimeException::class)
    fun helper_shouldExit_byDefault() {
        val props1 = withConfig(1, null)
        resetHelper.process(props1)

        val props2 = withConfig(2, null)
        resetHelper.process(props2)
    }

    @Test
    fun helper_shouldIgnore_whenDifferentVersionAndFlag() {
        val props1 = withConfig(1, EXIT)
        resetHelper.process(props1)
        val testFile = createFile()

        val props2 = withConfig(2, IGNORE)
        resetHelper.process(props2)
        assertTrue(testFile.exists())
        assertEquals(1, resetHelper.getDatabaseVersion(versionFile))
    }

    @Test
    @Throws(IOException::class)
    fun helper_shouldPutVersion_afterDatabaseReset() {
        val config = ConfigFactory.empty()
                .withValue("database.reset", ConfigValueFactory.fromAnyRef(true))

        val systemProperties = SPO(config)
        systemProperties.setDataBaseDir(databaseDir)
        systemProperties.setDatabaseVersion(33)
        val testFile = createFile()

        assertTrue(testFile.exists())
        resetHelper.process(systemProperties)
        assertEquals(33, resetHelper.getDatabaseVersion(versionFile))

        assertFalse(testFile.exists()) // reset should have cleared file
    }


    // HELPERS

    private fun createFile(): File {
        val testFile = File(databaseDir!! + "/empty.file")
        testFile.parentFile.mkdirs()
        try {
            testFile.createNewFile()
        } catch (e: IOException) {
            throw RuntimeException("Can't create file in database dir")
        }

        return testFile
    }

    private fun withConfig(databaseVersion: Int, behavior: Behavior?): SystemProperties {
        var config = ConfigFactory.empty()
                // reset is true for tests
                .withValue("database.reset", ConfigValueFactory.fromAnyRef(false))

        if (behavior != null) {
            config = config.withValue("database.incompatibleDatabaseBehavior",
                    ConfigValueFactory.fromAnyRef(behavior.toString().toLowerCase()))
        }


        val systemProperties = SPO(config)
        systemProperties.setDataBaseDir(databaseDir)
        systemProperties.setDatabaseVersion(databaseVersion)
        return systemProperties
    }

    class SPO(config: Config) : SystemProperties(config) {

        fun setDatabaseVersion(databaseVersion: Int?) {
            this.databaseVersion = databaseVersion
        }
    }
}
