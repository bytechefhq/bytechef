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
        "com.bytechef.ee.platform.ai.eval.dataset.service",
        "com.bytechef.ee.platform.ai.eval.experiment.service",
        "com.bytechef.encryption",
        "com.bytechef.file.storage"
    },
    excludeFilters = {
        @Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayChatCompletionApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayEmbeddingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayModelApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.gateway\\.public_\\.web\\.rest\\.AiGatewayRoutingApiController",
                "com\\.bytechef\\.ee\\.automation\\.ai\\.eval\\.experiment\\.public_\\.web\\.rest\\..+Test.*",
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

        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new EncryptedStringWrapperToStringConverter(encryption),
                new StringToEncryptedStringWrapperConverter(encryption));
        }
    }
}
