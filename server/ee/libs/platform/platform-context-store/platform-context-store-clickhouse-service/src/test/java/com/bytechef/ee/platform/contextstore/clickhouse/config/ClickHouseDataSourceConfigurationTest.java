/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 * @version ee
 */
class ClickHouseDataSourceConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
        .withUserConfiguration(TestPropertiesConfiguration.class, ClickHouseDataSourceConfiguration.class);

    @Test
    void testDefaultDeploymentSkipsAllClickHouseBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean("clickHouseDataSource");
            assertThat(context).doesNotHaveBean("clickHouseJdbcTemplate");
            assertThat(context).doesNotHaveBean("clickHouseTransactionManager");
            assertThat(context).doesNotHaveBean("clickHouseNamedParameterJdbcOperations");
        });
    }

    @Test
    void testUrlConfiguredButContextStoreDisabledSkipsCascade() {
        runner
            .withPropertyValues(
                "bytechef.context-store.clickhouse.url=jdbc:clickhouse://localhost:8123/default")
            .run(context -> assertThat(context).doesNotHaveBean("clickHouseDataSource"));
    }

    @Test
    void testUrlConfiguredActivatesFullCascade() {
        runner
            .withPropertyValues(
                "bytechef.context-store.enabled=true",
                "bytechef.context-store.clickhouse.url=jdbc:clickhouse://localhost:8123/default",
                "bytechef.context-store.clickhouse.username=ch_user",
                "bytechef.context-store.clickhouse.password=ch_pw")
            .run(context -> {
                assertThat(context).hasBean("clickHouseDataSource");
                assertThat(context).hasBean("clickHouseJdbcTemplate");
                assertThat(context).hasBean("clickHouseTransactionManager");
                assertThat(context).hasBean("clickHouseNamedParameterJdbcOperations");

                DataSource dataSource = context.getBean("clickHouseDataSource", DataSource.class);
                assertThat(dataSource).isNotNull();

                JdbcTemplate jdbcTemplate = context.getBean("clickHouseJdbcTemplate", JdbcTemplate.class);
                assertThat(jdbcTemplate.getDataSource()).isSameAs(dataSource);
            });
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestPropertiesConfiguration {
    }
}
