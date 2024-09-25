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

package com.bytechef.platform.customcomponent.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class CustomComponentFileStorageImpl implements CustomComponentFileStorage {

    private static final String CUSTOM_COMPONENTS_FILES_DIR = "custom_components";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public CustomComponentFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteCustomComponentFile(FileEntry componentFile) {
        fileStorageService.deleteFile(CUSTOM_COMPONENTS_FILES_DIR, componentFile);
    }

    @Override
    public URL getCustomComponentFileURL(@NonNull FileEntry componentFile) {
        return fileStorageService.getFileEntryURL(CUSTOM_COMPONENTS_FILES_DIR, componentFile);
    }

    @Override
    public FileEntry storeCustomComponentFile(String filename, @NonNull byte[] bytes) {
        Validate.notNull(filename, "'filename' must not be null");
        Validate.notNull(bytes, "'bytes' must not be null");

        return fileStorageService.storeFileContent(CUSTOM_COMPONENTS_FILES_DIR, filename, bytes, false);
    }
}
