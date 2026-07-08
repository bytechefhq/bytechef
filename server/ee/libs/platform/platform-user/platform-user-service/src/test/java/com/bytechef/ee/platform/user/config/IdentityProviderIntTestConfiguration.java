/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jdbc.config.AuditingJdbcConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.encryption", "com.bytechef.ee.platform.user"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    AuditingJdbcConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@Configuration
public class IdentityProviderIntTestConfiguration extends AbstractJdbcConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }
}
