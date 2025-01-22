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

package com.bytechef.platform.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage;
import com.bytechef.config.ApplicationProperties.Workflow.OutputStorage;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.file.storage.FilesFileStorageImpl;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.file.storage.TriggerFileStorageImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class FileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfiguration.class);

    @Bean
    FilesFileStorage filesFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        FileStorage.Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow trigger output storage provider type enabled: %s".formatted(
                    StringUtils.lowerCase(provider.name())));
        }

        return new FilesFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }

    @Bean
    TriggerFileStorage triggerFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        OutputStorage.Provider provider = applicationProperties.getWorkflow()
            .getOutputStorage()
            .getProvider();

        if (logger.isInfoEnabled()) {
            String providerName = provider.name();

            logger.info("Files storage provider type enabled: %s".formatted(providerName.toLowerCase()));
        }

        return new TriggerFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
