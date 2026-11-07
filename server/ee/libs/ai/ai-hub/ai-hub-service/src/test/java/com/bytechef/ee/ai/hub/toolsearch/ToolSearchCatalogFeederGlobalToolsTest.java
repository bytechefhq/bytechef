/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class ToolSearchCatalogFeederGlobalToolsTest {

    @Mock
    private VectorToolIndex vectorToolIndex;

    @Mock
    private JdbcTemplate pgVectorJdbcTemplate;

    private static ToolCallback toolCallback(String name, String description) {
        ToolCallback toolCallback = mock(ToolCallback.class);
        ToolDefinition toolDefinition = mock(ToolDefinition.class);

        lenient()
            .when(toolDefinition.name())
            .thenReturn(name);
        lenient()
            .when(toolDefinition.description())
            .thenReturn(description);

        when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);

        return toolCallback;
    }

    @Test
    void testMetaTableIsSchemaQualifiedToTheConfiguredSchema() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            vectorToolIndex, pgVectorJdbcTemplate, "public", "ai_hub_tool_search_vector_store");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        ArgumentCaptor<String> createSql = ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).execute(createSql.capture());

        assertThat(createSql.getValue()).contains("public.ai_hub_tool_search_catalog_meta");
    }

    @Test
    void testReindexDeletesStaleRowsFromEarlierJvmRunsByMetadata() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            vectorToolIndex, pgVectorJdbcTemplate, "public", "ai_hub_tool_search_vector_store");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:build", List.of(toolCallback("createDataTable", "Create a data table")));

        ArgumentCaptor<String> deleteSql = ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).update(deleteSql.capture(), eq("ai_hub_tool_catalog:global:build"));

        assertThat(deleteSql.getValue())
            .startsWith("DELETE FROM public.ai_hub_tool_search_vector_store")
            .contains("metadata->>'sessionId'");
    }

    @Test
    void testPopulateGlobalToolsIndexesEachToolWithNonBlankSummary() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            vectorToolIndex, pgVectorJdbcTemplate, "public", "ai_hub_tool_search_vector_store");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:build",
            List.of(toolCallback("listProjects", "List all projects"), toolCallback("blank", "  ")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ToolReference>> referencesCaptor = ArgumentCaptor.forClass(List.class);

        verify(vectorToolIndex).clearIndex("ai_hub_tool_catalog:global:build");
        verify(vectorToolIndex, times(1)).indexTools(
            eq("ai_hub_tool_catalog:global:build"), referencesCaptor.capture());

        assertThat(referencesCaptor.getValue())
            .extracting(ToolReference::toolName)
            .containsExactly("listProjects");
    }

    @Test
    void testPopulateGlobalToolsSkipsWhenHashUnchanged() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            vectorToolIndex, pgVectorJdbcTemplate, "public", "ai_hub_tool_search_vector_store");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).update(any(), eq("ai_hub_tool_catalog:global:ask"), hashCaptor.capture(), any());

        reset(vectorToolIndex, pgVectorJdbcTemplate);

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), eq("ai_hub_tool_catalog:global:ask")))
            .thenReturn(hashCaptor.getValue());

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        verify(vectorToolIndex, never()).indexTools(any(), any());
    }
}
