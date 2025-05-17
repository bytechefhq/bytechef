/*
 * Copyright 2025 ByteChef
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

    @Bean
    CustomComponentFileStorage customComponentFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        return new CustomComponentFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
