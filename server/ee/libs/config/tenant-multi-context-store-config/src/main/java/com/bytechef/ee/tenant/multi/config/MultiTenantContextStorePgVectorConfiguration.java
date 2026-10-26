/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import com.bytechef.ee.platform.contextstore.service.ContextStoreVectorStoreMetadataService;
import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorLoader;
import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorStore;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@EnableConfigurationProperties(PgVectorStoreProperties.class)
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore", name = "provider", havingValue = "pgvector")
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore.pgvector", name = "url")
class MultiTenantContextStorePgVectorConfiguration {

    @Bean
    VectorStore contextStorePgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel,
        PgVectorStoreProperties properties, BatchingStrategy batchingStrategy) {

        return MultiTenantPgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .vectorTableName("cs_" + properties.getTableName())
            .distanceType(properties.getDistanceType())
            .idType(properties.getIdType())
            .batchingStrategy(batchingStrategy)
            .build();
    }

    @Bean
    ContextStoreVectorStoreMetadataService contextStoreVectorStoreMetadataService(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, ObjectMapper objectMapper,
        PgVectorStoreProperties properties) {

        String vectorTableName = "cs_" + properties.getTableName();

        return new ContextStoreVectorStoreMetadataService(
            pgVectorJdbcTemplate, objectMapper,
            () -> TenantContext.getCurrentDatabaseSchema(MultiTenantPgVectorStore.VECTORSTORE_SCHEMA_SUFFIX) + "."
                + vectorTableName);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
    MultiTenantPgVectorLoader contextStoreMultiTenantPgVectorLoader(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, PgVectorStoreProperties properties,
        TenantService tenantService) {

        return new MultiTenantPgVectorLoader(
            pgVectorJdbcTemplate, properties, "cs_" + properties.getTableName(), tenantService);
    }
}
