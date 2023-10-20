
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

    public void deleteFile(String context, FileEntry fileEntry) {
        Path path = resolveDirectory(context);
        String url = fileEntry.getUrl();

        boolean deleted = path.resolve(url.replace("file:", ""))
            .toFile()
            .delete();

        if (!deleted) {
            throw new FileStorageException("File %s cannot be deleted".formatted(path));
        }
    }

    @Override
    public boolean fileExists(String context, FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectory(context);
        String url = fileEntry.getUrl();

        return path.resolve(url.replace("file:", ""))
            .toFile()
            .exists();
    }

    @Override
    public InputStream getFileStream(String context, FileEntry fileEntry) {
        Path path = resolveDirectory(context);
        String url = fileEntry.getUrl();

        try {
            return Files.newInputStream(path.resolve(url.replace("file:", "")), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry, ioe);
        }
    }

    @Override
    public String readFileToString(String context, FileEntry fileEntry) throws FileStorageException {
        Path path = resolveDirectory(context);
        String url = fileEntry.getUrl();

        try {
            return Files.readString(path.resolve(url.replace("file:", "")));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + fileEntry, ioe);
        }
    }

    @Override
    public FileEntry storeFileContent(String context, String fileName, byte[] data) throws FileStorageException {
        Objects.requireNonNull(fileName, "Filename is required");
        Objects.requireNonNull(data, "Content is required");

        return storeFileContent(context, fileName, new ByteArrayInputStream(data));
    }

    @Override
    public FileEntry storeFileContent(String context, String fileName, String data) throws FileStorageException {
        Objects.requireNonNull(fileName, "Filename is required");
        Objects.requireNonNull(data, "Content is required");

        return storeFileContent(context, fileName, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public FileEntry storeFileContent(String context, String fileName, InputStream inputStream)
        throws FileStorageException {

        Path path = resolveDirectory(context);

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

    private Path resolveDirectory(String context) {
        try {
            return Files.createDirectories(rootLocation.resolve(context));
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }

    private String generate() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }
}
