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
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.config.Base64FileStorageConfiguration;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;
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
 * Shared Spring Boot integration-test configuration for the AI Gateway service module.
 *
 * @version ee
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.automation.ai.gateway.service",
        "com.bytechef.ee.automation.ai.gateway.cleanup",
        "com.bytechef.ee.automation.ai.gateway.budget",
        "com.bytechef.ee.automation.ai.gateway.ratelimit",
        "com.bytechef.ee.automation.ai.gateway.routing",
        "com.bytechef.ee.automation.ai.gateway.cost",
        "com.bytechef.ee.automation.ai.gateway.evaluation",
        "com.bytechef.ee.automation.ai.gateway.facade",
        // AiGatewayFacadeImpl applies the inline content guardrails on both the sync and the streaming path, so
        // AiGatewayGuardrails is a mandatory collaborator. Mirror production wiring by scanning the package rather
        // than registering a stub, so the guardrail chain the facade runs in production is the one under test.
        "com.bytechef.ee.automation.ai.gateway.guardrail",
        // The standalone guardrail engine (platform-ai-guardrails) that the adapter above delegates to — AiGuardrails,
        // AiGuardrailMetrics, and the prompt-based classifiers.
        "com.bytechef.ee.platform.ai.guardrails",
        // Pulls in AiObservabilityExportExecutor's collaborators that live in the moved/extracted EE platform
        // modules: AiLlmUsageServiceImpl (llm-usage), AiPromptServiceImpl + AiPromptVersionServiceImpl (prompt),
        // and the workspace-agnostic platform-ai-gateway services + cross-cutting helpers (cache, compression,
        // cost, metrics, provider factories, retry handler, routing strategies, in-memory rate limiter). Also
        // pulls in the automation-side workspace-scoping services for eval + prompt that gateway facades
        // collaborate with (AiExternalScoreFacadeImpl, AiGatewayFacade).
        // Without these the integration context fails to start with NoSuchBeanDefinitionException because
        // @SpringBootTest(classes = ...) does not anchor scanning at an application package.
        "com.bytechef.ee.automation.ai.eval",
        "com.bytechef.ee.automation.ai.observability",
        "com.bytechef.ee.automation.ai.prompt",
        "com.bytechef.ee.platform.ai.eval",
        "com.bytechef.ee.platform.ai.gateway",
        "com.bytechef.ee.platform.ai.model.catalog",
        "com.bytechef.ee.platform.ai.llm.usage",
        "com.bytechef.ee.platform.ai.observability",
        "com.bytechef.ee.platform.ai.prompt",
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

    // AiObservabilityWebhookDeliveryServiceImpl autowires a TaskScheduler for its delayed-retry logic. No other
    // wiring in this test context supplies one (Spring Boot does not register a default TaskScheduler), so the
    // integration context fails to start with NoSuchBeanDefinitionException. A small single-thread pool is enough
    // for the tests, which don't exercise the scheduled-retry path under load.
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

        // Without these converters Spring Data JDBC treats EncryptedStringWrapper as a @MappedCollection and
        // emits a LEFT OUTER JOIN on a non-existent `encrypted_string_wrapper` table, which surfaces as a
        // BadSqlGrammarException on every query that touches AiGatewayProvider.apiKey or
        // AiObservabilityWebhookSubscription.secret. Mirror the production wiring from JdbcConfiguration.
        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new EncryptedStringWrapperToStringConverter(encryption),
                new StringToEncryptedStringWrapperConverter(encryption));
        }
    }
}
