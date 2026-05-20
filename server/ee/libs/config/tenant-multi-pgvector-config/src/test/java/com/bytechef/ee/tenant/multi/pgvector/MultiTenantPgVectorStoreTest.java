/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Unit tests for {@link MultiTenantPgVectorStore}.
 *
 * <p>
 * Since spring-ai 2.0.0-M6 {@code PgVectorFilterExpressionConverter.convertExpression} returns a complete SQL predicate
 * ({@code metadata::jsonb @@ '...'::jsonpath}) rather than a bare JSONPath. These tests pin that the converter output
 * is embedded into the generated SQL exactly once, without being wrapped a second time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class MultiTenantPgVectorStoreTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Mockito ArgumentCaptor in a verify() call; no SQL is executed")
    void testDoSimilaritySearchEmbedsFilterClauseWithoutDoubleWrapping() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[] {
            0.1f, 0.2f
        });

        MultiTenantPgVectorStore vectorStore = MultiTenantPgVectorStore.builder(jdbcTemplate, embeddingModel)
            .vectorTableName("kb_vector_store")
            .build();

        Filter.Expression filterExpression = new FilterExpressionBuilder()
            .eq("knowledge_base_id", 1050)
            .build();

        SearchRequest searchRequest = SearchRequest.builder()
            .query("query")
            .topK(10)
            .filterExpression(filterExpression)
            .build();

        vectorStore.doSimilaritySearch(searchRequest);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any());

        String sql = sqlCaptor.getValue();

        assertThat(sql)
            .contains("AND metadata::jsonb @@ '$.\"knowledge_base_id\" == 1050'::jsonpath")
            .doesNotContain("@@ 'metadata::jsonb @@");
    }

    @Test
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Mockito ArgumentCaptor in a verify() call; no SQL is executed")
    void testDoDeleteByFilterEmbedsFilterClauseWithoutDoubleWrapping() {
        MultiTenantPgVectorStore vectorStore = MultiTenantPgVectorStore.builder(jdbcTemplate, embeddingModel)
            .vectorTableName("kb_vector_store")
            .build();

        Filter.Expression filterExpression = new FilterExpressionBuilder()
            .eq("knowledge_base_id", 1050)
            .build();

        vectorStore.doDelete(filterExpression);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate).update(sqlCaptor.capture());

        String sql = sqlCaptor.getValue();

        assertThat(sql)
            .contains("WHERE metadata::jsonb @@ '$.\"knowledge_base_id\" == 1050'::jsonpath")
            .doesNotContain("?::jsonpath");
    }
}
