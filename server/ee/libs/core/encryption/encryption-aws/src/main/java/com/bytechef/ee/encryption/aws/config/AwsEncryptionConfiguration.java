/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.config;

import com.bytechef.ee.encryption.aws.AwsEncryptionKey;
import com.bytechef.encryption.EncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Caardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "aws")
public class AwsEncryptionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsEncryptionConfiguration.class);

    public AwsEncryptionConfiguration() {
        if (log.isInfoEnabled()) {
            log.info("Encryption provider type enabled: aws");
        }
    }

    @Bean
    EncryptionKey encryptionKey() {
        return new AwsEncryptionKey();
    }
}
