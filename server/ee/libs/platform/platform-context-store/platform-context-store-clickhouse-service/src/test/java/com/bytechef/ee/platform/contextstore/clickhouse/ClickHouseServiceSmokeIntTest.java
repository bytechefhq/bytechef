/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Phase 16 commit 8 — full-app-shape smoke test. Boots a minimal Spring Boot application context with the
 * clickhouse-service module's component-scanned beans + auto-configuration enabled, then asserts the conditional
 * cascade keeps all ClickHouse beans out when the URL property is unset.
 *
 * <p>
 * This is the test that catches "module accidentally tries to load a DataSource at boot even though the operator hasn't
 * opted in" — the kind of regression that the per-config {@code ApplicationContextRunner} test misses because it
 * doesn't run the full Spring Boot lifecycle (auto-config discovery, {@code PostConstruct}, etc.). The server-app
 * IntTest would catch this too, but pulling the module into server-app's compile classpath has a separate WIP issue
 * (unrelated upstream {@code WEBSOCKET} enum/mapper drift) blocking that path right now.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@SpringBootTest(classes = ClickHouseServiceSmokeIntTest.SmokeApplication.class)
class ClickHouseServiceSmokeIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testContextLoadsWithoutClickHouseUrl() {
        // No bytechef.context-store.clickhouse.url means the @ConditionalOnProperty on
        // ClickHouseDataSourceConfiguration short-circuits the whole config class → no DataSource bean, no
        // JdbcTemplate, no TransactionManager, no @Component-discovered ClickHouseTableProvisionerImpl.
        assertThat(applicationContext.containsBean("clickHouseDataSource")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseJdbcTemplate")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseTransactionManager")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseNamedParameterJdbcOperations")).isFalse();
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableProvisioner.class)).isEmpty();
        // Mutable-schema follow-up: the migrator follows the same conditional cascade as the provisioner.
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableMigrator.class)).isEmpty();
        // Defensive: no rogue DataSource beans were created at boot.
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
    }

    // Tiny Spring Boot application that scans only this module's clickhouse package. Lets the full @SpringBootTest
    // lifecycle run (auto-config discovery, post-processors, lifecycle callbacks) against the production-shaped
    // wiring without dragging in the full server-app classpath. DataSourceAutoConfiguration is excluded because
    // Spring Boot would otherwise try to materialise a default app DataSource at boot — server-app wires its own
    // primary DataSource in production.
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @EnableConfigurationProperties(ApplicationProperties.class)
    @ComponentScan(basePackages = "com.bytechef.ee.platform.contextstore.clickhouse")
    static class SmokeApplication {
    }
}
