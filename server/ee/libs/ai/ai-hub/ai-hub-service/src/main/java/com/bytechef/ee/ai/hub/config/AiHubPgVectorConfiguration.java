/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.pgvector.util.PgVectorSchemaUtils;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
@EnableConfigurationProperties(PgVectorStoreProperties.class)
public class AiHubPgVectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiHubPgVectorConfiguration.class);

    @Bean("toolSearchPgVectorStore")
    public @Nullable VectorStore toolSearchPgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider,
        ObjectProvider<EmbeddingModel> embeddingModelProvider, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        ObjectProvider<BatchingStrategy> batchingStrategyProvider) {

        JdbcTemplate pgVectorJdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        BatchingStrategy batchingStrategy = batchingStrategyProvider.getIfAvailable();

        if (pgVectorJdbcTemplate == null || embeddingModel == null || batchingStrategy == null) {
            log.info(
                "AI Hub tool search disabled: it needs the pgvector store (bytechef.ai.vectorstore.provider=pgvector " +
                    "and bytechef.ai.vectorstore.pgvector.url) and an embedding model");

            return null;
        }

        PgVectorSchemaUtils.initializeSchema(
            () -> buildToolSearchVectorStore(
                pgVectorJdbcTemplate, embeddingModel, properties, observationRegistry, customObservationConvention,
                batchingStrategy, true)
                    .afterPropertiesSet());

        return buildToolSearchVectorStore(
            pgVectorJdbcTemplate, embeddingModel, properties, observationRegistry, customObservationConvention,
            batchingStrategy, false);
    }

    public static String vectorTableName(PgVectorStoreProperties properties) {
        return "ai_hub_tool_search_" + properties.getTableName();
    }

    public static PgVectorStore buildToolSearchVectorStore(
        JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy, boolean initializeSchema) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(PgVectorStore.PgIdType.TEXT)
            .vectorTableName(vectorTableName(properties))
            .vectorTableValidationsEnabled(properties.isSchemaValidation())
            .dimensions(properties.getDimensions())
            .distanceType(properties.getDistanceType())
            .removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
            .indexType(properties.getIndexType())
            .initializeSchema(initializeSchema)
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
            .batchingStrategy(batchingStrategy)
            .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
            .build();
    }
}
