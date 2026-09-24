/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.config;

import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Test sources share the scanned {@code com.bytechef.ee.platform.audit} tree with production ones, so the exclude
 * filter keeps the classes nested inside a sibling integration test out of this context. Several of those nested
 * {@code Config} classes contribute a mock {@code AuditEventService} bean under the same default bean name
 * ("auditEventService") as the real scanned {@code @Service}. Reaching one would silently override the real bean
 * instead of producing an ambiguity error, so {@link com.bytechef.ee.platform.audit.aspect.AuditAspect} would resolve
 * the mock instead of the real, DB-persisting service that {@code AuditAspectIntTest} depends on.
 *
 * <p>
 * The pattern stops at nested classes on purpose: {@code AuditIntTestConfiguration} itself lives in this tree and its
 * own nested {@code AuditIntTestJdbcConfiguration} must still be reachable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.audit",
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*IntTest\\$.*"))
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class AuditIntTestConfiguration {

    @Bean
    ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }

    @Bean
    TenantService tenantService() {
        return mock(TenantService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AuditIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
