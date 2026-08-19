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
class OpenDataTableTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenDataTableTabName() {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(null);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openDataTableTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("dataTableId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(null);

        String result = callback.call("{\"dataTableId\":\"42\",\"name\":\"Customer Records\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("dataTableId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("Customer Records");
    }

    @Test
    void testCallRecordsDataTableReferenceWhenRecorderPresent() {
        AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        callback.call("{\"dataTableId\":\"42\",\"name\":\"Customer Records\"}", toolContext);

        verify(artifactRecorder).recordReference(
            "thread-9", 42L, "DATA_TABLE_REFERENCED", "42", "Customer Records");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(null);

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenDataTableIdMissing() throws Exception {
        OpenDataTableTabToolCallback callback = new OpenDataTableTabToolCallback(null);

        String result = callback.call("{\"name\":\"Customer Records\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
