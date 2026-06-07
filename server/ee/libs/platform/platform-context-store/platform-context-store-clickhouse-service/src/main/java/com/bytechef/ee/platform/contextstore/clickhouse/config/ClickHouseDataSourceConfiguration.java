/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.config;

import com.bytechef.config.ApplicationProperties;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionManager;

/**
 * Optional ClickHouse datasource for the Context Store records backend.
 *
 * <p>
 * Gate model:
 * <ul>
 * <li><b>Class-level:</b> {@code bytechef.context-store.clickhouse.url} must be set. Without a URL the whole
 * configuration is skipped — no driver loading, no bean instantiation. Operators opt in by setting the URL.</li>
 * <li><b>Cascade:</b> {@code @ConditionalOnBean(name = "clickHouseDataSource")} on the JdbcTemplate, transaction
 * manager, and downstream beans (the repository impl, the table provisioner). When ClickHouse opts in, the
 * {@code @Primary} ClickHouse repository wins over the Postgres adapter for record reads/writes deployment-wide — there
 * is no per-source backend choice.</li>
 * </ul>
 *
 * <p>
 * Pattern mirrors {@code com.bytechef.pgvector.config.PgVectorJdbcConfiguration} so operators recognise the shape: URL
 * gate → cascade. Both are intentionally minimal — no transaction-manager-specific tuning, no health probes.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "bytechef.context-store.clickhouse", name = "url")
class ClickHouseDataSourceConfiguration {

    private final ApplicationProperties.ContextStore.ClickHouse clickhouse;

    @SuppressFBWarnings("EI")
    ClickHouseDataSourceConfiguration(ApplicationProperties applicationProperties) {
        this.clickhouse = applicationProperties.getContextStore()
            .getClickhouse();
    }

    @Bean
    DataSource clickHouseDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .driverClassName("com.clickhouse.jdbc.ClickHouseDriver")
            .url(clickhouse.getUrl())
            .username(clickhouse.getUsername())
            .password(clickhouse.getPassword())
            .build();
    }

    @Bean
    @ConditionalOnBean(name = "clickHouseDataSource")
    JdbcTemplate clickHouseJdbcTemplate(@Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }

    @Bean
    @ConditionalOnBean(name = "clickHouseDataSource")
    NamedParameterJdbcOperations clickHouseNamedParameterJdbcOperations(
        @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {

        return new NamedParameterJdbcTemplate(clickHouseDataSource);
    }

    @Bean
    @ConditionalOnBean(name = "clickHouseDataSource")
    TransactionManager clickHouseTransactionManager(
        @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {

        return new JdbcTransactionManager(clickHouseDataSource);
    }
}
