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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class FilesystemFileStorageService implements FileStorageService {

    private static final String URL_PREFIX = "file:";

    private final Path baseDirPath;

    public FilesystemFileStorageService(String baseDir) {
        this.baseDirPath = Paths.get(baseDir);
    }

    @Override
    public void deleteFile(String directory, FileEntry fileEntry) {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        Path filePath = directoryPath.resolve(removeUrlPrefix(url, directory));

        File file = filePath.toFile();

        boolean deleted = file.delete();

        if (!deleted) {
            throw new FileStorageException("File %s cannot be deleted".formatted(directoryPath));
        }
    }

    @Override
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        File file = getFile(directory, fileEntry);

        return file.exists();
    }

    @Override
    public boolean fileExists(String directory, String filename) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);

        Path filePath = directoryPath.resolve(filename);

        File file = filePath.toFile();

        return file.exists();
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
        Path directoryPath = resolveDirectoryPath(directory);

        try (Stream<Path> stream = Files.walk(directoryPath)) {
            return stream.filter(path -> !Files.isDirectory(path))
                .map(path -> new FileEntry(String.valueOf(path.getFileName()), getUrl(directory, directoryPath, path)))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public URL getFileEntryURL(String directory, FileEntry fileEntry) {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        try {
            URI uri = directoryPath.resolve(removeUrlPrefix(url, directory))
                .toUri();

            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String directory, FileEntry fileEntry) {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        try {
            return Files.newInputStream(
                directoryPath.resolve(removeUrlPrefix(url, directory)), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        try {
            return Files.newOutputStream(
                directoryPath.resolve(removeUrlPrefix(url, directory)), StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        try {
            return Files.readAllBytes(directoryPath.resolve(removeUrlPrefix(url, directory)));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException {
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        try {
            return Files.readString(directoryPath.resolve(removeUrlPrefix(url, directory)));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
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
        Path directoryPath = resolveDirectoryPath(directory);
        String url = fileEntry.getUrl();

        Path filePath = directoryPath.resolve(removeUrlPrefix(url, directory));

        return filePath.toFile();
    }

    private FileEntry doStoreFileContent(
        String directory, String filename, InputStream inputStream, boolean generateFilename) {

        directory = StringUtils.replace(directory.replaceAll("[^0-9a-zA-Z/_]", ""), " ", "");

        Path directoryPath = resolveDirectoryPath(directory.toLowerCase());

        Path filePath = directoryPath;

        filePath = filePath.resolve(
            generateFilename
                ? UUID.randomUUID() + filename.substring(filename.lastIndexOf(".")) : filename);

        try {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file " + filename, ioe);
        }

        File file = filePath.toFile();

        if (file.length() == 0) {
            throw new FileStorageException("Failed to store empty file " + filename);
        }

        return new FileEntry(filename, getUrl(directory, directoryPath, filePath));
    }

    private static String getUrl(String directory, Path directoryPath, Path filePath) {
        return URL_PREFIX + "/" + directory + File.separator + directoryPath.relativize(filePath);
    }

    private static String removeUrlPrefix(String url, String directory) {
        return url.replace(URL_PREFIX + "/" + directory + File.separator, "");
    }

    private Path resolveDirectoryPath(String directory) {
        try {
            Path tenantDirectoryPath = baseDirPath.resolve(TenantContext.getCurrentTenantId());

            return Files.createDirectories(tenantDirectoryPath.resolve(directory));
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }
}
