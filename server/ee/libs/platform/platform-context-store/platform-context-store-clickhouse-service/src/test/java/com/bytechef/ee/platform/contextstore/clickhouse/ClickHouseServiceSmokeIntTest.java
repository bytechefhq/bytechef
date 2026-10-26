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
 * @author Ivica Cardic
 * @version ee
 */
@SpringBootTest(classes = ClickHouseServiceSmokeIntTest.SmokeApplication.class)
class ClickHouseServiceSmokeIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testContextLoadsWithoutClickHouseUrl() {
        assertThat(applicationContext.containsBean("clickHouseDataSource")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseJdbcTemplate")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseTransactionManager")).isFalse();
        assertThat(applicationContext.containsBean("clickHouseNamedParameterJdbcOperations")).isFalse();
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableProvisioner.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(ClickHouseTableMigrator.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @EnableConfigurationProperties(ApplicationProperties.class)
    @ComponentScan(basePackages = "com.bytechef.ee.platform.contextstore.clickhouse")
    static class SmokeApplication {
    }
}
