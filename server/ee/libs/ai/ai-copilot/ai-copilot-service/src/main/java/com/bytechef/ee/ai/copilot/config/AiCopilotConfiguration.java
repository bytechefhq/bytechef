/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.github.mizosoft.methanol.Methanol;
import io.micrometer.observation.ObservationRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotConfiguration {

    private final String model;
    private final Double temperature;
    private final String openAiApiKey;
    private final ApplicationProperties.Ai.Copilot.Vectorstore.PgVector pgVector;

    public AiCopilotConfiguration(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.Copilot copilot = applicationProperties.getAi()
            .getCopilot();

        ApplicationProperties.Ai.OpenAi openAi = copilot.getOpenAi();

        ApplicationProperties.Ai.OpenAi.Chat.Options options = openAi.getChat()
            .getOptions();

        this.model = options.getModel();
        this.openAiApiKey = openAi.getApiKey();
        this.pgVector = copilot.getVectorstore()
            .getPgVector();
        this.temperature = options.getTemperature();
    }

    @Bean
    OpenAiApi openAiApi() {
        HttpClient httpClient = Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofSeconds(60))
            .defaultHeaders(httpHeaders -> httpHeaders.setHeader("Accept-Encoding", "gzip, deflate"))
            .headersTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .requestTimeout(Duration.ofSeconds(60))
            .build();

        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(jdkClientHttpRequestFactory);

        return OpenAiApi.builder()
            .apiKey(openAiApiKey)
            .restClientBuilder(builder)
            .build();
    }

    @Bean
    ChatClient.Builder chatClientBuilder(OpenAiApi openAiApi) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(temperature)
                    .build())
            .build();

        return ChatClient.builder(chatModel);
    }

    DataSource pgVectorDataSource() {
        return DataSourceBuilder.create()
            .type(org.postgresql.ds.PGSimpleDataSource.class)
            .url(pgVector.getUrl())
            .username(pgVector.getUsername())
            .password(pgVector.getPassword())
            .build();
    }

    JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(pgVectorDataSource());
    }

    @Bean
    public PgVectorStore vectorStore(
        EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        var initializeSchema = properties.isInitializeSchema();

        return PgVectorStore.builder(pgVectorJdbcTemplate(), embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName(properties.getTableName())
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
