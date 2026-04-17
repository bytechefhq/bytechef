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
import com.bytechef.config.ApplicationProperties.Ai.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.OpenAi;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        @Qualifier("copilotEmbeddingModel") EmbeddingModel copilotEmbeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, copilotEmbeddingModel)
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

    @Bean("copilotEmbeddingModel")
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "openai")
    OpenAiEmbeddingModel copilotOpenAiEmbeddingModel(
        ApplicationProperties applicationProperties, OpenAiApi openAiApi) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        OpenAi openAi = ai.getOpenAi();

        OpenAi.Embedding.Options openAiEmbeddingOptions = openAi.getEmbedding()
            .getOptions();

        return new OpenAiEmbeddingModel(
            openAiApi, MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(openAiEmbeddingOptions.getModel())
                .build());
    }

    @Bean("copilotEmbeddingModel")
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    OpenAiEmbeddingModel copilotAnthropicOpenAiEmbeddingModel(
        ApplicationProperties applicationProperties, OpenAiApi openAiApi) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        Anthropic anthropic = ai.getAnthropic();

        Anthropic.Embedding.OpenAi.Options anthropicEmbeddingOpenAiOptions = anthropic.getEmbedding()
            .getOpenAi()
            .getOptions();

        return new OpenAiEmbeddingModel(
            openAiApi, MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(anthropicEmbeddingOpenAiOptions.getModel())
                .build());
    }
}
