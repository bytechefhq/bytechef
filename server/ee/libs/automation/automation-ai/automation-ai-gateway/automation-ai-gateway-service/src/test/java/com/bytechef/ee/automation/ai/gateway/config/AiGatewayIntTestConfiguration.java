/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedStringWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToEncryptedStringWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.config.Base64FileStorageConfiguration;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.automation.ai.gateway.service",
        "com.bytechef.ee.automation.ai.gateway.cleanup",
        "com.bytechef.ee.automation.ai.gateway.budget",
        "com.bytechef.ee.automation.ai.gateway.ratelimit",
        "com.bytechef.ee.automation.ai.gateway.routing",
        "com.bytechef.ee.automation.ai.gateway.cost",
        "com.bytechef.ee.automation.ai.gateway.facade",
        "com.bytechef.ee.automation.ai.gateway.guardrail",
        "com.bytechef.ee.platform.ai.guardrails",
        "com.bytechef.ee.automation.ai.observability",
        "com.bytechef.ee.platform.ai.gateway",
        "com.bytechef.ee.platform.ai.model.catalog",
        "com.bytechef.ee.platform.ai.llm.usage",
        "com.bytechef.ee.platform.ai.observability",
        "com.bytechef.ee.embedded.connected.user.service",
        "com.bytechef.ee.embedded.connected.user.event",
        "com.bytechef.ee.embedded.connected.user.gateway.facade",
        "com.bytechef.encryption",
        "com.bytechef.file.storage"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    Base64FileStorageConfiguration.class, JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AiGatewayIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    SensitiveDataRedactor sensitiveDataRedactor() {
        return new SensitiveDataRedactor(List.of());
    }

    @Bean
    JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver() {
        return Mockito.mock(JobPrincipalWorkspaceResolver.class);
    }

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("aiGatewayIntTestScheduler-");
        scheduler.initialize();

        return scheduler;
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiGatewayIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;

        @SuppressFBWarnings("EI2")
        public AiGatewayIntTestJdbcConfiguration(Encryption encryption) {
            this.encryption = encryption;
        }

        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new EncryptedStringWrapperToStringConverter(encryption),
                new StringToEncryptedStringWrapperConverter(encryption));
        }
    }
}
