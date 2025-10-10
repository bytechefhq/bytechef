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

package com.bytechef.platform.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Ivica Cardic
 */
public class TempFileStorageImpl implements TempFileStorage {

    public static final String TEMP_DIR = "temp";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public TempFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public long getContentLength(FileEntry fileEntry) {
        return fileStorageService.getContentLength(TEMP_DIR, fileEntry);
    }

    @Override
    public InputStream getInputStream(FileEntry fileEntry) {
        return fileStorageService.getInputStream(TEMP_DIR, fileEntry);
    }

    @Override
    public OutputStream getOutputStream(FileEntry fileEntry) {
        return fileStorageService.getOutputStream(TEMP_DIR, fileEntry);
    }

    @Override
    public String readFileToString(FileEntry fileEntry) {
        return fileStorageService.readFileToString(TEMP_DIR, fileEntry);
    }

    @Override
    public FileEntry storeFileContent(String filename, String data) {
        return fileStorageService.storeFileContent(TEMP_DIR, filename, data);
    }

    @Override
    public FileEntry storeFileContent(String filename, InputStream inputStream) {
        return fileStorageService.storeFileContent(TEMP_DIR, filename, inputStream);
    }
}
