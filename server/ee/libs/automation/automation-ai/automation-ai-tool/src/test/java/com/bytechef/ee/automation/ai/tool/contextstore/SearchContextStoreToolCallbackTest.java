/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SearchContextStoreToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    @Test
    void testToolDefinitionExposesName() {
        SearchContextStoreToolCallback callback = new SearchContextStoreToolCallback(
            mock(ContextStoreQueryService.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(SearchContextStoreToolCallback.TOOL_NAME);
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("sourceId");
    }

    @Test
    void testCallReturnsItemsAndCursor() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(workspaceId));

        ContextStoreQueryService queryService = mock(ContextStoreQueryService.class);

        ContextStoreRecord record = new ContextStoreRecord();

        record.setId(7L);
        record.setSourceRecordId("ext-1");
        record.setPayload(Map.of("name", "Acme"));

        when(queryService.search(any(ContextStoreQuery.class)))
            .thenReturn(new ContextStoreSearchResult(List.of(record), "cursor-2"));

        SearchContextStoreToolCallback callback = new SearchContextStoreToolCallback(queryService, workspaceService);

        String result = callback.call(
            "{\"sourceId\":42,\"limit\":10}", toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("items")
            .isArray()).isTrue();
        assertThat(node.get("items")).hasSize(1);
        assertThat(node.get("items")
            .get(0)
            .get("sourceRecordId")
            .asText()).isEqualTo("ext-1");
        assertThat(node.get("nextCursor")
            .asText()).isEqualTo("cursor-2");

        ArgumentCaptor<ContextStoreQuery> captor = ArgumentCaptor.forClass(ContextStoreQuery.class);

        verify(queryService).search(captor.capture());

        ContextStoreQuery captured = captor.getValue();

        assertThat(captured.sourceId()).isEqualTo(sourceId);
        assertThat(captured.limit()).isEqualTo(10);
    }

    @Test
    void testCallRejectsCrossWorkspaceSource() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(999L));

        SearchContextStoreToolCallback callback = new SearchContextStoreToolCallback(
            mock(ContextStoreQueryService.class), workspaceService);

        String result = callback.call(
            "{\"sourceId\":42}", toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found in the current workspace");
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() throws Exception {
        SearchContextStoreToolCallback callback = new SearchContextStoreToolCallback(
            mock(ContextStoreQueryService.class), mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{\"sourceId\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testCallClampsLimitAtMax() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(workspaceId));

        ContextStoreQueryService queryService = mock(ContextStoreQueryService.class);

        when(queryService.search(any(ContextStoreQuery.class)))
            .thenReturn(new ContextStoreSearchResult(List.of(), null));

        SearchContextStoreToolCallback callback = new SearchContextStoreToolCallback(queryService, workspaceService);

        callback.call("{\"sourceId\":42,\"limit\":99999}", toolContext(workspaceId));

        ArgumentCaptor<ContextStoreQuery> captor = ArgumentCaptor.forClass(ContextStoreQuery.class);

        verify(queryService).search(captor.capture());

        assertThat(captor.getValue()
            .limit()).isEqualTo(ContextStoreQuery.MAX_LIMIT);
    }
}
