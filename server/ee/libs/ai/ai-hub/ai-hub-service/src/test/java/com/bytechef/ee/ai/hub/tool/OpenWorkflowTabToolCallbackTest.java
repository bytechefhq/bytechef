/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
class OpenWorkflowTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenWorkflowTabName() {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openWorkflowTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("workflowId");
        assertThat(definition.inputSchema()).contains("projectId");
        assertThat(definition.inputSchema()).contains("projectWorkflowId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        String result = callback.call(
            "{\"workflowId\":\"42\",\"projectId\":\"99\",\"projectWorkflowId\":7,\"name\":\"Leads Sync\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("workflowId")
            .asText()).isEqualTo("42");
        assertThat(node.get("projectId")
            .asText()).isEqualTo("99");
        assertThat(node.get("projectWorkflowId")
            .asLong()).isEqualTo(7L);
        assertThat(node.get("name")
            .asText()).isEqualTo("Leads Sync");
    }

    @Test
    void testCallRecordsWorkflowReferenceViaDedupPath() {
        AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        String input = "{\"workflowId\":\"wf-1\",\"projectId\":\"9\",\"projectWorkflowId\":55,\"name\":\"My Flow\"}";

        callback.call(input, toolContext);

        verify(artifactRecorder).recordWorkflowReference("thread-9", 42L, "wf-1", 9L, 55L, "My Flow");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenWorkflowIdMissing() throws Exception {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        String result = callback.call("{\"projectId\":\"99\",\"projectWorkflowId\":7,\"name\":\"Leads Sync\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenProjectIdMissing() throws Exception {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        String result = callback.call("{\"workflowId\":\"42\",\"projectWorkflowId\":7,\"name\":\"Leads Sync\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenProjectWorkflowIdMissing() throws Exception {
        OpenWorkflowTabToolCallback callback = new OpenWorkflowTabToolCallback(null);

        String result = callback.call("{\"workflowId\":\"42\",\"projectId\":\"99\",\"name\":\"Leads Sync\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
