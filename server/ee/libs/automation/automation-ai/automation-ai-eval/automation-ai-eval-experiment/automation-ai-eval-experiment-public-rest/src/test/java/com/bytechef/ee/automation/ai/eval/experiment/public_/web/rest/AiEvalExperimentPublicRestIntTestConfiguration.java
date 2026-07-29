/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.public_.web.rest;

import com.bytechef.commons.data.jdbc.converter.EncryptedStringWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToEncryptedStringWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.eval.experiment.config.AiEvalExperimentAsyncConfiguration;
import com.bytechef.ee.automation.ai.eval.experiment.config.AiEvalExperimentRetryConfiguration;
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
 * Integration-test configuration for the AI Gateway experiment public-rest module. Boots the controller + facade +
 * services + repositories against a real Testcontainers Postgres so HTTP tests can exercise the full Spring web stack.
 * Scans dataset + experiment service packages (whose impls are package-private and therefore cannot be referenced from
 * a cross-package {@code @Import}), plus the experiment public-rest controller package. Excludes the broader gateway
 * service scan (provider factories, chat-completion flow, RAG, etc.) which is orthogonal to experiments and pulls in
 * beans that would require extra mocks.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.automation.ai.eval.dataset.service",
        "com.bytechef.ee.automation.ai.eval.experiment.executor",
        "com.bytechef.ee.automation.ai.eval.experiment.public_.web.rest",
        "com.bytechef.ee.automation.ai.eval.experiment.service",
        "com.bytechef.ee.automation.ai.gateway.public_.web.rest",
        "com.bytechef.ee.automation.ai.observability.service",
        "com.bytechef.ee.platform.ai.eval.dataset.service",
        "com.bytechef.ee.platform.ai.eval.experiment.service",
        "com.bytechef.ee.platform.ai.observability.service",
        "com.bytechef.encryption",
        "com.bytechef.file.storage"
    },
    excludeFilters = {
        // The gateway's public_.web.rest package also contains the chat / embedding / routing controllers which drag
        // in AiGatewayFacade and the whole provider/compression/evaluation/rag stack. The experiment HTTP int test is
        // orthogonal to those; restrict the scan to the exception handler + workspace header resolver. Same idea
        // for the observability cleanup runner, alert evaluator, and exporter — they reach across the gateway/
        // workspace settings/scheduler surfaces that this slice doesn't exercise.
        @Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiExternalScoreController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayChatCompletionApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayEmbeddingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayModelApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayOtlpController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayRoutingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayRoutingPolicyTagApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayScoreApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.observability\\.service\\.AiObservabilityAlertEvaluator",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.observability\\.service\\.AiObservabilityExportExecutor",
                // The dispatcher lives in the platform observability package, not the automation one — it moved there
                // with the migration onto the central Notification registry, and it reads Notification rows through a
                // NotificationService this slice does not scan.
                "com\\.bytechef\\.ee\\.platform\\.ai\\.observability\\.service\\.AiObservabilityNotificationDispatcher",
                // Nested test-scope configs that happen to live in the same packages would otherwise be picked up.
                "com\\.bytechef\\.ee\\.automation\\.ai\\.eval\\.experiment\\.public_\\.web\\.rest\\..+Test.*",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.experiment\\.public_\\.web\\.rest\\..+Test.*",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\..+Test.*"
            })
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    AiEvalExperimentAsyncConfiguration.class, AiEvalExperimentRetryConfiguration.class,
    Base64FileStorageConfiguration.class, JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
@SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
public class AiEvalExperimentPublicRestIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("aiEvalExperimentPublicRestIntTestScheduler-");
        scheduler.initialize();

        return scheduler;
    }

    // Mirror AiGatewayPublicRestIntTestConfiguration: the production security chain is not on this test classpath, so
    // Spring Boot's default security auto-configuration would lock every request down to authenticated-only. A single
    // permitAll chain lets the HTTP tests hit the controller directly. CSRF is disabled because the API-key clients
    // these endpoints are designed for cannot attach CSRF tokens — this is a test-only bean, not a production posture.
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
    public static class AiEvalExperimentPublicRestIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;

        @SuppressFBWarnings("EI2")
        public AiEvalExperimentPublicRestIntTestJdbcConfiguration(Encryption encryption) {
            this.encryption = encryption;
        }

        // Required for any entities that use EncryptedStringWrapper — Spring Data JDBC otherwise treats the wrapper
        // as a @MappedCollection and emits a LEFT OUTER JOIN against a non-existent table.
        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new EncryptedStringWrapperToStringConverter(encryption),
                new StringToEncryptedStringWrapperConverter(encryption));
        }
    }
}
