/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.config;

import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Integration-test configuration for the AI Gateway dataset module. Scans only the dataset service package — the impl
 * classes are package-private and therefore cannot be referenced from a cross-package {@code @Import}. Mirrors
 * {@code AiExperimentIntTestConfiguration} but kept narrow to the dataset module's own packages so the test classpath
 * stays minimal.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ComponentScan(basePackages = "com.bytechef.ee.platform.ai.eval.dataset.service")
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AiEvalDatasetIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiEvalDatasetIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
