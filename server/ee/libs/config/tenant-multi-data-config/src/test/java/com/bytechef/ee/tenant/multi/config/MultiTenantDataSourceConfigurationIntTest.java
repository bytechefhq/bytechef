/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link MultiTenantDataSourceConfiguration} to verify that the correct DataSource is loaded when
 * multi-tenant mode is enabled with EE edition.
 *
 * <p>
 * These tests verify that when bytechef.edition=ee and bytechef.tenant.mode=multi, the MultiTenantDataSource is the
 * primary DataSource bean.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class MultiTenantDataSourceConfigurationIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withUserConfiguration(MultiTenantDataSourceConfiguration.class, TestConfiguration.class)
        .withPropertyValues(
            "bytechef.edition=ee",
            "bytechef.tenant.mode=multi",
            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
            "spring.datasource.password=" + postgreSQLContainer.getPassword());

    @Test
    void testDataSourceIsMultiTenantDataSourceInMultiTenantMode() {
        ApplicationContextRunner multiDataSourceContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(
                MultiTenantDataSourceConfiguration.class, TestConfiguration.class,
                CompetingDataSourceConfiguration.class)
            .withPropertyValues(
                "bytechef.edition=ee",
                "bytechef.tenant.mode=multi",
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword());

        multiDataSourceContextRunner.run(context -> {
            assertThat(context).hasBean("dataSource");
            assertThat(context).hasBean("competingDataSource");

            DataSource primaryDataSource = context.getBean(DataSource.class);

            assertThat(primaryDataSource).isInstanceOf(MultiTenantDataSource.class);
        });
    }

    @Test
    void testDataSourceNotLoadedWithoutEEEdition() {
        ApplicationContextRunner nonEEContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(MultiTenantDataSourceConfiguration.class, TestConfiguration.class)
            .withPropertyValues(
                "bytechef.tenant.mode=multi",
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword());

        nonEEContextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(dataSource).isNotInstanceOf(MultiTenantDataSource.class);
        });
    }

    @Test
    void testDataSourceNotLoadedWithoutMultiTenantMode() {
        ApplicationContextRunner singleTenantContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(MultiTenantDataSourceConfiguration.class, TestConfiguration.class)
            .withPropertyValues(
                "bytechef.edition=ee",
                "bytechef.tenant.mode=single",
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword());

        singleTenantContextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(dataSource).isNotInstanceOf(MultiTenantDataSource.class);
        });
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        TenantService tenantService() {
            return mock(TenantService.class);
        }
    }

    @Configuration
    static class CompetingDataSourceConfiguration {

        @Bean
        DataSource competingDataSource() {
            HikariDataSource dataSource = new HikariDataSource();

            dataSource.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
            dataSource.setUsername(postgreSQLContainer.getUsername());
            dataSource.setPassword(postgreSQLContainer.getPassword());

            return dataSource;
        }
    }
}
