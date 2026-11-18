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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
final class ContainerArchive {

    private static final Logger log = LoggerFactory.getLogger(ContainerArchive.class);

    private static final int ARCHIVED_DIRECTORY_MODE = 0b111_111_111;

    private static final int ARCHIVED_FILE_MODE = 0b110_110_110;

    static final long DEFAULT_MAX_EXTRACTED_BYTES = 256L * 1024 * 1024;

    static final int DEFAULT_MAX_EXTRACTED_ENTRIES = 10_000;

    static long maxExtractedBytes = DEFAULT_MAX_EXTRACTED_BYTES;

    static int maxExtractedEntries = DEFAULT_MAX_EXTRACTED_ENTRIES;

    private ContainerArchive() {
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    static InputStream toTar(Path directoryPath) throws IOException {
        Path archivePath = Files.createTempFile("bytechef-archive-" + UUID.randomUUID() + "-", ".tar");

        try {
            try (OutputStream outputStream = Files.newOutputStream(archivePath);
                TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(outputStream)) {

                tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

                for (Path path : listArchivablePaths(directoryPath)) {
                    writeArchiveEntry(tarArchiveOutputStream, directoryPath, path);
                }
            }

            return newSelfDeletingInputStream(archivePath);
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(archivePath);

            throw exception;
        }
    }

    static void extractTar(InputStream inputStream, Path targetDirectoryPath) throws IOException {
        extractTar(inputStream, targetDirectoryPath, maxExtractedBytes);
    }

    static void extractTar(InputStream inputStream, Path targetDirectoryPath, long maxExtractedBytes)
        throws IOException {

        extractTar(inputStream, targetDirectoryPath, maxExtractedBytes, maxExtractedEntries);
    }

    static void extractTar(
        InputStream inputStream, Path targetDirectoryPath, long maxExtractedBytes, int maxExtractedEntries)
        throws IOException {

        Path targetPath = targetDirectoryPath.toAbsolutePath()
            .normalize();

        Files.createDirectories(targetPath);

        ExtractionBudget extractionBudget = new ExtractionBudget(maxExtractedBytes, maxExtractedEntries);

        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {
            TarArchiveEntry tarArchiveEntry;

            while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) != null) {
                extractEntry(tarArchiveInputStream, tarArchiveEntry, targetPath, extractionBudget);
            }
        }
    }

    private static void createDirectoryInside(Path targetPath, Path directoryPath, String entryName)
        throws IOException {

        Path currentPath = targetPath;

        for (Path name : targetPath.relativize(directoryPath)) {
            currentPath = currentPath.resolve(name);

            if (Files.isSymbolicLink(currentPath)) {
                throw new IOException(
                    "Tar entry '%s' extracts through the symbolic link '%s'".formatted(entryName, currentPath));
            }

            if (!Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(currentPath);
            } else if (!Files.isDirectory(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                    "Tar entry '%s' extracts through '%s', which is not a directory".formatted(entryName, currentPath));
            }
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not remove {}", path, exception);
        }
    }

    private static void extractEntry(
        TarArchiveInputStream tarArchiveInputStream, TarArchiveEntry tarArchiveEntry, Path targetPath,
        ExtractionBudget extractionBudget) throws IOException {

        String entryName = tarArchiveEntry.getName();

        if (tarArchiveEntry.isSymbolicLink() || tarArchiveEntry.isLink()) {
            log.debug("Skipping link tar entry {}", entryName);

            return;
        }

        boolean directory = tarArchiveEntry.isDirectory();

        if (!directory && !tarArchiveEntry.isFile()) {
            log.debug("Skipping tar entry {} of an unsupported type", entryName);

            return;
        }

        Path entryPath = resolveInside(targetPath, entryName);

        if (entryPath.equals(targetPath)) {
            if (directory) {
                return;
            }

            throw new IOException("Tar entry '%s' names the target directory itself".formatted(entryName));
        }

        extractionBudget.countEntry(entryName);

        if (directory) {
            createDirectoryInside(targetPath, entryPath, entryName);
        } else {
            writeEntryFile(tarArchiveInputStream, entryPath, targetPath, entryName, extractionBudget);
        }
    }

    private static List<Path> listArchivablePaths(Path directoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(directoryPath)) {
            return paths.filter(path -> !path.equals(directoryPath))
                .filter(path -> !Files.isSymbolicLink(path))
                .filter(
                    path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ||
                        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .sorted(Comparator.naturalOrder())
                .toList();
        }
    }

    private static InputStream newSelfDeletingInputStream(Path path) throws IOException {
        return new FilterInputStream(Files.newInputStream(path)) {

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    deleteQuietly(path);
                }
            }
        };
    }

    private static Path resolveInside(Path directoryPath, String entryName) throws IOException {
        Path candidatePath;

        try {
            candidatePath = directoryPath.resolve(entryName)
                .normalize();
        } catch (RuntimeException exception) {
            throw new IOException("Tar entry '%s' is not a usable path".formatted(entryName), exception);
        }

        if (!candidatePath.startsWith(directoryPath)) {
            throw new IOException(
                "Tar entry '%s' resolves outside the target directory".formatted(entryName));
        }

        return candidatePath;
    }

    private static void writeArchiveEntry(
        TarArchiveOutputStream tarArchiveOutputStream, Path directoryPath, Path path) throws IOException {

        Path relativePath = directoryPath.relativize(path);

        String entryName = relativePath.toString()
            .replace(path.getFileSystem()
                .getSeparator(), "/");

        boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);

        TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
            path, directory ? entryName + "/" : entryName, LinkOption.NOFOLLOW_LINKS);

        tarArchiveEntry.setMode(directory ? ARCHIVED_DIRECTORY_MODE : ARCHIVED_FILE_MODE);

        tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

        if (!directory) {
            try (InputStream inputStream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                inputStream.transferTo(tarArchiveOutputStream);
            }
        }

        tarArchiveOutputStream.closeArchiveEntry();
    }

    private static void writeEntryFile(
        TarArchiveInputStream tarArchiveInputStream, Path entryPath, Path targetPath, String entryName,
        ExtractionBudget extractionBudget) throws IOException {

        Path parentPath = entryPath.getParent();

        if (parentPath != null) {
            createDirectoryInside(targetPath, parentPath, entryName);
        }

        if (Files.isSymbolicLink(entryPath)) {
            throw new IOException(
                "Tar entry '%s' would be written through the symbolic link '%s'".formatted(entryName, entryPath));
        }

        try (OutputStream outputStream = Files.newOutputStream(
            entryPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS)) {

            extractionBudget.transfer(tarArchiveInputStream, outputStream, entryName);
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(entryPath);

            throw exception;
        }
    }

    private static final class ExtractionBudget {

        private static final int BUFFER_SIZE = 8 * 1024;

        private final long maxExtractedBytes;
        private final int maxExtractedEntries;

        private long extractedBytes;
        private int extractedEntries;

        ExtractionBudget(long maxExtractedBytes, int maxExtractedEntries) {
            this.maxExtractedBytes = maxExtractedBytes;
            this.maxExtractedEntries = maxExtractedEntries;
        }

        void countEntry(String entryName) throws IOException {
            extractedEntries++;

            if (extractedEntries > maxExtractedEntries) {
                throw new IOException(
                    "Tar entry '%s' takes the extracted archive past the %d entry ceiling".formatted(
                        entryName, maxExtractedEntries));
            }
        }

        void transfer(InputStream inputStream, OutputStream outputStream, String entryName) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];

            int read = inputStream.read(buffer);

            while (read != -1) {
                extractedBytes += read;

                if (extractedBytes > maxExtractedBytes) {
                    throw new IOException(
                        "Tar entry '%s' takes the extracted archive past the %d byte ceiling".formatted(
                            entryName, maxExtractedBytes));
                }

                outputStream.write(buffer, 0, read);

                read = inputStream.read(buffer);
            }
        }
    }
}
