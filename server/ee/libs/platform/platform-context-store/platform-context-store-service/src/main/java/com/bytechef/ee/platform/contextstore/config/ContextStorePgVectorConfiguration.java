/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.config;

import com.bytechef.ee.platform.contextstore.service.ContextStoreVectorStoreMetadataService;
import com.bytechef.tenant.annotation.ConditionalOnSingleTenant;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers the Spring AI {@link PgVectorStore} backing the Context Store semantic-search add-on along with the
 * companion {@link ContextStoreVectorStoreMetadataService} used by the embedding listener for hash-skip lookups.
 *
 * <p>
 * Both beans are gated on {@link EmbeddingModel} presence and on {@link ConditionalOnSingleTenant}; multi-tenant
 * deployments register the per-tenant-schema twin {@code MultiTenantContextStorePgVectorConfiguration} instead. The
 * vector table is auto-created by {@code PgVectorStore.initializeSchema(true)}; no Liquibase migration is required.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Configuration
@EnableConfigurationProperties(PgVectorStoreProperties.class)
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnSingleTenant
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
class ContextStorePgVectorConfiguration {

    @Bean
    public VectorStore contextStorePgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName("cs_" + properties.getTableName())
            .vectorTableValidationsEnabled(properties.isSchemaValidation())
            .dimensions(properties.getDimensions())
            .distanceType(properties.getDistanceType())
            .removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
            .indexType(properties.getIndexType())
            .initializeSchema(true)
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
            .batchingStrategy(batchingStrategy)
            .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
            .build();
    }

    @Bean
    public ContextStoreVectorStoreMetadataService contextStoreVectorStoreMetadataService(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        ObjectMapper objectMapper, PgVectorStoreProperties properties) {

        String schemaName = properties.getSchemaName();
        String tableName = "cs_" + properties.getTableName();
        String fullTableName = (schemaName != null && !schemaName.isBlank())
            ? schemaName + "." + tableName
            : tableName;

        return new ContextStoreVectorStoreMetadataService(pgVectorJdbcTemplate, objectMapper, () -> fullTableName);
    }
}
