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
 * Pins the conditional wiring: without {@code bytechef.context-store.clickhouse.url}, none of the ClickHouse beans
 * exist; with the URL set, the datasource and its cascade materialise.
 *
 * <p>
 * This is the verification gate for Phase 16 commit 2 — the module compiles AND the conditional shape behaves as
 * advertised, so an app that pulls in this module without operator opt-in starts cleanly.
 * </p>
 *
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
        // bytechef.context-store.enabled is the master switch — even with a ClickHouse url present, the cascade must
        // stay dormant when the feature is disabled. Pins the second @ConditionalOnProperty gate added alongside the
        // url check.
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
