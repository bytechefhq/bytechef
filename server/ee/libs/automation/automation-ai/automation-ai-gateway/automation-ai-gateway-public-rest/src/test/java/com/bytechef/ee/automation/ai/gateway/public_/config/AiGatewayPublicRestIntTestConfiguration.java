/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.config;

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
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Shared Spring Boot integration-test configuration for the AI Gateway public REST module. Boots the controller +
 * facade + services + repositories against a real Testcontainers Postgres, bypassing the API-key filter chain so HTTP
 * tests can exercise the plain request/response path.
 *
 * @author Ivica Cardic
 * @version ee
 */
// Narrow the scan to just the OTLP controller + its dependency chain. The chat/embedding/routing/model/score
// controllers pull in AiGatewayFacade which in turn drags in the whole provider/compression/evaluation/rag stack;
// those are orthogonal to OTLP ingest and would require mocking several more beans, so they are excluded below.
// AiGatewayFacadeImpl is excluded, and the gateway.facade beans that depend on it (or on collaborators outside this
// narrow scan -- AiEvalExecutor in gateway.evaluation) are excluded too. AiExternalScoreFacade is kept because
// AiExternalScoreController (a non-excluded controller) needs it.
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.automation.ai.eval",
        "com.bytechef.ee.automation.ai.gateway.facade",
        "com.bytechef.ee.automation.ai.gateway.public_.web.rest",
        "com.bytechef.ee.automation.ai.gateway.service",
        "com.bytechef.ee.automation.ai.gateway.cleanup",
        "com.bytechef.ee.automation.ai.gateway.budget",
        "com.bytechef.ee.automation.ai.gateway.ratelimit",
        "com.bytechef.ee.automation.ai.gateway.routing",
        "com.bytechef.ee.automation.ai.gateway.cost",
        "com.bytechef.ee.automation.ai.observability",
        "com.bytechef.ee.automation.ai.prompt",
        "com.bytechef.ee.platform.ai.eval",
        "com.bytechef.ee.platform.ai.gateway",
        "com.bytechef.ee.platform.ai.llm.usage",
        "com.bytechef.ee.platform.ai.observability",
        "com.bytechef.ee.platform.ai.prompt",
        "com.bytechef.encryption",
        "com.bytechef.file.storage"
    },
    excludeFilters = {
        @Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.facade\\.AiGatewayFacadeImpl",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.facade\\.AiEvalRuleFacadeImpl",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.facade\\.AiGatewayPlaygroundFacadeImpl",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayChatCompletionApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayEmbeddingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayModelApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayRoutingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayScoreApiController",
                // Nested test-scope configs that happen to live in the same packages (WebMvcTest slice fixtures)
                // would otherwise be picked up by this component scan and clash with real beans.
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\..+Test.*"
            })
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    Base64FileStorageConfiguration.class, JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
@SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
public class AiGatewayPublicRestIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("aiGatewayPublicRestIntTestScheduler-");
        scheduler.initialize();

        return scheduler;
    }

    // The production AiGatewaySecurityConfiguration is NOT imported into this context (it lives in
    // com.bytechef.ee.automation.ai.gateway.config which this test does not component-scan). That means no API-key
    // filter gets registered, but Spring Boot's default security auto-configuration still locks every request down
    // to authenticated-only. A single permitAll chain lets the HTTP tests hit the controller directly.
    // CSRF is disabled because OTLP clients cannot attach CSRF tokens — this is a test-only bean, not a production
    // security posture.
    @Bean
    @Primary
    SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest()
                .permitAll())
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiGatewayPublicRestIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;

        @SuppressFBWarnings("EI2")
        public AiGatewayPublicRestIntTestJdbcConfiguration(Encryption encryption) {
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
