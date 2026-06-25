/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.licence.Ed25519Verifier;
import com.bytechef.ee.platform.licence.LicenceCheckInTask;
import com.bytechef.ee.platform.licence.LicenceFileParser;
import com.bytechef.ee.platform.licence.OfflineLicenceManager;
import com.bytechef.ee.platform.licence.repository.LicenceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.licence.repository")
public class LicenceConfiguration {

    // Replace with the production Keygen account Ed25519 verify key (raw 32-byte hex).
    private static final String DEFAULT_PUBLIC_KEY =
        "30c2934a7fa474e3085d0ddaec73e31cbb5d607081b85522e27ec885234ef5ad";

    @Bean
    Ed25519Verifier ed25519Verifier(ApplicationProperties applicationProperties) {
        String publicKey = applicationProperties.getLicence()
            .getPublicKey();

        if (publicKey == null || publicKey.isBlank()) {
            publicKey = DEFAULT_PUBLIC_KEY;
        }

        return new Ed25519Verifier(publicKey);
    }

    @Bean
    LicenceFileParser licenceFileParser(Ed25519Verifier ed25519Verifier) {
        return new LicenceFileParser(ed25519Verifier);
    }

    @Bean
    OfflineLicenceManager licenceManager(
        LicenceFileParser licenceFileParser, LicenceRepository licenceRepository,
        ApplicationProperties applicationProperties) {

        return new OfflineLicenceManager(
            licenceFileParser, licenceRepository, Clock.systemUTC(),
            applicationProperties.getLicence()
                .getGracePeriodDays());
    }

    @Bean
    ApplicationRunner licenceBootstrapRunner(
        OfflineLicenceManager licenceManager, ApplicationProperties applicationProperties) {

        return args -> {
            String inline = System.getenv("BYTECHEF_LICENSE");

            licenceManager.bootstrap(
                applicationProperties.getLicence()
                    .getPath(),
                inline);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.licence.check-in", name = "enabled", havingValue = "true")
    LicenceCheckInTask licenceCheckInTask(
        OfflineLicenceManager licenceManager, ApplicationProperties applicationProperties) {

        return new LicenceCheckInTask(
            licenceManager, applicationProperties.getLicence(), HttpClient.newHttpClient());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.licence.check-in", name = "enabled", havingValue = "true")
    SchedulingConfigurer licenceCheckInScheduler(
        LicenceCheckInTask licenceCheckInTask, ApplicationProperties applicationProperties) {

        return taskRegistrar -> taskRegistrar.addFixedRateTask(
            licenceCheckInTask::checkIn, applicationProperties.getLicence()
                .getCheckIn()
                .getInterval());
    }
}
