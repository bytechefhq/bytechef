/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage.config;

import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.ai.tool.usage",
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class))
@Import({
    LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
public class AiToolUsageIntTestConfiguration {

    public static final BigDecimal SCRAPE_RATE = new BigDecimal("0.002000");

    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder()
            .build();
    }

    @Bean
    ToolCostEstimator toolCostEstimator() {
        return (toolName, unitCount) -> "scrape".equals(toolName)
            ? SCRAPE_RATE.multiply(BigDecimal.valueOf(unitCount)) : BigDecimal.ZERO;
    }

    @Configuration
    public static class AiToolUsageIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        @Override
        protected List<?> userConverters() {
            return List.of();
        }
    }
}
