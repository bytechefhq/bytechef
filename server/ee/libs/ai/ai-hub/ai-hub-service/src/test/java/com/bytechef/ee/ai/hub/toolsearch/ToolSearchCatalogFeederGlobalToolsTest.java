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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private VectorToolIndex vectorToolIndex;

    @Mock
    private JdbcTemplate pgVectorJdbcTemplate;

    private static ToolCallback toolCallback(String name, String description) {
        ToolCallback toolCallback = org.mockito.Mockito.mock(ToolCallback.class);
        ToolDefinition toolDefinition = org.mockito.Mockito.mock(ToolDefinition.class);

        org.mockito.Mockito.lenient()
            .when(toolDefinition.name())
            .thenReturn(name);
        org.mockito.Mockito.lenient()
            .when(toolDefinition.description())
            .thenReturn(description);

        when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);

        return toolCallback;
    }

    @Test
    void testMetaTableIsSchemaQualifiedToTheConfiguredSchema() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate, "public");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        // The bookkeeping table must be referenced by its schema-qualified name so it co-locates with the vector rows
        // in the one shared schema, rather than following the connection's per-tenant search_path.
        ArgumentCaptor<String> createSql = ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).execute(createSql.capture());

        assertThat(createSql.getValue()).contains("public.ai_hub_tool_search_catalog_meta");
    }

    @Test
    void testPopulateGlobalToolsIndexesEachToolWithNonBlankSummary() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate, "public");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:build",
            List.of(toolCallback("listProjects", "List all projects"), toolCallback("blank", "  ")));

        // Blank-summary tools are filtered out, and the survivors are embedded in a single batched indexTools call
        // rather than one indexTool round-trip each.
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
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate, "public");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).update(any(), eq("ai_hub_tool_catalog:global:ask"), hashCaptor.capture(), any());

        org.mockito.Mockito.reset(vectorToolIndex, pgVectorJdbcTemplate);

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), eq("ai_hub_tool_catalog:global:ask")))
            .thenReturn(hashCaptor.getValue());

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        verify(vectorToolIndex, never()).indexTools(any(), any());
    }

    @Test
    void testPopulateSourcesToolsFromStubEnumeration() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate, "public");

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
        when(clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS))
            .thenReturn(List.of());

        feeder.populate();

        // Population must read the index-stub enumeration, never the full-load catalog method.
        verify(clusterElementDefinitionService).getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);
        verify(clusterElementDefinitionService, never())
            .getClusterElementDefinitions(BaseToolFunction.TOOLS);
    }
}
