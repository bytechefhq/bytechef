/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.file.storage.aws.AwsFileStorageService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorageImpl;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class CustomComponentFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CustomComponentFileStorageConfiguration.class);

    private final ApplicationProperties applicationProperties;
    private final AwsFileStorageService awsFileStorageService;

    @SuppressFBWarnings("EI")
    public CustomComponentFileStorageConfiguration(
        ApplicationProperties applicationProperties,
        @Autowired(required = false) AwsFileStorageService awsFileStorageService) {

        this.applicationProperties = applicationProperties;
        this.awsFileStorageService = awsFileStorageService;
    }

    @Bean
    CustomComponentFileStorage customComponentFileStorage(ApplicationProperties applicationProperties) {
        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        if (provider == null) {
            provider = Provider.FILESYSTEM;
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                "Custom component file storage provider type enabled: %s".formatted(
                    StringUtils.lowerCase(provider.name())));
        }

        return new CustomComponentFileStorageImpl(getFileStorageService(provider));
    }

    private FileStorageService getFileStorageService(Provider provider) {
        return switch (provider) {
            case Provider.AWS -> awsFileStorageService;
            case Provider.FILESYSTEM -> new FilesystemFileStorageService(getBasedir());
            case Provider.JDBC -> new Base64FileStorageService();
        };
    }

    private String getBasedir() {
        return applicationProperties.getFileStorage()
            .getFilesystem()
            .getBasedir();
    }
}
