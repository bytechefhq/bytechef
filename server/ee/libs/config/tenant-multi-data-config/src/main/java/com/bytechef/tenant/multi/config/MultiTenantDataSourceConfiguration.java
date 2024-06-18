/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.tenant.multi.config;

import com.bytechef.tenant.multi.sql.MultiTenantDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(value = "bytechef.tenant.mode", havingValue = "multi")
@ConditionalOnEEVersion
public class MultiTenantDataSourceConfiguration {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        final HikariDataSource dataSource = properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();

        if (properties.getName() != null) {
            dataSource.setPoolName(properties.getName());
        }

        return new MultiTenantDataSource(dataSource);
    }

}
