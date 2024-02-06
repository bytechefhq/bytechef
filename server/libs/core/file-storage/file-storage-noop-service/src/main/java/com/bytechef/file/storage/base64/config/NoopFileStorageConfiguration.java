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

package com.bytechef.file.storage.base64.config;

import com.bytechef.file.storage.base64.service.NoopFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "file-storage.provider", havingValue = "noop")
public class NoopFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NoopFileStorageConfiguration.class);

    public NoopFileStorageConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("File storage provider type enabled: base64");
        }
    }

    @Bean
    FileStorageService fileStorageService() {
        return new NoopFileStorageService();
    }
}
