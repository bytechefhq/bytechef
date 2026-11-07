/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.config;

import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.platform.component.rule")
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class ComponentRuleIntTestConfiguration {

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.builder()
            .build();
    }

    @Bean
    PropertyService propertyService() {
        return Mockito.mock(PropertyService.class);
    }

    @Bean
    JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver() {
        return Mockito.mock(JobPrincipalWorkspaceResolver.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ComponentRuleIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
