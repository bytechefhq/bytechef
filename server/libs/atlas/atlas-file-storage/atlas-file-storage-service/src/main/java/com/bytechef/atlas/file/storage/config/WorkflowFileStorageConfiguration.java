
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
import com.bytechef.file.storage.config.FileStorageProperties;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowFileStorageConfiguration {

    @Bean
    @ConditionalOnProperty("bytechef.workflow.async.output-storage.provider")
    WorkflowFileStorageFacade workflowAsyncFileStorageFacade(
        FileStorageProperties fileStorageProperties, ObjectMapper objectMapper,
        @Value("${bytechef.workflow.async.output-storage.provider}") String workflowAsyncOutputStorageProvider) {

        return new WorkflowFileStorageFacadeImpl(
            getFileStorageService(fileStorageProperties, workflowAsyncOutputStorageProvider), objectMapper);
    }

    @Bean
    @ConditionalOnProperty("bytechef.workflow.sync.output-storage.provider")
    WorkflowFileStorageFacade workflowSyncFileStorageFacade(
        FileStorageProperties fileStorageProperties, ObjectMapper objectMapper,
        @Value("${bytechef.workflow.sync.output-storage.provider}") String workflowAsyncOutputStorageProvider) {

        return new WorkflowFileStorageFacadeImpl(
            getFileStorageService(fileStorageProperties, workflowAsyncOutputStorageProvider), objectMapper);
    }

    private static FileStorageService getFileStorageService(
        FileStorageProperties fileStorageProperties, String workflowAsyncOutputStorageProvider) {

        return switch (workflowAsyncOutputStorageProvider) {
            case "base64" -> new Base64FileStorageService();
            case "filesystem" -> new FilesystemFileStorageService(fileStorageProperties.getFilesystemDir());
            default -> throw new IllegalArgumentException(
                "Output storage %s does not exist".formatted(workflowAsyncOutputStorageProvider));
        };
    }
}
