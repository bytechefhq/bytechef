/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.aws.secretsmanager;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.credential.store.aws.secretsmanager.config.AwsSecretsManagerCredentialStoreConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(ApplicationProperties.class)
@ImportAutoConfiguration({
    io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration.class,
    io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration.class,
    io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration.class,
    io.awspring.cloud.autoconfigure.config.secretsmanager.SecretsManagerAutoConfiguration.class
})
@Import(AwsSecretsManagerCredentialStoreConfiguration.class)
class AwsSecretsManagerCredentialStoreIntTestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .build();
    }
}
