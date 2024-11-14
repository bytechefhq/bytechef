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

package com.bytechef.platform.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class FilesFileStorageImpl implements FilesFileStorage {

    public static final String FILES_DIR = "files";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FilesFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public InputStream getFileStream(@NonNull FileEntry fileEntry) {
        return fileStorageService.getFileStream(FILES_DIR, fileEntry);
    }

    @Override
    public String readFileToString(@NonNull FileEntry fileEntry) {
        return fileStorageService.readFileToString(FILES_DIR, fileEntry);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String fileName, @NonNull String data) {
        return fileStorageService.storeFileContent(FILES_DIR, fileName, data);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String submittedFileName, @NonNull InputStream inputStream) {
        return fileStorageService.storeFileContent(FILES_DIR, submittedFileName, inputStream);
    }
}
