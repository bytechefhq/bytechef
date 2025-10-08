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

/**
 * @author Ivica Cardic
 */
public class SharedTemplateFileStorageImpl implements SharedTemplateFileStorage {

    public static final String SHARED_TEMPLATES_DIR = "shared_templates";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public SharedTemplateFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteFile(FileEntry fileEntry) {
        fileStorageService.deleteFile(SHARED_TEMPLATES_DIR, fileEntry);
    }

    @Override
    public InputStream getInputStream(FileEntry fileEntry) {
        return fileStorageService.getInputStream(SHARED_TEMPLATES_DIR, fileEntry);
    }

    @Override
    public FileEntry storeFileContent(String filename, InputStream inputStream) {
        return fileStorageService.storeFileContent(SHARED_TEMPLATES_DIR, filename, inputStream);
    }
}
