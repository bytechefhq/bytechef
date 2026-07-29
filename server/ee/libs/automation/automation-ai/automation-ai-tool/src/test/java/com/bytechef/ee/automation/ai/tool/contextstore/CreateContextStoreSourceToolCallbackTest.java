/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
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
class CreateContextStoreSourceToolCallbackTest {

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
        CreateContextStoreSourceToolCallback callback = new CreateContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(CreateContextStoreSourceToolCallback.TOOL_NAME);
        assertThat(definition.description()).isNotBlank();
        // Source absorbed entity — schema lists the inline record-shape fields, not a nested entities array.
        assertThat(definition.inputSchema()).contains("entityName");
        assertThat(definition.inputSchema()).contains("indexedFields");
    }

    @Test
    void testCallDelegatesToFacade() throws Exception {
        long workspaceId = 1L;

        ContextStoreSource created = new ContextStoreSource();

        created.setId(50L);
        created.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);
        created.setEnabled(true);
        created.setWorkflowId("wf-123");

        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);

        when(facade.create(eq(workspaceId), any(CreateContextStoreSourceInput.class))).thenReturn(created);

        CreateContextStoreSourceToolCallback callback = new CreateContextStoreSourceToolCallback(facade);

        String input =
            """
                {
                    "contextStoreId": 7,
                    "name": "Airtable Customers",
                    "entityName": "customers",
                    "idField": "id",
                    "indexedFields": {"email": "TEXT"},
                    "sourceComponentName": "airtable",
                    "sourceComponentVersion": 1,
                    "sourceClusterElementName": "read",
                    "cadence": "@hourly"
                }""";

        String result = callback.call(input, toolContext(workspaceId));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("id")
            .asLong()).isEqualTo(50L);
        assertThat(node.get("workflowId")
            .asText()).isEqualTo("wf-123");
        assertThat(node.get("status")
            .asText()).isEqualTo("BUILDING_PREVIEW");

        ArgumentCaptor<CreateContextStoreSourceInput> captor =
            ArgumentCaptor.forClass(CreateContextStoreSourceInput.class);

        verify(facade).create(eq(workspaceId), captor.capture());

        CreateContextStoreSourceInput captured = captor.getValue();

        assertThat(captured.name()).isEqualTo("Airtable Customers");
        assertThat(captured.entityName()).isEqualTo("customers");
        assertThat(captured.idField()).isEqualTo("id");
        assertThat(captured.sourceClusterElementName()).isEqualTo("read");
        assertThat(captured.indexedFields()
            .get("email")).isEqualTo("TEXT");
    }

    @Test
    void testCallOmitsClusterElementNameWhenNotProvided() throws Exception {
        long workspaceId = 1L;

        ContextStoreSource created = new ContextStoreSource();

        created.setId(51L);
        created.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);
        created.setEnabled(true);
        created.setWorkflowId("wf-124");

        WorkspaceContextStoreSourceFacade facade = mock(WorkspaceContextStoreSourceFacade.class);

        when(facade.create(eq(workspaceId), any(CreateContextStoreSourceInput.class))).thenReturn(created);

        CreateContextStoreSourceToolCallback callback = new CreateContextStoreSourceToolCallback(facade);

        String input =
            """
                {
                    "contextStoreId": 7,
                    "name": "Airtable Customers",
                    "entityName": "customers",
                    "idField": "id",
                    "indexedFields": {"email": "TEXT"},
                    "sourceComponentName": "airtable",
                    "sourceComponentVersion": 1,
                    "cadence": "@hourly"
                }""";

        callback.call(input, toolContext(workspaceId));

        ArgumentCaptor<CreateContextStoreSourceInput> captor =
            ArgumentCaptor.forClass(CreateContextStoreSourceInput.class);

        verify(facade).create(eq(workspaceId), captor.capture());

        CreateContextStoreSourceInput captured = captor.getValue();

        assertThat(captured.sourceClusterElementName()).isNull();
    }

    @Test
    void testCallReturnsErrorOnMissingFields() throws Exception {
        CreateContextStoreSourceToolCallback callback = new CreateContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class));

        String result = callback.call("{\"name\":\"x\"}", toolContext(1L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() throws Exception {
        CreateContextStoreSourceToolCallback callback = new CreateContextStoreSourceToolCallback(
            mock(WorkspaceContextStoreSourceFacade.class));

        String input =
            """
                {
                    "name": "x",
                    "entityName": "customers",
                    "idField": "id",
                    "indexedFields": {"email": "TEXT"},
                    "sourceComponentName": "airtable",
                    "sourceComponentVersion": 1,
                    "cadence": "@hourly"
                }""";

        String result = callback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }
}
