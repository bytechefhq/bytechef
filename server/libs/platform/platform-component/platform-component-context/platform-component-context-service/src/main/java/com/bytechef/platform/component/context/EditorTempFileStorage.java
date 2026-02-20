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

package com.bytechef.platform.component.context;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Ivica Cardic
 */
class EditorTempFileStorage implements TempFileStorage {

    private static final String EDITOR_TEMP_DIR = "editor/temp";

    private final FileStorageService fileStorageService;

    EditorTempFileStorage(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public long getContentLength(FileEntry fileEntry) {
        return fileStorageService.getContentLength(EDITOR_TEMP_DIR, fileEntry);
    }

    public InputStream getInputStream(FileEntry fileEntry) {
        return fileStorageService.getInputStream(EDITOR_TEMP_DIR, fileEntry);
    }

    public OutputStream getOutputStream(FileEntry fileEntry) {
        return fileStorageService.getOutputStream(EDITOR_TEMP_DIR, fileEntry);
    }

    public String readFileToString(FileEntry fileEntry) {
        return fileStorageService.readFileToString(EDITOR_TEMP_DIR, fileEntry);
    }

    public FileEntry storeFileContent(String filename, String data) {
        return fileStorageService.storeFileContent(EDITOR_TEMP_DIR, filename, data);
    }

    public FileEntry storeFileContent(String filename, InputStream inputStream) {
        return fileStorageService.storeFileContent(EDITOR_TEMP_DIR, filename, inputStream);
    }
}
