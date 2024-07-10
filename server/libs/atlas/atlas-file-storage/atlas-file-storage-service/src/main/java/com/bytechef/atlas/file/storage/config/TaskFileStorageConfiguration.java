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

package com.bytechef.atlas.file.storage.config;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.OutputStorage.Provider;
import com.bytechef.ee.file.storage.aws.service.AwsFileStorageService;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.base64.service.NoopFileStorageService;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TaskFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TaskFileStorageConfiguration.class);

    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public TaskFileStorageConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    TaskFileStorage workflowTaskFileStorageFacade(ApplicationProperties applicationProperties) {
        Provider provider = applicationProperties.getWorkflow()
            .getOutputStorage()
            .getProvider();

        if (logger.isInfoEnabled()) {
            logger.info("Workflow task output storage provider type enabled: %s".formatted(provider));
        }

        return new TaskFileStorageImpl(getFileStorageService(provider));
    }

    private FileStorageService getFileStorageService(Provider provider) {
        return switch (provider) {
            case Provider.AWS -> new AwsFileStorageService();
            case Provider.BASE64 -> new Base64FileStorageService();
            case Provider.FILESYSTEM -> new FilesystemFileStorageService(getBasedir());
            case Provider.NOOP -> new NoopFileStorageService();
        };
    }

    private String getBasedir() {
        return applicationProperties.getFileStorage()
            .getFilesystem()
            .getBasedir();
    }
}
