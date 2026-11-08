/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OpenDataTableTabToolCallbackTest {

    private static final ToolContext TOOL_CONTEXT = new ToolContext(
        new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

    @Test
    void testToolDefinitionExposesOpenDataTableTabName() {
        ToolDefinition toolDefinition = new OpenDataTableTabToolCallback(null).getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("openDataTableTab");
        assertThat(toolDefinition.description()).isNotBlank();
        assertThat(toolDefinition.inputSchema()).contains("dataTableId", "name");
    }

    @Test
    void testCallEchoesArgumentsAndRecordsTheDataTableReference() {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(artifactRecorder);

        JsonNode resultJsonNode = readTree(callback.call("{\"dataTableId\":\"5\",\"name\":\"Leads\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
        assertThat(resultJsonNode.get("dataTableId")
            .asText()).isEqualTo("5");
        assertThat(resultJsonNode.get("name")
            .asText()).isEqualTo("Leads");

        verify(artifactRecorder).recordReference("thread-9", 42L, "DATA_TABLE_REFERENCED", "5", "Leads");
    }

    @Test
    void testCallWithoutAToolContextOpensWithoutRecording() {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(artifactRecorder);

        JsonNode resultJsonNode = readTree(callback.call("{\"dataTableId\":\"5\",\"name\":\"Leads\"}"));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testRecorderFailureStillOpensTheTab() {
        doThrow(new IllegalStateException("db down")).when(artifactRecorder)
            .recordReference(anyString(), any(), anyString(), anyString(), anyString());

        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(artifactRecorder);

        JsonNode resultJsonNode = readTree(callback.call("{\"dataTableId\":\"5\",\"name\":\"Leads\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
    }

    @Test
    void testCallRejectsAMissingDataTableId() {
        JsonNode resultJsonNode = readTree(
            new OpenDataTableTabToolCallback(artifactRecorder).call("{\"name\":\"Leads\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("error")
            .asText()).contains("dataTableId");

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void testCallRejectsABlankName() {
        JsonNode resultJsonNode = readTree(
            new OpenDataTableTabToolCallback(null).call("{\"dataTableId\":\"5\",\"name\":\" \"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("error")
            .asText()).contains("name");
    }

    @Test
    void testCallReportsInvalidInput() {
        JsonNode resultJsonNode = readTree(new OpenDataTableTabToolCallback(null).call("not json", TOOL_CONTEXT));

        assertThat(resultJsonNode.has("error")).isTrue();
    }

    private static JsonNode readTree(String json) {
        return JsonUtils.read(json, JsonNode.class);
    }
}
