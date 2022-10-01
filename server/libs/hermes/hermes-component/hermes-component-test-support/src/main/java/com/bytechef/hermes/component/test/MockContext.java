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

package com.bytechef.hermes.component.test;

import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.impl.FileEntryImpl;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class MockContext implements Context {

    private static final FileStorageService fileStorageService = new Base64FileStorageService();

    public MockContext() {}

    @Override
    public Optional<ConnectionParameters> fetchConnection() {
        return Optional.empty();
    }

    @Override
    public ConnectionParameters getConnection() {
        return null;
    }

    @Override
    public InputStream getFileStream(FileEntry fileEntry) {
        return fileStorageService.getFileStream(
                com.bytechef.hermes.file.storage.domain.FileEntry.of(fileEntry.toMap()));
    }

    @Override
    public void publishProgressEvent(int progress) {}

    @Override
    public String readFileToString(FileEntry fileEntry) {
        return fileStorageService.readFileToString(
                com.bytechef.hermes.file.storage.domain.FileEntry.of(fileEntry.toMap()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, String data) {
        return new FileEntryImpl(fileStorageService.storeFileContent(fileName, data));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        return new FileEntryImpl(fileStorageService.storeFileContent(fileName, inputStream));
    }
}
