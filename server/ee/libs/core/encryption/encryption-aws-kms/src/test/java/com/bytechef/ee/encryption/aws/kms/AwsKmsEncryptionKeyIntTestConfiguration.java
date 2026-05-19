/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.cloud.aws.config.AwsCloudProviderConfiguration;
import com.bytechef.ee.encryption.aws.kms.config.AwsKmsEncryptionConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    AwsCloudProviderConfiguration.class, AwsKmsEncryptionConfiguration.class
})
class AwsKmsEncryptionKeyIntTestConfiguration {
}
