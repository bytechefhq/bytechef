/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenFileTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenFileTabName() {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openFileTab");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("fileId");
        assertThat(definition.inputSchema()).contains("name");
    }

    @Test
    void testCallEchoesArgumentsAsOpenedPayload() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("{\"fileId\":\"42\",\"name\":\"spec.md\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("fileId")
            .asText()).isEqualTo("42");
        assertThat(node.get("name")
            .asText()).isEqualTo("spec.md");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenFileIdMissing() throws Exception {
        OpenFileTabToolCallback callback = new OpenFileTabToolCallback();

        String result = callback.call("{\"name\":\"spec.md\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
