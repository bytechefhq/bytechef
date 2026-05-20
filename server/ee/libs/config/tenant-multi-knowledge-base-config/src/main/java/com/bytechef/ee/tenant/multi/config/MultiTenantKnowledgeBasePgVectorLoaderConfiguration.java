/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorLoader;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Registers the multi-tenant Knowledge Base PgVector loader independently of the {@code bytechef.ai.knowledge-base}
 * feature flag. Mirrors {@link MultiTenantDataSourceConfiguration#multiTenantLiquibaseChangelogLoader}: the loader runs
 * during a normal startup and during the dedicated {@code liquibase} migration run, so per-tenant vector tables are
 * provisioned regardless of whether the Knowledge Base feature is enabled.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@EnableConfigurationProperties(PgVectorStoreProperties.class)
class MultiTenantKnowledgeBasePgVectorLoaderConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
    MultiTenantPgVectorLoader knowledgeBaseMultiTenantPgVectorLoader(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, PgVectorStoreProperties properties,
        TenantService tenantService) {

        return new MultiTenantPgVectorLoader(
            pgVectorJdbcTemplate, properties, "kb_" + properties.getTableName(), tenantService);
    }
}
