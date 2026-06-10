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
 * <p>
 * Gated on the PgVector provider being configured ({@code bytechef.ai.vectorstore.provider=pgvector} plus a
 * {@code bytechef.ai.vectorstore.pgvector.url}) because the loader depends on the {@code pgVectorJdbcTemplate} bean,
 * which only exists under those same conditions (see {@code PgVectorJdbcConfiguration} and
 * {@link MultiTenantPgVectorDataSourceConfiguration}). Deployments such as the {@code liquibase} migration run that do
 * not configure a PgVector store therefore skip this loader instead of failing context startup with an unsatisfied
 * dependency. There is nothing to provision when PgVector is not the configured vector store.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore", name = "provider", havingValue = "pgvector")
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore.pgvector", name = "url")
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
