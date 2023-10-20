/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.file.storage.filesystem;

import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.exception.FileStorageException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * @author Ivica Cardic
 */
public class FileSystemFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public FileSystemFileStorageService(String fileStorageDir) {
        this.rootLocation = Paths.get(fileStorageDir);
    }

    @Override
    public void deleteFile(String url) throws FileStorageException {
        Path path = resolveDirectory();

        try {
            Files.delete(path.resolve(url));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to delete file " + url, ioe);
        }
    }
        }
    }

    @Override
    public boolean fileExists(String url) throws FileStorageException {
        Path path = resolveDirectory();

        return path.resolve(url).toFile().exists();
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) throws FileStorageException {
        return storeFileContent(fileName, new ByteArrayInputStream(data.getBytes()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) throws FileStorageException {
        Path path = resolveDirectory();

        path = path.resolve(UUIDGenerator.generate());

        try {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file " + fileName, ioe);
        }

        File file = path.toFile();

        if (file.length() == 0) {
            throw new FileStorageException("Failed to store empty file " + fileName);
        }

        return FileEntry.of(fileName, path.toString());
    }

    @Override
    public String readFileContent(String url) throws FileStorageException {
        Path path = resolveDirectory();

        try {
            return Files.readString(path.resolve(url));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    @Override
    public InputStream getFileContentStream(String url) {
        Path path = resolveDirectory();

        try {
            return Files.newInputStream(path.resolve(url), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to open file " + url, ioe);
        }
    }

    private Path resolveDirectory() {
        try {
            return Files.createDirectories(rootLocation);
        } catch (IOException ioe) {
            throw new FileStorageException("Could not initialize storage", ioe);
        }
    }
}
