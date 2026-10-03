/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.file.storage.filesystem.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class FilesystemFileStorageServiceTest {

    private static final String TEST_STRING = "test string";

    private static final FilesystemFileStorageService fileStorageService = new FilesystemFileStorageService(
        "/tmp/test/bytechef/files");

    @Test
    public void testDeleteFile() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileStorageService.readFileToString("data", fileEntry))
            .isEqualTo(TEST_STRING);

        fileStorageService.deleteFile("data", fileEntry);

        Assertions.assertThat(fileStorageService.fileExists("data", fileEntry))
            .isFalse();
    }

    @Test
    public void testOpenInputStream() throws IOException {
        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        InputStream inputStream = fileStorageService.getInputStream("data", fileEntry);

        Assertions.assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo(TEST_STRING);
    }

    @Test
    public void testRead() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileStorageService.readFileToString("data", fileEntry))
            .isEqualTo(TEST_STRING);
    }

    @Test
    public void testStoreAndReadFilesWithoutExtension() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "LICENSE", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileStorageService.readFileToString("data", fileEntry))
            .isEqualTo(TEST_STRING);
        Assertions.assertThat(fileEntry.getExtension())
            .isEqualTo("bin");
        Assertions.assertThat(fileEntry.getMimeType())
            .isEqualTo("application/octet-stream");
        Assertions.assertThat(fileEntry.getUrl())
            .doesNotContain("LICENSE");

        fileEntry = fileStorageService.storeFileContent(
            "data", ".env", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileStorageService.readFileToString("data", fileEntry))
            .isEqualTo(TEST_STRING);
        Assertions.assertThat(fileEntry.getExtension())
            .isEqualTo("bin");
        Assertions.assertThat(fileEntry.getMimeType())
            .isEqualTo("application/octet-stream");
        Assertions.assertThat(fileEntry.getUrl())
            .doesNotContain(".env");

        fileEntry = fileStorageService.storeFileContent(
            "data", ".config.json", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileStorageService.readFileToString("data", fileEntry))
            .isEqualTo(TEST_STRING);
        Assertions.assertThat(fileEntry.getExtension())
            .isEqualTo("json");
        Assertions.assertThat(fileEntry.getMimeType())
            .isEqualTo("application/json");
        Assertions.assertThat(fileEntry.getUrl())
            .doesNotContain(".config.json");
    }

    @Test
    public void testWrite() {
        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "fileName.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        String path = fileEntry.getUrl();

        Assertions.assertThat(path)
            .startsWith("file:/data/");

        String url = fileEntry.getUrl();

        Assertions
            .assertThat(
                Files.contentOf(
                    new File("/tmp/test/bytechef/files/public" + url.replace("file:", "")), StandardCharsets.UTF_8))
            .isEqualTo(TEST_STRING);
    }

    @Test
    public void testGetInputStreamRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.getInputStream("data", malicious));
    }

    @Test
    public void testGetInputStreamRejectsParentTraversal() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "file:/data/../../../etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.getInputStream("data", malicious));
    }

    @Test
    public void testReadFileToStringRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.readFileToString("data", malicious));
    }

    @Test
    public void testReadFileToBytesRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.readFileToBytes("data", malicious));
    }

    @Test
    public void testGetContentLengthRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.getContentLength("data", malicious));
    }

    @Test
    public void testGetOutputStreamRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.getOutputStream("data", malicious));
    }

    @Test
    public void testDeleteFileRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.deleteFile("data", malicious));
    }

    @Test
    public void testGetFileEntryURLRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.getFileEntryURL("data", malicious));
    }

    @Test
    public void testFileExistsRejectsAbsolutePath() {
        FileEntry malicious = new FileEntry("file", "txt", "text/plain", "/etc/passwd");

        assertThrows(FileStorageException.class,
            () -> fileStorageService.fileExists("data", malicious));
    }

    @Test
    public void testGetFileEntriesReturnsBareNamesRecursively() {
        String directory = "entries_" + System.nanoTime();

        fileStorageService.storeFileContent(directory, "top.json", TEST_STRING, false);
        fileStorageService.storeFileContent(directory + "/nested", "deep.json", TEST_STRING, false);

        Assertions.assertThat(fileStorageService.getFileEntries(directory))
            .extracting(FileEntry::getName)
            .containsExactlyInAnyOrder("top.json", "deep.json");
    }

    @Test
    public void testGetFileEntriesRoundTripsDirectoryNeedingNormalization() {
        String directory = "Auto-Memory-" + System.nanoTime();

        fileStorageService.storeFileContent(directory, "memo.json", TEST_STRING, false);

        Assertions.assertThat(fileStorageService.getFileEntries(directory))
            .extracting(FileEntry::getName)
            .containsExactly("memo.json");
    }

    @Test
    public void testOverwritingAFileLeavesOnlyTheNewContentAndNoTemporaryFile() {
        String directory = "atomic" + System.nanoTime();

        fileStorageService.storeFileContent(directory, "entry.json", "first", false);

        FileEntry fileEntry = fileStorageService.storeFileContent(directory, "entry.json", "second", false);

        Assertions.assertThat(fileStorageService.readFileToString(directory, fileEntry))
            .isEqualTo("second");
        Assertions.assertThat(fileStorageService.getFileEntries(directory))
            .extracting(FileEntry::getName)
            .containsExactly("entry.json");
        Assertions.assertThat(listDirectoryOf(directory, fileEntry))
            .containsExactly("entry.json");
    }

    /**
     * A write interrupted by a crash leaves its temporary file next to the real ones. Listing it would hand callers a
     * half-written file — the auto-memory store would log it as a corrupt document on every listing.
     */
    @Test
    public void testGetFileEntriesSkipsALeftoverTemporaryFile() throws IOException {
        String directory = "leftover" + System.nanoTime();

        FileEntry fileEntry = fileStorageService.storeFileContent(directory, "entry.json", TEST_STRING, false);

        URL fileUrl = fileStorageService.getFileEntryURL(directory, fileEntry);

        try {
            Path filePath = Path.of(fileUrl.toURI());

            Path parentDirectoryPath = Objects.requireNonNull(filePath.getParent());

            java.nio.file.Files.writeString(parentDirectoryPath.resolve(".123.bytechef-tmp"), "{\"id\":");
        } catch (URISyntaxException uriSyntaxException) {
            throw new IllegalStateException(uriSyntaxException);
        }

        Assertions.assertThat(fileStorageService.getFileEntries(directory))
            .extracting(FileEntry::getName)
            .containsExactly("entry.json");
    }

    @Test
    public void testFailedEmptyWriteKeepsThePreviousContent() {
        String directory = "atomic" + System.nanoTime();

        FileEntry fileEntry = fileStorageService.storeFileContent(directory, "entry.json", "first", false);

        assertThrows(
            FileStorageException.class, () -> fileStorageService.storeFileContent(directory, "entry.json", "", false));

        Assertions.assertThat(fileStorageService.readFileToString(directory, fileEntry))
            .isEqualTo("first");
        Assertions.assertThat(listDirectoryOf(directory, fileEntry))
            .containsExactly("entry.json");
    }

    @Test
    public void testGetFileEntriesReturnsEmptySetForMissingDirectory() {
        Assertions.assertThat(fileStorageService.getFileEntries("missing-" + System.nanoTime()))
            .isEmpty();
    }

    /**
     * The atomic write must not leave stored files owner-only. Files.createTempFile creates its file rw------- on
     * POSIX; the write only escapes that because Files.copy with REPLACE_EXISTING recreates the file under the process
     * umask before it is moved into place. Writing into the temporary file in place instead would keep rw-------, and
     * other users of a shared volume could no longer read stored files. A stored file must end up with exactly the
     * permissions of a file created the ordinary way in the same directory.
     */
    @Test
    public void testStoredFileGetsTheSamePermissionsAsAnOrdinaryNewFile() throws IOException {
        Assumptions.assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix"));

        FileEntry fileEntry = fileStorageService.storeFileContent(
            "permissions", "stored.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Path storedPath = toPath("permissions", fileEntry);
        Path referencePath = storedPath.resolveSibling("reference-" + System.nanoTime() + ".txt");

        java.nio.file.Files.createFile(referencePath);

        try {
            Set<PosixFilePermission> expectedPermissions = java.nio.file.Files.getPosixFilePermissions(referencePath);

            Assertions.assertThat(java.nio.file.Files.getPosixFilePermissions(storedPath))
                .isEqualTo(expectedPermissions);
        } finally {
            java.nio.file.Files.deleteIfExists(referencePath);
        }
    }

    @Test
    public void testLookupsOfANeverWrittenDirectoryDoNotCreateIt() {
        FileEntry probeEntry = fileStorageService.storeFileContent(
            "probe", "probe.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Path probeDirectoryPath = Objects.requireNonNull(toPath("probe", probeEntry).getParent());
        Path tenantDirectoryPath = Objects.requireNonNull(probeDirectoryPath.getParent());
        String directory = "neverwritten" + System.nanoTime();

        Assertions.assertThat(fileStorageService.getFileEntries(directory))
            .isEmpty();
        Assertions.assertThat(fileStorageService.fileExists(directory, "missing.txt"))
            .isFalse();
        Assertions.assertThat(tenantDirectoryPath.resolve(directory))
            .doesNotExist();
    }

    /**
     * A directory the process cannot read is not an empty one: reporting it as empty would let a caller conclude its
     * files are gone. Running as root would read it anyway, so the test needs an unprivileged POSIX user.
     */
    @Test
    public void testAnUnreadableDirectoryIsNotReportedAsEmptyOrMissing() throws IOException {
        Assumptions.assumeTrue(FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix"));

        String directory = "locked" + System.nanoTime() + "/inner";

        FileEntry fileEntry = fileStorageService.storeFileContent(
            directory, "kept.txt", new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8)));

        Path innerDirectoryPath = Objects.requireNonNull(toPath(directory, fileEntry).getParent());
        Path lockedDirectoryPath = Objects.requireNonNull(innerDirectoryPath.getParent());
        Set<PosixFilePermission> originalPermissions = java.nio.file.Files.getPosixFilePermissions(lockedDirectoryPath);

        java.nio.file.Files.setPosixFilePermissions(lockedDirectoryPath, Set.of());

        try {
            Assumptions.assumeFalse(java.nio.file.Files.isReadable(innerDirectoryPath));

            assertThrows(FileStorageException.class, () -> fileStorageService.getFileEntries(directory));
            assertThrows(FileStorageException.class, () -> fileStorageService.fileExists(directory, "kept.txt"));
            assertThrows(FileStorageException.class, () -> fileStorageService.fileExists(directory, fileEntry));
        } finally {
            java.nio.file.Files.setPosixFilePermissions(lockedDirectoryPath, originalPermissions);
        }
    }

    private static Path toPath(String directory, FileEntry fileEntry) {
        URL fileUrl = fileStorageService.getFileEntryURL(directory, fileEntry);

        try {
            return Path.of(fileUrl.toURI());
        } catch (URISyntaxException uriSyntaxException) {
            throw new IllegalStateException(uriSyntaxException);
        }
    }

    private static String[] listDirectoryOf(String directory, FileEntry fileEntry) {
        URL fileUrl = fileStorageService.getFileEntryURL(directory, fileEntry);

        try {
            File parentDirectory = new File(fileUrl.toURI()).getParentFile();

            return parentDirectory.list();
        } catch (URISyntaxException uriSyntaxException) {
            throw new IllegalStateException(uriSyntaxException);
        }
    }
}
