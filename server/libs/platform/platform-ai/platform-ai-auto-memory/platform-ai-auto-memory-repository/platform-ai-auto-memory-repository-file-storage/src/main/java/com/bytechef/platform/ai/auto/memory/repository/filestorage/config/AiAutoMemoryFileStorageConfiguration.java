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

package com.bytechef.platform.ai.auto.memory.repository.filestorage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.AutoMemory;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.ai.auto.memory.repository.filestorage.FileStorageAiAutoMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AiAutoMemoryFileStorageConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "bytechef.ai.auto-memory", name = "provider", havingValue = "filesystem")
    static class FilesystemProviderConfiguration {

        @Bean
        FileStorageAiAutoMemoryRepository fileStorageAiAutoMemoryRepository(
            ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

            return createRepository(applicationProperties, fileStorageServiceRegistry);
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "bytechef.ai.auto-memory", name = "provider", havingValue = "aws")
    static class AwsProviderConfiguration {

        @Bean
        FileStorageAiAutoMemoryRepository fileStorageAiAutoMemoryRepository(
            ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

            return createRepository(applicationProperties, fileStorageServiceRegistry);
        }
    }

    static FileStorageAiAutoMemoryRepository createRepository(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        AutoMemory autoMemory = applicationProperties.getAi()
            .getAutoMemory();

        AutoMemory.Provider provider = autoMemory.getProvider();

        FileStorageService fileStorageService = fileStorageServiceRegistry.getFileStorageService(provider.name());

        if (fileStorageService == null) {
            throw new IllegalStateException(
                "bytechef.ai.auto-memory.provider=" + provider + " but no FileStorageService of type " +
                    provider.name() + " is registered. For AWS, ensure the EE file-storage-aws module is on the " +
                    "classpath and configured.");
        }

        return new FileStorageAiAutoMemoryRepository(fileStorageService);
    }
}
