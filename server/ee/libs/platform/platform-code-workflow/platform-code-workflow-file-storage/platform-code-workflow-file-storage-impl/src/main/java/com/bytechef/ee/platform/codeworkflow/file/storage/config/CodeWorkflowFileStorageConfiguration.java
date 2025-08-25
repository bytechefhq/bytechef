/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorageImpl;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public class CodeWorkflowFileStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CodeWorkflowFileStorageConfiguration.class);

    @Bean
    CodeWorkflowFileStorage codeWorkflowFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {
        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        return new CodeWorkflowFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
