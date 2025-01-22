/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorageImpl;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class CustomComponentFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CustomComponentFileStorageConfiguration.class);

    @Bean
    CustomComponentFileStorage customComponentFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

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

        return new CustomComponentFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
