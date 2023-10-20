
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

package com.bytechef.atlas.file.storage.config;

import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.filesystem.config.FilesystemFileStorageProperties;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(FilesystemFileStorageProperties.class)
public class WorkflowFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowFileStorageConfiguration.class);

    private final FilesystemFileStorageProperties filesystemFileStorageProperties;

    @SuppressFBWarnings("EI")
    public WorkflowFileStorageConfiguration(FilesystemFileStorageProperties filesystemFileStorageProperties) {
        this.filesystemFileStorageProperties = filesystemFileStorageProperties;
    }

    @Bean
    @ConditionalOnProperty("bytechef.workflow.async.output-storage.provider")
    WorkflowFileStorageFacade workflowAsyncFileStorageFacade(
        ObjectMapper objectMapper,
        @Value("${bytechef.workflow.async.output-storage.provider}") String workflowAsyncOutputStorageProvider) {

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow async output storage provider type enabled: %s".formatted(
                    workflowAsyncOutputStorageProvider));
        }

        return new WorkflowFileStorageFacadeImpl(
            getFileStorageService(workflowAsyncOutputStorageProvider), objectMapper);
    }

    @Bean
    @ConditionalOnProperty("bytechef.workflow.sync.output-storage.provider")
    WorkflowFileStorageFacade workflowSyncFileStorageFacade(
        ObjectMapper objectMapper,
        @Value("${bytechef.workflow.sync.output-storage.provider}") String workflowSyncOutputStorageProvider) {

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow sync output storage provider type enabled: %s".formatted(workflowSyncOutputStorageProvider));
        }

        return new WorkflowFileStorageFacadeImpl(
            getFileStorageService(workflowSyncOutputStorageProvider), objectMapper);
    }

    private FileStorageService getFileStorageService(String workflowAsyncOutputStorageProvider) {
        return switch (workflowAsyncOutputStorageProvider) {
            case "base64" -> new Base64FileStorageService();
            case "filesystem" -> new FilesystemFileStorageService(filesystemFileStorageProperties.getBasedir());
            default -> throw new IllegalArgumentException(
                "Output storage %s does not exist".formatted(workflowAsyncOutputStorageProvider));
        };
    }
}
