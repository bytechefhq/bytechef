
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.file.storage.filesystem.service;

import com.bytechef.hermes.file.storage.domain.FileEntry;
import com.bytechef.hermes.file.storage.exception.FileStorageException;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
public class FilesystemFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public FilesystemFileStorageService(String fileStorageDir) {
        this.rootLocation = Paths.get(fileStorageDir);
    }

    public void deleteFile(FileEntry fileEntry) {
        Path path = resolveDirectory();
        String url = fileEntry.getUrl();

        path.resolve(url.replace("file:", ""))
            .toFile()
            .delete();
    }

    @Override
    public boolean fileExists(FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectory();
        String url = fileEntry.getUrl();

        return path.resolve(url.replace("file:", ""))
            .toFile()
            .exists();
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) throws FileStorageException {
        Objects.requireNonNull(fileName, "Filename is required");
        Objects.requireNonNull(data, "Content is required");

        return storeFileContent(fileName, new ByteArrayInputStream(data.getBytes()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) throws FileStorageException {
        Path path = resolveDirectory();

        path = path.resolve(generate());

        try {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file " + fileName, ioe);
        }

        File file = path.toFile();

        if (file.length() == 0) {
            throw new FileStorageException("Failed to store empty file " + fileName);
        }

        return new FileEntry(fileName, "file:" + path);
    }

    @Override
    public String readFileToString(FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectory();
        String url = fileEntry.getUrl();

        try {
            return Files.readString(path.resolve(url.replace("file:", "")));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry, ioe);
        }
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        Path path = resolveDirectory();
        String url = fileEntry.getUrl();

        try {
            return Files.newInputStream(path.resolve(url.replace("file:", "")), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry, ioe);
        }
    }

    private Path resolveDirectory() {
        try {
            return Files.createDirectories(rootLocation);
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }

    private String generate() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }
}
