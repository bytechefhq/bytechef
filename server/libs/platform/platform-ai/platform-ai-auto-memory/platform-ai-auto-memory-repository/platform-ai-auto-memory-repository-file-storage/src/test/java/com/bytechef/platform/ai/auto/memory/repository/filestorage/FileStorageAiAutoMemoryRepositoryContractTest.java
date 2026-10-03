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

package com.bytechef.platform.ai.auto.memory.repository.filestorage;

import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepositoryContractTests;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the shared repository contract against the file-storage binding, backed by the filesystem provider.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class FileStorageAiAutoMemoryRepositoryContractTest extends AiAutoMemoryRepositoryContractTests {

    @TempDir
    private Path tempDir;

    private FileStorageAiAutoMemoryRepository fileStorageAiAutoMemoryRepository;

    @BeforeEach
    void beforeEach() {
        fileStorageAiAutoMemoryRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()));
    }

    @Override
    protected AiAutoMemoryRepository getAiAutoMemoryRepository() {
        return fileStorageAiAutoMemoryRepository;
    }
}
