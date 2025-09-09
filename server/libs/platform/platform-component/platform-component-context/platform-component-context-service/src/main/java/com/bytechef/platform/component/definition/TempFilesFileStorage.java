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

package com.bytechef.platform.component.definition;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.constant.PlatformConstants;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.file.storage.FilesFileStorageImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public final class TempFilesFileStorage implements FilesFileStorage {

    private final FilesFileStorage filesFileStorage;

    public TempFilesFileStorage() {
        Path tempDirPath;

        try {
            tempDirPath = Files.createTempDirectory("files_file_storage");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File tempDir = tempDirPath.toFile();

        this.filesFileStorage = new FilesFileStorageImpl(new FilesystemFileStorageService(tempDir.getAbsolutePath()));
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        return filesFileStorage.getFileStream(fileEntry);
    }

    @Override
    public String readFileToString(FileEntry fileEntry) {
        if (Objects.equals(fileEntry.getUrl(), PlatformConstants.FILE_ENTRY_SAMPLE_URL)) {
            return "This is a sample file content";
        }

        return filesFileStorage.readFileToString(fileEntry);
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return filesFileStorage.storeFileContent(fileName, data);
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        return filesFileStorage.storeFileContent(fileName, inputStream);
    }
}
