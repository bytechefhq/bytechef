/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.platform.component.policy")
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class ComponentPolicyIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ComponentPolicyIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
