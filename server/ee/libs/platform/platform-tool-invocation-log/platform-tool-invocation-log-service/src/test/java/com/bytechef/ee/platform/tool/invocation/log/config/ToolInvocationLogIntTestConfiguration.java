/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.tenant.single.SingleTenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.tool.invocation.log",
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class))
@Import({
    LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class, SingleTenantService.class
})
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Configuration
public class ToolInvocationLogIntTestConfiguration {

    @Configuration
    public static class ToolInvocationLogIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        @Override
        protected List<?> userConverters() {
            return List.of();
        }
    }
}
