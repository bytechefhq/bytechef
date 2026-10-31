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
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Since spring-ai 2.0.0-M6 {@code PgVectorFilterExpressionConverter.convertExpression} returns a complete SQL predicate
 * ({@code metadata::jsonb @@ '...'::jsonpath}) rather than a bare JSONPath. These tests run filtered search and
 * delete-by-filter against a real pgvector database: a clause wrapped a second time is either invalid SQL or matches
 * nothing, so the exact matching documents prove it is embedded once.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class MultiTenantPgVectorStoreIntTest {

    private static final String TABLE_NAME = "kb_vector_store";
    private static final String TENANT_ID = "000001";
    private static final String VECTOR_SCHEMA = "bytechef_vectorstore_" + TENANT_ID;

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    private final String firstMatchingDocumentId = UUID.randomUUID()
        .toString();
    private final String secondMatchingDocumentId = UUID.randomUUID()
        .toString();
    private final String otherDocumentId = UUID.randomUUID()
        .toString();

    private HikariDataSource hikariDataSource;
    private MultiTenantPgVectorStore multiTenantPgVectorStore;
    private JdbcTemplate plainJdbcTemplate;

    @BeforeEach
    void beforeEach() {
        hikariDataSource = new HikariDataSource();

        hikariDataSource.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
        hikariDataSource.setUsername(postgreSQLContainer.getUsername());
        hikariDataSource.setPassword(postgreSQLContainer.getPassword());

        plainJdbcTemplate = new JdbcTemplate(hikariDataSource);

        JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(new MultiTenantPgVectorDataSource(hikariDataSource));

        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TENANT_ID));

        PgVectorStoreProperties pgVectorStoreProperties = new PgVectorStoreProperties();

        pgVectorStoreProperties.setDimensions(3);
        pgVectorStoreProperties.setDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE);
        pgVectorStoreProperties.setIdType(PgVectorStore.PgIdType.UUID);
        pgVectorStoreProperties.setIndexType(PgVectorStore.PgIndexType.HNSW);

        MultiTenantPgVectorLoader multiTenantPgVectorLoader = new MultiTenantPgVectorLoader(
            tenantJdbcTemplate, pgVectorStoreProperties, TABLE_NAME, tenantService);

        multiTenantPgVectorLoader.afterPropertiesSet();

        multiTenantPgVectorStore = MultiTenantPgVectorStore.builder(tenantJdbcTemplate, new ConstantEmbeddingModel())
            .vectorTableName(TABLE_NAME)
            .build();

        TenantContext.setCurrentTenantId(TENANT_ID);

        multiTenantPgVectorStore.add(
            List.of(
                new Document(firstMatchingDocumentId, "first", Map.of("knowledge_base_id", 1050)),
                new Document(secondMatchingDocumentId, "second", Map.of("knowledge_base_id", 1050)),
                new Document(otherDocumentId, "other", Map.of("knowledge_base_id", 2000))));
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();

        plainJdbcTemplate.execute("DROP SCHEMA IF EXISTS " + VECTOR_SCHEMA + " CASCADE");

        hikariDataSource.close();
    }

    @Test
    void testDoSimilaritySearchEmbedsFilterClauseWithoutDoubleWrapping() {
        List<Document> unfilteredDocuments = multiTenantPgVectorStore.similaritySearch(
            SearchRequest.builder()
                .query("query")
                .topK(10)
                .build());

        assertThat(unfilteredDocuments).hasSize(3);

        List<Document> documents = multiTenantPgVectorStore.similaritySearch(
            SearchRequest.builder()
                .query("query")
                .topK(10)
                .filterExpression(knowledgeBaseIdFilter())
                .build());

        assertThat(documents)
            .extracting(Document::getId)
            .containsExactlyInAnyOrder(firstMatchingDocumentId, secondMatchingDocumentId);
    }

    @Test
    void testDoDeleteByFilterEmbedsFilterClauseWithoutDoubleWrapping() {
        multiTenantPgVectorStore.delete(knowledgeBaseIdFilter());

        List<String> remainingIds = plainJdbcTemplate.queryForList(
            "SELECT id::text FROM " + VECTOR_SCHEMA + "." + TABLE_NAME, String.class);

        assertThat(remainingIds).containsExactly(otherDocumentId);
    }

    private static Filter.Expression knowledgeBaseIdFilter() {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        return filterExpressionBuilder.eq("knowledge_base_id", 1050)
            .build();
    }

    /**
     * Returns the same vector for every input so similarity never excludes a row and only the filter decides.
     */
    private static final class ConstantEmbeddingModel implements EmbeddingModel {

        private static final float[] VECTOR = {
            1f, 1f, 1f
        };

        @Override
        public EmbeddingResponse call(EmbeddingRequest embeddingRequest) {
            List<String> instructions = embeddingRequest.getInstructions();

            List<Embedding> embeddings = new ArrayList<>();

            for (int index = 0; index < instructions.size(); index++) {
                embeddings.add(new Embedding(VECTOR.clone(), index));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return VECTOR.clone();
        }

        @Override
        public int dimensions() {
            return VECTOR.length;
        }
    }
}
