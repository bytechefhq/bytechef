/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client.config;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.platform.knowledgebase",
        "com.bytechef.jackson.config",
        "com.bytechef.platform.tag"
    },
    excludeFilters = @Filter(
        type = FilterType.REGEX, pattern = "com\\.bytechef\\.platform\\.knowledgebase\\..*IntTest.*"))
@EnableAutoConfiguration(excludeName = {
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
    "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"
})
@Import(LiquibaseConfiguration.class)
@Configuration
public class KnowledgeBaseRemoteClientIntTestConfiguration {

    public static final String FAILING_TEXT = "boom";

    private static final int DIMENSIONS = 1536;

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> pgVectorContainer() {
        return new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"));
    }

    @Bean
    JdbcTemplate pgVectorJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    BatchingStrategy pgVectorStoreBatchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new HashEmbeddingModel();
    }

    @Bean
    FileStorageService fileStorageService() {
        return mock(FileStorageService.class);
    }

    @Bean
    FileStorageServiceRegistry fileStorageServiceRegistry(
        @Qualifier("fileStorageService") FileStorageService fileStorageService) {

        FileStorageServiceRegistry fileStorageServiceRegistry = mock(FileStorageServiceRegistry.class);

        when(fileStorageServiceRegistry.getFileStorageService(anyString())).thenReturn(fileStorageService);

        return fileStorageServiceRegistry;
    }

    @Bean
    KnowledgeBaseFileStorage knowledgeBaseFileStorage() {
        KnowledgeBaseFileStorage knowledgeBaseFileStorage = mock(KnowledgeBaseFileStorage.class);

        when(knowledgeBaseFileStorage.storeChunkContent(anyLong(), anyString())).thenAnswer(
            invocation -> new FileEntry(
                "chunk-" + invocation.getArgument(0) + ".txt", "memory://chunk/" + invocation.getArgument(0)));

        return knowledgeBaseFileStorage;
    }

    @Bean
    MessageBroker messageBroker() {
        return mock(MessageBroker.class);
    }

    @Bean
    ObjectMapper jacksonObjectMapper() {
        return JsonMapper.builder()
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class KnowledgeBaseRemoteClientIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public KnowledgeBaseRemoteClientIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new FileEntryToStringConverter(objectMapper), new StringToFileEntryConverter(objectMapper));
        }
    }

    private static final class HashEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();

            for (int index = 0; index < instructions.size(); index++) {
                embeddings.add(new Embedding(embed(instructions.get(index)), index));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public float[] embed(String text) {
            if (FAILING_TEXT.equals(text)) {
                throw new IllegalStateException("Embedding failed");
            }

            byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
            long seed = 1125899906842597L;

            for (byte value : bytes) {
                seed = 31 * seed + value;
            }

            long state = seed == 0 ? 1 : seed;
            float[] vector = new float[DIMENSIONS];

            for (int index = 0; index < DIMENSIONS; index++) {
                state ^= state << 13;
                state ^= state >>> 7;
                state ^= state << 17;

                vector[index] = (state >>> 40) / (float) (1L << 24);
            }

            return vector;
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }
    }
}
