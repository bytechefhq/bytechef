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

import com.bytechef.ee.file.storage.aws.service.AwsFileStorageService;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.base64.service.NoopFileStorageService;
import com.bytechef.file.storage.filesystem.config.FilesystemFileStorageProperties;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.file.storage.TriggerFileStorageImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(FilesystemFileStorageProperties.class)
public class TriggerFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TriggerFileStorageConfiguration.class);

    private final FilesystemFileStorageProperties filesystemFileStorageProperties;

    @SuppressFBWarnings("EI")
    public TriggerFileStorageConfiguration(FilesystemFileStorageProperties filesystemFileStorageProperties) {
        this.filesystemFileStorageProperties = filesystemFileStorageProperties;
    }

    @Bean
    TriggerFileStorage workflowTriggerFileStorageFacade(
        @Value("${bytechef.workflow.output-storage.provider}") String workflowOutputStorageProvider) {

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow trigger output storage provider type enabled: %s".formatted(workflowOutputStorageProvider));
        }

        return new TriggerFileStorageImpl(getFileStorageService(workflowOutputStorageProvider));
    }

    private FileStorageService getFileStorageService(String workflowOutputStorageProvider) {
        return switch (workflowOutputStorageProvider) {
            case "aws" -> new AwsFileStorageService();
            case "base64" -> new Base64FileStorageService();
            case "filesystem" -> new FilesystemFileStorageService(filesystemFileStorageProperties.getBasedir());
            case "noop" -> new NoopFileStorageService();
            default -> throw new IllegalArgumentException(
                "Output storage %s does not exist".formatted(workflowOutputStorageProvider));
        };
    }
}
