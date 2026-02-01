/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorStore;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Multi-tenant configuration for the Knowledge Base PgVector store.
 *
 * <p>
 * This configuration creates a {@link MultiTenantPgVectorStore} that dynamically resolves the schema from
 * {@link com.bytechef.tenant.TenantContext} on each operation, enabling multi-tenant isolation at the database schema
 * level.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class MultiTenantKnowledgeBasePgVectorConfiguration {

    @Bean
    VectorStore knowledgeBasePgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel,
        PgVectorStoreProperties properties, BatchingStrategy batchingStrategy) {

        return MultiTenantPgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .vectorTableName("kb_" + properties.getTableName())
            .distanceType(properties.getDistanceType())
            .idType(properties.getIdType())
            .batchingStrategy(batchingStrategy)
            .build();
    }
}
