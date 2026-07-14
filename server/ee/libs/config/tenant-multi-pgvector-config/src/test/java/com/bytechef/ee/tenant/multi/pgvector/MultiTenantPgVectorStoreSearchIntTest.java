/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.multi.sql.MultiTenantPgVectorDataSource;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class MultiTenantPgVectorStoreSearchIntTest {

    private static final String TABLE_NAME = "kb_vector_store";
    private static final String TENANT_ID = "000001";
    private static final int DIMENSIONS = 3;

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    private HikariDataSource hikariDataSource;
    private JdbcTemplate tenantJdbcTemplate;

    @BeforeEach
    void setUp() {
        hikariDataSource = new HikariDataSource();

        hikariDataSource.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
        hikariDataSource.setUsername(postgreSQLContainer.getUsername());
        hikariDataSource.setPassword(postgreSQLContainer.getPassword());

        tenantJdbcTemplate = new JdbcTemplate(new MultiTenantPgVectorDataSource(hikariDataSource));

        TenantContext.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();

        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }

    @Test
    void testSimilaritySearchResolvesPgVectorOperatorFromPublicSchema() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TENANT_ID));

        PgVectorStoreProperties properties = new PgVectorStoreProperties();

        properties.setDimensions(DIMENSIONS);
        properties.setIdType(PgVectorStore.PgIdType.UUID);
        properties.setIndexType(PgVectorStore.PgIndexType.NONE);
        properties.setDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE);

        MultiTenantPgVectorLoader loader = new MultiTenantPgVectorLoader(
            tenantJdbcTemplate, properties, TABLE_NAME, tenantService);

        loader.afterPropertiesSet();

        MultiTenantPgVectorStore vectorStore =
            MultiTenantPgVectorStore.builder(tenantJdbcTemplate, new FixedEmbeddingModel(DIMENSIONS))
                .vectorTableName(TABLE_NAME)
                .idType(PgVectorStore.PgIdType.UUID)
                .dimensions(DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .build();

        Document document = Document.builder()
            .id(UUID.randomUUID()
                .toString())
            .text("ByteChef knowledge base content")
            .metadata(Map.of("knowledge_base_id", 1050))
            .build();

        vectorStore.add(List.of(document));

        SearchRequest searchRequest = SearchRequest.builder()
            .query("query")
            .topK(10)
            .filterExpression(
                new FilterExpressionBuilder()
                    .eq("knowledge_base_id", 1050)
                    .build())
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)
            .getText()).isEqualTo("ByteChef knowledge base content");
    }

    private static final class FixedEmbeddingModel implements EmbeddingModel {

        private final int dimensions;

        private FixedEmbeddingModel(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();

            List<String> instructions = request.getInstructions();

            for (int i = 0; i < instructions.size(); i++) {
                embeddings.add(new Embedding(vector(), i));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vector();
        }

        @Override
        public float[] embed(String text) {
            return vector();
        }

        @Override
        public List<float[]> embed(
            List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {

            List<float[]> embeddings = new ArrayList<>();

            for (Document ignored : documents) {
                embeddings.add(vector());
            }

            return embeddings;
        }

        @Override
        public int dimensions() {
            return dimensions;
        }

        private float[] vector() {
            float[] embedding = new float[dimensions];

            embedding[0] = 1.0f;

            return embedding;
        }
    }
}
