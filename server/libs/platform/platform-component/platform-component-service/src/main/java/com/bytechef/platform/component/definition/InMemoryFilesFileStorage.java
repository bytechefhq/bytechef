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

package com.bytechef.platform.component.definition;

import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.file.storage.FilesFileStorageImpl;
import java.io.InputStream;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class InMemoryFilesFileStorage implements FilesFileStorage {

    private final FilesFileStorage filesFileStorage = new FilesFileStorageImpl(new Base64FileStorageService());

    @Override
    public InputStream getFileStream(@NonNull FileEntry fileEntry) {
        return filesFileStorage.getFileStream(fileEntry);
    }

    @Override
    public String readFileToString(@NonNull FileEntry fileEntry) {
        return filesFileStorage.readFileToString(fileEntry);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String fileName, @NonNull String data) {
        return filesFileStorage.storeFileContent(fileName, data);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String submittedFileName, @NonNull InputStream inputStream) {
        return filesFileStorage.storeFileContent(submittedFileName, inputStream);
    }
}
