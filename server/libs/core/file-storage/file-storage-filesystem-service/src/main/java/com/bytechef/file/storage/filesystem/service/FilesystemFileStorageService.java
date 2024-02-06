/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class FilesystemFileStorageService implements FileStorageService {

    private static final String FILE = "file:";

    private final Path baseDirPath;

    public FilesystemFileStorageService(String baseDir) {
        this.baseDirPath = Paths.get(baseDir);
    }

    @Override
    public void deleteFile(String directoryPath, FileEntry fileEntry) {
        Path path = resolveDirectoryPath(directoryPath);
        String url = fileEntry.getUrl();

        boolean deleted = path.resolve(url.replace(FILE, ""))
            .toFile()
            .delete();

        if (!deleted) {
            throw new FileStorageException("File %s cannot be deleted".formatted(path));
        }
    }

    @Override
    public boolean fileExists(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectoryPath(directoryPath);
        String url = fileEntry.getUrl();

        return path.resolve(url.replace(FILE, ""))
            .toFile()
            .exists();
    }

    @Override
    public InputStream getFileStream(String directoryPath, FileEntry fileEntry) {
        Path path = resolveDirectoryPath(directoryPath);
        String url = fileEntry.getUrl();

        try {
            return Files.newInputStream(path.resolve(url.replace(FILE, "")), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public byte[] readFileToBytes(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectoryPath(directoryPath);
        String url = fileEntry.getUrl();

        try {
            return Files.readAllBytes(path.resolve(url.replace(FILE, "")));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public String readFileToString(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectoryPath(directoryPath);
        String url = fileEntry.getUrl();

        try {
            return Files.readString(path.resolve(url.replace(FILE, "")));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, byte[] data) throws FileStorageException {
        Validate.notNull(directoryPath, "directory is required");
        Validate.notNull(fileName, "fileName is required");
        Validate.notNull(data, "data is required");

        return storeFileContent(directoryPath, fileName, new ByteArrayInputStream(data));
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, String data) throws FileStorageException {
        Validate.notNull(directoryPath, "directory is required");
        Validate.notNull(fileName, "fileName is required");
        Validate.notNull(data, "data is required");

        return storeFileContent(directoryPath, fileName,
            new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, InputStream inputStream)
        throws FileStorageException {

        Validate.notNull(directoryPath, "directory is required");
        Validate.notNull(fileName, "fileName is required");
        Validate.notNull(inputStream, "inputStream is required");

        return doStoreFileContent(directoryPath, fileName, inputStream);
    }

    private FileEntry doStoreFileContent(String directory, String fileName, InputStream inputStream) {
        directory = StringUtils.replace(directory.replaceAll("[^0-9a-zA-Z/_]", ""), " ", "");

        Path path = resolveDirectoryPath(directory.toLowerCase());

        path = path.resolve(generateUuid());

        try {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file " + fileName, ioe);
        }

        File file = path.toFile();

        if (file.length() == 0) {
            throw new FileStorageException("Failed to store empty file " + fileName);
        }

        return new FileEntry(fileName, FILE + path.toString());
    }

    private Path resolveDirectoryPath(String directory) {
        try {
            return Files.createDirectories(baseDirPath.resolve(directory));
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }

    private String generateUuid() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }
}
