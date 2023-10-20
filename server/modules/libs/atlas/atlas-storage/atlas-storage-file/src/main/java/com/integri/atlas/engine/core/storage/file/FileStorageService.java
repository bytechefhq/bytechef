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

package com.integri.atlas.engine.core.storage.file;

import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.storage.exception.StorageException;
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
public class FileStorageService implements StorageService {

    private final Path rootLocation;

    public FileStorageService(String fileStorageDir) {
        this.rootLocation = Paths.get(fileStorageDir);
    }

    @Override
    public String write(String bucketName, String fileName, InputStream inputStream) throws StorageException {
        if (fileName.contains("..")) {
            // This is a security check

            throw new StorageException("Cannot store file with relative path outside current directory " + fileName);
        }

        Path path = resolveDirectory(bucketName);

        path = path.resolve(fileName);

        try {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new StorageException("Failed to store file " + fileName, ioe);
        }

        File file = path.toFile();

        if (file.length() == 0) {
            throw new StorageException("Failed to store empty file " + fileName);
        }

        return path.toString();
    }

    @Override
    public InputStream openInputStream(String bucketName, String fileName) {
        Path path = resolveDirectory(bucketName);

        try {
            return Files.newInputStream(path.resolve(fileName), StandardOpenOption.READ);
        } catch (IOException ioe) {
            throw new StorageException("Failed to open file " + fileName, ioe);
        }
    }

    private Path resolveDirectory(String bucketName) {
        try {
            return Files.createDirectories(rootLocation.resolve(bucketName));
        } catch (IOException ioe) {
            throw new StorageException("Could not initialize storage", ioe);
        }
    }
}
