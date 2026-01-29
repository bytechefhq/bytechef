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

package com.bytechef.automation.knowledgebase.file.storage.config;

import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorageImpl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
class KnowledgeBaseFileStorageConfiguration {

    @Bean
    KnowledgeBaseFileStorage knowledgeBaseFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        return new KnowledgeBaseFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
