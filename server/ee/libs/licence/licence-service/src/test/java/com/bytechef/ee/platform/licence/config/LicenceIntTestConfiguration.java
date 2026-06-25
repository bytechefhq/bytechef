/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Spring Boot test configuration for licence service integration tests. Enables JDBC infrastructure and Liquibase
 * migrations, but does NOT import {@link com.bytechef.ee.platform.licence.config.LicenceConfiguration} (the
 * auto-configuration). Licence beans ({@link com.bytechef.ee.platform.licence.Ed25519Verifier},
 * {@link com.bytechef.ee.platform.licence.LicenceFileParser},
 * {@link com.bytechef.ee.platform.licence.OfflineLicenceManager}) are declared in the test class itself so the test
 * keypair can be injected.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(exclude = LicenceConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.licence.repository")
@Import(LiquibaseConfiguration.class)
@Configuration
public class LicenceIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class LicenceIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
