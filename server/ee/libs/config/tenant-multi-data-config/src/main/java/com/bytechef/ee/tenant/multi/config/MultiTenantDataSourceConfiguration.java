/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import com.bytechef.ee.tenant.multi.liquibase.MultiTenantLiquibaseChangelogLoader;
import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
public class MultiTenantDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        final HikariDataSource dataSource = properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();

        if (properties.getName() != null) {
            dataSource.setPoolName(properties.getName());
        }

        return new MultiTenantDataSource(dataSource);
    }

    @Bean
    MultiTenantLiquibaseChangelogLoader multiTenantLiquibaseChangelogLoader(TenantService tenantService) {
        return new MultiTenantLiquibaseChangelogLoader(tenantService);
    }
}
