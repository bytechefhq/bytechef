/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Repository scanning deliberately relies on the module's own {@code ResourceGrantJdbcRepositoryConfiguration}
 * auto-configuration rather than declaring {@code @EnableJdbcRepositories} here. Declaring it locally made this test
 * pass while the application context still had no repository bean — the test proved its own wiring instead of the
 * production wiring.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.platform.resource.grant")
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class ResourceGrantIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ResourceGrantIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
