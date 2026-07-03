/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.Docs.Embedding.Provider;
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

@Configuration
@EnableJdbcRepositories(
    basePackages = "com.bytechef.ee.ai.copilot.repository",
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
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot.docs.embedding", name = "provider")
    public VectorStore copilotDocsLoaderVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy, ApplicationProperties applicationProperties) {

        return buildVectorStore(
            pgVectorJdbcTemplate, copilotDocsEmbeddingModel(applicationProperties), properties, observationRegistry,
            customObservationConvention, batchingStrategy);
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

    private static EmbeddingModel copilotDocsEmbeddingModel(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Copilot.Docs.Embedding embedding = ai.getCopilot()
            .getDocs()
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
                "Copilot docs embedding provider is set to OPENAI but 'bytechef.ai.copilot.docs.embedding.api-key' " +
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
