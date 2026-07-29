/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GetContextStoreRecordToolCallbackTest {

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
        GetContextStoreRecordToolCallback callback = new GetContextStoreRecordToolCallback(
            mock(ContextStoreQueryService.class), mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(GetContextStoreRecordToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("sourceRecordId");
    }

    @Test
    void testCallReturnsRecord() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(workspaceId));

        ContextStoreRecord record = new ContextStoreRecord();

        record.setId(7L);
        record.setSourceRecordId("ext-1");
        record.setPayload(Map.of("name", "Acme"));

        ContextStoreQueryService queryService = mock(ContextStoreQueryService.class);

        when(queryService.get(eq(sourceId), eq("ext-1")))
            .thenReturn(Optional.of(record));

        GetContextStoreRecordToolCallback callback =
            new GetContextStoreRecordToolCallback(queryService, workspaceService);

        String result = callback.call(
            "{\"sourceId\":42,\"sourceRecordId\":\"ext-1\"}", toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(7L);
        assertThat(node.get("sourceRecordId")
            .asText()).isEqualTo("ext-1");
    }

    @Test
    void testCallReturnsErrorWhenRecordMissing() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(workspaceId));

        ContextStoreQueryService queryService = mock(ContextStoreQueryService.class);

        when(queryService.get(eq(sourceId), eq("missing")))
            .thenReturn(Optional.empty());

        GetContextStoreRecordToolCallback callback =
            new GetContextStoreRecordToolCallback(queryService, workspaceService);

        String result = callback.call(
            "{\"sourceId\":42,\"sourceRecordId\":\"missing\"}", toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found");
    }

    @Test
    void testCallRejectsCrossWorkspaceSource() throws Exception {
        long workspaceId = 1L;
        long sourceId = 42L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.fetchWorkspaceIdByContextStoreSourceId(sourceId))
            .thenReturn(Optional.of(999L));

        GetContextStoreRecordToolCallback callback = new GetContextStoreRecordToolCallback(
            mock(ContextStoreQueryService.class), workspaceService);

        String result = callback.call(
            "{\"sourceId\":42,\"sourceRecordId\":\"ext-1\"}", toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found in the current workspace");
    }
}
