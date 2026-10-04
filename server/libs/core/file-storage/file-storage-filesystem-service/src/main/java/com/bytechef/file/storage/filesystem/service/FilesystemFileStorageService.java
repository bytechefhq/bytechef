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

package com.bytechef.file.storage.filesystem.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class FilesystemFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FilesystemFileStorageService.class);

    private static final String TEMPORARY_FILE_SUFFIX = ".bytechef-tmp";
    private static final String URL_PREFIX = "file:";

    private final Path baseDirPath;

    /**
     * <b>Security Note:</b> Path traversal is intentional for this component. The baseDir parameter is sourced from
     * application configuration properties ({@code bytechef.file-storage.filesystem.base-path}), not from untrusted
     * user input. This allows deployment-specific storage location configuration.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public FilesystemFileStorageService(String baseDir) {
        this.baseDirPath = Paths.get(baseDir);
    }

    @Override
    public void deleteFile(String directory, FileEntry fileEntry) {
        if (fileEntry != null) {
            Path directoryPath = resolveDirectoryPath(directory);
            Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

            File file = filePath.toFile();

            if (!file.exists()) {
                return;
            }

            boolean deleted = file.delete();

            if (!deleted) {
                throw new FileStorageException("File %s cannot be deleted".formatted(filePath));
            }
        }
    }

    @Override
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        File file = getFile(directory, fileEntry);

        return pathExists(file.toPath());
    }

    @Override
    public boolean fileExists(String directory, String filename) throws FileStorageException {
        Path directoryPath = getTenantDirectoryPath(directory);

        return pathExists(directoryPath.resolve(filename));
    }

    @Override
    public long getContentLength(String directory, FileEntry fileEntry) throws FileStorageException {
        File file = getFile(directory, fileEntry);

        return file.length();
    }

    @Override
    public FileEntry getFileEntry(String directory, String filename) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);

        Path filePath = directoryPath.resolve(filename);

        FileEntry fileEntry = new FileEntry(filename, getUrl(directory, directoryPath, filePath));

        fileExists(directory, fileEntry);

        return fileEntry;
    }

    @Override
    public Set<FileEntry> getFileEntries(String directory) throws FileStorageException {
        Path directoryPath = getTenantDirectoryPath(directory);
        Set<FileEntry> fileEntries = new HashSet<>();

        if (!pathExists(directoryPath)) {
            return fileEntries;
        }

        if (!Files.isDirectory(directoryPath)) {
            throw new FileStorageException("Not a directory: " + directory);
        }

        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                    String filename = String.valueOf(path.getFileName());

                    if (!basicFileAttributes.isDirectory() && !filename.endsWith(TEMPORARY_FILE_SUFFIX)) {
                        fileEntries.add(new FileEntry(filename, getUrl(directory, directoryPath, path)));
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException ioException) throws IOException {
                    return skipVanished(ioException);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException ioException) throws IOException {
                    return ioException == null ? FileVisitResult.CONTINUE : skipVanished(ioException);
                }
            });
        } catch (IOException ioException) {
            throw new FileStorageException(ioException.getMessage(), ioException);
        }

        return fileEntries;
    }

    @Override
    public URL getFileEntryURL(String directory, FileEntry fileEntry) {
        Path directoryPath = resolveDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        try {
            return filePath.toUri()
                .toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String directory, FileEntry fileEntry) {
        Path directoryPath = resolveDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        try {
            return Files.newInputStream(filePath, StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry.getUrl(), ioe);
        }
    }

    @Override
    public OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        try {
            return Files.newOutputStream(filePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry.getUrl(), ioe);
        }
    }

    @Override
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry.getUrl(), ioe);
        }
    }

    @Override
    public String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        try {
            return Files.readString(filePath);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry.getUrl(), ioe);
        }
    }

    @Override
    public String getType() {
        return ApplicationProperties.FileStorage.Provider.FILESYSTEM.name();
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data) throws FileStorageException {
        return storeFileContent(directory, filename, data, true);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename)
        throws FileStorageException {

        Assert.notNull(directory, "directory is required");
        Assert.notNull(filename, "fileName is required");
        Assert.notNull(data, "data is required");

        return doStoreFileContent(directory, filename, new ByteArrayInputStream(data), generateFilename);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data) throws FileStorageException {
        return storeFileContent(directory, filename, data, true);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data, boolean generateFilename)
        throws FileStorageException {

        Assert.notNull(directory, "directory is required");
        Assert.notNull(filename, "fileName is required");
        Assert.notNull(data, "data is required");

        return doStoreFileContent(
            directory, filename, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), generateFilename);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, InputStream inputStream)
        throws FileStorageException {

        return storeFileContent(directory, filename, inputStream, true);
    }

    @Override
    public FileEntry storeFileContent(
        String directory, String filename, InputStream inputStream, boolean generateFilename)
        throws FileStorageException {

        Assert.notNull(directory, "directory is required");
        Assert.notNull(filename, "fileName is required");
        Assert.notNull(inputStream, "inputStream is required");

        return doStoreFileContent(directory, filename, inputStream, generateFilename);
    }

    private File getFile(String directory, FileEntry fileEntry) {
        Path directoryPath = getTenantDirectoryPath(directory);
        Path filePath = resolveFilePath(directoryPath, directory, fileEntry.getUrl());

        return filePath.toFile();
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static Path resolveFilePath(Path directoryPath, String directory, String url) {
        String relative = removeUrlPrefix(url, directory);
        Path candidate = Paths.get(relative);

        if (candidate.isAbsolute()) {
            throw new FileStorageException("Invalid file path");
        }

        Path resolved = directoryPath.resolve(candidate)
            .normalize();

        if (!resolved.startsWith(directoryPath.normalize())) {
            throw new FileStorageException("Invalid file path");
        }

        return resolved;
    }

    private FileEntry doStoreFileContent(
        String directory, String filename, InputStream inputStream, boolean generateFilename) {

        Path directoryPath = resolveDirectoryPath(directory);

        Path filePath = directoryPath;

        if (generateFilename) {
            int dotIndex = filename.lastIndexOf('.');
            UUID uuid = UUID.randomUUID();

            String generatedName = dotIndex > 0 ? uuid + filename.substring(dotIndex) : uuid.toString();

            filePath = filePath.resolve(generatedName);
        } else {
            filePath = filePath.resolve(filename);
        }

        writeAtomically(inputStream, directoryPath, filePath, filename);

        return new FileEntry(filename, getUrl(directory, directoryPath, filePath));
    }

    private static void writeAtomically(InputStream inputStream, Path directoryPath, Path filePath, String filename) {
        Path temporaryFilePath = null;

        try {
            temporaryFilePath = Files.createTempFile(directoryPath, ".", TEMPORARY_FILE_SUFFIX);

            Files.copy(inputStream, temporaryFilePath, StandardCopyOption.REPLACE_EXISTING);

            if (Files.size(temporaryFilePath) == 0) {
                throw new FileStorageException("Failed to store empty file " + filename);
            }

            Files.move(
                temporaryFilePath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            temporaryFilePath = null;
        } catch (IOException ioException) {
            throw new FileStorageException("Failed to store file " + filename, ioException);
        } finally {
            deleteQuietly(temporaryFilePath);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ioException) {
            log.warn("Failed to delete temporary file {}", path, ioException);
        }
    }

    private static String getUrl(String directory, Path directoryPath, Path filePath) {
        return URL_PREFIX + "/" + directory + File.separator + directoryPath.relativize(filePath);
    }

    private static String removeUrlPrefix(String url, String directory) {
        return url.replace(URL_PREFIX + "/" + directory + File.separator, "");
    }

    private Path resolveDirectoryPath(String directory) {
        try {
            return Files.createDirectories(getTenantDirectoryPath(directory));
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }

    /**
     * <b>Security Note:</b> Path traversal is intentional for this component. This service manages file storage for
     * workflow artifacts and is designed to access files under a configured base directory. File paths are derived from
     * tenant context and internal directory parameters, not from untrusted user input. Access control is handled
     * through tenant isolation.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private Path getTenantDirectoryPath(String directory) {
        Path tenantDirectoryPath = baseDirPath.resolve(TenantContext.getCurrentTenantId());

        return tenantDirectoryPath.resolve(normalizeDirectory(directory));
    }

    private static boolean pathExists(Path path) {
        try {
            Files.readAttributes(path, BasicFileAttributes.class);

            return true;
        } catch (NoSuchFileException noSuchFileException) {
            return false;
        } catch (IOException ioException) {
            throw new FileStorageException("Cannot access " + path, ioException);
        }
    }

    private static FileVisitResult skipVanished(IOException ioException) throws IOException {
        if (ioException instanceof NoSuchFileException) {
            return FileVisitResult.CONTINUE;
        }

        throw ioException;
    }

    private static String normalizeDirectory(String directory) {
        String normalizedDirectory = StringUtils.replace(directory.replaceAll("[^0-9a-zA-Z/_]", ""), " ", "");

        return normalizedDirectory.toLowerCase();
    }
}
