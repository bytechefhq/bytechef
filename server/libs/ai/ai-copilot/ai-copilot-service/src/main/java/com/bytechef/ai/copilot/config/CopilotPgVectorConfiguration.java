/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.Embedding.Provider;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
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
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 */
@Configuration
@EnableJdbcRepositories(
    basePackages = "com.bytechef.ai.copilot.repository",
    jdbcAggregateOperationsRef = "pgVectorJdbcAggregateTemplate",
    transactionManagerRef = "pgVectorTransactionManager")
@EnableConfigurationProperties(PgVectorStoreProperties.class)
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotPgVectorConfiguration {

    @Bean
    public VectorStore copilotPgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return buildVectorStore(
            pgVectorJdbcTemplate, embeddingModel, properties, observationRegistry, customObservationConvention,
            batchingStrategy);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot.embedding", name = "provider")
    public VectorStore copilotDocsLoaderVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        @Qualifier("copilotEmbeddingModel") EmbeddingModel copilotEmbeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return buildVectorStore(
            pgVectorJdbcTemplate, copilotEmbeddingModel, properties, observationRegistry, customObservationConvention,
            batchingStrategy);
    }

    private static VectorStore buildVectorStore(
        JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName("copilot_" + properties.getTableName())
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

    /**
     * The internal, fixed-key Copilot embedding model used to <b>write</b> into the Copilot pgvector store (docs
     * indexing) and, shared across modules, to write the AI Hub tool-search catalog. Kept environment-independent (a
     * single configured {@code bytechef.ai.copilot.embedding.*} key) so boot-time indexing never depends on a
     * per-environment provider being activated. Query-time reads still resolve the {@code @Primary} embedding model (in
     * EE the per-environment {@code CatalogEmbeddingModel}); the two must resolve to the same underlying model for
     * vectors to be comparable.
     *
     * <p>
     * {@code defaultCandidate = false} keeps it out of unqualified {@code EmbeddingModel} autowiring so it never
     * collides with the primary/query embedding model — consumers must ask for it by the {@code copilotEmbeddingModel}
     * qualifier.
     * </p>
     */
    @Bean(defaultCandidate = false)
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot.embedding", name = "provider")
    public EmbeddingModel copilotEmbeddingModel(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Copilot.Embedding embedding = ai.getCopilot()
            .getEmbedding();

        if (embedding.getProvider() == Provider.OLLAMA) {
            String model = ai.getProvider()
                .getEmbedding()
                .getOllama()
                .getOptions()
                .getModel();

            return OllamaEmbeddingModel.builder()
                .ollamaApi(
                    OllamaApi.builder()
                        .build())
                .options(
                    OllamaEmbeddingOptions.builder()
                        .model(model)
                        .build())
                .build();
        }

        String apiKey = embedding.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Copilot embedding provider is set to OPENAI but 'bytechef.ai.copilot.embedding.api-key' " +
                    "is not configured");
        }

        String model = ai.getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .getModel();

        return new OpenAiEmbeddingModel(
            OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(60))
                .build(),
            MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(model)
                .build());
    }
}
