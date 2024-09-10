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
import com.bytechef.ee.file.storage.aws.service.AwsFileStorageService;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.file.storage.FilesFileStorageImpl;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.file.storage.TriggerFileStorageImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class PlatformFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PlatformFileStorageConfiguration.class);

    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public PlatformFileStorageConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    FilesFileStorage filesFileStorage(ApplicationProperties applicationProperties) {
        FileStorage.Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        if (logger.isInfoEnabled()) {
            String providerName = provider.name();

            logger.info(
                "Workflow trigger output storage provider type enabled: %s".formatted(providerName.toLowerCase()));
        }

        return new FilesFileStorageImpl(getFilesFileStorageService(provider));
    }

    @Bean
    TriggerFileStorage triggerFileStorage(ApplicationProperties applicationProperties) {
        OutputStorage.Provider provider = applicationProperties.getWorkflow()
            .getOutputStorage()
            .getProvider();

        if (logger.isInfoEnabled()) {
            String providerName = provider.name();

            logger.info("Files storage provider type enabled: %s".formatted(providerName.toLowerCase()));
        }

        return new TriggerFileStorageImpl(getTriggerFileStorageService(provider));
    }

    private FileStorageService getFilesFileStorageService(FileStorage.Provider provider) {
        return switch (provider) {
            case FileStorage.Provider.AWS -> new AwsFileStorageService();
            case FileStorage.Provider.FILESYSTEM -> new FilesystemFileStorageService(getBasedir());
            case FileStorage.Provider.JDBC -> new Base64FileStorageService();
        };
    }

    private FileStorageService getTriggerFileStorageService(OutputStorage.Provider provider) {
        return switch (provider) {
            case OutputStorage.Provider.AWS -> new AwsFileStorageService();
            case OutputStorage.Provider.FILESYSTEM -> new FilesystemFileStorageService(getBasedir());
            case OutputStorage.Provider.JDBC -> new Base64FileStorageService();
        };
    }

    private String getBasedir() {
        return applicationProperties.getFileStorage()
            .getFilesystem()
            .getBasedir();
    }
}
