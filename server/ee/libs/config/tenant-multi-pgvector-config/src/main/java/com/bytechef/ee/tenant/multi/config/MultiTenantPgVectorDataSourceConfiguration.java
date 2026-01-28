/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorLoader;
import com.bytechef.ee.tenant.multi.sql.MultiTenantPgVectorDataSource;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "spring.ai.vectorstore", name = "type", havingValue = "pgvector")
class MultiTenantPgVectorDataSourceConfiguration {

    private final ApplicationProperties.Ai.Vectorstore.PgVector pgVector;

    @SuppressFBWarnings("EI")
    MultiTenantPgVectorDataSourceConfiguration(ApplicationProperties applicationProperties) {
        this.pgVector = applicationProperties.getAi()
            .getVectorstore()
            .getPgVector();
    }

    @Bean
    DataSource pgVectorDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(pgVector.getUrl())
            .username(pgVector.getUsername())
            .password(pgVector.getPassword())
            .build();

        return new MultiTenantPgVectorDataSource(dataSource);
    }

    @Bean
    MultiTenantPgVectorLoader multiTenantPgVectorLoader(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        PgVectorStoreProperties properties, TenantService tenantService) {

        return new MultiTenantPgVectorLoader(pgVectorJdbcTemplate, properties, tenantService);
    }
}
