/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.file.delete

import org.gradle.internal.time.Time
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Assume
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

abstract class AbstractSymlinkDeleterTest extends Specification {
    static final boolean FOLLOW_SYMLINKS = true

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    Deleter deleter = new Deleter({ Time.clock().currentTime }, false)

    def doesNotDeleteFilesInsideSymlinkDir() {
        Assume.assumeTrue(canCreateSymbolicLinkToDirectory())

        given:
        def keepTxt = tmpDir.createFile("originalDir", "keep.txt")
        def originalDir = keepTxt.parentFile
        def link = new File(tmpDir.getTestDirectory(), "link")

        when:
        createSymbolicLink(link, originalDir)

        then:
        link.exists()

        when:
        boolean didWork = delete(false, link)

        then:
        !link.exists()
        originalDir.assertExists()
        keepTxt.assertExists()
        didWork

        cleanup:
        link.delete()
    }

    def deletesFilesInsideSymlinkDirWhenNeeded() {
        Assume.assumeTrue(canCreateSymbolicLinkToDirectory())

        given:
        def keepTxt = tmpDir.createFile("originalDir", "keep.txt")
        def originalDir = keepTxt.parentFile
        def link = new File(tmpDir.getTestDirectory(), "link")

        when:
        createSymbolicLink(link, originalDir)

        then:
        link.exists()

        when:
        boolean didWork = delete(FOLLOW_SYMLINKS, link)

        then:
        !link.exists()
        keepTxt.assertDoesNotExist()
        didWork

        cleanup:
        link.delete()
    }

    @Unroll
    def "does not follow symlink to a file when followSymlinks is #followSymlinks"() {
        Assume.assumeTrue(canCreateSymbolicLinkToFile())

        given:
        def originalFile = tmpDir.createFile("originalFile", "keep.txt")
        def link = new File(tmpDir.getTestDirectory(), "link")

        when:
        createSymbolicLink(link, originalFile)

        then:
        link.exists()

        when:
        boolean didWork = delete(followSymlinks, link)

        then:
        !link.exists()
        originalFile.assertExists()
        didWork

        cleanup:
        link.delete()

        where:
        followSymlinks << [true, false]
    }

    private boolean delete(boolean followSymlinks, File... paths) {
        return deleter.deleteInternal(paths as List) { file ->
            file.directory || (followSymlinks && Files.isSymbolicLink(file.toPath()))
        }
    }

    protected abstract void createSymbolicLink(File link, TestFile target)

    protected abstract boolean canCreateSymbolicLinkToFile()

    protected abstract boolean canCreateSymbolicLinkToDirectory()
}
