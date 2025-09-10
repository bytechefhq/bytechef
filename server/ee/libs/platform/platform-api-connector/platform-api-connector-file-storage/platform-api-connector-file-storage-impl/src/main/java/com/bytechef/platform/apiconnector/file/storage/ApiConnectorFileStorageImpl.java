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

package com.bytechef.platform.apiconnector.file.storage;

import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class ApiConnectorFileStorageImpl implements ApiConnectorFileStorage {

    private static final String API_CONNECTORS_DEFINITIONS_DIR = "api_connectors/definitions";
    private static final String API_CONNECTORS_SPECIFICATIONS_DIR = "api_connectors/specifications";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public ApiConnectorFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteApiConnectorDefinition(FileEntry fileEntry) {
        fileStorageService.deleteFile(API_CONNECTORS_DEFINITIONS_DIR, fileEntry);
    }

    @Override
    public void deleteApiConnectorSpecification(FileEntry fileEntry) {
        fileStorageService.deleteFile(API_CONNECTORS_SPECIFICATIONS_DIR, fileEntry);
    }

    @Override
    public String readApiConnectorDefinition(FileEntry fileEntry) {
        return CompressionUtils.decompressToString(
            fileStorageService.readFileToBytes(API_CONNECTORS_DEFINITIONS_DIR, fileEntry));
    }

    @Override
    public String readApiConnectorSpecification(FileEntry fileEntry) {
        return CompressionUtils.decompressToString(
            fileStorageService.readFileToBytes(API_CONNECTORS_SPECIFICATIONS_DIR, fileEntry));
    }

    @Override
    public FileEntry storeApiConnectorDefinition(String filename, String definition) {
        return fileStorageService.storeFileContent(
            API_CONNECTORS_DEFINITIONS_DIR, filename, CompressionUtils.compress(definition));
    }

    @Override
    public FileEntry storeApiConnectorSpecification(String filename, String definition) {
        return fileStorageService.storeFileContent(
            API_CONNECTORS_SPECIFICATIONS_DIR, filename, CompressionUtils.compress(definition));
    }
}
