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

package com.bytechef.platform.apiconnector.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.file.storage.aws.AwsFileStorageService;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorageImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class ApiConnectorFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ApiConnectorFileStorageConfiguration.class);

    private final ApplicationProperties applicationProperties;
    private final AwsFileStorageService awsFileStorageService;

    @SuppressFBWarnings("EI")
    public ApiConnectorFileStorageConfiguration(
        ApplicationProperties applicationProperties,
        @Autowired(required = false) AwsFileStorageService awsFileStorageService) {

        this.applicationProperties = applicationProperties;
        this.awsFileStorageService = awsFileStorageService;
    }

    @Bean
    ApiConnectorFileStorage apiConnectorFileStorage(ApplicationProperties applicationProperties) {
        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        if (provider == null) {
            provider = Provider.FILESYSTEM;
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                "API connector file storage provider type enabled: %s".formatted(
                    StringUtils.lowerCase(provider.name())));
        }

        return new ApiConnectorFileStorageImpl(getFileStorageService(provider));
    }

    private FileStorageService getFileStorageService(Provider provider) {
        return switch (provider) {
            case Provider.AWS -> awsFileStorageService;
            default -> new FilesystemFileStorageService(getBasedir());
        };
    }

    private String getBasedir() {
        return applicationProperties.getFileStorage()
            .getFilesystem()
            .getBasedir();
    }
}
