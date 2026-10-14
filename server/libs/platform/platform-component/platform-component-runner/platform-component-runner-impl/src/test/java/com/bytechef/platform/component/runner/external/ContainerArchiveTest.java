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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ContainerArchiveTest {

    @TempDir
    private Path tempDirPath;

    @AfterEach
    void restoreTheDefaultCeilings() {
        useByteCeiling(ContainerArchive.DEFAULT_MAX_EXTRACTED_BYTES);
        useEntryCeiling(ContainerArchive.DEFAULT_MAX_EXTRACTED_ENTRIES);
    }

    @Test
    void testToTarAndExtractTarRoundTripANestedFile() throws IOException {
        Path sourcePath = Files.createDirectories(tempDirPath.resolve("source"));

        Files.writeString(sourcePath.resolve("input.json"), "{\"a\":1}", StandardCharsets.UTF_8);

        Path nestedPath = Files.createDirectories(sourcePath.resolve("output/nested"));

        Files.writeString(nestedPath.resolve("report.csv"), "a,b\n", StandardCharsets.UTF_8);

        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = ContainerArchive.toTar(sourcePath)) {
            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.readString(targetPath.resolve("input.json"))).isEqualTo("{\"a\":1}");
        assertThat(Files.readString(targetPath.resolve("output/nested/report.csv"))).isEqualTo("a,b\n");
    }

    @Test
    void testToTarAndExtractTarRoundTripAnEmptyDirectory() throws IOException {
        Path sourcePath = Files.createDirectories(tempDirPath.resolve("source"));

        Files.createDirectories(sourcePath.resolve("output/empty"));

        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = ContainerArchive.toTar(sourcePath)) {
            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(targetPath.resolve("output/empty")).isDirectory();
    }

    @Test
    void testToTarSkipsASymbolicLink() throws IOException {
        Path sourcePath = Files.createDirectories(tempDirPath.resolve("source"));

        Files.writeString(sourcePath.resolve("kept.txt"), "kept", StandardCharsets.UTF_8);

        Path outsideFilePath = writeOutsideFile("secret.txt", "host-only-secret");

        assumeTrue(createSymbolicLink(sourcePath.resolve("leak.txt"), outsideFilePath));

        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = ContainerArchive.toTar(sourcePath)) {
            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.readString(targetPath.resolve("kept.txt"))).isEqualTo("kept");
        assertThat(Files.exists(targetPath.resolve("leak.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
    }

    @Test
    void testToTarArchivesModesAContainerUserOtherThanRootCanWrite() throws IOException {
        Path sourcePath = Files.createDirectories(tempDirPath.resolve("source"));

        Files.createDirectory(sourcePath.resolve("output"));

        Path scriptPath = sourcePath.resolve("script.py");

        Files.writeString(scriptPath, "print(1)", StandardCharsets.UTF_8);

        assumeTrue(restrictPermissions(sourcePath.resolve("output"), "rwx------"));
        assumeTrue(restrictPermissions(scriptPath, "rw-------"));

        List<TarArchiveEntry> entries = readEntries(sourcePath);

        TarArchiveEntry directoryEntry = findEntry(entries, "output/");

        assertThat(directoryEntry.isDirectory()).isTrue();
        assertThat(directoryEntry.getMode() & 0b111_111_111).isEqualTo(0b111_111_111);

        TarArchiveEntry fileEntry = findEntry(entries, "script.py");

        assertThat(fileEntry.isFile()).isTrue();
        assertThat(fileEntry.getMode() & 0b111_111_111).isEqualTo(0b110_110_110);
    }

    @Test
    void testExtractTarCreatesMissingIntermediateDirectories() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("a/b/c/deep.txt", "deep")
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.readString(targetPath.resolve("a/b/c/deep.txt"))).isEqualTo("deep");
    }

    @Test
    void testExtractTarAcceptsTheArchiveRootEntry() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withDirectory("./")
            .withFile("./output/result.json", "{}")
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.readString(targetPath.resolve("output/result.json"))).isEqualTo("{}");
    }

    @Test
    void testExtractTarAcceptsAnEntryNormalisingBackInside() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("a/../b.txt", "inside")
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.readString(targetPath.resolve("b.txt"))).isEqualTo("inside");
    }

    @Test
    void testExtractTarRejectsAnEntryEscapingTheTargetDirectory() throws IOException {
        Path targetPath = Files.createDirectories(tempDirPath.resolve("target"));

        try (InputStream inputStream = new TarBuilder().withFile("../escape.txt", "escaped")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("../escape.txt");
        }

        assertThat(Files.exists(tempDirPath.resolve("escape.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(listRelativePaths(targetPath)).isEmpty();
    }

    @Test
    void testExtractTarRejectsAnEntryEscapingThroughANestedDirectory() throws IOException {
        Path targetPath = Files.createDirectories(tempDirPath.resolve("target"));

        try (InputStream inputStream = new TarBuilder().withFile("nested/../../escape.txt", "escaped")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("nested/../../escape.txt");
        }

        assertThat(Files.exists(tempDirPath.resolve("escape.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(listRelativePaths(targetPath)).isEmpty();
    }

    @Test
    void testExtractTarRejectsAnAbsoluteEntry() throws IOException {
        Path targetPath = Files.createDirectories(tempDirPath.resolve("target"));
        Path outsidePath = Files.createDirectories(tempDirPath.resolve("outside"));

        Path plantedPath = outsidePath.resolve("planted.txt");

        try (InputStream inputStream = new TarBuilder().withFile(plantedPath.toString(), "planted")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(plantedPath.toString());
        }

        assertThat(Files.exists(plantedPath, LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(listRelativePaths(targetPath)).isEmpty();

        try (InputStream inputStream = new TarBuilder().withFile("/etc/passwd", "root::0:0::/:/bin/sh")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("/etc/passwd");
        }

        assertThat(listRelativePaths(targetPath)).isEmpty();
    }

    @Test
    void testExtractTarSkipsASymbolicLinkEntry() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        Path outsideFilePath = writeOutsideFile("secret.txt", "host-only-secret");

        try (InputStream inputStream = new TarBuilder().withSymbolicLink("leak.txt", outsideFilePath.toString())
            .withFile("kept.txt", "kept")
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.exists(targetPath.resolve("leak.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(targetPath.resolve("kept.txt"))).isEqualTo("kept");
        assertThat(Files.readString(outsideFilePath)).isEqualTo("host-only-secret");
    }

    @Test
    void testExtractTarSkipsAHardLinkEntry() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        Path outsideFilePath = writeOutsideFile("secret.txt", "host-only-secret");

        try (InputStream inputStream = new TarBuilder().withFile("kept.txt", "kept")
            .withHardLink("leak.txt", outsideFilePath.toString())
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.exists(targetPath.resolve("leak.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(targetPath.resolve("kept.txt"))).isEqualTo("kept");
        assertThat(Files.readString(outsideFilePath)).isEqualTo("host-only-secret");
    }

    @Test
    void testExtractTarRejectsAnEntryExtractingThroughASymbolicLinkedDirectory() throws IOException {
        Path targetPath = Files.createDirectories(tempDirPath.resolve("target"));
        Path outsidePath = Files.createDirectories(tempDirPath.resolve("outside"));

        assumeTrue(createSymbolicLink(targetPath.resolve("escape"), outsidePath));

        try (InputStream inputStream = new TarBuilder().withFile("escape/planted.txt", "planted")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("escape/planted.txt");
        }

        assertThat(Files.exists(outsidePath.resolve("planted.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(listRelativePaths(targetPath)).containsExactly("escape");
    }

    @Test
    void testExtractTarRejectsADirectoryEntryOnASymbolicLinkedDirectory() throws IOException {
        Path targetPath = Files.createDirectories(tempDirPath.resolve("target"));
        Path outsidePath = Files.createDirectories(tempDirPath.resolve("outside"));

        assumeTrue(createSymbolicLink(targetPath.resolve("escape"), outsidePath));

        try (InputStream inputStream = new TarBuilder().withDirectory("escape/")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("escape");
        }

        assertThat(listRelativePaths(outsidePath)).isEmpty();
        assertThat(listRelativePaths(targetPath)).containsExactly("escape");
    }

    @Test
    void testExtractTarDoesNotLetOneEntryPlantTheLinkTheNextWalksThrough() throws IOException {
        Path targetPath = tempDirPath.resolve("target");
        Path outsidePath = Files.createDirectories(tempDirPath.resolve("outside"));

        try (InputStream inputStream = new TarBuilder().withSymbolicLink("escape", outsidePath.toString())
            .withFile("escape/planted.txt", "planted")
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath);
        }

        assertThat(Files.exists(outsidePath.resolve("planted.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.isSymbolicLink(targetPath.resolve("escape"))).isFalse();
        assertThat(Files.readString(targetPath.resolve("escape/planted.txt"))).isEqualTo("planted");
    }

    @Test
    void testExtractTarRejectsAnEntryPastTheCeiling() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("big.bin", "x".repeat(100))
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath, 16))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("big.bin")
                .hasMessageContaining("16");
        }

        assertThat(Files.exists(targetPath.resolve("big.bin"), LinkOption.NOFOLLOW_LINKS)).isFalse();
    }

    @Test
    void testExtractTarCountsTheCeilingAcrossEveryEntry() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("first.bin", "x".repeat(100))
            .withFile("second.bin", "y".repeat(100))
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath, 150))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("second.bin");
        }

        assertThat(Files.readString(targetPath.resolve("first.bin"))).isEqualTo("x".repeat(100));
    }

    @Test
    void testExtractTarAcceptsAnArchiveInsideTheCeiling() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("small.bin", "x".repeat(100))
            .build()) {

            ContainerArchive.extractTar(inputStream, targetPath, 100);
        }

        assertThat(Files.readString(targetPath.resolve("small.bin"))).isEqualTo("x".repeat(100));
    }

    @Test
    void testTheDefaultCeilingIsGenerousButFinite() {
        assertThat(ContainerArchive.DEFAULT_MAX_EXTRACTED_BYTES).isBetween(1024L * 1024, 4L * 1024 * 1024 * 1024);
        assertThat(ContainerArchive.DEFAULT_MAX_EXTRACTED_ENTRIES).isBetween(100, 1_000_000);
    }

    @Test
    void testTheTwoArgumentExtractTarEnforcesTheDefaultByteCeiling() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        useByteCeiling(16);

        try (InputStream inputStream = new TarBuilder().withFile("big.bin", "x".repeat(100))
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("big.bin")
                .hasMessageContaining("16");
        }
    }

    @Test
    void testExtractTarRejectsAnArchivePastTheEntryCeiling() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withFile("first.txt", "")
            .withFile("second.txt", "")
            .withFile("third.txt", "")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath, 1024, 2))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("third.txt")
                .hasMessageContaining("2 entry ceiling");
        }

        assertThat(Files.exists(targetPath.resolve("third.txt"), LinkOption.NOFOLLOW_LINKS)).isFalse();
    }

    @Test
    void testTheEntryCeilingCountsDirectoryEntriesToo() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        try (InputStream inputStream = new TarBuilder().withDirectory("a/")
            .withDirectory("a/b/")
            .withDirectory("a/b/c/")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath, 1024, 2))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("2 entry ceiling");
        }
    }

    @Test
    void testTheTwoArgumentExtractTarEnforcesTheDefaultEntryCeiling() throws IOException {
        Path targetPath = tempDirPath.resolve("target");

        useEntryCeiling(1);

        try (InputStream inputStream = new TarBuilder().withFile("first.txt", "a")
            .withFile("second.txt", "b")
            .build()) {

            assertThatThrownBy(() -> ContainerArchive.extractTar(inputStream, targetPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("second.txt")
                .hasMessageContaining("1 entry ceiling");
        }
    }

    private static void useByteCeiling(long maxExtractedBytes) {
        ContainerArchive.maxExtractedBytes = maxExtractedBytes;
    }

    private static void useEntryCeiling(int maxExtractedEntries) {
        ContainerArchive.maxExtractedEntries = maxExtractedEntries;
    }

    private static boolean createSymbolicLink(Path linkPath, Path targetPath) {
        try {
            Files.createSymbolicLink(linkPath, targetPath);

            return true;
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private static TarArchiveEntry findEntry(List<TarArchiveEntry> entries, String entryName) {
        return entries.stream()
            .filter(entry -> entryName.equals(entry.getName()))
            .findFirst()
            .orElseThrow();
    }

    private static List<TarArchiveEntry> readEntries(Path directoryPath) throws IOException {
        List<TarArchiveEntry> entries = new ArrayList<>();

        try (InputStream inputStream = ContainerArchive.toTar(directoryPath);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {

            TarArchiveEntry tarArchiveEntry;

            while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) != null) {
                entries.add(tarArchiveEntry);
            }
        }

        return entries;
    }

    private static boolean restrictPermissions(Path path, String permissions) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));

            return true;
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private static List<String> listRelativePaths(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(candidate -> !candidate.equals(path))
                .map(path::relativize)
                .map(Path::toString)
                .sorted(Comparator.naturalOrder())
                .toList();
        }
    }

    private Path writeOutsideFile(String fileName, String content) throws IOException {
        Path outsidePath = Files.createDirectories(tempDirPath.resolve("outside"));

        Path filePath = outsidePath.resolve(fileName);

        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        return filePath;
    }

    private static final class TarBuilder {

        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        private final TarArchiveOutputStream tarArchiveOutputStream;

        TarBuilder() {
            tarArchiveOutputStream = new TarArchiveOutputStream(byteArrayOutputStream);

            tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        }

        InputStream build() throws IOException {
            tarArchiveOutputStream.close();

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        TarBuilder withDirectory(String entryName) throws IOException {
            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(entryName, TarConstants.LF_DIR, true);

            tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
            tarArchiveOutputStream.closeArchiveEntry();

            return this;
        }

        TarBuilder withFile(String entryName, String content) throws IOException {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(entryName, TarConstants.LF_NORMAL, true);

            tarArchiveEntry.setSize(bytes.length);

            tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
            tarArchiveOutputStream.write(bytes);
            tarArchiveOutputStream.closeArchiveEntry();

            return this;
        }

        TarBuilder withHardLink(String entryName, String linkName) throws IOException {
            return withLink(entryName, linkName, TarConstants.LF_LINK);
        }

        TarBuilder withSymbolicLink(String entryName, String linkName) throws IOException {
            return withLink(entryName, linkName, TarConstants.LF_SYMLINK);
        }

        private TarBuilder withLink(String entryName, String linkName, byte linkFlag) throws IOException {
            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(entryName, linkFlag, true);

            tarArchiveEntry.setLinkName(linkName);
            tarArchiveEntry.setSize(0);

            tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
            tarArchiveOutputStream.closeArchiveEntry();

            return this;
        }
    }
}
