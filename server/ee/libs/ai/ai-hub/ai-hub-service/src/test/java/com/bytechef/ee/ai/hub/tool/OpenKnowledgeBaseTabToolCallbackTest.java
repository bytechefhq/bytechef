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
class OpenKnowledgeBaseTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenKnowledgeBaseTabName() {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback(null);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openKnowledgeBaseTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("knowledgeBaseId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback(null);

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"name\":\"Product Docs\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("knowledgeBaseId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("Product Docs");
    }

    @Test
    void testCallRecordsKnowledgeBaseReferenceWhenRecorderPresent() {
        AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        callback.call("{\"knowledgeBaseId\":\"42\",\"name\":\"Product Docs\"}", toolContext);

        verify(artifactRecorder).recordReference(
            "thread-9", 42L, "KB_REFERENCED", "42", "Product Docs");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback(null);

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenKnowledgeBaseIdMissing() throws Exception {
        OpenKnowledgeBaseTabToolCallback callback = new OpenKnowledgeBaseTabToolCallback(null);

        String result = callback.call("{\"name\":\"Product Docs\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
