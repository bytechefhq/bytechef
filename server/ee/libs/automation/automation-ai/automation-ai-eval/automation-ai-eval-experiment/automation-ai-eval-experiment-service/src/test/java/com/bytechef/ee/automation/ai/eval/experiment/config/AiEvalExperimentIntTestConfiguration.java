/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

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
 * @author Ivica Cardic
 * @version ee
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.ee.automation.ai.eval.dataset.service",
        "com.bytechef.ee.automation.ai.eval.experiment.executor",
        "com.bytechef.ee.automation.ai.eval.experiment.service",
        "com.bytechef.ee.platform.ai.eval.dataset.service",
        "com.bytechef.ee.platform.ai.eval.experiment.service",
        "com.bytechef.encryption",
        "com.bytechef.file.storage"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    AiEvalExperimentAsyncConfiguration.class, Base64FileStorageConfiguration.class, JacksonConfiguration.class,
    LiquibaseConfiguration.class
})
@Configuration
public class AiEvalExperimentIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("aiEvalExperimentIntTestScheduler-");
        scheduler.initialize();

        return scheduler;
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiEvalExperimentIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;

        @SuppressFBWarnings("EI2")
        public AiEvalExperimentIntTestJdbcConfiguration(Encryption encryption) {
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
