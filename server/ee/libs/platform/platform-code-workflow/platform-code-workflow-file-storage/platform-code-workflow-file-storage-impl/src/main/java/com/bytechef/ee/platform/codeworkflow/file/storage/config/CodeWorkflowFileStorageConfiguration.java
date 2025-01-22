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

package com.bytechef.ee.platform.codeworkflow.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorageImpl;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class CodeWorkflowFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CodeWorkflowFileStorageConfiguration.class);

    @Bean
    CodeWorkflowFileStorage codeWorkflowFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {
        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        if (provider == null) {
            provider = Provider.FILESYSTEM;
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                "Code workflow file storage provider type enabled: %s".formatted(
                    StringUtils.lowerCase(provider.name())));
        }

        return new CodeWorkflowFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
