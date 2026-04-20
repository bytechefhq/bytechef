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

package com.bytechef.automation.knowledgebase.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Anthropic;
import com.bytechef.tenant.annotation.ConditionalOnSingleTenant;
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
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PgVectorStoreProperties.class)
@ConditionalOnSingleTenant
class KnowledgeBasePgVectorConfiguration {

    @Bean
    public VectorStore knowledgeBasePgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        @Qualifier("knowledgeBaseEmbeddingModel") EmbeddingModel knowledgeBaseEmbeddingModel,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        return PgVectorStore.builder(pgVectorJdbcTemplate, knowledgeBaseEmbeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName("kb_" + properties.getTableName())
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

    @Bean("knowledgeBaseEmbeddingModel")
    @ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "provider", havingValue = "openai")
    OpenAiEmbeddingModel knowledgeBaseOpenAiEmbeddingModel(
        ApplicationProperties applicationProperties, OpenAiApi openAiApi) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.OpenAi openAi = ai.getOpenAi();

        ApplicationProperties.Ai.OpenAi.Embedding.Options openAiEmbeddingOptions = openAi.getEmbedding()
            .getOptions();

        return new OpenAiEmbeddingModel(
            openAiApi, MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(openAiEmbeddingOptions.getModel())
                .build());
    }

    @Bean("knowledgeBaseEmbeddingModel")
    @ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "provider", havingValue = "anthropic")
    OpenAiEmbeddingModel knowledgeBaseAnthropicOpenAiEmbeddingModel(
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
