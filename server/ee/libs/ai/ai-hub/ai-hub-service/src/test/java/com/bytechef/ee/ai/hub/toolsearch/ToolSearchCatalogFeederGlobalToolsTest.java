/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
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
    void testPopulateGlobalToolsIndexesEachToolWithNonBlankSummary() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate);

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:build",
            List.of(toolCallback("listProjects", "List all projects"), toolCallback("blank", "  ")));

        verify(vectorToolIndex).clearIndex("ai_hub_tool_catalog:global:build");
        verify(vectorToolIndex, times(1)).indexTool(eq("ai_hub_tool_catalog:global:build"), any());
    }

    @Test
    void testPopulateGlobalToolsSkipsWhenHashUnchanged() {
        ToolSearchCatalogFeeder feeder = new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, vectorToolIndex, pgVectorJdbcTemplate);

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), any()))
            .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        org.mockito.ArgumentCaptor<String> hashCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        verify(pgVectorJdbcTemplate).update(any(), eq("ai_hub_tool_catalog:global:ask"), hashCaptor.capture(), any());

        org.mockito.Mockito.reset(vectorToolIndex, pgVectorJdbcTemplate);

        when(pgVectorJdbcTemplate.queryForObject(any(), eq(String.class), eq("ai_hub_tool_catalog:global:ask")))
            .thenReturn(hashCaptor.getValue());

        feeder.populateGlobalTools(
            "ai_hub_tool_catalog:global:ask", List.of(toolCallback("listProjects", "List all projects")));

        verify(vectorToolIndex, never()).indexTool(any(), any());
    }
}
