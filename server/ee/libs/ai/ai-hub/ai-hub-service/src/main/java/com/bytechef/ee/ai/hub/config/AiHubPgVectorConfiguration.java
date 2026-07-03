/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Pgvector store dedicated to the AI Hub Tool Search Tool catalog index. Distinct table
 * (<code>ai_hub_tool_search_*</code>) so KB chunk embeddings and tool-discovery embeddings cannot bleed across each
 * other's similarity searches — a query for "send a slack message" must not return KB docs and vice-versa. Same
 * pgvector instance, same connection pool, same embedding model — only the table is split.
 *
 * <p>
 * {@code initializeSchema=true} makes Spring AI create the table on first boot; no Liquibase migration is required for
 * the vector store table itself. The hash-based catalog refresh in {@code ToolSearchCatalogFeeder} keeps embedding cost
 * down on subsequent boots.
 * </p>
 *
 * <p>
 * <b>{@code idType} override:</b> hardcoded to {@link PgVectorStore.PgIdType#TEXT} regardless of the global
 * {@code spring.ai.vectorstore.pgvector.id-type} property. The upstream {@code VectorToolIndex} generates sequential
 * numeric-string IDs ({@code "0"}, {@code "1"}, ...) via {@code AtomicInteger#getAndIncrement} — these are not
 * parseable as UUIDs and would crash {@code PgVectorStore.convertIdToPgType} on first insert if the table column were
 * typed {@code uuid}. The shared KB store keeps the global setting because its ingestion path generates real UUIDs.
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.hub.enabled} — separate from the shared pgvector configuration so a deployment can enable
 * Tool Search for AI Hub without bringing up the KB vector store, and vice versa.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(PgVectorStoreProperties.class)
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubPgVectorConfiguration {

    @Bean("toolSearchPgVectorStore")
    public VectorStore toolSearchPgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        // Bean name is owned by another module (the embedding model is shared platform infra); locally we
        // bind it to a neutral name so this configuration class doesn't carry the upstream naming.
        EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(PgVectorStore.PgIdType.TEXT)
            .vectorTableName("ai_hub_tool_search_" + properties.getTableName())
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
}
