/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.file.storage.aws.config;

import com.bytechef.ee.file.storage.aws.service.AwsFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "file-storage.provider", havingValue = "aws")
public class AwsFileStorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsFileStorageConfiguration.class);

    public AwsFileStorageConfiguration() {
        if (log.isInfoEnabled()) {
            log.info("File storage provider type enabled: aws");
        }
    }

    @Bean
    FileStorageService fileStorageService() {
        return new AwsFileStorageService();
    }
}
