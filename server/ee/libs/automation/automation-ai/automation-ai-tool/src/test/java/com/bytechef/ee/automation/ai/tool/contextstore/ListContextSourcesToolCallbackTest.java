/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import java.util.List;
import java.util.Map;
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
class ListContextSourcesToolCallbackTest {

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
        ListContextSourcesToolCallback callback = new ListContextSourcesToolCallback(
            mock(WorkspaceContextStoreSourceService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(ListContextSourcesToolCallback.TOOL_NAME);
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsSourcesWithInlineRecordShape() throws Exception {
        long workspaceId = 1L;

        ContextStoreSource source = new ContextStoreSource();

        source.setId(42L);
        source.setName("Airtable Customers");
        source.setEntityName("customers");
        source.setIdField("id");
        source.setIndexedFields(Map.of("email", "TEXT", "createdAt", "TIMESTAMP"));
        source.setStatus(ContextStoreSourceStatus.READY);
        source.setEnabled(true);
        source.setSourceComponentName("airtable");
        source.setSourceComponentVersion(1);
        source.setCadence("@hourly");

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.getAllSourcesByWorkspaceId(workspaceId)).thenReturn(List.of(source));

        ListContextSourcesToolCallback callback = new ListContextSourcesToolCallback(workspaceService);

        String result = callback.call("{}", toolContext(workspaceId));

        JsonNode array = jsonMapper.readTree(result);

        assertThat(array.isArray()).isTrue();
        assertThat(array).hasSize(1);

        JsonNode first = array.get(0);

        assertThat(first.get("sourceId")
            .asLong()).isEqualTo(42L);
        assertThat(first.get("status")
            .asText()).isEqualTo("READY");
        // Record-shape fields live inline on the source now — no nested entities array.
        assertThat(first.get("entityName")
            .asText()).isEqualTo("customers");
        assertThat(first.get("idField")
            .asText()).isEqualTo("id");
        assertThat(first.get("indexedFields")
            .has("email")).isTrue();
    }

    @Test
    void testCallReturnsEmptyArrayForEmptyWorkspace() throws Exception {
        long workspaceId = 1L;

        WorkspaceContextStoreSourceService workspaceService = mock(WorkspaceContextStoreSourceService.class);

        when(workspaceService.getAllSourcesByWorkspaceId(workspaceId)).thenReturn(List.of());

        ListContextSourcesToolCallback callback = new ListContextSourcesToolCallback(workspaceService);

        String result = callback.call("{}", toolContext(workspaceId));

        JsonNode array = jsonMapper.readTree(result);

        assertThat(array.isArray()).isTrue();
        assertThat(array).isEmpty();
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() throws Exception {
        ListContextSourcesToolCallback callback = new ListContextSourcesToolCallback(
            mock(WorkspaceContextStoreSourceService.class));

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
